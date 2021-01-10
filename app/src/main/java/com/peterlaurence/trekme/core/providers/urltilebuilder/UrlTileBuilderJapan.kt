package com.peterlaurence.trekme.core.providers.urltilebuilder

class UrlTileBuilderJapan : UrlTileBuilder {
    override fun build(level: Int, row: Int, col: Int): String {
        return "http://cyberjapandata.gsi.go.jp/xyz/std/$level/$col/$row.png"
    }
}