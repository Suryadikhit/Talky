package com.example.talky

import com.google.gson.annotations.SerializedName
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

data class UserResponse(
    val id: String,
    val username: String,
    @SerializedName("profile_pic") val profilePic: String, // ✅ Correct mapping
    @SerializedName("phone_number") val phoneNumber: String // ✅ Correct mapping
)

object RetrofitClient {
    private const val BASE_URL = "http://172.20.10.2:4000" // Change to your actual server IP

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(40, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .writeTimeout(40, TimeUnit.SECONDS)
        .build()

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

interface ApiService {
    @Multipart
    @POST("upload-profile")  // ✅ Correct endpoint as per your server
    suspend fun uploadProfilePicture(
        @Part profile: MultipartBody.Part,
        @Part("username") username: RequestBody, // ✅ User-provided username
        @Part("uid") uid: RequestBody // ✅ UID
    ): Response<UploadResponse>



    @POST("/save-user")
    suspend fun saveUser(
        @Body userMap: Map<String, String?>
    ): Response<GenericResponse>

    @GET("/get-user")
    suspend fun getUser(@Query("uid") uid: String): Response<UserResponse>

}

data class UploadResponse(val imageUrl: String)

data class GenericResponse(
    val message: String,
    val statusCode: Int? = null
)
