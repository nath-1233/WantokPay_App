package com.example.wantokpay.security

import com.example.wantokpay.data.AuthChallenge
import com.example.wantokpay.data.AuthSession
import com.example.wantokpay.data.BankAccountVerification
import com.example.wantokpay.data.BankInstitution
import com.example.wantokpay.data.BankVerificationStatus
import com.example.wantokpay.data.LoginCredentials
import com.example.wantokpay.data.PasswordResetRequest
import com.example.wantokpay.data.RegistrationRequest
import com.example.wantokpay.data.UserAccountStatus
import com.example.wantokpay.data.UserProfile
import com.example.wantokpay.data.UserRole
import com.example.wantokpay.data.VerificationTier
import com.example.wantokpay.data.formatPhoneNumber
import com.example.wantokpay.data.maskBankAccountNumber
import com.example.wantokpay.data.normalizeLoginIdentifier
import de.mkammerer.argon2.Argon2Factory
import java.security.SecureRandom
import java.util.Locale

class WantokPayAuthService {
    private val argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id)
    private val secureRandom = SecureRandom()

    private val users = linkedMapOf<String, UserProfile>()
    private val passwordHashes = linkedMapOf<String, String>()
    private val challenges = linkedMapOf<String, AuthChallenge>()
    private val resetCodes = linkedMapOf<String, PasswordResetRequest>()
    private val sessions = linkedMapOf<String, AuthSession>()

    init {
        registerBootstrapUser()
    }

    fun getSupportedBanks(): List<BankInstitution> = BankInstitution.values().toList()

    fun registerUser(request: RegistrationRequest): Result<UserProfile> {
        val fullName = request.fullName.trim()
        val email = request.email.trim()
        val phone = request.phoneNumber.trim()

        if (fullName.isEmpty()) return Result.failure(IllegalArgumentException("Full name is required."))
        if (!email.contains("@") || !email.contains(".")) return Result.failure(IllegalArgumentException("A valid email address is required."))
        if (phone.isBlank()) return Result.failure(IllegalArgumentException("A phone number is required."))
        if (!request.acceptedTerms) return Result.failure(IllegalArgumentException("You must accept the terms and conditions."))
        if (request.password != request.confirmPassword) return Result.failure(IllegalArgumentException("Passwords do not match."))
        if (!isStrongPassword(request.password)) return Result.failure(IllegalArgumentException("Password is too weak. Use at least 12 characters, including numbers and symbols."))

        val normalizedEmail = normalizeLoginIdentifier(email)
        val normalizedPhone = normalizePhoneNumber(phone)
        if (users.values.any { normalizeLoginIdentifier(it.email) == normalizedEmail || normalizePhoneNumber(it.phoneNumber) == normalizedPhone }) {
            return Result.failure(IllegalArgumentException("An account already exists for that email address or phone number."))
        }

        val user = UserProfile(
            fullName = fullName,
            phoneNumber = formatPhoneNumber(phone),
            email = email,
            role = request.role,
            verificationTier = request.verificationTier,
            accountStatus = UserAccountStatus.ACTIVE,
            bankVerificationStatus = BankVerificationStatus.PENDING,
            isTwoFactorEnabled = true
        )

        users[user.id] = user
        passwordHashes[user.id] = hashPassword(request.password)
        return Result.success(user)
    }

    fun login(credentials: LoginCredentials): Result<AuthChallenge> {
        val user = findUserByIdentifier(credentials.identifier)
            ?: return Result.failure(IllegalArgumentException("Invalid credentials."))

        val storedHash = passwordHashes[user.id]
            ?: return Result.failure(IllegalArgumentException("Invalid credentials."))

        if (!argon2.verify(storedHash, credentials.password.toCharArray())) {
            return Result.failure(IllegalArgumentException("Invalid credentials."))
        }

        if (user.accountStatus != UserAccountStatus.ACTIVE) {
            return Result.failure(IllegalStateException("Your account is ${user.accountStatus.name.lowercase(Locale.US)} and cannot login."))
        }

        val otp = generateOtp()
        val issuedAt = System.currentTimeMillis()
        val challenge = AuthChallenge(
            userId = user.id,
            otpHash = hashPassword(otp),
            issuedAtEpochMs = issuedAt,
            expiresAtEpochMs = issuedAt + (5 * 60 * 1000L),
            communicationChannel = if (user.email.contains("@")) "EMAIL" else "SMS",
            isPasswordReset = false
        )

        challenges[challenge.challengeId] = challenge
        return Result.success(challenge)
    }

    fun verifyOtp(challengeId: String, otp: String): Result<AuthSession> {
        val challenge = challenges[challengeId]
            ?: return Result.failure(IllegalArgumentException("The authentication challenge is invalid or expired."))

        if (System.currentTimeMillis() > challenge.expiresAtEpochMs) {
            challenges.remove(challengeId)
            return Result.failure(IllegalArgumentException("The OTP has expired. Please request a new one."))
        }

        if (challenge.attemptCount >= challenge.maximumAttempts) {
            challenges.remove(challengeId)
            return Result.failure(IllegalArgumentException("Too many failed OTP attempts. Please request a new code."))
        }

        if (!argon2.verify(challenge.otpHash, otp.toCharArray())) {
            challenge.attemptCount += 1
            return Result.failure(IllegalArgumentException("The OTP is incorrect. ${challenge.maximumAttempts - challenge.attemptCount} attempts remaining."))
        }

        challenges.remove(challengeId)
        val user = users[challenge.userId]
            ?: return Result.failure(IllegalStateException("User profile no longer exists."))

        val utcNow = System.currentTimeMillis()
        val session = AuthSession(
            userId = user.id,
            deviceName = "WantokPay Android",
            ipAddress = "127.0.0.1",
            isRemembered = false,
            createdAtEpochMs = utcNow,
            expiresAtEpochMs = utcNow + (30 * 60 * 1000L)
        )

        sessions[session.sessionId] = session
        return Result.success(session)
    }

    fun resendOtp(userId: String): Result<AuthChallenge> {
        val user = users[userId] ?: return Result.failure(IllegalArgumentException("User not found."))
        val otp = generateOtp()
        val issuedAt = System.currentTimeMillis()
        val challenge = AuthChallenge(
            userId = user.id,
            otpHash = hashPassword(otp),
            issuedAtEpochMs = issuedAt,
            expiresAtEpochMs = issuedAt + (5 * 60 * 1000L),
            communicationChannel = if (user.email.contains("@")) "EMAIL" else "SMS",
            isPasswordReset = false
        )
        challenges[challenge.challengeId] = challenge
        return Result.success(challenge)
    }

    fun forgotPassword(identifier: String): Result<String> {
        val user = findUserByIdentifier(identifier)
            ?: return Result.failure(IllegalArgumentException("No account matches that phone number or email address."))

        val resetCode = generateOtp()
        val normalizedKey = normalizeLoginIdentifier(identifier)
        resetCodes[normalizedKey] = PasswordResetRequest(
            userId = user.id,
            code = hashPassword(resetCode),
            expiresAtEpochMs = System.currentTimeMillis() + (10 * 60 * 1000L)
        )
        return Result.success(resetCode)
    }

    fun resetPassword(identifier: String, resetCode: String, newPassword: String): Result<UserProfile> {
        val user = findUserByIdentifier(identifier)
            ?: return Result.failure(IllegalArgumentException("No account matches that phone number or email address."))

        val normalizedKey = normalizeLoginIdentifier(identifier)
        val storedRequest = resetCodes[normalizedKey]
            ?: return Result.failure(IllegalArgumentException("No active password reset code found."))

        if (System.currentTimeMillis() > storedRequest.expiresAtEpochMs) {
            resetCodes.remove(normalizedKey)
            return Result.failure(IllegalArgumentException("The password reset code has expired."))
        }

        if (!argon2.verify(storedRequest.code, resetCode.toCharArray())) {
            return Result.failure(IllegalArgumentException("The reset code is invalid."))
        }

        if (!isStrongPassword(newPassword)) {
            return Result.failure(IllegalArgumentException("Password is too weak. Use at least 12 characters, including numbers and symbols."))
        }

        passwordHashes[user.id] = hashPassword(newPassword)
        resetCodes.remove(normalizedKey)
        return Result.success(user)
    }

    fun verifyBankAccount(
        userId: String,
        institution: BankInstitution,
        accountNumber: String,
        consentGiven: Boolean = true
    ): Result<BankAccountVerification> {
        val user = users[userId] ?: return Result.failure(IllegalArgumentException("User not found."))
        val cleanedNumber = accountNumber.trim()
        if (cleanedNumber.isBlank()) return Result.failure(IllegalArgumentException("A bank account number is required."))
        if (!consentGiven) return Result.failure(IllegalArgumentException("Customer consent is required before bank verification."))

        val resultStatus = if (institution.supportsVerifiedIntegration) BankVerificationStatus.VERIFIED else BankVerificationStatus.PENDING
        val verification = BankAccountVerification(
            userId = user.id,
            institution = institution,
            accountNumber = cleanedNumber,
            maskedAccountNumber = maskBankAccountNumber(cleanedNumber),
            status = resultStatus,
            consentGiven = consentGiven
        )

        user.bankInstitution = institution
        user.bankAccountReference = verification.maskedAccountNumber
        user.bankVerificationStatus = resultStatus
        if (user.verificationTier.ordinal < VerificationTier.TIER_1.ordinal) {
            user.verificationTier = VerificationTier.TIER_1
        }

        return Result.success(verification)
    }

    fun logoutSession(sessionId: String): Boolean = sessions.remove(sessionId) != null

    fun logoutAllSessions(userId: String): Int {
        val count = sessions.values.count { it.userId == userId }
        sessions.entries.removeIf { it.value.userId == userId }
        return count
    }

    fun getSession(sessionId: String): AuthSession? = sessions[sessionId]

    fun getUserByIdentifier(identifier: String): UserProfile? = findUserByIdentifier(identifier)

    private fun registerBootstrapUser() {
        val demoUser = UserProfile(
            fullName = "Samuel Karani",
            phoneNumber = "+675 7123 4567",
            email = "samuel.karani@wantokpay.com.pg",
            role = UserRole.INDIVIDUAL,
            verificationTier = VerificationTier.TIER_2,
            accountStatus = UserAccountStatus.ACTIVE,
            bankVerificationStatus = BankVerificationStatus.PENDING,
            isTwoFactorEnabled = true
        )

        users[demoUser.id] = demoUser
        passwordHashes[demoUser.id] = hashPassword("StrongPass#2025")
    }

    private fun findUserByIdentifier(identifier: String): UserProfile? {
        val candidate = normalizeLoginIdentifier(identifier)
        return users.values.firstOrNull { user ->
            normalizeLoginIdentifier(user.email) == candidate ||
                normalizePhoneNumber(user.phoneNumber) == normalizePhoneNumber(identifier)
        }
    }

    private fun generateOtp(): String {
        val value = secureRandom.nextInt(1000000)
        return String.format(Locale.US, "%06d", value)
    }

    private fun hashPassword(password: String): String = argon2.hash(10, 65536, 3, password.toCharArray())

    private fun isStrongPassword(password: String): Boolean {
        if (password.length < 12) return false
        if (!password.any { it.isDigit() }) return false
        if (!password.any { it.isLetter() }) return false
        if (!password.any { !it.isLetterOrDigit() }) return false
        return true
    }

    private fun normalizePhoneNumber(phone: String): String = phone.filter { it.isDigit() || it == '+' }.trim()
}
