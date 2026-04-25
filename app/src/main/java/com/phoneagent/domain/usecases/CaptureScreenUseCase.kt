package com.phoneagent.domain.usecases

import android.graphics.Bitmap
import com.phoneagent.service.ScreenCaptureService
import kotlinx.coroutines.delay
import javax.inject.Inject

class CaptureScreenUseCase @Inject constructor() {
    
    suspend fun execute(): String? {
        if (!ScreenCaptureService.isRunning()) return null
        
        // Wait for UI to settle
        delay(300)
        return ScreenCaptureService.instance?.captureScreenAsBase64(70)
    }
    
    suspend fun captureAsBitmap(): Bitmap? {
        if (!ScreenCaptureService.isRunning()) return null
        delay(300)
        return ScreenCaptureService.instance?.captureScreen()
    }
}
