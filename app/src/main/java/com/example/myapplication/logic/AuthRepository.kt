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
 * Callers (MapsActivity) only interact via [buildSignInIntent],
 * [revokeAndSignIn], and [getCalendarToken] — they never know how
 * authentication is implemented.
 *
 * [signInClient] is injected via the constructor (default: built from
 * [buildDefaultSignInClient]) so tests can pass a mock without triggering
 * real Google Play Services calls.
 *
 * [dispatchers] is injected so unit tests can swap in [TestDispatchers]
 * and avoid real IO.
 */
class AuthRepository(
    private val context: Context,
    private val signInClient: GoogleSignInClient = buildDefaultSignInClient(context),
    private val dispatchers: DispatcherProvider  = DefaultDispatcherProvider()
) {
    companion object {
        private const val CALENDAR_SCOPE =
            "oauth2:https://www.googleapis.com/auth/calendar.readonly"

        /**
         * Builds the production [GoogleSignInClient].
         * Extracted as a top-level factory so the default parameter above
         * stays readable and the options are defined in one place.
         */
        fun buildDefaultSignInClient(context: Context): GoogleSignInClient {
            val options = GoogleSignInOptions
                .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(Scope("https://www.googleapis.com/auth/calendar.readonly"))
                .build()
            return GoogleSignIn.getClient(context, options)
        }
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
                android.util.Log.w("AuthRepository", "UserRecoverableAuthException")
                CrashReporter.recordNonFatal(e, "calendar_user_recoverable_auth")
                null
            } catch (e: Exception) {
                android.util.Log.e("AuthRepository", "Token error: ${e.message}")
                CrashReporter.recordNonFatal(e, "calendar_token_refresh_failed")
                null
            }
        }

    /**
     * Signs the user out of Google.
     * Calls [onComplete] when finished so the caller can update UI state.
     */
    fun signOut(onComplete: () -> Unit) {
        signInClient.signOut().addOnCompleteListener { onComplete() }
    }

    /** Returns true if there is a currently signed-in Google account. */
    fun isSignedIn(): Boolean =
        GoogleSignIn.getLastSignedInAccount(context) != null

    /** Returns the email of the signed-in account, or null. */
    fun getSignedInEmail(): String? =
        GoogleSignIn.getLastSignedInAccount(context)?.email
}
