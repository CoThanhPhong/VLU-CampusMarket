package com.example.vlu_campusmarket.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.vlu_campusmarket.R

class MenuFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Trả về layout giỏ hàng (Bạn có thể tạo file fragment_cart.xml sau)
        return inflater.inflate(R.layout.fragment_menu, container, false)
    }
}