package com.onechat.client

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SignupValidationTest {
    @Test fun rejectsShortPasswordBeforeCallingRender() {
        assertFalse(SignupValidation.validPassword("12345"))
        assertNotNull(SignupValidation.error("lucas", "12345", "private-invite-key"))
    }

    @Test fun acceptsValidUsernameAndEightBytePassword() {
        assertNull(SignupValidation.error("lucas", "password123", "private-invite-key"))
        assertTrue(SignupValidation.validUsername("lucas"))
    }

    @Test fun followsBackendUsernameContract() {
        assertFalse(SignupValidation.validUsername("lu"))
        assertFalse(SignupValidation.validUsername("8lucas"))
        assertFalse(SignupValidation.validUsername("lucas-p"))
        assertTrue(SignupValidation.validUsername("Lucas_2026"))
    }

    @Test fun checksUtf8BytesRatherThanCharacters() {
        assertEquals(10, SignupValidation.passwordBytes("ééééé"))
        assertTrue(SignupValidation.validPassword("ééééé"))
        assertFalse(SignupValidation.validPassword("1234567"))
        assertFalse(SignupValidation.validPassword("a".repeat(73)))
    }
}
