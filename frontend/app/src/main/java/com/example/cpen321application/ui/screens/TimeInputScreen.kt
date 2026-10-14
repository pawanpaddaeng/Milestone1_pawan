package com.example.cpen321application.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign

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
