package com.example.cpen321application

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import com.google.gson.Gson
import okhttp3.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign

/*  class for the pixel stream updates sent be websocket
    helps directly parse an update to usable object with gson
 */
data class PixelUpdate(val x: Int, val y: Int, val color: String)

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

// a simple screen to display the information for Milestone 1 task 1
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

// a screen to take user time input and call the callback function with that input
@Composable
fun TimeInputScreen(
    onTimeSubmitted: (totalSeconds: Int) -> Unit,
    onBack: () -> Unit = {}
) {
    var minutesText by remember { mutableStateOf("") }
    var secondsText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current

    val minutes = minutesText.toIntOrNull() ?: 0
    val seconds = secondsText.toIntOrNull() ?: 0
    val totalSeconds = (minutes * 60) + seconds

    // check the values submitted or ask again
    fun handleSubmit() {
        focusManager.clearFocus()
        if (seconds >= 60) {
            errorMessage = "Seconds must be between 0 and 59"
            return
        }
        if (totalSeconds <= 0) {
            errorMessage = "Please enter a duration greater than 0"
            return
        }
        errorMessage = null
        onTimeSubmitted(totalSeconds)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Set Duration",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(32.dp))

        // put input fields in a row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Minutes Field
            OutlinedTextField(
                value = minutesText,
                onValueChange = { input ->
                    if (input.all { it.isDigit() } && input.length <= 3) {
                        minutesText = input
                        errorMessage = null
                    }
                },
                label = { Text("Minutes") },
                modifier = Modifier.weight(1f)
            )

            Text(
                text = ":",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            // Seconds Field
            OutlinedTextField(
                value = secondsText,
                onValueChange = { input ->
                    if (input.all { it.isDigit() } && input.length <= 2) {
                        secondsText = input
                        errorMessage = null
                    }
                },
                label = { Text("Seconds") },
                modifier = Modifier.weight(1f)
            )
        }

        // display error from bad input
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { handleSubmit() },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Confirm")
        }
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onBack) {
            Text("Back")
        }
    }
}

// a popup screen for when the timer runs out
@Composable
fun TimeUpScreen(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = """
                Time's Up!
                Not much to see here.
                Other features coming soon....
            """.trimIndent(),
            )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onBack,
            modifier = Modifier
                .height(50.dp)
        ) {
            Text("Back")
        }
    }
}

/*  a screen that draws a grid for the pixel are and initiates the
    persistent connection to the server's websocket
 */
@Composable
fun PixelArtScreen(
    websocketURL: String = BuildConfig.API_WEB_SOCKET,
    onBack: () -> Unit) {

    //create a mutable grid that can be updated
    val gridState = remember {
        mutableStateMapOf<Pair<Int, Int>, Color>().apply {
            for (x in 0 until 16) {
                for (y in 0 until 16) {
                    put(x to y, Color(0xFFD3D3D3))
                }
            }
        }
    }

    /*  use a disposable effect to connect to the server so that the connection
        can also be cleaned
     */
    DisposableEffect(websocketURL) {
        //connect to websocket and define a callback to update grid state
        val client = PixelWebSocketClient(websocketURL) { update ->
            try {
                // parse color string
                val parsedColor = Color(update.color.toColorInt())
                if (update.x in 0..15 && update.y in 0..15) {
                    gridState[update.x to update.y] = parsedColor
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        client.connect()

        onDispose {
            client.disconnect()
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Live Pixel Stream",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(24.dp))

        // 16x16 Canvas Renderer
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth()
                .background(Color(0xFFF8F9FA))
        ) {
            //canvas for a modifiable screen that uses grid state and can be seen on display
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cellSizeX = size.width / 16f
                val cellSizeY = size.height / 16f

                // draw each pixel cell stored in gridState
                gridState.forEach { (coord, color) ->
                    drawRect(
                        color = color,
                        topLeft = Offset(coord.first * cellSizeX, coord.second * cellSizeY),
                        size = Size(cellSizeX, cellSizeY)
                    )
                }
                //simple border for visual clarity
                drawRect(
                    color = Color(0xFFD3D3D3),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onBack) {
            Text("Back")
        }
    }

}

/*
    A helper class that deals with connecting to a websocket at 'url'
    and sending the PixelUpdate data it receives to the callback
 */
class PixelWebSocketClient(
    private val url: String,
    private val onPixelReceived: (PixelUpdate) -> Unit
) {
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null
    private val gson = Gson()

    fun connect() {
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val update = gson.fromJson(text, PixelUpdate::class.java)
                    onPixelReceived(update)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                t.printStackTrace()
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "Screen closed")
    }
}