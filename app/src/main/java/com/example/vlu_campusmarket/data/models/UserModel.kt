package com.example.vlu_campusmarket.data.models

<<<<<<< HEAD
import com.google.gson.annotations.SerializedName

data class UserProfile(
    @SerializedName(value = "id", alternate = ["_id"])
    val id: String = "",
    val name: String = "",
    val bio: String? = null,
    val avatar: String? = null,
    val phone: String = ""
)

data class UserModel(
    // MongoDB thường trả về "_id" thay vì "id" → nhận cả hai tên
=======
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class UserProfile(
    @SerializedName(value = "id", alternate = ["_id", "userID"])
    val id: String = "",
    val name: String = "Sinh viên VLU",
    val bio: String? = null,
    val avatar: String? = null,
    val phone: String = "",
    @SerializedName("products")
    val rawProducts: List<JsonElement> = emptyList()
) {
    val productIds: List<String>
        get() = rawProducts.mapNotNull { element ->
            when {
                element.isJsonPrimitive -> element.asString
                element.isJsonObject -> {
                    val obj = element.asJsonObject
                    when {
                        obj.has("_id") -> obj.get("_id").asString
                        obj.has("id") -> obj.get("id").asString
                        else -> null
                    }
                }
                else -> null
            }
        }

    val productModels: List<ProductModel>
        get() = rawProducts.mapNotNull { element ->
            try {
                if (element.isJsonObject) {
                    Gson().fromJson(element, ProductModel::class.java)
                } else null
            } catch (e: Exception) {
                null
            }
        }
}

data class UserModel(
>>>>>>> features/optimize-leakCanary
    @SerializedName(value = "id", alternate = ["_id"])
    val id: String = "",
    val mssv: String = "",
    val email: String = "",
<<<<<<< HEAD
    val role: String = "student",
=======
    val role: String = "user",
>>>>>>> features/optimize-leakCanary
    val profile: UserProfile? = null
)
