package com.peterlaurence.trekme.ui.mapcreate

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.mapsource.MapSource
import com.peterlaurence.trekme.core.mapsource.MapSourceBundle
import com.peterlaurence.trekme.core.mapsource.MapSourceCredentials
import com.peterlaurence.trekme.databinding.FragmentMapCreateBinding
import com.peterlaurence.trekme.ui.mapcreate.MapSourceAdapter.MapSourceSelectionListener
import com.peterlaurence.trekme.util.isEnglish
import com.peterlaurence.trekme.util.isFrench
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * This fragment is used for displaying available WMTS map sources.
 *
 * @author peterLaurence on 08/04/18
 */
@AndroidEntryPoint
class MapCreateFragment : Fragment(), MapSourceSelectionListener {
    @Inject lateinit var mapSourceCredentials: MapSourceCredentials
    private lateinit var mapSourceSet: Array<MapSource>

    private var _binding: FragmentMapCreateBinding? = null
    private val binding get() = _binding!!

    private var selectedMapSource: MapSource? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        /**
         * When the app is in english, put [MapSource.USGS] in front.
         * When in french, put [MapSource.IGN] in front.
         */
        mapSourceSet = mapSourceCredentials.supportedMapSource.sortedBy {
            if (isEnglish(context) && it == MapSource.USGS) {
                -1
            } else if (isFrench(context) && it == MapSource.IGN) {
                -1
            } else {
                0
            }
        }.toTypedArray()
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMapCreateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.nextButton.setOnClickListener {
            val mapSource = selectedMapSource ?: return@setOnClickListener
            when (mapSource) {
                MapSource.IGN -> {
                    /* Check whether credentials are already set or not */
                    val ignCredentials = mapSourceCredentials.getIGNCredentials()
                    if (ignCredentials == null) {
                        showIgnCredentialsFragment()
                    } else {
                        showWmtsViewFragment(mapSource)
                    }
                }
                else -> showWmtsViewFragment(mapSource)
            }
        }

        binding.settingsButton.setOnClickListener {
            val mapSource = selectedMapSource ?: return@setOnClickListener
            if (mapSource == MapSource.IGN) {
                showIgnCredentialsFragment()
            }
        }

        val viewManager = LinearLayoutManager(context)
        val viewAdapter = MapSourceAdapter(
                mapSourceSet, this, context?.getColor(R.color.colorAccent)
                ?: Color.BLUE,
                context?.getColor(R.color.colorPrimaryTextWhite)
                        ?: Color.WHITE, context?.getColor(R.color.colorPrimaryTextBlack)
                ?: Color.BLACK
        )

        /* Item decoration : divider */
        val dividerItemDecoration = DividerItemDecoration(
                context,
                DividerItemDecoration.VERTICAL
        )
        val divider = this.context?.getDrawable(R.drawable.divider)
        if (divider != null) {
            dividerItemDecoration.setDrawable(divider)
        }

        binding.recyclerviewMapCreate.apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
            addItemDecoration(dividerItemDecoration)
        }
    }

    private fun showWmtsViewFragment(mapSource: MapSource) {
        val bundle = MapSourceBundle(mapSource)
        val action = MapCreateFragmentDirections.actionMapCreateFragmentToGoogleMapWmtsViewFragment(bundle)
        findNavController().navigate(action)
    }

    private fun showIgnCredentialsFragment() {
        val action = MapCreateFragmentDirections.actionMapCreateFragmentToIgnCredentialsFragment()
        findNavController().navigate(action)
    }

    /**
     * For instance, settings are only relevant for [MapSource.IGN] provider.
     */
    private fun setButtonsAvailability(m: MapSource) {
        binding.nextButton.visibility = View.VISIBLE

        binding.settingsButton.visibility = if (m == MapSource.IGN) {
            View.VISIBLE
        } else {
            View.INVISIBLE
        }
    }

    override fun onMapSourceSelected(m: MapSource) {
        selectedMapSource = m
        setButtonsAvailability(m)
    }
}