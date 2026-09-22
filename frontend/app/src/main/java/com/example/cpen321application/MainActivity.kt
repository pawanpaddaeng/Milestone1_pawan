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
fun ThreeButtonsScreen(
    onNavigateToDetailsScreen: (String) -> Unit,
    onNavigateToPixelScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var statusText by remember { mutableStateOf("Press any button to trigger an action") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status Display
        //StatusDisplayCard(statusText = statusText)
        Text(
            text = statusText,
            //style = CPEN321ApplicationTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Button 1: Immediate Action
        Button(
            onClick = {
                statusText = "Button 1 clicked!"
                scope.launch {
                    val googleToken = triggerGoogleSignIn(context)
                    //get user logged in and save the name/profile(later)
                    if (googleToken != null){
                        statusText = "Token!!!!!!!"
                        val requestBody = AuthRequest(idToken = googleToken)
                        val response = serverApi.retrofitService.authAndInfo(requestBody)
                        if(response.isSuccessful){
                            val authData: AuthResponse? = response.body()
                            firstName = authData?.user?.firstName
                            lastName = authData?.user?.lastName
                            statusText = "Welcome ${authData?.user?.firstName}, wait for details..."
                            //generate the output:
                            val serverIPRes = serverApi.retrofitService.getServerIp4()
                            var serverIP: String? = "Could not get"
                            if(serverIPRes.isSuccessful){
                                val ipBody: ServerInfo? = serverIPRes.body()
                                serverIP = ipBody?.msg
                            }
                            val clientIP = getClientIP4()
                            val serverTimeRes = serverApi.retrofitService.getServerTime()
                            var serverTime: String? = "Could not get"
                            if(serverTimeRes.isSuccessful){
                                val timeBody: ServerInfo? = serverTimeRes.body()
                                serverTime = timeBody?.msg
                            }
                            val clientTime = getLocalTimeString()
                            val dev : User = serverApi.retrofitService.getAuthName()
                            statusText = """
                                Status: System Diagnostics Ready
                                --------------------------------
                                Server Public IP: $serverIP
                                Client IP: $clientIP
                                Server Time: $serverTime
                                Client Time: $clientTime
                                Developer: ${dev.firstName} ${dev.lastName}
                                User Signed In: $firstName $lastName
                            """.trimIndent()

                            onNavigateToDetailsScreen(statusText)

                        } else {
                            statusText = "Server error code: ${response.code()}, " +
                                    "${response.errorBody()?.string()}"
                        }
                    }

                }
                //Toast.makeText(context, "First action executed", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                //.fillMaxWidth()
                .padding(vertical = 6.dp)
        ) {
            Text("Task1 : Sign in and info")
        }

        // Button 2: Async Background Task
        Button(
            onClick = {
                statusText = "Running background task..."
                onNavigateToPixelScreen()
            },
            modifier = Modifier
                //.fillMaxWidth()
                .padding(vertical = 6.dp)
        ) {
            Text("Fetch Data (Async)")
        }

        // Button 3: Custom Action / Reset
        Button(
            onClick = {
                statusText = "Press any button to trigger an action"
                statusText = """
                    Status: System Diagnostics Ready
                    
                    Server Public IP: 203.0.113.195
                    Client IP: 10.0.2.15
                    Server Time: 23:38:05 GMT+00:00
                    Client Time: 23 23 23 23 23 
                    Developer: Pawanpreet Padda
                    User: Pawanpreet Padda
                """.trimIndent()
//                statusText = getLocalTimeString()
//                scope.launch {
//                    statusText = getClientIP4()
//                }

                //Toast.makeText(context, "State Reset", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                //.fillMaxWidth()
                .padding(vertical = 6.dp)
        ) {
            Text("Reset Status")
        }
    }
}

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "three_buttons"
    ) {
        composable("three_buttons") {
            ThreeButtonsScreen(
                onNavigateToDetailsScreen = { status ->
                    // URL-encode strings containing special characters, spaces, or newlines
                    //val encodedStatus = URLEncoder.encode(status, StandardCharsets.UTF_8.toString())
                    val encodedStatus = Uri.encode(status)
                    navController.navigate("detail_screen/$encodedStatus")
                    //navController.navigate("detail_screen")
                },
                onNavigateToPixelScreen = {
                    navController.navigate(("pixel_screen"))
                },
                modifier = modifier
            )
        }

//        composable("detail_screen") {
//            DetailScreen(onBack = { navController.popBackStack() } )
//        }
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
                onBack = { navController.popBackStack() }
            )
        }

        composable ("pixel_screen") {
            PixelArtScreen(onBack = { navController.popBackStack() } )
        }
    }
}

@Composable
fun DetailScreen(statusData: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Welcome to the Details Screen!")
        Text(text = statusData)
        Button(onClick = onBack) {
            Text("Go Back")
        }
    }
}

@Composable
fun PixelArtScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Welcome to the Pixel Screen!")
        Button(onClick = onBack) {
            Text("Go Back")
        }
    }
}

@Composable
fun StatusDisplayCard(
    statusText: String,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
            .animateContentSize(), // Smoothly resizes the card when text changes size
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 6.dp // Shadow depth
        ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "CONSOLE OUTPUT",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Dynamic Status Text Body
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace // Gives an API/diagnostic terminal look
                ),
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.3
            )
        }
    }
}

//private val credentialManager = CredentialManager.create(context)
fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
private suspend fun triggerGoogleSignIn(context: Context): String? {
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