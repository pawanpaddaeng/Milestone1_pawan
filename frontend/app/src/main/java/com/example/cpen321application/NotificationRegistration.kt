package com.example.cpen321application

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

// result of trying to register this device for push notifications
enum class NotifRegistrationResult {
    // the server saved this device's Firebase token
    REGISTERED,
    // nothing to do right now (Firebase not set up or server unreachable), the user stays signed in
    SKIPPED,
    // the session can't be used or renewed, the user has to sign in again
    SIGN_IN_REQUIRED
}

/*
    Sends this device's Firebase token to the server (POST api/register_notif)
    so the user can get push notifications. Runs at app start when a saved JWT
    exists and after sign in / sign up.
    If the server says the JWT expired, it signs in again silently with the same
    Google account, saves the new JWT and retries once
 */
object NotificationRegistration {
    private const val TAG = "NotifRegistration"

    // server replies the flow cares about
    private enum class Reply { SUCCESS, EXPIRED, REJECTED, UNREACHABLE }

    suspend fun register(context: Context): NotifRegistrationResult {
        val fcmToken = getFirebaseToken() ?: return NotifRegistrationResult.SKIPPED
        val currentJwt = jwtToken ?: return NotifRegistrationResult.SIGN_IN_REQUIRED

        return when (sendToken(currentJwt, fcmToken)) {
            Reply.SUCCESS -> NotifRegistrationResult.REGISTERED
            Reply.UNREACHABLE -> NotifRegistrationResult.SKIPPED
            Reply.REJECTED -> NotifRegistrationResult.SIGN_IN_REQUIRED
            Reply.EXPIRED -> {
                val renewedJwt = silentRelogin(context, currentJwt)
                    ?: return NotifRegistrationResult.SIGN_IN_REQUIRED
                jwtToken = renewedJwt
                AuthTokenStore.save(context, renewedJwt)

                when (sendToken(renewedJwt, fcmToken)) {
                    Reply.SUCCESS -> NotifRegistrationResult.REGISTERED
                    Reply.UNREACHABLE -> NotifRegistrationResult.SKIPPED
                    else -> NotifRegistrationResult.SIGN_IN_REQUIRED
                }
            }
        }
    }

    /*  sends a token Firebase just replaced (onNewToken). This can run while the
        app is closed, so it can't renew an expired session or show sign in: it
        only sends the token if a saved JWT exists and ignores failures. The next
        app launch runs register() again, which handles renewal
     */
    suspend fun registerNewToken(context: Context, fcmToken: String) {
        val jwt = jwtToken ?: AuthTokenStore.load(context) ?: return
        val reply = sendToken(jwt, fcmToken)
        Log.d(TAG, "Sent new Firebase token, server reply: $reply")
    }

    private suspend fun sendToken(jwt: String, fcmToken: String): Reply {
        return try {
            val response = ServerApi.retrofitService.registerNotif(
                authorization = "Bearer $jwt",
                request = RegisterNotifRequest(fcmToken = fcmToken)
            )
            when (readAuthResponse(response)?.msg) {
                "success" -> Reply.SUCCESS
                "expired_token" -> Reply.EXPIRED
                // no_user, invalid_token, server_error or an unreadable reply
                else -> Reply.REJECTED
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not reach the server to register notifications", e)
            Reply.UNREACHABLE
        }
    }

    /*  gets a new JWT without the user's help: silent Google sign in, then the
        login api. The new token must be for the same user as the expired one,
        otherwise a different Google account was picked and it is rejected
     */
    private suspend fun silentRelogin(context: Context, expiredJwt: String): String? {
        val googleToken = triggerSilentGoogleSignIn(context) ?: return null
        return try {
            val reply = readAuthResponse(
                ServerApi.retrofitService.login(LoginRequest(idToken = googleToken))
            )
            val newJwt = reply?.jwtToken?.takeIf { reply.msg == "signedIn" } ?: return null
            val previousUser = AuthTokenStore.userIdOf(expiredJwt)
            newJwt.takeIf { previousUser != null && AuthTokenStore.userIdOf(it) == previousUser }
        } catch (e: Exception) {
            Log.w(TAG, "Silent sign in failed", e)
            null
        }
    }

    // this device's Firebase token, or null if Firebase isn't set up (no google-services.json)
    private suspend fun getFirebaseToken(): String? {
        return try {
            suspendCancellableCoroutine { continuation ->
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    continuation.resume(if (task.isSuccessful) task.result else null)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firebase is not set up, skipping notification registration", e)
            null
        }
    }
}
