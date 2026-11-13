package com.example.vlu_campusmarket.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.vlu_campusmarket.R
import com.example.vlu_campusmarket.databinding.ActivityLoginBinding
import com.example.vlu_campusmarket.ui.main.MainActivity
import com.example.vlu_campusmarket.utils.SessionManager

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Kiểm tra ngay lập tức đồng bộ qua SessionManager (0ms, không nháy màn hình đăng nhập)
        val sessionManager = SessionManager(this)
        if (sessionManager.isLoggedIn()) {
            navigateToMain()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Kiểm tra tự động đăng nhập nếu có phiên duy trì
        viewModel.checkAutoLogin { autoLoggedIn ->
            if (autoLoggedIn) {
                navigateToMain()
            }
        }

        // Load logo VLU với Glide
        Glide.with(this)
            .load("https://online.vlu.edu.vn/Content/logo/logosona.jpg")
            .placeholder(R.drawable.ic_launcher_foreground)
            .into(binding.ivLogo)

        binding.btnInfo.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Thông tin đăng nhập")
                .setMessage("Sử dụng tài khoản vlu online để đăng nhập")
                .setPositiveButton("Đã hiểu", null)
                .show()
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnLogin.setOnClickListener {
            val mssv = binding.etMssv.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val rememberMe = binding.cbRememberMe.isChecked

            if (mssv.isEmpty() || password.isEmpty()) {
                showError("Vui lòng nhập đầy đủ MSSV và mật khẩu")
                return@setOnClickListener
            }

            binding.tvError.visibility = View.GONE
            binding.btnLogin.isEnabled = false
            binding.btnLogin.text = "Đang xử lý..."

            viewModel.login(mssv, password, rememberMe) { success, message ->
                binding.btnLogin.isEnabled = true
                binding.btnLogin.text = "Đăng nhập"

                if (success) {
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    navigateToMain()
                } else {
                    showError(message)
                }
            }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }
}
