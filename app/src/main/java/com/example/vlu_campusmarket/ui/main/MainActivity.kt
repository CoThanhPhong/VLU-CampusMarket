package com.example.vlu_campusmarket.ui.main

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.example.vlu_campusmarket.R
import com.example.vlu_campusmarket.data.local.AppDatabase
import com.example.vlu_campusmarket.data.remote.RetrofitClient
import com.example.vlu_campusmarket.data.repository.OrderRepository
import com.example.vlu_campusmarket.databinding.ActivityMainBinding
import com.example.vlu_campusmarket.ui.fragment.CartFragment
import com.example.vlu_campusmarket.ui.fragment.CreatePostFragment
import com.example.vlu_campusmarket.ui.fragment.HomeFragment
import com.example.vlu_campusmarket.ui.fragment.MenuFragment
import com.example.vlu_campusmarket.ui.fragment.OrderFragment
import com.example.vlu_campusmarket.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentTab = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState != null) {
            currentTab = savedInstanceState.getInt("current_tab", 0)
        }

        setSupportActionBar(binding.toolbarHeader)

        // Lắng nghe thay đổi BackStack để bật/tắt Nút quay lại (Back Button) trên Toolbar
        supportFragmentManager.addOnBackStackChangedListener {
            val hasBackStack = supportFragmentManager.backStackEntryCount > 0
            supportActionBar?.setDisplayHomeAsUpEnabled(hasBackStack)
            supportActionBar?.setDisplayShowHomeEnabled(hasBackStack)
        }

        val selectTab: (Int) -> Unit = { index ->
            currentTab = index
            supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            when (index) {
                0 -> replaceFragment(HomeFragment(), "Trang chủ")
                1 -> replaceFragment(CartFragment(), "Giỏ hàng")
                2 -> replaceFragment(CreatePostFragment(), "Tạo bài đăng mới")
                3 -> replaceFragment(OrderFragment(), "Hóa đơn của tôi")
                4 -> replaceFragment(MenuFragment(), "Menu")
            }
        }

        if (savedInstanceState == null) {
            replaceFragment(HomeFragment(), "Trang chủ")
        } else {
            selectTab(currentTab)
            binding.bottomBar.post {
                binding.bottomBar.selectTab(currentTab, false)
            }
        }

        // Bắt sự kiện chuyển đổi tab trên thanh Navigation (5 tabs)
        binding.bottomBar.onItemSelected = { index ->
            selectTab(index)
        }

        startGlobalNotificationSync()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("current_tab", currentTab)
    }

    private fun startGlobalNotificationSync() {
        lifecycleScope.launch(Dispatchers.IO) {
            var prevPendingIds = setOf<String>()
            var prevConfirmedIds = setOf<String>()

            while (isActive) {
                delay(5000) // Polling ngầm mỗi 5 giây trên toàn bộ Activity
                try {
                    val context = applicationContext
                    val db = AppDatabase.getDatabase(context)
                    val apiService = RetrofitClient.getInstance(context)
                    val sessionManager = SessionManager(context)
                    val user = sessionManager.fetchUser() ?: continue

                    // 1. Kiểm tra đơn hàng mới cho tài khoản ADMIN
                    if (user.role.equals("ADMIN", ignoreCase = true)) {
                        val response = apiService.getAllOrders()
                        if (response.isSuccessful && response.body()?.success == true) {
                            val orders = response.body()?.data ?: emptyList()
                            val pendingIds = orders
                                .filter { it.orderStatus.equals("Chờ xác nhận", ignoreCase = true) || it.orderStatus.equals("pending", ignoreCase = true) }
                                .map { it._id }
                                .toSet()

                            val newPending = pendingIds - prevPendingIds
                            if (newPending.isNotEmpty() && prevPendingIds.isNotEmpty()) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@MainActivity, "🔔 Thông báo: Có ${newPending.size} đơn hàng mới vừa được đặt/thanh toán!", Toast.LENGTH_LONG).show()
                                }
                            }
                            prevPendingIds = pendingIds
                        }
                    }

                    // 2. Kiểm tra hóa đơn được duyệt cho tài khoản Người mua (USER)
                    val orderRepo = OrderRepository(db.orderDao(), apiService, db.userDao(), db.productDao())
                    orderRepo.fetchRemoteOrders()
                    val userOrders = orderRepo.getOrdersLocal(user.id).firstOrNull() ?: emptyList()
                    val confirmedIds = userOrders
                        .filter { it.orderStatus.equals("Đã xác nhận", ignoreCase = true) || it.orderStatus.equals("approved", ignoreCase = true) }
                        .map { it._id }
                        .toSet()

                    val newConfirmed = confirmedIds - prevConfirmedIds
                    if (newConfirmed.isNotEmpty() && prevConfirmedIds.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "🎉 Hóa đơn của bạn vừa được cửa hàng xác nhận thành công!", Toast.LENGTH_LONG).show()
                        }
                    }
                    prevConfirmedIds = confirmedIds

                } catch (e: Exception) {
                    // Sync ngầm không ngắt ứng dụng
                }
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
