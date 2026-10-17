package com.example.cpen321application

import retrofit2.Retrofit
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.POST
import retrofit2.http.Header
import com.google.gson.annotations.SerializedName

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

// body for POST api/auth/signup, field names match the backend spec
data class SignUpRequest(
    @SerializedName("id_token") val idToken: String,
    val email: String,
    @SerializedName("f_name") val firstName: String,
    @SerializedName("l_name") val lastName: String,
    val birthYear: Int
)

// body for POST api/auth/login
data class LoginRequest(
    @SerializedName("id_token") val idToken: String
)

/*  response from api/auth/signup and api/auth/login:
    msg is "signedIn" with a jwtToken on success, "already_exists" (sign up) or
    "no_user" (login) for account problems, otherwise an error code
    (invalid_request, invalid_token, server_error) with an error description
 */
data class AuthTokenResponse(
    val msg: String?,
    @SerializedName("jwt_token") val jwtToken: String?,
    val error: String?
)

// body for POST api/register_notif, the JWT goes in the Authorization header
data class RegisterNotifRequest(
    @SerializedName("fcm_token") val fcmToken: String
)

/*  a game as the server sends it, the same shape for every endpoint that returns
    games. Times are UTC ISO 8601 strings, e.g. "2026-10-18T02:00:00Z"
 */
data class GameDto(
    @SerializedName("game_id") val gameId: Int,
    @SerializedName("organizer_id") val organizerId: Int,
    val title: String,
    val sport: String,
    val description: String?,
    @SerializedName("starts_at") val startsAt: String,
    @SerializedName("ends_at") val endsAt: String?,
    @SerializedName("location_name") val locationName: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    @SerializedName("max_players") val maxPlayers: Int,
    @SerializedName("player_count") val playerCount: Int,
    @SerializedName("skill_level") val skillLevel: String,
    val status: String,
    @SerializedName("created_at") val createdAt: String
)

// reply from POST api/games: msg "success" with the created game, or an error
data class CreateGameResponse(
    val msg: String?,
    val game: GameDto?,
    val error: String?
)

interface NetworkConnection {
    @POST("api/auth/google")
    suspend fun authAndInfo(@Body request: AuthRequest) : Response<AuthResponse>

    @POST("api/auth/signup")
    suspend fun signUp(@Body request: SignUpRequest) : Response<AuthTokenResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest) : Response<AuthTokenResponse>

    // responds msg "success", or an auth error (expired_token, invalid_token, no_user, ...)
    @POST("api/register_notif")
    suspend fun registerNotif(
        @Header("Authorization") authorization: String,
        @Body request: RegisterNotifRequest
    ) : Response<AuthTokenResponse>

    // create game skeleton: the server always creates the same game for now
    @POST("api/games")
    suspend fun createGame(@Header("Authorization") authorization: String) : Response<CreateGameResponse>
}

/*  reads the json reply of the auth style endpoints, error responses (4xx/5xx)
    carry the same json shape in the error body. Returns null if it can't be read
 */
fun readAuthResponse(response: Response<AuthTokenResponse>): AuthTokenResponse? {
    return response.body() ?: try {
        com.google.gson.Gson().fromJson(response.errorBody()?.string(), AuthTokenResponse::class.java)
    } catch (e: Exception) {
        null
    }
}

// main object instance with retrofit service used to connect to server
object ServerApi {
    val retrofitService : NetworkConnection by lazy {
        retrofit.create(NetworkConnection::class.java)
    }
}

/*  create game skeleton: asks the server to create its placeholder game for the
    signed in user. Returns the created game, or null if it failed. The server
    sends a debug push notification about it a few seconds later
 */
suspend fun createDebugGame(): GameDto? {
    val jwt = jwtToken ?: return null
    return try {
        val body = ServerApi.retrofitService.createGame(authorization = "Bearer $jwt").body()
        body?.game?.takeIf { body.msg == "success" }
    } catch (e: Exception) {
        null
    }
}
