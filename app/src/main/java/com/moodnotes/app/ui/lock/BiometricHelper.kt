package com.moodnotes.app.ui.lock

import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import com.moodnotes.app.R

/**
 * 生物识别封装（平台 API，不依赖 androidx.biometric / FragmentActivity）：
 * 检查设备能力并弹出系统 BiometricPrompt。
 */
object BiometricHelper {

    private const val WEAK = BiometricManager.Authenticators.BIOMETRIC_WEAK

    fun canAuthenticate(context: Context): Boolean {
        val manager = context.getSystemService(BiometricManager::class.java) ?: return false
        return manager.canAuthenticate(WEAK) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticate(activity: android.app.Activity, onResult: (Boolean) -> Unit) {
        if (!canAuthenticate(activity)) {
            onResult(false)
            return
        }
        val prompt = BiometricPrompt.Builder(activity)
            .setTitle(activity.getString(R.string.biometric_prompt_title))
            .setSubtitle(activity.getString(R.string.biometric_prompt_subtitle))
            // 附带 DEVICE_CREDENTIAL 时系统自带取消入口，无需 negative button
            .setAllowedAuthenticators(WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()
        val cancellation = CancellationSignal()
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onResult(true)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onResult(false)
            }
        }
        runCatching {
            prompt.authenticate(cancellation, activity.mainExecutor, callback)
        }.onFailure { onResult(false) }
    }
}

