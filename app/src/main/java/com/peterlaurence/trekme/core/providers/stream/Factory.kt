package com.peterlaurence.trekme.core.providers.stream

import com.peterlaurence.trekme.core.map.TileStreamProvider
import com.peterlaurence.trekme.core.mapsource.*
import com.peterlaurence.trekme.core.providers.urltilebuilder.*

/**
 * This is the unique place of the app (excluding tests), where we create a [TileStreamProvider]
 * from a [WmtsSource].
 */
fun createTileStreamProvider(wmtsSource: WmtsSource, mapSourceData: MapSourceData): TileStreamProvider {
    return when (wmtsSource) {
        WmtsSource.IGN -> {
            val ignSourceData = mapSourceData as? IgnSourceData
                    ?: throw Exception("Missing API for IGN source")
            val urlTileBuilder = UrlTileBuilderIgn(ignSourceData.api, ignSourceData.layer.wmtsName)
            TileStreamProviderIgn(urlTileBuilder, ignSourceData.layer)
        }
        WmtsSource.USGS -> {
            val urlTileBuilder = UrlTileBuilderUSGS()
            TileStreamProviderUSGS(urlTileBuilder)
        }
        WmtsSource.OPEN_STREET_MAP -> {
            val osmSourceData = mapSourceData as OsmSourceData
            val urlTileBuilder = UrlTileBuilderOSM(osmSourceData.layer.id)
            TileStreamProviderOSM(urlTileBuilder)
        }
        WmtsSource.IGN_SPAIN -> {
            val urlTileBuilder = UrlTileBuilderIgnSpain()
            TileStreamProviderIgnSpain(urlTileBuilder)
        }
        WmtsSource.SWISS_TOPO -> {
            val urlTileBuilder = UrlTileBuilderSwiss()
            TileStreamProviderSwiss(urlTileBuilder)
        }
        WmtsSource.ORDNANCE_SURVEY -> {
            val ordnanceSurveyData = mapSourceData as? OrdnanceSurveyData
                    ?: throw Exception("Missing API for Ordnance Survey source")
            val urlTileBuilder = UrlTileBuilderOrdnanceSurvey(ordnanceSurveyData.api)
            TileStreamProviderOrdnanceSurvey(urlTileBuilder)
        }
        WmtsSource.JAPAN_GSI -> {
            val urlTileBuilder = UrlTileBuilderJapan()
            TileStreamProviderJapan(urlTileBuilder)
        }
    }
}
