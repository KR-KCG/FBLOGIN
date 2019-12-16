package com.kcg.fblogin

import android.app.Activity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

class GoogleLogin {

    private var gso: GoogleSignInOptions? = null
    private fun getGso(): GoogleSignInOptions =
        if (LoginHelper.googleClientId.isBlank()) throw IllegalArgumentException("Please reset your Google Client ID.")
        else
            gso?.let { it }
                ?: let {
                    gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(LoginHelper.googleClientId)
                        .requestEmail()
                        .build()

                    gso!!
                }

    private var client: GoogleSignInClient? = null
    fun getClient(activity: Activity): GoogleSignInClient =
        client?.let { it }
            ?: let {
                client = GoogleSignIn.getClient(activity, getGso())

                client!!
            }
}