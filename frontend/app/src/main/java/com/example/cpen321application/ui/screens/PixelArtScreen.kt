package com.example.cpen321application.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.core.graphics.toColorInt
import com.google.gson.Gson
import okhttp3.*
import com.example.cpen321application.BuildConfig

/*  class for the pixel stream updates sent be websocket
    helps directly parse an update to usable object with gson
 */
data class PixelUpdate(val x: Int, val y: Int, val color: String)

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
