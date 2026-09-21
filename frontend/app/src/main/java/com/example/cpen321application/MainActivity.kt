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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.material3.Button
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
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

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
                    ThreeButtonsScreen(modifier = Modifier.padding(innerPadding))
                }
            }

        }
    }
}

@Composable
fun ThreeButtonsScreen(modifier: Modifier = Modifier) {
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
                    if (googleToken != null){
                        statusText = "Token!!!!!!!"
                    }
                }
                //Toast.makeText(context, "First action executed", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                //.fillMaxWidth()
                .padding(vertical = 6.dp)
        ) {
            Text("First Action")
        }

        // Button 2: Async Background Task
        Button(
            onClick = {
                statusText = "Running background task..."
//                coroutineScope.launch {
//                    val result = simulateNetworkCall()
//                    statusText = result
//                }
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