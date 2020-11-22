package com.peterlaurence.trekme.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.*
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.peterlaurence.trekme.MainActivity
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.TrekMeContext
import com.peterlaurence.trekme.core.appName
import com.peterlaurence.trekme.core.events.AppEventBus
import com.peterlaurence.trekme.core.events.StandardMessage
import com.peterlaurence.trekme.core.track.TrackStatCalculator
import com.peterlaurence.trekme.repositories.recording.GpxRecordRepository
import com.peterlaurence.trekme.repositories.recording.LiveRoutePoint
import com.peterlaurence.trekme.service.event.GpxFileWriteEvent
import com.peterlaurence.trekme.util.gpx.model.*
import com.peterlaurence.trekme.util.gpx.writeGpx
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * A [Started service](https://developer.android.com/guide/components/services.html#CreatingAService)
 * to perform Gpx recordings, even if the user is not interacting with the main application.
 * It is started in the foreground to avoid Android 8.0 (API lvl 26)
 * [background execution limits](https://developer.android.com/about/versions/oreo/background.html).
 * So when there is a Gpx recording, the user can always see it with the icon on the upper left
 * corner of the device.
 *
 * It uses the legacy location API in android.location, not the Google Location Services API, part
 * of Google Play Services. This is because we absolutely need to use only the [LocationManager.GPS_PROVIDER].
 * The fused provider don't give us the hand on that.
 *
 * @author P.Laurence on 17/12/17 -- converted to Kotlin on 20/04/19
 */
@AndroidEntryPoint
class GpxRecordService : Service() {

    @Inject
    lateinit var trekMeContext: TrekMeContext

    @Inject
    lateinit var repository: GpxRecordRepository

    @Inject
    lateinit var eventBus: AppEventBus

    private var serviceLooper: Looper? = null
    private var serviceHandler: Handler? = null
    private var locationManager: LocationManager? = null
    private var locationListener: LocationListener? = null
    private var locationCounter: Long = 0

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var trackStatCalculator: TrackStatCalculator? = null

    override fun onCreate() {
        super.onCreate()

        /* Start up the thread for background execution of tasks withing the service.  Note that we
         * create a separate thread because the service normally runs in the process's main thread,
         * which we don't want to block.
         * We also make it low priority so CPU-intensive work will not disrupt our UI.
         */
        val thread = HandlerThread(THREAD_NAME, Thread.MIN_PRIORITY)
        thread.start()

        /* Get the HandlerThread's Looper and use it for our Handler */
        val looper = thread.looper
        serviceLooper = looper
        serviceHandler = Handler(looper)

        /* Prepare the stat calculator */
        trackStatCalculator = TrackStatCalculator()

        locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                locationCounter++
                /* Drop points that aren't precise enough */
                if (location.accuracy > 15) {
                    return
                }

                /* Drop the first 3 points, so the GPS stabilizes */
                if (locationCounter <= 3) {
                    return
                }

                val altitude = if (location.altitude != 0.0) location.altitude else null
                val trackPoint = TrackPoint(location.latitude,
                        location.longitude, altitude, location.time, "")
                repository.addTrackPoint(trackPoint)
                trackStatCalculator?.addTrackPoint(trackPoint)
                trackStatCalculator?.getStatistics()?.also { stats ->
                    repository.postTrackStatistics(stats)
                }
            }

            override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}

            override fun onProviderEnabled(provider: String) {}

            override fun onProviderDisabled(provider: String) {}
        }

        startLocationUpdates()

        /* Register a subscriber coroutine to the stop signal */
        scope.launch {
            repository.stopRecordingSignal.collect {
                createGpx()
            }
        }
    }

    /**
     * When we stop recording the location events, create a [Gpx] object for further
     * serialization.
     * Whatever the outcome of this process, a [GpxFileWriteEvent] is emitted in the
     * [THREAD_NAME] thread.
     */
    private fun createGpx() {
        serviceHandler?.post {
            val trkSegList = ArrayList<TrackSegment>()
            val trackPoints = repository.liveRouteFlow.replayCache.mapNotNull {
                if (it is LiveRoutePoint) it.pt else null
            }
            trkSegList.add(TrackSegment(trackPoints))

            /* Name the track using the current date */
            val date = Date()
            val dateFormat = SimpleDateFormat("dd-MM-yyyy_HH'h'mm-ss's'", Locale.ENGLISH)
            val trackName = "track-" + dateFormat.format(date)

            val track = Track(trkSegList, trackName)
            track.statistics = trackStatCalculator?.getStatistics()

            /* Make the metadata */
            val metadata = Metadata(trackName, date.time, trackStatCalculator?.getBounds())

            val trkList = ArrayList<Track>()
            trkList.add(track)

            val wayPoints = ArrayList<TrackPoint>()

            val gpx = Gpx(metadata, trkList, wayPoints, appName, GPX_VERSION)
            try {
                val gpxFileName = "$trackName.gpx"
                val recordingsDir = trekMeContext.recordingsDir
                        ?: error("Recordings dir is mandatory")
                val gpxFile = File(recordingsDir, gpxFileName)
                val fos = FileOutputStream(gpxFile)
                writeGpx(gpx, fos)
                repository.postGpxFileWriteEvent(GpxFileWriteEvent(gpxFile, gpx))
            } catch (e: Exception) {
                eventBus.postMessage(StandardMessage(getString(R.string.service_gpx_error)))
            } finally {
                stop()
            }
        }
    }

    /**
     * Called when the service is started.
     */
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        val icon = BitmapFactory.decodeResource(resources,
                R.mipmap.ic_launcher)

        val notificationBuilder = NotificationCompat.Builder(applicationContext, NOTIFICATION_ID)
                .setContentTitle(getText(R.string.app_name))
                .setContentText(getText(R.string.service_gpx_record_action))
                .setSmallIcon(R.drawable.ic_my_location_black_24dp)
                .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                .setContentIntent(pendingIntent)
                .setOngoing(true)

        if (Build.VERSION.SDK_INT >= 26) {
            /* This is only needed on Devices on Android O and above */
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val mChannel = NotificationChannel(NOTIFICATION_ID, getText(R.string.service_gpx_record_name), NotificationManager.IMPORTANCE_DEFAULT)
            mChannel.enableLights(true)
            mChannel.lightColor = Color.MAGENTA
            notificationManager.createNotificationChannel(mChannel)
            notificationBuilder.setChannelId(NOTIFICATION_ID)
        }
        val notification = notificationBuilder.build()

        startForeground(SERVICE_ID, notification)

        repository.setServiceState(true)

        return START_NOT_STICKY
    }

    /**
     * Stop the service and send the status.
     */
    private fun stop() {
        repository.resetLiveRoute()
        repository.postTrackStatistics(null)
        repository.setServiceState(false)
        stopSelf()
    }

    override fun onBind(intent: Intent): IBinder? {
        // We don't provide binding, so return null
        return null
    }

    override fun onDestroy() {
        stopLocationUpdates()
        serviceLooper?.quitSafely()
        scope.cancel()
    }

    /**
     * Only use locations from the GPS.
     */
    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        val locationListener = locationListener ?: return

        runCatching {
            locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 2f, locationListener, serviceLooper)
        }.onFailure {
            eventBus.postMessage(StandardMessage(getString(R.string.service_gpx_location_error)))
        }
    }

    private fun stopLocationUpdates() {
        locationListener?.also { locationManager?.removeUpdates(it) }
    }

    companion object {
        private const val GPX_VERSION = "1.1"
        private const val NOTIFICATION_ID = "peterlaurence.GpxRecordService"
        private const val SERVICE_ID = 126585
    }
}

private const val THREAD_NAME = "GpxRecordServiceThread"
