package com.example.cpen321application.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.cpen321application.AuthRequest
import com.example.cpen321application.AuthResponse
import com.example.cpen321application.ServerApi
import com.example.cpen321application.firstName
import com.example.cpen321application.lastName
import com.example.cpen321application.triggerGoogleSignIn

/*
    Sign in screen for SportsNow:
    Sign In verifies the user with Google and the backend, then calls
    onSignedIn. Sign Up calls onNavigateToSignUp to go to the sign up page
 */
@Composable
fun SignInScreen(
    onSignedIn: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    modifier: Modifier = Modifier,
    infoMessage: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var statusText by remember { mutableStateOf<String?>(null) }
    var isSigningIn by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = "SportsNow",
            color = Color.Black,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "Sign Into the SportsNow Account",
            color = Color.Black,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Verify with Google to log into Account",
            color = Color.Black,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))
        SignInPillButton(
            text = "Sign In",
            enabled = !isSigningIn,
            onClick = {
                isSigningIn = true
                statusText = null
                scope.launch {
                    val googleToken = triggerGoogleSignIn(context)
                    if (googleToken == null) {
                        statusText = "Google sign in failed. Make sure a Google account " +
                                "is added to this device"
                        isSigningIn = false
                        return@launch
                    }

                    // valid token, verify the account with the server
                    try {
                        val response = ServerApi.retrofitService
                            .authAndInfo(AuthRequest(idToken = googleToken))
                        if (response.isSuccessful) {
                            val authData: AuthResponse? = response.body()
                            firstName = authData?.user?.firstName
                            lastName = authData?.user?.lastName
                            onSignedIn()
                        } else {
                            statusText = "Server Verification failed, try again."
                        }
                    } catch (e: Exception) {
                        statusText = "Cannot Reach Server"
                    }
                    isSigningIn = false
                }
            }
        )

        // divider with "or Create a New Account" in the middle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFD9D9D9))
            Text(
                text = "or Create a New Account",
                color = Color(0xFF757575),
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFD9D9D9))
        }

        SignInPillButton(
            text = "Sign Up",
            enabled = !isSigningIn,
            onClick = onNavigateToSignUp
        )

        // shows any sign in errors below the buttons
        Box(
            modifier = Modifier.padding(top = 24.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            if (isSigningIn) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else if (statusText != null) {
                Text(
                    text = statusText!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            } else if (infoMessage != null) {
                // e.g. sent back from sign up because the account already exists
                Text(
                    text = infoMessage,
                    color = Color.Black,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.weight(1.4f))
    }
}

// white rounded button with a shadow used on the sign in screen
@Composable
private fun SignInPillButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    ElevatedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = Color.White,
            contentColor = Color.Black
        ),
        elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 4.dp)
    ) {
        Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}
