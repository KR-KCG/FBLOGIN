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
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

object LoginHelper {

    enum class LoginType {
        GOOGLE, FACEBOOK
    }

    private const val TAG = "LOGIN_HELPER"

    // 이 값을 google-services.json 내에 있는 R.string.default_web_client_id 값을 넣어야함
    var googleClientId = ""
    private var activity: Activity? = null
    private var signInResult: ((String?) -> Unit)? = null

    /**
     * @param activity Activity
     * @param googleClientId Google Client Id
     * @param signInResult Function(String)
     *          Parameter String : UID
     *          Success Login : parameter is NotNull And NotBlank string
     *          Failed Login : parameter is Null Or Blank string
     */
    fun init(
        activity: Activity,
        googleClientId: String,
        signInResult: ((String?) -> Unit)?
    ) {
        this.activity = activity
        this.googleClientId = googleClientId
        this.signInResult = signInResult
    }

    private val auth by lazy { FirebaseAuth.getInstance() }
    fun isSignin(): Boolean = auth.currentUser != null
    fun getUID(): String? = auth.currentUser?.uid
    fun getEmail(): String? = auth.currentUser?.email
    fun getProviderId(): String? = auth.currentUser?.providerId
    fun signOut(): Unit = auth.signOut()

    private var google: GoogleLogin? = null
    private const val GOOGLE_LOGIN_REQUEST_CODE = 1000001

    private val callbackManager by lazy { CallbackManager.Factory.create() }
    private const val FACEBOOK_LOGIN_REQUEST_CODE = 1000002
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
            }
        }
    }

    fun loginActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
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

            FACEBOOK_LOGIN_REQUEST_CODE -> callbackManager.onActivityResult(
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
        Log.d(TAG, "handleFacebookAccessToken:$token")
        signInWithCredential(FacebookAuthProvider.getCredential(token.token))
    }

    private fun signInWithCredential(credential: AuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(activity!!) {
                if (it.isSuccessful && auth.currentUser != null) {
                    signInResult?.invoke(getUID())
                } else signInResult?.invoke(null)
            }
    }
}