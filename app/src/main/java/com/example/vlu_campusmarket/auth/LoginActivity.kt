package com.example.vlu_campusmarket.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.vlu_campusmarket.MainActivity
import com.example.vlu_campusmarket.R
import com.example.vlu_campusmarket.util.applySystemBarsPadding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var progress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        findViewById<View>(R.id.main).applySystemBarsPadding()

        auth = FirebaseAuth.getInstance()
        edtEmail = findViewById(R.id.edtEmail)
        edtPassword = findViewById(R.id.edtPassword)
        btnLogin = findViewById(R.id.btnLogin)
        progress = findViewById(R.id.progress)

        btnLogin.setOnClickListener { login() }
        findViewById<View>(R.id.tvRegister).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        findViewById<View>(R.id.tvForgot).setOnClickListener { resetPassword() }
    }

    override fun onStart() {
        super.onStart()
        // Đã có phiên đăng nhập hợp lệ -> vào thẳng màn hình chính
        val user = auth.currentUser
        if (user != null && user.isEmailVerified) goMain()
    }

    private fun login() {
        val email = edtEmail.text.toString().trim()
        val pw = edtPassword.text.toString()

        if (!AuthValidator.isValidEmail(email)) {
            edtEmail.error = getString(R.string.error_invalid_email)
            return
        }
        if (!AuthValidator.isValidPassword(pw)) {
            edtPassword.error = getString(R.string.error_password_min)
            return
        }

        setLoading(true)
        auth.signInWithEmailAndPassword(email, pw).addOnCompleteListener(this) { task ->
            setLoading(false)
            if (!task.isSuccessful) {
                Toast.makeText(this, R.string.msg_login_failed, Toast.LENGTH_SHORT).show()
                return@addOnCompleteListener
            }
            val user = auth.currentUser
            if (user != null && user.isEmailVerified) {
                goMain()
            } else {
                Toast.makeText(this, R.string.msg_verify_email_first, Toast.LENGTH_LONG).show()
                auth.signOut()
            }
        }
    }

    private fun resetPassword() {
        val email = edtEmail.text.toString().trim()
        if (!AuthValidator.isValidEmail(email)) {
            edtEmail.error = getString(R.string.error_email_for_reset)
            return
        }
        auth.sendPasswordResetEmail(email).addOnCompleteListener(this) { t ->
            Toast.makeText(
                this,
                if (t.isSuccessful) R.string.msg_reset_sent else R.string.msg_reset_failed,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !loading
    }

    private fun goMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
