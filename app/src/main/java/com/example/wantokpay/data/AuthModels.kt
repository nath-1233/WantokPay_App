package com.example.wantokpay.data

import java.util.Locale
import java.util.UUID

enum class UserAccountStatus {
    ACTIVE,
    SUSPENDED,
    RESTRICTED
}

enum class BankVerificationStatus {
    VERIFIED,
    PENDING,
    REJECTED
}

enum class BankInstitution(
    val displayName: String,
    val code: String,
    val supportsVerifiedIntegration: Boolean = true
) {
    BSP("BSP Financial Group Limited (BSP)", "BSP", true),
    ANZ("ANZ Bank (PNG) Limited", "ANZ", true),
    WESTPAC("Westpac Bank (PNG) Limited", "WESTPAC", true),
    KINA_BANK("Kina Bank (Kina Securities Limited)", "KINA", true),
    TISA_BANK("TISA Bank Limited", "TISA", true),
    CREDITBANK_PNG("CreditBank PNG (Credit Corporation Finance Limited)", "CREDITBANK", false),
    NATIONAL_BANKING_CORPORATION("National Banking Corporation Limited (NBC)", "NBC", true)
}

data class RegistrationRequest(
    val fullName: String,
    val phoneNumber: String,
    val email: String,
    val password: String,
    val confirmPassword: String,
    val acceptedTerms: Boolean = false,
    val role: UserRole = UserRole.INDIVIDUAL,
    val verificationTier: VerificationTier = VerificationTier.TIER_1
)

data class LoginCredentials(
    val identifier: String,
    val password: String,
    val rememberMe: Boolean = false
)

data class AuthChallenge(
    val challengeId: String = UUID.randomUUID().toString(),
    val userId: String,
    val otpHash: String,
    val issuedAtEpochMs: Long = System.currentTimeMillis(),
    val expiresAtEpochMs: Long = issuedAtEpochMs + (5 * 60 * 1000L),
    val maximumAttempts: Int = 5,
    var attemptCount: Int = 0,
    val communicationChannel: String = "SMS",
    val isPasswordReset: Boolean = false
)

data class AuthSession(
    val sessionId: String = UUID.randomUUID().toString(),
    val userId: String,
    val deviceName: String,
    val ipAddress: String,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val expiresAtEpochMs: Long = createdAtEpochMs + (30 * 60 * 1000L),
    val isRemembered: Boolean = false,
    val lastAccessedAtEpochMs: Long = createdAtEpochMs
)

data class BankAccountVerification(
    val userId: String,
    val institution: BankInstitution,
    val accountNumber: String,
    val maskedAccountNumber: String,
    val status: BankVerificationStatus,
    val consentGiven: Boolean = true,
    val verifiedAtEpochMs: Long = System.currentTimeMillis()
)

data class PasswordResetRequest(
    val userId: String,
    val code: String,
    val expiresAtEpochMs: Long,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

fun formatPhoneNumber(raw: String): String {
    val digits = raw.filter { it.isDigit() }
    return if (digits.length >= 10) {
        val cleaned = digits.takeLast(8)
        "+675 $cleaned".trim()
    } else {
        raw.trim()
    }
}

fun normalizeLoginIdentifier(raw: String): String = raw.trim().lowercase(Locale.US)

fun maskBankAccountNumber(raw: String): String {
    val digitsOnly = raw.filter { it.isDigit() }
    if (digitsOnly.length <= 4) return "****"
    val visiblePrefix = digitsOnly.take(4)
    val visibleSuffix = digitsOnly.takeLast(3)
    return "$visiblePrefix******$visibleSuffix"
}
