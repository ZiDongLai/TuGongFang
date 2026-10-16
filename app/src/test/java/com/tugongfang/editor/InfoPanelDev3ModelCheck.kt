package com.tugongfang.editor

import com.tugongfang.editor.model.layer.InfoPanelLayer
import com.tugongfang.editor.model.layer.InfoTemplateType

/** Standalone Kotlin smoke check: run from kotlinc even without Android SDK. */
object InfoPanelDev3ModelCheck {
    @JvmStatic fun main(args: Array<String>) {
        val old = InfoPanelLayer(templateType=InfoTemplateType.MovieContinuity)
        check(!old.overlayMode) { "Legacy card must remain footer by default" }
        val overlay = old.copy(overlayMode=true,widthRatio=.80f,heightRatio=.35f,
            offsetXRatio=.03f,offsetYRatio=.23f,rotation=18f,darkBackground=true,
            primary="角色A",secondary="A03",note="现场记录")
        check(overlay.overlayMode && overlay.widthRatio == .80f)
        check(overlay.darkBackground && overlay.positionTop==old.positionTop)
        check(overlay.note=="现场记录" && overlay.primary=="角色A")
        val copied=overlay.copy(id="new-object-id")
        check(copied.id != overlay.id && copied.widthRatio==overlay.widthRatio)
        val top=overlay.copy(overlayMode=false,positionTop=true,rotation=0f)
        check(top.positionTop && !top.overlayMode)
        println("PASS: 5 DEV3 model/compatibility checks")
    }
}
