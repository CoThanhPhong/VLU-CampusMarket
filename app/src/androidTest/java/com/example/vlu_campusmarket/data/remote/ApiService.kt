package com.example.vlu_campusmarket.data.remote
import com.example.vlu_campusmarket.data.model.product
import retrofit2.Call
import retrofit2.http.GET

interface ApiService {
    @GET("api/products/g")
    fun getProducts(): Call<List<product>>
}