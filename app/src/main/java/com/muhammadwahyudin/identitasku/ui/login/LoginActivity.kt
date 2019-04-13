package com.muhammadwahyudin.identitasku.ui.login

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import com.muhammadwahyudin.identitasku.BuildConfig
import com.muhammadwahyudin.identitasku.R
import com.muhammadwahyudin.identitasku.biometric.BiometricCallback
import com.muhammadwahyudin.identitasku.biometric.BiometricManager
import com.muhammadwahyudin.identitasku.data.Constants
import com.muhammadwahyudin.identitasku.data.db.AppDatabase
import com.muhammadwahyudin.identitasku.ui._base.BaseActivity
import com.muhammadwahyudin.identitasku.ui._views.RegisterSuccessDialog
import com.muhammadwahyudin.identitasku.ui.home.HomeActivity
import com.muhammadwahyudin.identitasku.utils.BiometricUtils
import com.orhanobut.hawk.Hawk
import kotlinx.android.synthetic.main.activity_login.*
import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.Appcompat
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance


/**
 * A register & login screen that offers login via password/fingerprint.
 */
class LoginActivity : BaseActivity(), KodeinAware {
    override val kodein by closestKodein()
    private val appDatabase by instance<AppDatabase>()

    private var isRegistered = false
    private var wrongPasswordInputAttempt = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        isRegistered = Hawk.contains(Constants.SP_PASSWORD)

        if (BuildConfig.DEBUG)
            btn_login.setOnLongClickListener {
                val registerSuccessDialog = RegisterSuccessDialog(AnkoContext.create(this, contentView!!))
                registerSuccessDialog.onPositiveBtnClick = {
                    registerSuccessDialog.dialog.dismiss()
                }
                true
            }

        if (isRegistered) { // Login
            btn_login.isEnabled = false
            btn_login.setOnClickListener {
                if (wrongPasswordInputAttempt > 3) {
                    showForgotPasswordDialog()
                    wrongPasswordInputAttempt = 0
                } else
                    validateLogin()
            }
        } else { // First open / register
            tv_title.text = getString(R.string.register_title)
            btn_login.text = getString(R.string.button_register)
            textView2.visibility = View.GONE
            til_password_confirm.visibility = View.VISIBLE
            til_password_confirm.isEnabled = false
            btn_login.isEnabled = false
            btn_login_fp.hide()
            btn_login.setOnClickListener {
                register()
            }
        }

        password.doOnTextChanged { text, _, _, _ ->
            til_password.isErrorEnabled = false
            if (!isRegistered) {
                til_password_confirm.isEnabled = !text.isNullOrEmpty()
            } else {
                btn_login.isEnabled = !text.isNullOrEmpty()
            }
        }
        password_confirm.doOnTextChanged { text, _, _, _ ->
            til_password_confirm.isErrorEnabled = false
            if (!isRegistered) {
                btn_login.isEnabled = !text.isNullOrEmpty()
            }
        }

        btn_login_fp.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (BiometricUtils.isFingerprintAvailable(this))
                    loginWithFingerprint()
                else
                    showNeedToAddFingerprintDialog()
            }
        }


        // Hide login with fingerprint if device has no sensor
        if (!BiometricUtils.isHardwareSupported(this)) {
            textView2.visibility = View.GONE
            btn_login_fp.hide()
        } // Show fingerprint login, if has sensor, has enrolled & has registered
        else if (
            BiometricUtils.isHardwareSupported(this) &&
            BiometricUtils.isFingerprintAvailable(this) &&
            isRegistered
        ) {
            btn_login_fp.performClick()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun loginWithFingerprint() {
        BiometricManager.BiometricBuilder(this)
            .setTitle(getString(R.string.login_fingerprint_title))
            .setSubtitle(getString(R.string.app_name))
            .setDescription(getString(R.string.login_fingerprint_desc))
            .setNegativeButtonText(getString(R.string.dialog_btn_cancel))
            .build()
            .authenticate(object : BiometricCallback {
                override fun onBiometricAuthenticationNotAvailable() {
                }

                override fun onBiometricAuthenticationPermissionNotGranted() {
                    toast("Biometric auth permission not granted")
                }

                override fun onBiometricAuthenticationInternalError(error: String) {
                    toast("Biometric auth internal error: $error")
                }

                override fun onAuthenticationFailed() {
//                    toast("Auth failed")
                }

                override fun onAuthenticationCancelled() {
                }

                override fun onAuthenticationSuccessful() {
                    startActivity(intentFor<HomeActivity>().clearTop())
                    finish()
                }

                override fun onAuthenticationHelp(helpCode: Int, helpString: CharSequence) {
//                    toast("Auth help ($helpCode) $helpString")
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
//                    toast("Auth error ($errorCode) $errString")
                }

            })
    }

    private fun validateLogin() {
        val passwordEdt = password.text!!
        if (passwordEdt.isNotBlank() && passwordEdt.toString() == Hawk.get(Constants.SP_PASSWORD)) {
            startActivity(intentFor<HomeActivity>().clearTop())
            finish()
        } else {
            til_password.error = getString(R.string.text_hint_login_password_invalid)
            til_password.isErrorEnabled = true
            wrongPasswordInputAttempt += 1
        }
    }

    private fun register() {
        val passwordEdt = password.text!!
        val passwordConfirmEdt = password_confirm.text!!
        when {
            passwordEdt.isNotBlank() && passwordConfirmEdt.isNotBlank() -> {
                if (passwordEdt.toString() == passwordConfirmEdt.toString()) {
                    Hawk.put(Constants.SP_PASSWORD, passwordEdt.toString())
                    val registerSuccessDialog = RegisterSuccessDialog(AnkoContext.create(this, contentView!!))
                    registerSuccessDialog.onPositiveBtnClick = {
                        startActivity(intentFor<HomeActivity>().clearTop())
                        registerSuccessDialog.dialog.dismiss()
                        finish()
                    }
                } else {
                    til_password_confirm.error = getString(R.string.text_hint_register_password_confirmation_not_match)
                    til_password_confirm.isErrorEnabled = true
                }
            }
            passwordEdt.isBlank() -> {
                til_password.error = getString(R.string.text_hint_register_password_empty)
                til_password.isErrorEnabled = true
            }
            passwordEdt.isNotBlank() && passwordConfirmEdt.isBlank() -> {
                til_password_confirm.error = getString(R.string.text_hint_register_password_confirmation_empty)
                til_password_confirm.isErrorEnabled = true
            }
        }
    }

    private fun resetPassword() {
        alert(Appcompat) {
            title = getString(R.string.dialog_title_forgot_password_confirmation)
            message = getString(R.string.dialog_message_forgot_password_confirmation)
            positiveButton(getString(R.string.dialog_btn_no)) {
                it.dismiss()
            }
            negativeButton(getString(R.string.dialog_btn_yes)) {
                Hawk.deleteAll()
                doAsync {
                    appDatabase.dataDao().deleteAll()
                }
                it.dismiss()
                recreate()
            }
        }.show().apply {
            getButton(AlertDialog.BUTTON_NEGATIVE).textColor =
                ContextCompat.getColor(this@LoginActivity, R.color.red_500)
        }
    }

    private fun showForgotPasswordDialog() {
        alert(Appcompat) {
            title = getString(R.string.dialog_title_forgot_password)
            message = getString(R.string.dialog_message_forgot_password)
            isCancelable = false
            negativeButton(getString(R.string.dialog_btn_action_forgot_password)) {
                resetPassword()
                it.dismiss()
            }
            positiveButton(getString(R.string.dialog_btn_cancel_forgot_password)) {
                it.dismiss()
            }
        }.show().apply {
            getButton(AlertDialog.BUTTON_NEGATIVE).apply {
                textColor = ContextCompat.getColor(this@LoginActivity, R.color.red_500)
            }
        }
    }

    private fun showNeedToAddFingerprintDialog() {
        alert(
            Appcompat,
            getString(R.string.dialog_message_fingerprint_not_enrolled),
            getString(R.string.dialog_title_fingerprint_not_enrolled)
        ) {
            positiveButton(getString(R.string.dialog_btn_action_fingerprint_not_enrolled)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                    startActivity(Intent(Settings.ACTION_FINGERPRINT_ENROLL))
                else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
            }
            show()
        }
    }
}
