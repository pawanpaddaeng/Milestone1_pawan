package com.example.cpen321application

import android.app.Activity
import android.os.Bundle
import android.content.Context
import android.content.ContextWrapper
import android.content.MutableContextWrapper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.example.cpen321application.ui.theme.CPEN321ApplicationTheme
import androidx.credentials.CredentialManager
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.CustomCredential
import androidx.navigation.compose.rememberNavController
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavType
import android.net.Uri
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.milliseconds

/*
    The main activity class that connects our navigation and
    screen controller to the phone.
    Used as provided in the template
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CPEN321ApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavigation(modifier = Modifier.padding((innerPadding)))
                }
            }

        }
    }
}

//variables to store a logged-in user's name as returned by backend
var firstName: String? = "Not"
var lastName: String? = "Logged In"

/*
    App navigation composable:
    Deals with the screens defined in Screens.kt and handles
    their interactions with each other, such as switching
 */
@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()

    //timer to move navigation to another screen, runs in parallel until timeout
    fun startGlobalTimer(totalSeconds: Int) {
        coroutineScope.launch {
            delay((totalSeconds * 1000L).milliseconds)

            // navigate to TimeUpScreen regardless of current active screen
            navController.navigate("TimeUpScreen")
        }
    }

    /*
        Heart of the controller, defines the interactions and passes arguments
        around
     */
    NavHost(
        navController = navController,
        startDestination = "three_buttons"
    ) {
        composable("three_buttons") {
            /*  go to the home screen with 3 buttons, only the first button
                needs information passed to the detail screen for display
             */
            ThreeButtonsScreen(
                onNavigateToDetailsScreen = { status ->
                    val encodedStatus = Uri.encode(status)
                    navController.navigate("detail_screen/$encodedStatus")
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

//helper to ensure proper context is passed to credential manager
fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/*
    Sign in function that handles signing in with Google and returns
    the Google id token if sign in was successful
 */
suspend fun triggerGoogleSignIn(context: Context): String? {
    val activityContext = context.findActivity()
        ?: throw IllegalStateException("Google Sign-In requires an Activity Context")

    val credentialManager = CredentialManager.create(context)

    //see all google account options on the device(signed in or out)
    val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(BuildConfig.GOOGLE_CLIENT_ID)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    //mutable context to avoid memory leak as suggested by google
    val mutableContext = MutableContextWrapper(activityContext)

    return try {
            val result = credentialManager.getCredential(
                request = request,
                context = mutableContext
            )
            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {

                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                print("SUCCESS! Signed into google")
                idToken
            } else {
                print("Unexpected credential type returned")
                null
            }
        } catch (e: GetCredentialException) {
            // Handle failures and return null
            print("EROR: $e")
            null
        }

}
