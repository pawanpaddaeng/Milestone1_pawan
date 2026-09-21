package com.example.cpen321application
import kotlinx.serialization.SerialName
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.Response
import retrofit2.http.Body
//import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
//import kotlinx.serialization.json.Json
import okhttp3.MediaType
import kotlinx.serialization.Serializable
//import okhttp3.MediaType.Companion.toMediaType
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.POST

private val retrofit = Retrofit.Builder()
    .addConverterFactory(GsonConverterFactory.create())
    .baseUrl(BuildConfig.API_BASE_URL)
    .build()

//add datatypes for sending and receiving information
//@Serializable
data class StatusResponse(
    val status: String
)
data class User(
    val firstName: String,
    val lastName: String
)

data class AuthResponse(
    val message: String?,
    val user: User?,
    val error: String?
)

data class AuthRequest(
    val idToken : String?
)

interface NetworkConnection {
    @GET("health")
    suspend fun getStatus() : StatusResponse

    @POST("api/auth/google")
    suspend fun authAndInfo(@Body request: AuthRequest) : Response<AuthResponse>
}

object serverApi {
    val retrofitService : NetworkConnection by lazy {
        retrofit.create(NetworkConnection::class.java)
    }
}
