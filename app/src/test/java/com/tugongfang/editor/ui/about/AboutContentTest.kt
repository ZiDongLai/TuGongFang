package com.tugongfang.editor.ui.about

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AboutContentTest {
    @Test fun productAndFeaturesArePresent() {
        assertEquals("图工坊", AboutContent.APP_NAME)
        assertTrue(AboutContent.FEATURES.any { it.contains("拼图") })
        assertTrue(AboutContent.FEATURES.any { it.contains("马赛克") })
        assertTrue(AboutContent.ROADMAP.isNotBlank())
    }

    @Test fun contactsMatchUserProvidedData() {
        assertEquals("软件小工具@交流群2", AboutContent.QQ_GROUP_NAME)
        assertEquals("748934113", AboutContent.QQ_GROUP_NUMBER)
        assertFalse(AboutContent.WECHAT_GROUP.contains("https://"))
    }

    @Test fun unverifiedLicenseAndGithubAreNotInvented() {
        assertEquals("尚未声明（待确认）", AboutContent.LICENSE_NAME)
        assertTrue(AboutContent.LICENSE_DETAILS.contains("未附带 LICENSE"))
        assertTrue(AboutContent.REPOSITORY_NOTE.contains("待确认"))
    }

    @Test fun supportIsExplicitlyVoluntaryAndUnrestricted() {
        assertTrue(AboutContent.SUPPORT_NOTE.contains("完全自愿"))
        assertTrue(AboutContent.SUPPORT_NOTE.contains("不影响任何功能"))
        assertTrue(AboutContent.FEEDBACK_HINT.contains("隐私"))
    }
}
