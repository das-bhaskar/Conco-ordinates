package com.example.myapplication.logic

import android.content.Context
import com.example.myapplication.telemetry.CrashReporter
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.withContext

/**
 * Encapsulates all Google Sign-In and OAuth token logic.
 *
 * MapsActivity must not know how authentication works — it only calls
 * [buildSignInIntent], [handleSignInResult], and [revokeAndGetSignInIntent].
 *
 * [dispatchers] is injected so unit tests can swap in [TestDispatchers]
 * and avoid real IO.
 */
class AuthRepository(
    private val context: Context,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) {
    companion object {
        private const val CALENDAR_SCOPE =
            "oauth2:https://www.googleapis.com/auth/calendar.readonly"
    }

    private val signInOptions: GoogleSignInOptions =
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope("https://www.googleapis.com/auth/calendar.readonly"))
            .build()

    val signInClient: GoogleSignInClient by lazy {
        GoogleSignIn.getClient(context, signInOptions)
    }

    /** Returns an Intent that starts the Google account picker. */
    fun buildSignInIntent() = signInClient.signInIntent

    /**
     * Revokes any previously granted access so Google always shows the full
     * consent screen (including Calendar scope) on the next sign-in.
     * Calls [onReady] with the sign-in Intent once revocation completes.
     */
    fun revokeAndSignIn(onReady: (android.content.Intent) -> Unit) {
        signInClient.revokeAccess().addOnCompleteListener {
            onReady(buildSignInIntent())
        }
    }

    /**
     * Fetches a fresh OAuth token for the currently signed-in account.
     * Returns null if no account is signed in or the token cannot be obtained.
     */
    suspend fun getCalendarToken(): String? =
        withContext(dispatchers.io) {
            try {
                val account = GoogleSignIn.getLastSignedInAccount(context)
                if (account == null) {
                    android.util.Log.w("AuthRepository", "No signed-in account found")
                    null
                } else {
                    val token = GoogleAuthUtil.getToken(
                        context, account.account!!, CALENDAR_SCOPE
                    )
                    android.util.Log.d("AuthRepository", "Token obtained: ${token?.take(10)}...")
                    token
                }
            } catch (e: com.google.android.gms.auth.UserRecoverableAuthException) {
                // Consent was not granted — caller must re-launch sign-in with e.intent
                android.util.Log.w("AuthRepository", "UserRecoverableAuthException")
                CrashReporter.recordNonFatal(e, "calendar_user_recoverable_auth")
                null
            } catch (e: Exception) {
                android.util.Log.e("AuthRepository", "Token error: ${e.message}")
                CrashReporter.recordNonFatal(e, "calendar_token_refresh_failed")
                null
            }
        }

    /** Returns true if there is a currently signed-in Google account. */
    fun isSignedIn(): Boolean =
        GoogleSignIn.getLastSignedInAccount(context) != null

    /** Returns the email of the signed-in account, or null. */
    fun getSignedInEmail(): String? =
        GoogleSignIn.getLastSignedInAccount(context)?.email
}
