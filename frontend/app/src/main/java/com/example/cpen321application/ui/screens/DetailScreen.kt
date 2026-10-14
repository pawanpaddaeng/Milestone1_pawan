package com.example.cpen321application.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
