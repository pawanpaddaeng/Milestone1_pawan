package com.example.cpen321application

import android.app.Activity
import android.os.Bundle
import android.content.Context
import android.content.ContextWrapper
import android.content.MutableContextWrapper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material3.HorizontalDivider
import com.example.cpen321application.ui.theme.CPEN321ApplicationTheme
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.credentials.CredentialManager
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.CustomCredential
import androidx.navigation.compose.rememberNavController
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import androidx.navigation.navArgument
import androidx.navigation.NavType
import android.net.Uri
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.milliseconds

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CPEN321ApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    /*Greeting(
                        apiBaseUrl = BuildConfig.API_BASE_URL,
                        modifier = Modifier.padding(innerPadding)
                    )*/
                    //ThreeButtonsScreen(modifier = Modifier.padding(innerPadding))
                    AppNavigation(modifier = Modifier.padding((innerPadding)))
                }
            }

        }
    }
}
var firstName: String? = "Not"
var lastName: String? = "Logged In"


@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()

    fun startGlobalTimer(totalSeconds: Int) {
        coroutineScope.launch {
            // Wait for user-defined duration (seconds -> milliseconds)
            delay((totalSeconds * 1000L).milliseconds)

            // Force navigation to TimeUpScreen regardless of current active screen
            navController.navigate("TimeUpScreen")
        }
    }

    NavHost(
        navController = navController,
        startDestination = "three_buttons"
    ) {
        composable("three_buttons") {
            ThreeButtonsScreen(
                onNavigateToDetailsScreen = { status ->
                    val encodedStatus = Uri.encode(status)
                    navController.navigate("detail_screen/$encodedStatus")
                    //navController.navigate("detail_screen")
                },
                onNavigateToPixelScreen = {
                    navController.navigate(("pixel_screen"))
                },
                onNavigateToTimeScreen = {
                    navController.navigate(("TimeInputScreen"))
                },
                modifier = modifier
            )
        }

        composable(
            route = "detail_screen/{statusData}",
            arguments = listOf(
                navArgument("statusData") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            // Extract the string argument from the backStackEntry
            val statusData = backStackEntry.arguments?.getString("statusData") ?: "No Data"

            DetailScreen(
                statusData = statusData,
                onBack = {
                    navController.popBackStack() }
            )
        }

        composable ("pixel_screen") {
            PixelArtScreen(onBack = { navController.popBackStack() } )
        }

        composable ("TimeInputScreen"){
            TimeInputScreen(onTimeSubmitted = { time ->
                startGlobalTimer(time)
                navController.popBackStack() },
                onBack = { navController.popBackStack() })
        }

        composable("TimeUpScreen") {
            TimeUpScreen(onBack = { navController.popBackStack() })}
    }
}

//private val credentialManager = CredentialManager.create(context)
fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
suspend fun triggerGoogleSignIn(context: Context): String? {
    val activityContext = context.findActivity()
        ?: throw IllegalStateException("Google Sign-In requires an Activity Context")

    val credentialManager = CredentialManager.create(context)

    // 1. Configure Google ID Option
    val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false) // Set false to show ALL Google accounts on device
        .setServerClientId(BuildConfig.GOOGLE_CLIENT_ID)
        .build()

    // 2. Build the Credential Request
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    val mutableContext = MutableContextWrapper(activityContext)
    //coroutineScope {
    return try {
            val result = credentialManager.getCredential(
                request = request,
                context = mutableContext // Use MutableContextWrapper to avoid memory leak during configuration changes
            )
            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {

                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                // PRINT TOKEN TO LOGCAT
                print("SUCCESS! ID Token:\n$idToken")
                idToken
            } else {
                print("Unexpected credential type returned")
                null
            }
        } catch (e: GetCredentialException) {
            // Handle failures
            print("ERRRRRRROOOOOOOORRRRRR")
            null
        }

}

@Composable
fun Greeting(apiBaseUrl: String, modifier: Modifier = Modifier) {
    var statusText by remember { mutableStateOf("Checking backend at $apiBaseUrl/health...") }

    LaunchedEffect(apiBaseUrl) {
        statusText = fetchHealthStatus(apiBaseUrl)
    }

    Text(
        text = statusText,
        modifier = modifier
    )
}


private suspend fun fetchHealthStatus(apiBaseUrl: String): String = withContext(Dispatchers.IO) {
    val healthUrl = "${apiBaseUrl.trimEnd('/')}/health"
    try {
        val connection = (URL(healthUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5_000
            readTimeout = 5_000
        }

        when (val code = connection.responseCode) {
            HttpURLConnection.HTTP_OK -> {
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                "Backend healthy ($healthUrl): $body"
            }
            else -> {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }
                "Backend error ($healthUrl): HTTP $code${errorBody?.let { " — $it" } ?: ""}"
            }
        }
    } catch (e: Exception) {
        "Backend unreachable ($healthUrl): ${e.message ?: e.javaClass.simpleName}"
    }
}