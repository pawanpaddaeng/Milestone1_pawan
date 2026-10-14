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
import com.example.cpen321application.ui.screens.DetailScreen
import com.example.cpen321application.ui.screens.HomeScreen
import com.example.cpen321application.ui.screens.sampleJoinedGames
import com.example.cpen321application.ui.screens.sampleOrganizedGames
import com.example.cpen321application.ui.screens.PixelArtScreen
import com.example.cpen321application.ui.screens.SignInScreen
import com.example.cpen321application.ui.screens.SignUpScreen
import com.example.cpen321application.ui.screens.TimeInputScreen
import com.example.cpen321application.ui.screens.TimeUpScreen
import androidx.credentials.CredentialManager
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
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
        startDestination = "sign_in"
    ) {
        composable("sign_in") {
            /*  start at the sign in screen, once signed in go to the home
                screen and remove sign in so back doesn't return to it
             */
            SignInScreen(
                onSignedIn = {
                    navController.navigate("home") {
                        popUpTo("sign_in") { inclusive = true }
                    }
                },
                onNavigateToSignUp = { navController.navigate("sign_up") },
                modifier = modifier
            )
        }

        composable("home") {
            // TODO: replace the sample games with the user's games from the backend
            HomeScreen(
                joinedGames = sampleJoinedGames,
                organizedGames = sampleOrganizedGames,
                modifier = modifier
            )
        }

        composable("sign_up") {
            SignUpScreen(
                onCreateAccount = { details ->
                    // TODO: send details to the backend once it has a sign up endpoint
                    firstName = details.firstName
                    lastName = details.lastName
                    val encodedStatus = Uri.encode(
                        "Account created for: ${details.firstName} ${details.lastName}"
                    )
                    navController.navigate("detail_screen/$encodedStatus")
                },
                onNavigateToSignIn = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
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

    /*  fallback "Sign in with Google" flow: unlike the bottom sheet above, it
        still opens when no account is on the device and lets the user add one
     */
    val signInWithGoogleRequest = GetCredentialRequest.Builder()
        .addCredentialOption(
            GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_CLIENT_ID).build()
        )
        .build()

    //mutable context to avoid memory leak as suggested by google
    val mutableContext = MutableContextWrapper(activityContext)

    return try {
            val result = try {
                credentialManager.getCredential(
                    request = request,
                    context = mutableContext
                )
            } catch (e: NoCredentialException) {
                credentialManager.getCredential(
                    request = signInWithGoogleRequest,
                    context = mutableContext
                )
            }
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
