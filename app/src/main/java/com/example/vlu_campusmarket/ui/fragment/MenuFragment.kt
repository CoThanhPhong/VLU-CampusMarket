package com.example.vlu_campusmarket.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.vlu_campusmarket.R
import com.example.vlu_campusmarket.databinding.FragmentMenuBinding
import com.example.vlu_campusmarket.ui.admin.AdminDashboardFragment
import com.example.vlu_campusmarket.ui.admin.AdminOrderFragment
import com.example.vlu_campusmarket.ui.login.LoginActivity
import com.example.vlu_campusmarket.utils.LogoutHelper
import com.example.vlu_campusmarket.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MenuFragment : Fragment() {

    private var _binding: FragmentMenuBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sessionManager = SessionManager(requireContext())
        val userRole = sessionManager.fetchUser()?.role ?: ""

        // Nếu tài khoản có role user (không phải admin), ẩn mục quản trị admin
        if (userRole.lowercase() != "admin") {
            binding.btnMenuAdmin.visibility = View.GONE
        } else {
            binding.btnMenuAdmin.visibility = View.VISIBLE
        }

        binding.btnMenuAdmin.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentOutlet, AdminDashboardFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnMenuAdminOrders.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentOutlet, AdminOrderFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnMenuProfile.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentOutlet, ProfileFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnMenuLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất và xóa toàn bộ dữ liệu phiên không?")
                .setPositiveButton("Đăng xuất") { _, _ ->
                    val context = requireContext()
                    lifecycleScope.launch(Dispatchers.IO) {
                        LogoutHelper.performCompleteLogout(context)
                        withContext(Dispatchers.Main) {
                            val intent = Intent(context, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                        }
                    }
                }
                .setNegativeButton("Hủy", null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
