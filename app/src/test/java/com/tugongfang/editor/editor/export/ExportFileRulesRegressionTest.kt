package com.tugongfang.editor.editor.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** DEV20 regression for export resolution, naming and JPEG quality bounds. */
class ExportFileRulesRegressionTest {
    @Test fun largeImagesAreDownscaledWithoutAspectDistortion() {
        listOf(720 to 1280, 2048 to 2048, 4096 to 3072, 3000 to 6000).forEach { (w,h) ->
            listOf(1200,2048,4096).forEach { maxEdge ->
                val (outW,outH) = ExportFileRules.boundedDimensions(w,h,maxEdge)
                assertTrue(outW>0 && outH>0)
                assertTrue(maxOf(outW,outH)<=maxEdge)
                assertTrue(kotlin.math.abs(outW.toDouble()/outH - w.toDouble()/h)<.002)
            }
        }
    }

    @Test fun defaultResolutionDoesNotEnlargeOrShrink() {
        assertEquals(1200 to 1920,ExportFileRules.boundedDimensions(1200,1920,0))
        assertEquals(720 to 1280,ExportFileRules.boundedDimensions(720,1280,2048))
    }

    @Test fun fileNameAndQualityAreSafe() {
        assertEquals("name_bad_xx",ExportFileRules.safeNamePrefix("name:bad/xx"))
        assertEquals("图工坊",ExportFileRules.safeNamePrefix(" /: "))
        assertEquals(60,ExportFileRules.boundedQuality(0))
        assertEquals(100,ExportFileRules.boundedQuality(110))
    }
}
