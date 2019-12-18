package com.kcg.fblogin

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.core.app.ActivityCompat
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.*

object LoginHelper {

    enum class LoginType {
        GOOGLE, FACEBOOK
    }

    private const val TAG = "LOGIN_HELPER"

    var googleClientId = ""
    private var activity: Activity? = null
    private var signInResult: ((FirebaseUser?) -> Unit)? = null

    /**
     * @param activity Activity
     * @param googleClientId getString(R.string.default_web_client_id)
     * @param signInResult Function(String)
     *          Parameter FirebaseUser
     *          Success Login : parameter is NotNull
     *          Failed Login : parameter is Null
     */
    fun init(
        activity: Activity,
        googleClientId: String,
        signInResult: ((FirebaseUser?) -> Unit)?
    ) {
        this.activity = activity
        this.googleClientId = googleClientId
        this.signInResult = signInResult
    }

    private val auth by lazy { FirebaseAuth.getInstance() }
    fun isSignIn(): Boolean = auth.currentUser != null
    fun getUID(): String? = auth.currentUser?.uid
    fun getEmail(): String? = auth.currentUser?.email
    fun getFirebaseUser(): FirebaseUser? = auth.currentUser
    fun signOut(): Unit = auth.signOut()

    private var google: GoogleLogin? = null
    private const val GOOGLE_LOGIN_REQUEST_CODE = 9001

    private val callbackManager by lazy { CallbackManager.Factory.create() }
    fun signIn(type: LoginType) {
        activity
            ?: throw IllegalArgumentException("Please Do LoginHelper.init")

        when (type) {
            LoginType.GOOGLE -> {
                google ?: let { google = GoogleLogin() }
                ActivityCompat.startActivityForResult(
                    activity!!,
                    google!!.getClient(activity!!).signInIntent,
                    GOOGLE_LOGIN_REQUEST_CODE,
                    null
                )
            }

            LoginType.FACEBOOK -> {
                LoginManager.getInstance()
                    .registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
                        override fun onSuccess(result: LoginResult) {
                            Log.d(TAG, "facebook:onSuccess:$result")
                            firebaseAuthWithFacebook(result.accessToken)
                        }

                        override fun onCancel() {
                            Log.w(TAG, "Facebook sign in cancel")
                            signInResult?.invoke(null)
                        }

                        override fun onError(error: FacebookException?) {
                            Log.w(TAG, "Facebook sign in failed", error)
                            signInResult?.invoke(null)
                        }
                    })

                LoginManager.getInstance()
                    .logInWithReadPermissions(activity!!, arrayListOf("public_profile", "email"))
            }
        }
    }

    fun loginActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        when (requestCode) {
            GOOGLE_LOGIN_REQUEST_CODE -> {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    firebaseAuthWithGoogle(account!!)
                } catch (e: ApiException) {
                    Log.w(TAG, "Google sign in failed", e)
                    signInResult?.invoke(null)
                }
            }

            else -> callbackManager.onActivityResult(
                requestCode,
                resultCode,
                data
            )
        }
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        activity
            ?: throw IllegalArgumentException("Please Do LoginHelper.init")

        Log.d(TAG, "firebaseAuthWithGoogle:${acct.id}")
        signInWithCredential(GoogleAuthProvider.getCredential(acct.idToken, null))
    }

    private fun firebaseAuthWithFacebook(token: AccessToken) {
        Log.d(TAG, "handleFacebookAccessToken:${token.userId}")

        signInWithCredential(FacebookAuthProvider.getCredential(token.token))
    }

    private fun signInWithCredential(credential: AuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(activity!!) {
                if (it.isSuccessful) {
                    signInResult?.invoke(it.result?.user)
                } else signInResult?.invoke(null)
            }
    }
}