package com.example.vlu_campusmarket.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.vlu_campusmarket.R
import com.example.vlu_campusmarket.databinding.FragmentMenuBinding
import com.example.vlu_campusmarket.ui.admin.AdminDashboardFragment

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

        binding.btnMenuAdmin.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentOutlet, AdminDashboardFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnMenuProfile.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentOutlet, ProfileFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnMenuGuide.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Quy định mua bán VLU Market")
                .setMessage("1. Các thiết bị đăng tải phải là tài sản chính chủ hoặc có sự đồng ý của chủ sở hữu.\n2. Giá bán đăng rõ ràng theo đơn vị VNĐ.\n3. Giao dịch trực tiếp tại các cơ sở VLU để đảm bảo an toàn.")
                .setPositiveButton("Đã hiểu") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
