package com.example.vlu_campusmarket

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vlu_campusmarket.adapter.ProductAdapter
import com.example.vlu_campusmarket.auth.LoginActivity
import com.example.vlu_campusmarket.data.model.product
import com.example.vlu_campusmarket.util.applySystemBarsPadding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) { // chưa đăng nhập -> quay về Login
            goLogin()
            return
        }
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.main).applySystemBarsPadding()

        val name = user.displayName?.takeIf { it.isNotBlank() } ?: user.email.orEmpty()
        findViewById<TextView>(R.id.tvHello).text = getString(R.string.greeting, name)

        findViewById<View>(R.id.btnLogout).setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            goLogin()
        }

        val rv = findViewById<RecyclerView>(R.id.rvProducts)
        rv.layoutManager = GridLayoutManager(this, 2)
        rv.adapter = ProductAdapter(sampleData())
    }

    private fun goLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    // Dữ liệu mẫu - sau này thay bằng dữ liệu từ ApiService (Retrofit) hoặc Firestore
    private fun sampleData(): List<product> = listOf(
        sample("1", "iPhone 12 64GB", 8_500_000.0, "https://picsum.photos/id/1/400/300", "Cơ sở 3"),
        sample("2", "Samsung A54 128GB", 6_200_000.0, "https://picsum.photos/id/20/400/300", "Cơ sở 3"),
        sample("3", "Xiaomi Redmi Note 11", 3_300_000.0, "https://picsum.photos/id/60/400/300", "Cơ sở 3"),
        sample("4", "OPPO Reno 8", 5_400_000.0, "https://picsum.photos/id/119/400/300", "Cơ sở 1")
    )

    private fun sample(id: String, name: String, price: Double, img: String, local: String) =
        product(
            id = id,
            name = name,
            description = null,
            price = price,
            amount = 1,
            local = local,
            img = img,
            startDate = "2026-10-18",
            deviceType = "mobile"
        )
}
