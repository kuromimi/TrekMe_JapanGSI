package com.peterlaurence.trekme.core.mapsource

import android.util.Log
import com.google.gson.GsonBuilder
import com.peterlaurence.trekme.core.TrekMeContext
import com.peterlaurence.trekme.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.PrintWriter
import javax.inject.Inject


class MapSourceCredentials @Inject constructor(trekMeContext: TrekMeContext) {
    val supportedMapSource = MapSource.values()
    val gson = GsonBuilder().serializeNulls().setPrettyPrinting().create()
    private val TAG = javaClass.toString()
    private val configFile = File(trekMeContext.credentialsDir, "ign.json")

    lateinit var credentials: Credentials


    fun getIGNCredentials(): IGNCredentials? {
        val jsonString = try {
            if (configFile.exists()) {
                FileUtils.getStringFromFile(configFile)
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
        credentials = gson.fromJson(jsonString, Credentials::class.java) ?: Credentials(null)
        return credentials.ignCredentials
    }

    suspend fun saveIGNCredentials(ignCredentials: IGNCredentials): Boolean = withContext(Dispatchers.IO) {
        credentials.ignCredentials = ignCredentials

        val jsonString = gson.toJson(credentials)

        try {
            val writer = PrintWriter(configFile)
            writer.print(jsonString)
            writer.close()
            true
        } catch (e: IOException) {
            Log.e(TAG, "Error while saving the IGN credentials")
            Log.e(TAG, e.message, e)
            false
        }
    }
}

data class Credentials(var ignCredentials: IGNCredentials?)

data class IGNCredentials(val user: String?, val pwd: String?, val api: String?)

