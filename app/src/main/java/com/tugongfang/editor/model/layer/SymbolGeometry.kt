package com.tugongfang.editor.model.layer

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Common normalized [0,1] geometry rendered identically in Compose and JPG export. */
data class SymbolVertex(val x: Float, val y: Float)
data class SymbolLine(val vertices: List<SymbolVertex>, val closed: Boolean = false)
data class SymbolRing(val cx: Float, val cy: Float, val radius: Float)
data class SymbolGlyph(val strokes: List<SymbolLine>, val rings: List<SymbolRing> = emptyList())

object SymbolGeometry {
    fun glyph(kind: SymbolKind): SymbolGlyph {
        fun p(x: Float, y: Float) = SymbolVertex(x, y)
        fun line(vararg points: SymbolVertex, close: Boolean = false) = SymbolLine(points.toList(), close)
        return when (kind) {
            SymbolKind.Check -> SymbolGlyph(listOf(line(p(.13f,.51f),p(.39f,.76f),p(.87f,.22f))))
            SymbolKind.Cross -> SymbolGlyph(listOf(line(p(.19f,.19f),p(.81f,.81f)),line(p(.81f,.19f),p(.19f,.81f))))
            SymbolKind.Star -> {
                val pts = (0 until 10).map { i ->
                    val r = if (i % 2 == 0) .40f else .19f
                    val angle = -PI/2.0 + i * PI/5.0
                    p(.5f + r*cos(angle).toFloat(), .5f + r*sin(angle).toFloat())
                }
                SymbolGlyph(listOf(SymbolLine(pts,closed=true)))
            }
            SymbolKind.Heart -> SymbolGlyph(listOf(line(
                p(.50f,.87f),p(.33f,.72f),p(.17f,.56f),p(.13f,.41f),p(.15f,.28f),
                p(.23f,.19f),p(.33f,.16f),p(.43f,.19f),p(.50f,.28f),p(.57f,.19f),
                p(.67f,.16f),p(.77f,.19f),p(.85f,.28f),p(.87f,.41f),p(.83f,.56f),
                p(.67f,.72f),p(.50f,.87f),close=true)))
            SymbolKind.Alert -> SymbolGlyph(
                listOf(line(p(.50f,.31f),p(.50f,.56f))),
                listOf(SymbolRing(.5f,.5f,.40f),SymbolRing(.5f,.72f,.02f))
            )
            SymbolKind.Pin -> SymbolGlyph(listOf(line(
                p(.50f,.92f),p(.29f,.62f),p(.23f,.48f),p(.22f,.37f),p(.27f,.23f),
                p(.37f,.14f),p(.50f,.11f),p(.63f,.14f),p(.73f,.23f),p(.78f,.37f),
                p(.77f,.48f),p(.71f,.62f),p(.50f,.92f),close=true)), listOf(SymbolRing(.5f,.38f,.115f)))
            SymbolKind.Flag -> SymbolGlyph(listOf(
                line(p(.23f,.11f),p(.23f,.89f)),
                line(p(.23f,.15f),p(.80f,.15f),p(.65f,.36f),p(.80f,.57f),p(.23f,.57f))
            ))
            SymbolKind.Info -> SymbolGlyph(listOf(line(p(.50f,.43f),p(.50f,.73f))),
                listOf(SymbolRing(.5f,.5f,.40f),SymbolRing(.5f,.27f,.022f)))
        }
    }
}
