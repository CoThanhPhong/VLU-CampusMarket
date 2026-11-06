package com.example.vlu_campusmarket.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import coil.load // Thư viện Coil để load ảnh online
import com.example.vlu_campusmarket.databinding.ActivityLoginBinding
import com.example.vlu_campusmarket.ui.main.MainActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load logo trực tiếp từ URL online của VLU
        binding.ivLogo.load("https://online.vlu.edu.vn/Content/logo/logosona.jpg") {
            crossfade(true)
            // placeholder(R.drawable.ic_placeholder) // (Tuỳ chọn) Ảnh hiển thị tạm trong lúc chờ tải
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
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    showError(message)
                }
            }
        }
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }
}