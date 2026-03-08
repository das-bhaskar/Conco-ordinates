package com.example.myapplication.logic

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.kotlin.*

class AuthRepositoryTest {

    private val mockContext: Context = mock()
    private val mockSignInClient: GoogleSignInClient = mock()
    private val mockTask: Task<Void> = mock()

    private val testDispatchers = object : DispatcherProvider {
        override val main = kotlinx.coroutines.Dispatchers.Unconfined
        override val io = kotlinx.coroutines.Dispatchers.Unconfined
        override val default = kotlinx.coroutines.Dispatchers.Unconfined
    }

    private lateinit var authRepository: AuthRepository

    @Before
    fun setup() {
        authRepository = AuthRepository(
            context = mockContext,
            signInClient = mockSignInClient,
            dispatchers = testDispatchers
        )
    }

    @Test
    fun `signOut calls client and triggers callback`() {
        // 1. Setup the Task mock
        whenever(mockSignInClient.signOut()).thenReturn(mockTask)

        // 2. Capture the listener and force it to run
        whenever(mockTask.addOnCompleteListener(any())).thenAnswer {
            val listener = it.arguments[0] as OnCompleteListener<Void>
            listener.onComplete(mockTask)
            mockTask
        }

        var callbackCalled = false
        authRepository.signOut { callbackCalled = true }

        assertTrue("Callback should be triggered after signOut", callbackCalled)
        verify(mockSignInClient).signOut()
    }

    @Test
    fun `revokeAndSignIn calls revoke and then provides intent`() {
        whenever(mockSignInClient.revokeAccess()).thenReturn(mockTask)
        val expectedIntent = mock<Intent>()
        whenever(mockSignInClient.signInIntent).thenReturn(expectedIntent)

        whenever(mockTask.addOnCompleteListener(any())).thenAnswer {
            val listener = it.arguments[0] as OnCompleteListener<Void>
            listener.onComplete(mockTask)
            mockTask
        }

        var capturedIntent: Intent? = null
        authRepository.revokeAndSignIn { capturedIntent = it }

        assertNotNull("Intent should not be null", capturedIntent)
        assertEquals(expectedIntent, capturedIntent)
    }



}