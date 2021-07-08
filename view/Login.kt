package com.athanasioua.battleship.model.newp.view

import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.PromptInfo
import androidx.core.content.ContextCompat
import com.athanasioua.battleship.R
import com.athanasioua.battleship.model.newp.Utils
import java.util.*
import java.util.concurrent.Executor

class Login : AppCompatActivity(){
    private var executor: Executor? = null
    private var biometricPrompt: BiometricPrompt? = null
    private var promptInfo: PromptInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        if(checkDeviceForBiometricsSupport())
            showBiometricDialog()

        //TODO if no account --> Register
    }

    fun checkDeviceForBiometricsSupport(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val biometricManager = BiometricManager.from(Objects.requireNonNull(applicationContext))
            return when (biometricManager.canAuthenticate()) {
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE, BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> false
                BiometricManager.BIOMETRIC_SUCCESS -> true
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                    //TODO show error
                    false
                }
                else -> false
            }
        }
        return false
    }

    fun showBiometricDialog() {
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this,
                executor!!, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int,
                                               errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                /* Toast.makeText(getApplicationContext(),
                    "Authentication error: " + errString, Toast.LENGTH_SHORT)
                    .show();*/
            }

            override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Utils.isLoggedIn = true
                finish()
                /*  Toast.makeText(getApplicationContext(),
                    "Authentication succeeded!", Toast.LENGTH_SHORT).show();*/
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                /* Toast.makeText(getApplicationContext(), "Authentication failed",
                    Toast.LENGTH_SHORT)
                    .show();*/
            }
        })
        promptInfo = PromptInfo.Builder()
                .setTitle("Biometric login for my app")
                .setSubtitle("Log in using your biometric credential")
                .setNegativeButtonText("Use account password")
                //.setAllowedAuthenticators(BIOMETRIC_STRONG )
                .build()


      biometricPrompt!!.authenticate(promptInfo!!)

    }

}