package com.peterlaurence.trekme.core.providers.stream

import com.peterlaurence.trekme.core.map.OutOfBounds
import com.peterlaurence.trekme.core.map.TileResult
import com.peterlaurence.trekme.core.map.TileStreamProvider
import com.peterlaurence.trekme.core.providers.bitmap.TileStreamProviderHttp
import com.peterlaurence.trekme.core.providers.bitmap.TileStreamProviderRetry
import com.peterlaurence.trekme.core.providers.urltilebuilder.UrlTileBuilder

/**
 * A specific [TileStreamProvider] for Japan GSI Map.
 *
 * @author Kuromimi on 1/10/2021
 */
class TileStreamProviderJapan(urlTileBuilder: UrlTileBuilder) : TileStreamProvider {
    private val base = TileStreamProviderRetry(TileStreamProviderHttp(urlTileBuilder))

    override fun getTileStream(row: Int, col: Int, zoomLvl: Int): TileResult {
        /* Safeguard */
        if (zoomLvl > 18) return OutOfBounds

        return base.getTileStream(row, col, zoomLvl)
    }
}