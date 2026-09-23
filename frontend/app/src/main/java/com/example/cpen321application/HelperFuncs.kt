package com.example.cpen321application

import retrofit2.Retrofit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET
import kotlin.math.abs

/*
    A file with some helper function needed to get the details
    for task1
 */

// retrofit for getting client's ip from icanhazip
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

/*  helper function to get system time and return it as the
    required 24-hour format of : HH:MM:SS GMT+/-hh:mm
 */
fun getLocalTimeString(): String {
    val now = Date()
    val timeZone = TimeZone.getDefault()

    // format local time in 24-hour format (hh:mm:ss)
    val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.US).apply {
        this.timeZone = timeZone
    }
    val timeStr = timeFormatter.format(now)

    // 2. Format timezone offset as GMT+hh:mm or GMT-hh:mm
    val offsetMillis = timeZone.getOffset(now.time)
    val offsetHours = abs(offsetMillis) / (1000 * 60 * 60)
    val offsetMinutes = (abs(offsetMillis) / (1000 * 60)) % 60
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

// helper function that return's the client's public ip
suspend fun getClientIP4(): String{
    var ip = "Undefined"
    try {
        ip = GetClientIpApi.retrofitService.icanIP4().trim()
        println("Public IP: $ip")
    } catch (e: Exception) {
        println("Failed to fetch IP: ${e.message}")
    }
    return ip
}