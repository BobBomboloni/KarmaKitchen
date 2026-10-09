package com.example

import com.example.api.isBusyError
import com.example.api.isQuotaError
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiErrorTest {
    // Stands in for Firebase's exception of the same name, whose constructor is internal.
    private class QuotaExceededException(message: String) : Exception(message)

    @Test
    fun overloadedGeminiIsBusy() {
        assertTrue(isBusyError(Exception("The model is overloaded. Please try again later.")))
        assertTrue(isBusyError(Exception("HTTP 503 UNAVAILABLE")))
    }

    @Test
    fun setupProblemsAreNotBusy() {
        assertFalse(isBusyError(Exception("Firebase App Check token is invalid.")))
        assertFalse(isBusyError(Exception("models/gemini-x is not found for API version v1beta")))
        assertFalse(isQuotaError(Exception("Firebase App Check token is invalid.")))
    }

    @Test
    fun usedUpRequestsAreQuota() {
        assertTrue(isQuotaError(QuotaExceededException("You exceeded your current quota")))
        assertTrue(isQuotaError(Exception("429 RESOURCE_EXHAUSTED")))
        assertFalse(isBusyError(QuotaExceededException("You exceeded your current quota")))
    }
}
