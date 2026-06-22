package com.example.yjx_clockin

import org.junit.Test
import org.junit.Assert.*
import java.security.MessageDigest
import java.util.Base64

class LoginActivityTest {

    @Test
    fun testPasswordHashing() {
        val password = "testPassword123"
        val hashedPassword = hashPassword(password)
        
        assertNotNull("Hashed password should not be null", hashedPassword)
        assertNotEquals("Hashed password should not equal original", password, hashedPassword)
        
        val sameHash = hashPassword(password)
        assertEquals("Same password should produce same hash", hashedPassword, sameHash)
    }

    @Test
    fun testPasswordHashingEmpty() {
        val password = ""
        val hashedPassword = hashPassword(password)
        
        assertNotNull("Empty password hash should not be null", hashedPassword)
    }

    @Test
    fun testPasswordHashingSpecialChars() {
        val password = "p@ssw0rd!#$%^&*()"
        val hashedPassword = hashPassword(password)
        
        assertNotNull("Password with special chars should be hashed", hashedPassword)
        assertNotEquals("Hashed password should not equal original", password, hashedPassword)
    }

    private fun hashPassword(password: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(password.toByteArray())
            Base64.getEncoder().encodeToString(hash)
        } catch (e: Exception) {
            password
        }
    }
}