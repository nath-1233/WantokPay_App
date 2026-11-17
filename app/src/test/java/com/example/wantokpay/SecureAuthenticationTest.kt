package com.example.wantokpay

import com.example.wantokpay.data.BankInstitution
import com.example.wantokpay.data.BankVerificationStatus
import com.example.wantokpay.data.LoginCredentials
import com.example.wantokpay.data.RegistrationRequest
import com.example.wantokpay.data.UserRole
import com.example.wantokpay.data.VerificationTier
import com.example.wantokpay.security.WantokPayAuthService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SecureAuthenticationTest {
    private val authService = WantokPayAuthService()

    @Test
    fun registerUserAndLoginFlowSucceeds() {
        val request = RegistrationRequest(
            fullName = "Jane Doe",
            phoneNumber = "+675 7000 1001",
            email = "jane.doe@example.com",
            password = "StrongPass#2025",
            confirmPassword = "StrongPass#2025",
            acceptedTerms = true,
            role = UserRole.INDIVIDUAL,
            verificationTier = VerificationTier.TIER_1
        )

        val user = authService.registerUser(request).getOrThrow()
        assertEquals("Jane Doe", user.fullName)

        val challengeResult = authService.login(LoginCredentials("jane.doe@example.com", "StrongPass#2025"))
        assertTrue(challengeResult.isSuccess)

        val challenge = challengeResult.getOrThrow()
        assertNotNull(challenge.challengeId)
        assertFalse(challenge.otpHash.isBlank())
    }

    @Test
    fun invalidOtpIsRejected() {
        val request = RegistrationRequest(
            fullName = "Mark Tester",
            phoneNumber = "+675 7000 2002",
            email = "mark.tester@example.com",
            password = "StrongPass#2026",
            confirmPassword = "StrongPass#2026",
            acceptedTerms = true,
            role = UserRole.INDIVIDUAL,
            verificationTier = VerificationTier.TIER_1
        )
        val user = authService.registerUser(request).getOrThrow()
        val challenge = authService.login(LoginCredentials(user.email, "StrongPass#2026")).getOrThrow()

        val failedOtp = authService.verifyOtp(challenge.challengeId, "000000")
        assertFalse(failedOtp.isSuccess)
    }

    @Test
    fun bankVerificationAndResetCodeWork() {
        val request = RegistrationRequest(
            fullName = "Bank User",
            phoneNumber = "+675 7000 3003",
            email = "bank.user@example.com",
            password = "StrongPass#2027",
            confirmPassword = "StrongPass#2027",
            acceptedTerms = true,
            role = UserRole.INDIVIDUAL,
            verificationTier = VerificationTier.TIER_1
        )
        val user = authService.registerUser(request).getOrThrow()

        val verification = authService.verifyBankAccount(user.id, BankInstitution.BSP, "1234567890", true)
        assertTrue(verification.isSuccess)
        assertEquals(BankVerificationStatus.VERIFIED, verification.getOrThrow().status)
        assertTrue(verification.getOrThrow().maskedAccountNumber.contains("1234") || verification.getOrThrow().maskedAccountNumber.contains("789"))

        val resetCode = authService.forgotPassword(user.email).getOrThrow()
        assertFalse(resetCode.isBlank())
        val newUser = authService.resetPassword(user.email, resetCode, "NewStrongPass#2028").getOrThrow()
        assertEquals(user.id, newUser.id)
    }
}
