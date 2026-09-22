package com.example.cpen321application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.net.URL
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET

//retrofit for getting ip address
private val retrofit = Retrofit.Builder()
    .baseUrl("https://icanhazip.com/") // Fixed: added trailing slash
    .addConverterFactory(ScalarsConverterFactory.create())
    .build()

interface ClientApis {
    @GET("/")
    suspend fun icanIP4(): String
}

object GetClientIpApi { // Fixed: PascalCase for object name
    val retrofitService: ClientApis by lazy {
        retrofit.create(ClientApis::class.java)
    }
}

fun getLocalTimeString(): String {
    val now = Date()
    val timeZone = TimeZone.getDefault()

    // 1. Format local time in 24-hour format (hh:mm:ss)
    val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.US).apply {
        this.timeZone = timeZone
    }
    val timeStr = timeFormatter.format(now)

    // 2. Format timezone offset as GMT+hh:mm or GMT-hh:mm
    val offsetMillis = timeZone.getOffset(now.time)
    val offsetHours = Math.abs(offsetMillis) / (1000 * 60 * 60)
    val offsetMinutes = (Math.abs(offsetMillis) / (1000 * 60)) % 60
    val sign = if (offsetMillis >= 0) "+" else "-"

    val gmtOffsetStr = String.format(
        Locale.US,
        "GMT%s%02d:%02d",
        sign,
        offsetHours,
        offsetMinutes
    )

    return "$timeStr $gmtOffsetStr"
}

suspend fun getClientIP4(): String{
    var ip: String = "Undefined"
    try {
        ip = GetClientIpApi.retrofitService.icanIP4().trim()
        println("Public IP: $ip")
    } catch (e: Exception) {
        println("Failed to fetch IP: ${e.message}")
    }
    return ip
}