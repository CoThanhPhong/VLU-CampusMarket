package com.example.vlu_campusmarket

import com.example.vlu_campusmarket.auth.AuthValidator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testValidEmailWithDomain() {
        assertTrue(AuthValidator.isValidEmail("nguyen.a@vanlanguni.vn"))
    }

    @Test
    fun testInvalidEmailWrongDomain() {
        assertFalse(AuthValidator.isValidEmail("nguyen.a@gmail.com"))
    }

    @Test
    fun testInvalidEmailFormat() {
        assertFalse(AuthValidator.isValidEmail("not-an-email"))
        assertFalse(AuthValidator.isValidEmail(""))
        assertFalse(AuthValidator.isValidEmail(null))
    }

    @Test
    fun testValidPassword() {
        assertTrue(AuthValidator.isValidPassword("123456"))
        assertTrue(AuthValidator.isValidPassword("password123"))
    }

    @Test
    fun testInvalidPassword() {
        assertFalse(AuthValidator.isValidPassword("12345"))
        assertFalse(AuthValidator.isValidPassword(""))
        assertFalse(AuthValidator.isValidPassword(null))
    }
}
