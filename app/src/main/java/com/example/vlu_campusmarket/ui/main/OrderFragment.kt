package com.example.vlu_campusmarket.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.vlu_campusmarket.R

class OrderFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Trả về layout tương ứng của trang chủ
        return inflater.inflate(R.layout.fragment_order, container, false)
    }
}