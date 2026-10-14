package com.example.cpen321application.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import com.example.cpen321application.AuthRequest
import com.example.cpen321application.AuthResponse
import com.example.cpen321application.ServerApi
import com.example.cpen321application.ServerInfo
import com.example.cpen321application.User
import com.example.cpen321application.firstName
import com.example.cpen321application.lastName
import com.example.cpen321application.getClientIP4
import com.example.cpen321application.getLocalTimeString
import com.example.cpen321application.triggerGoogleSignIn

/*
    Main home screen with 3 buttons:
    Receives three callback functions that connect it to other
    screens through AppNavigation
 */
@Composable
fun ThreeButtonsScreen(
    onNavigateToDetailsScreen: (String) -> Unit,
    onNavigateToPixelScreen: () -> Unit,
    onNavigateToTimeScreen: () -> Unit,
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
        // a text box for some interactive updates
        Text(
            text = statusText,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Button 1: Does Sign in and if successful displays the server info
        Button(
            onClick = {
                statusText = "Starting Task 1 login..."
                scope.launch {
                    val googleToken = triggerGoogleSignIn(context)

                    // valid token start the authentication with server
                    if (googleToken != null){
                        statusText = "Verifying login..."
                        val requestBody = AuthRequest(idToken = googleToken)
                        var response: retrofit2.Response<AuthResponse>?
                        try{
                            response = ServerApi.retrofitService.authAndInfo(requestBody)
                            if(response.isSuccessful){
                                // server verified account now get all the needed info to display
                                val authData: AuthResponse? = response.body()
                                firstName = authData?.user?.firstName
                                lastName = authData?.user?.lastName
                                statusText = "Welcome ${authData?.user?.firstName}, wait for details..."

                                val serverIPRes = ServerApi.retrofitService.getServerIp4()
                                var serverIP: String? = "Could not get IP"
                                if(serverIPRes.isSuccessful){
                                    val ipBody: ServerInfo? = serverIPRes.body()
                                    serverIP = ipBody?.msg
                                }
                                val clientIP = getClientIP4()
                                val serverTimeRes = ServerApi.retrofitService.getServerTime()
                                var serverTime: String? = "Could not get time"
                                if(serverTimeRes.isSuccessful){
                                    val timeBody: ServerInfo? = serverTimeRes.body()
                                    serverTime = timeBody?.msg
                                }
                                val clientTime = getLocalTimeString()
                                val dev : User = ServerApi.retrofitService.getAuthName()

                                // generate the formatted string to display
                                val task1Details= """
                                Status: System Diagnostics Ready
                                --------------------------------
                                Server Public IP: $serverIP
                                Client IP: $clientIP
                                Server Time: $serverTime
                                Client Time: $clientTime
                                Developer: ${dev.firstName} ${dev.lastName}
                                User Signed In: $firstName $lastName
                            """.trimIndent()

                                onNavigateToDetailsScreen(task1Details)

                            } else {
                                // could not contact server, either server offline or check IP
                                statusText = "Server error code: ${response.code()}, " +
                                        "${response.errorBody()?.string()}"
                            }
                        }catch (e: Exception){
                            statusText = "Cannot Reach Server: $e"
                        }

                    } else {
                        statusText = """ 
                            Error: No Google Account in the device
                            Steps: Go to Device Settings and have at least one Google Account
                        """.trimIndent()
                    }

                }
            },
            modifier = Modifier
                .padding(vertical = 6.dp)
        ) {
            Text("Sign in and info")
        }

        // a button that takes you to the pixel art screen
        Button(
            onClick = {
                statusText = "Starting..."
                onNavigateToPixelScreen()
            },
            modifier = Modifier
                .padding(vertical = 6.dp)
        ) {
            Text("Get Pixel Art")
        }

        // button to take you to enter time screen
        Button(
            onClick = {
                statusText = "Preparing to set timer..."
                onNavigateToTimeScreen()
            },
            modifier = Modifier
                .padding(vertical = 6.dp)
        ) {
            Text("Set Timer")
        }
    }
}
