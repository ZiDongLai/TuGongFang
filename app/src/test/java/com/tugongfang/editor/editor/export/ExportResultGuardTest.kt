package com.tugongfang.editor.editor.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CancellationException

class ExportResultGuardTest {
    @Test fun successfulRenderIsSuccessfulResult() {
        assertEquals(42, ExportResultGuard.run { 42 }.getOrThrow())
    }

    @Test fun actualRenderErrorIsReportedAsFailure() {
        val result = ExportResultGuard.run<Int> { throw IllegalStateException("corrupt image") }
        assertTrue(result.isFailure)
        assertEquals("corrupt image", result.exceptionOrNull()?.message)
    }

    @Test fun coroutineCancellationMustPropagate() {
        var caught = false
        try {
            ExportResultGuard.run<Int> { throw CancellationException("left editor") }
        } catch (expected: CancellationException) {
            caught = true
            assertEquals("left editor", expected.message)
        }
        assertTrue(caught)
    }

    @Test fun imageNameHasExtensionAndUniqueSuffix() {
        val stamp = "20261016_123456_007"
        val jpg = ExportFileRules.uniqueOutputName("图工坊", ExportImageFormat.JPG, stamp, "a1b2c3d4")
        val png = ExportFileRules.uniqueOutputName("图工坊", ExportImageFormat.PNG, stamp, "f0e1d2c3")
        assertEquals("图工坊_20261016_123456_007_a1b2c3d4.jpg", jpg)
        assertTrue(png.endsWith(".png"))
        assertFalse(png == jpg)
        assertFalse(jpg == ExportFileRules.uniqueOutputName("图工坊", ExportImageFormat.JPG, stamp, "00000000"))
    }

    @Test fun badNamesCannotInjectUnsafeExtensionOrToken() {
        val filename = ExportFileRules.uniqueOutputName("../../测试:/项目", ExportImageFormat.JPG,
            "20261016_123456_123", "11223344")
        assertFalse(filename.contains('/'))
        assertFalse(filename.contains('\\'))
        assertTrue(filename.endsWith(".jpg"))
    }
}
