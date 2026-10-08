package com.example

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.ChatRepository
import com.example.data.UserRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class KhahishRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun userProfile_saveAndRetrieve_succeeds() = runBlocking {
        val uid = signInTestUser("alice_${UUID.randomUUID().toString().take(6)}@example.com")
        val userRepo = UserRepository(firestore)

        val saveResult = withTimeout(5000L) {
            userRepo.saveProfile(
                username = "alice_${UUID.randomUUID().toString().take(4)}",
                displayName = "Alice Wonderland",
                bio = "Testing Khahish"
            )
        }
        assertTrue("Profile save should succeed", saveResult.isSuccess)

        val profileResult = withTimeout(5000L) { userRepo.getProfile(uid) }
        assertTrue("Profile fetch should succeed", profileResult.isSuccess)
        val profile = profileResult.getOrNull()
        assertNotNull("Profile should exist", profile)
        assertEquals("Alice Wonderland", profile?.displayName)
    }

    @Test
    fun chatRoom_sendMessageAndRead_succeedsForParticipants() = runBlocking {
        val aliceUid = signInTestUser("alice_chat_${UUID.randomUUID().toString().take(6)}@example.com")
        val chatRepo = ChatRepository(firestore)

        val bobUid = "bob_${UUID.randomUUID().toString().take(6)}"

        val chatResult = withTimeout(5000L) { chatRepo.getOrCreateChat(bobUid) }
        chatResult.exceptionOrNull()?.let {
            println("CHAT RESULT EXCEPTION: ${it.message}")
            it.printStackTrace()
        }
        assertTrue("Chat creation should succeed: ${chatResult.exceptionOrNull()?.message}", chatResult.isSuccess)
        val chatId = chatResult.getOrThrow()

        val sendResult = withTimeout(5000L) {
            chatRepo.sendMessage(
                chatId = chatId,
                text = "Hello from Alice!",
                senderName = "Alice"
            )
        }
        assertTrue("Send message should succeed", sendResult.isSuccess)

        val messages = withTimeout(5000L) {
            chatRepo.observeMessages(chatId).first { it.isNotEmpty() }
        }
        assertEquals(1, messages.size)
        assertEquals("Hello from Alice!", messages[0].text)
    }

    @Test
    fun unauthenticatedAccess_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val chatRepo = ChatRepository(firestore)

        try {
            withTimeout(3000L) {
                chatRepo.observeUserChats("any_user").first()
            }
            fail("Unauthenticated observation should fail")
        } catch (e: Exception) {
            // Expected failure (either Firestore exception or IllegalStateException from requireUserId)
            assertTrue(true)
        }
    }
}
