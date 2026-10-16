package com.tugongfang.editor

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AboutPageAndroidTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun homepageAboutOpensAndUsesInstalledVersion() {
        rule.onNodeWithText("关于", useUnmergedTree = true).performScrollTo().performClick()
        rule.onNodeWithText("图工坊", useUnmergedTree = true).assertIsDisplayed()
        val ctx = rule.activity
        val installedVersion = ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName
        rule.onNodeWithText("版本 $installedVersion", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("QQ群：软件小工具@交流群2", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
    }

    @Test fun qrDialogOpensAndCanClose() {
        rule.onNodeWithText("关于", useUnmergedTree = true).performScrollTo().performClick()
        rule.onNodeWithText("微信收款码", useUnmergedTree = true).performScrollTo().performClick()
        rule.onNodeWithText("完全自愿，不影响任何功能的正常使用", useUnmergedTree = true)
            .assertIsDisplayed()
        rule.onNodeWithText("关闭", useUnmergedTree = true).performClick()
        rule.onNodeWithText("支持开发", useUnmergedTree = true).assertIsDisplayed()
    }
}
