package com.tugongfang.editor.model.layer

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.hypot
import kotlin.random.Random

class DoodleEraseSpatialIndexTest {
    private fun brute(path: List<DoodleErasePoint>, p: DoodleErasePoint, r: Float): Boolean {
        if (path.size == 1) return hypot(p.x-path[0].x,p.y-path[0].y) <= r
        return path.zipWithNext().any { (a,b) ->
            val dx=b.x-a.x; val dy=b.y-a.y
            val ll=dx*dx+dy*dy
            val t=if(ll < .0001f) 0f else (((p.x-a.x)*dx+(p.y-a.y)*dy)/ll).coerceIn(0f,1f)
            hypot(p.x-(a.x+t*dx),p.y-(a.y+t*dy)) <= r
        }
    }
    @Test fun indexedHitsMatchSequentialGeometry() {
        val rng=Random(3902)
        repeat(40) {
            val path=List(rng.nextInt(1,80)) { DoodleErasePoint(rng.nextFloat()*1000, rng.nextFloat()*1800) }
            val r=4f+rng.nextFloat()*40
            val index=DoodleEraseSpatialIndex(path,r+2)
            repeat(80) {
                val point=DoodleErasePoint(rng.nextFloat()*1000,rng.nextFloat()*1800)
                assertEquals(brute(path,point,r),index.touches(point,r))
            }
        }
    }
    @Test fun degenerateAndFarPathsAreHandled() {
        val p=listOf(DoodleErasePoint(10f,20f),DoodleErasePoint(10f,20f))
        val index=DoodleEraseSpatialIndex(p,12f)
        assertEquals(true,index.touches(DoodleErasePoint(11f,21f),3f))
        assertEquals(false,index.touches(DoodleErasePoint(1000f,1000f),3f))
    }
}
