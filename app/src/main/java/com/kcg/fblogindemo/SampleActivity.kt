package com.kcg.fblogindemo

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseUser
import com.kcg.fblogin.LoginHelper
import kotlinx.android.synthetic.main.activity_sample.*


class SampleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample)

        LoginHelper.init(
            this,
            getString(R.string.default_web_client_id),
            object : LoginHelper.SignInResult {
                override fun invoke(firebaseUser: FirebaseUser?) {
                    firebaseUser?.let {
                        Toast.makeText(
                            this@SampleActivity,
                            "성공 : ${it.uid}",
                            Toast.LENGTH_SHORT
                        ).show()
                    } ?: let {
                        Toast.makeText(
                            this@SampleActivity,
                            "실패",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            })

        loginGoogle.setOnClickListener { LoginHelper.signIn(LoginHelper.LoginType.GOOGLE) }
        loginFacebook.setOnClickListener { LoginHelper.signIn(LoginHelper.LoginType.FACEBOOK) }
        logOut.setOnClickListener { LoginHelper.signOut() }
        userid.setOnClickListener {
            Toast.makeText(
                this@SampleActivity,
                LoginHelper.getUID(),
                Toast.LENGTH_SHORT
            ).show()
        }
        email.setOnClickListener {
            Toast.makeText(
                this@SampleActivity,
                LoginHelper.getEmail(),
                Toast.LENGTH_SHORT
            ).show()
        }
        firebaseUser.setOnClickListener {
            Toast.makeText(
                this@SampleActivity,
                "${LoginHelper.getFirebaseUser()}",
                Toast.LENGTH_SHORT
            ).show()
        }
        isLogin.setOnClickListener {
            Toast.makeText(
                this@SampleActivity,
                "${LoginHelper.isSignIn()}",
                Toast.LENGTH_SHORT
            ).show()
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        data?.let {
            LoginHelper.loginActivityResult(requestCode, resultCode, it)
        }
    }
}
