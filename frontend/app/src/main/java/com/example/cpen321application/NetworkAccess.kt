package com.example.cpen321application

import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.POST

/*  retrofit for server connection and auto parsing to objects
    using gson
 */
private val retrofit = Retrofit.Builder()
    .addConverterFactory(GsonConverterFactory.create())
    .baseUrl(BuildConfig.API_BASE_URL)
    .build()

//add datatypes for sending and receiving information (used by gson for parsing)
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

data class ServerInfo(
    val msg : String
)

interface NetworkConnection {
    @GET("getIP4")
    suspend fun getServerIp4() : Response<ServerInfo>

    @GET("getTime")
    suspend fun  getServerTime() : Response<ServerInfo>

    @GET("getAuthorName")
    suspend fun getAuthName() : User

    @POST("api/auth/google")
    suspend fun authAndInfo(@Body request: AuthRequest) : Response<AuthResponse>
}

// main object instance with retrofit service used to connect to server
object ServerApi {
    val retrofitService : NetworkConnection by lazy {
        retrofit.create(NetworkConnection::class.java)
    }
}
