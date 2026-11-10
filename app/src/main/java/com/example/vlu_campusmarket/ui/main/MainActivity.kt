package com.example.vlu_campusmarket.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.vlu_campusmarket.R
import com.example.vlu_campusmarket.databinding.ActivityMainBinding
import com.example.vlu_campusmarket.ui.fragment.CartFragment
import com.example.vlu_campusmarket.ui.fragment.CreatePostFragment
import com.example.vlu_campusmarket.ui.fragment.HomeFragment
import com.example.vlu_campusmarket.ui.fragment.MenuFragment
import com.example.vlu_campusmarket.ui.fragment.OrderFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarHeader)

        if (savedInstanceState == null) {
            replaceFragment(HomeFragment(), "Trang chủ")
        }

        // Bắt sự kiện chuyển đổi tab trên thanh Curved Bottom Bar
        binding.bottomBar.onItemSelected = { index ->
            when (index) {
                0 -> replaceFragment(HomeFragment(), "Trang chủ")
                1 -> replaceFragment(CartFragment(), "Giỏ hàng")
                2 -> replaceFragment(CreatePostFragment(), "Tạo bài đăng mới")
                3 -> replaceFragment(OrderFragment(), "Đơn hàng")
                4 -> replaceFragment(MenuFragment(), "Danh mục Menu")
            }
        }
    }

    private fun replaceFragment(fragment: Fragment, title: String) {
        supportActionBar?.title = title
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragmentOutlet, fragment)
            .commit()
    }
}