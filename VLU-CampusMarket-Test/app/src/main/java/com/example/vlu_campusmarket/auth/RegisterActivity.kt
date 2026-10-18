package com.example.vlu_campusmarket.auth

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.vlu_campusmarket.R
import com.example.vlu_campusmarket.util.applySystemBarsPadding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest

class RegisterActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var edtName: EditText
    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText
    private lateinit var edtConfirm: EditText
    private lateinit var btnRegister: Button
    private lateinit var progress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)
        findViewById<View>(R.id.main).applySystemBarsPadding()

        auth = FirebaseAuth.getInstance()
        edtName = findViewById(R.id.edtName)
        edtEmail = findViewById(R.id.edtEmail)
        edtPassword = findViewById(R.id.edtPassword)
        edtConfirm = findViewById(R.id.edtConfirm)
        btnRegister = findViewById(R.id.btnRegister)
        progress = findViewById(R.id.progress)

        btnRegister.setOnClickListener { register() }
    }

    private fun register() {
        val name = edtName.text.toString().trim()
        val email = edtEmail.text.toString().trim()
        val pw = edtPassword.text.toString()
        val confirm = edtConfirm.text.toString()

        if (name.isEmpty()) { edtName.error = getString(R.string.error_name_required); return }
        if (!AuthValidator.isValidEmail(email)) { edtEmail.error = getString(R.string.error_invalid_email); return }
        if (!AuthValidator.isValidPassword(pw)) { edtPassword.error = getString(R.string.error_password_min); return }
        if (pw != confirm) { edtConfirm.error = getString(R.string.error_password_mismatch); return }

        setLoading(true)
        auth.createUserWithEmailAndPassword(email, pw).addOnCompleteListener(this) { task ->
            if (!task.isSuccessful) {
                setLoading(false)
                Toast.makeText(
                    this,
                    getString(R.string.msg_register_failed, task.exception?.message.orEmpty()),
                    Toast.LENGTH_LONG
                ).show()
                return@addOnCompleteListener
            }
            val user = auth.currentUser
            if (user == null) { setLoading(false); return@addOnCompleteListener }

            // Lưu tên hiển thị XONG rồi mới gửi email xác thực & đăng xuất,
            // tránh signOut() chạy trước khi updateProfile hoàn tất.
            user.updateProfile(userProfileChangeRequest { displayName = name })
                .addOnCompleteListener(this) {
                    user.sendEmailVerification().addOnCompleteListener(this) {
                        Toast.makeText(this, R.string.msg_verification_sent, Toast.LENGTH_LONG).show()
                        auth.signOut()
                        finish()
                    }
                }
        }
    }

    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        btnRegister.isEnabled = !loading
    }
}
