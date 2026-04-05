package com.example.myapplication.logic

import android.accounts.Account
import android.content.Context
import android.content.Intent
import com.example.myapplication.telemetry.CrashReporter
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryTest {

    private lateinit var context: Context
    private lateinit var signInClient: GoogleSignInClient
    private lateinit var dispatchers: DispatcherProvider
    private lateinit var repository: AuthRepository

    @org.junit.Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        context = mockk(relaxed = true)
        signInClient = mockk(relaxed = true)

        dispatchers = object : DispatcherProvider {
            override val main = Dispatchers.Unconfined
            override val io = Dispatchers.Unconfined
            override val default = Dispatchers.Unconfined
        }

        mockkStatic(GoogleSignIn::class)
        mockkStatic(GoogleAuthUtil::class)
        mockkObject(CrashReporter)

        every { CrashReporter.recordNonFatal(any(), any()) } returns Unit

        repository = AuthRepository(
            context = context,
            signInClient = signInClient,
            dispatchers = dispatchers
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
        unmockkAll()
    }

    @Test
    fun `buildSignInIntent returns sign in client intent`() {
        val expectedIntent = Intent("test.signin")

        every { signInClient.signInIntent } returns expectedIntent

        val result = repository.buildSignInIntent()

        assertSame(expectedIntent, result)
    }


    @Test
    fun `getCalendarToken returns null when no signed in account`() = runTest {
        every { GoogleSignIn.getLastSignedInAccount(context) } returns null

        val result = repository.getCalendarToken()

        assertNull(result)
        verify(exactly = 1) { GoogleSignIn.getLastSignedInAccount(context) }
        verify(exactly = 0) {
            GoogleAuthUtil.getToken(
                any<Context>(),
                any<Account>(),
                any<String>()
            )
        }
    }

    @Test
    fun `getCalendarToken returns token when account exists`() = runTest {
        val account = mockk<GoogleSignInAccount>()
        val androidAccount = Account("test@example.com", "com.google")
        val expectedToken = "token-123456"

        every { GoogleSignIn.getLastSignedInAccount(context) } returns account
        every { account.account } returns androidAccount
        every {
            GoogleAuthUtil.getToken(
                context,
                androidAccount,
                "oauth2:https://www.googleapis.com/auth/calendar.readonly"
            )
        } returns expectedToken

        val result = repository.getCalendarToken()

        assertEquals(expectedToken, result)
    }

    @Test
    fun `getCalendarToken returns null and records non fatal for recoverable auth exception`() = runTest {
        val account = mockk<GoogleSignInAccount>()
        val androidAccount = Account("test@example.com", "com.google")
        val exception = mockk<UserRecoverableAuthException>(relaxed = true)

        every { GoogleSignIn.getLastSignedInAccount(context) } returns account
        every { account.account } returns androidAccount
        every {
            GoogleAuthUtil.getToken(
                context,
                androidAccount,
                "oauth2:https://www.googleapis.com/auth/calendar.readonly"
            )
        } throws exception

        val result = repository.getCalendarToken()

        assertNull(result)
        verify(exactly = 1) {
            CrashReporter.recordNonFatal(exception, "calendar_user_recoverable_auth")
        }
    }

    @Test
    fun `getCalendarToken returns null and records non fatal for generic exception`() = runTest {
        val account = mockk<GoogleSignInAccount>()
        val androidAccount = Account("test@example.com", "com.google")
        val exception = IOException("network failure")

        every { GoogleSignIn.getLastSignedInAccount(context) } returns account
        every { account.account } returns androidAccount
        every {
            GoogleAuthUtil.getToken(
                context,
                androidAccount,
                "oauth2:https://www.googleapis.com/auth/calendar.readonly"
            )
        } throws exception

        val result = repository.getCalendarToken()

        assertNull(result)
        verify(exactly = 1) {
            CrashReporter.recordNonFatal(exception, "calendar_token_refresh_failed")
        }
    }



    @Test
    fun `isSignedIn returns true when account exists`() {
        val account = mockk<GoogleSignInAccount>()

        every { GoogleSignIn.getLastSignedInAccount(context) } returns account

        val result = repository.isSignedIn()

        assertTrue(result)
    }

    @Test
    fun `isSignedIn returns false when account does not exist`() {
        every { GoogleSignIn.getLastSignedInAccount(context) } returns null

        val result = repository.isSignedIn()

        assertFalse(result)
    }

    @Test
    fun `getSignedInEmail returns email when account exists`() {
        val account = mockk<GoogleSignInAccount>()

        every { GoogleSignIn.getLastSignedInAccount(context) } returns account
        every { account.email } returns "test@example.com"

        val result = repository.getSignedInEmail()

        assertEquals("test@example.com", result)
    }

    @Test
    fun `getSignedInEmail returns null when no account exists`() {
        every { GoogleSignIn.getLastSignedInAccount(context) } returns null

        val result = repository.getSignedInEmail()

        assertNull(result)
    }

    private fun completedVoidTask(): Task<Void> = Tasks.forResult(null)
}