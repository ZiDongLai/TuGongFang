package com.tugongfang.editor.model.collage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class CollageTemplateDev41Test {
    @Test fun exactTemplateCountByPhotoCount() {
        assertEquals(87, CollageTemplate.entries.size)
        for (count in 1..12) {
            assertEquals(if (count == 1) 1 else if (count == 2) 6 else 8,
                templatesForPhotoCount(count).size)
            assertTrue(templatesForPhotoCount(count).all { it.slotCount == count })
        }
        assertEquals(54,CollageTemplate.entries.count { it.layoutStyle != CollageLayoutStyle.Legacy })
    }

    @Test fun oldIdentifiersPreserved() {
        val oldNames = listOf("TwoColumns","TwoRows","ThreeColumns","HeroThree",
            "FourGrid","SixGrid","OnePhoto","ThreeRows","FourColumns","FourHero",
            "N5Grid","N5Rows","N5Hero","SixRows","SixHero") +
            (7..12).flatMap { listOf("N${it}Grid","N${it}Rows","N${it}Hero") }
        assertEquals(33,oldNames.size)
        assertEquals(oldNames,CollageTemplate.entries.take(33).map { it.name })
    }

    @Test fun allLayoutsFullyCoverWithoutOverlaps() {
        for (template in CollageTemplate.entries) {
            val rects = CollageProject(template=template,gapRatio=0f).rects()
            assertEquals(template.name,template.slotCount,rects.size)
            for (rect in rects) {
                assertTrue(template.name,rect.left>=0f && rect.top>=0f && rect.right<=1.00001f && rect.bottom<=1.00001f)
                assertTrue(template.name,rect.width>0f && rect.height>0f)
            }
            val area = rects.sumOf { (it.width*it.height).toDouble() }
            assertTrue("${template.name}: area $area",abs(area-1.0)<0.00002)
            for (x in 0 until 25) for (y in 0 until 25) {
                val px=(x+.5f)/25f
                val py=(y+.5f)/25f
                assertEquals("${template.name} [$x,$y]",1,
                    rects.count { px>=it.left && px<it.right && py>=it.top && py<it.bottom })
            }
        }
    }

    @Test fun switchingStylesPreservesPerImageTransformAndCount() {
        for (count in 2..12) {
            val base = CollageProject(template = defaultTemplateForPhotoCount(count),
                slots=(0 until count).map { n ->
                    CollageSlot(imageId="photo$n",zoom=1f+n*.1f,panX=n*2f,rotation=n*3f,
                        fitMode=if (n%2==0) CollageFitMode.Fill else CollageFitMode.Fit)
                })
            for (template in templatesForPhotoCount(count)) {
                val changed = base.changeTemplate(template)
                assertEquals(template,changed.template)
                assertEquals(base.slots,changed.slots)
                assertEquals(count,changed.rects().size)
            }
        }
    }

    @Test fun savedTemplateNamesRoundTrip() {
        CollageTemplate.entries.forEach {
            assertEquals(it,enumValueOf<CollageTemplate>(it.name))
        }
    }
}
