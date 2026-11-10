package com.example.vlu_campusmarket.ui.main

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
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

        // Lắng nghe thay đổi BackStack để bật/tắt Nút quay lại (Back Button) trên Toolbar
        supportFragmentManager.addOnBackStackChangedListener {
            val hasBackStack = supportFragmentManager.backStackEntryCount > 0
            supportActionBar?.setDisplayHomeAsUpEnabled(hasBackStack)
            supportActionBar?.setDisplayShowHomeEnabled(hasBackStack)
        }

        if (savedInstanceState == null) {
            replaceFragment(HomeFragment(), "Trang chủ")
        }

        // Bắt sự kiện chuyển đổi tab trên thanh Navigation
        binding.bottomBar.onItemSelected = { index ->
            // Xóa hết backstack khi người dùng chuyển giữa các Tab chính
            supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)

            when (index) {
                0 -> replaceFragment(HomeFragment(), "Trang chủ")
                1 -> replaceFragment(CartFragment(), "Giỏ hàng")
                2 -> replaceFragment(CreatePostFragment(), "Tạo bài đăng mới")
                3 -> replaceFragment(OrderFragment(), "Đơn hàng")
                4 -> replaceFragment(MenuFragment(), "Danh mục Menu")
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun replaceFragment(fragment: Fragment, title: String) {
        supportActionBar?.title = title
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragmentOutlet, fragment)
            .commit()
    }
}
