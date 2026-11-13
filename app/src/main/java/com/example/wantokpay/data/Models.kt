package com.example.wantokpay.data

import java.util.UUID

enum class UserRole(val displayName: String) {
    INDIVIDUAL("Individual Customer"),
    MERCHANT_OWNER("Merchant Owner"),
    MERCHANT_MANAGER("Merchant Manager"),
    CASHIER("Cashier"),
    COMMUNITY_ADMIN("Community Group Admin"),
    GROUP_TREASURER("Group Treasurer"),
    AGENT("Agent"),
    CUSTOMER_SUPPORT("Customer Support Officer"),
    FINANCE_OFFICER("Finance Officer"),
    RISK_COMPLIANCE("Risk & Compliance Officer"),
    SYSTEM_ADMIN("System Administrator"),
    DEVELOPER("Third-Party Developer")
}

enum class VerificationTier(val label: String, val dailyLimitKina: Double) {
    TIER_0("Unverified", 200.0),
    TIER_1("Basic KYC (ID Document)", 2000.0),
    TIER_2("Enhanced KYC (NID + Proof of Address)", 10000.0),
    TIER_3("Full Corporate / Merchant Verification", 100000.0)
}

data class UserProfile(
    val id: String = UUID.randomUUID().toString(),
    val fullName: String,
    val phoneNumber: String,
    val email: String,
    val role: UserRole = UserRole.INDIVIDUAL,
    val verificationTier: VerificationTier = VerificationTier.TIER_1,
    val nidOrPassportNumber: String = "PNG-NID-8839210",
    val isBiometricEnabled: Boolean = true,
    val securityPinHash: String = "1234",
    val preferredLanguage: String = "English" // English or Tok Pisin
)

enum class TransactionType(val label: String) {
    TRANSFER("Wallet Transfer"),
    MERCHANT_PAYMENT("Merchant QR Payment"),
    BILL_PAYMENT("Utility / Bill Payment"),
    AIRTIME_TOPUP("Airtime & Data Purchase"),
    CASH_IN("Agent Cash In"),
    CASH_OUT("Agent Cash Out"),
    GROUP_COLLECTION("Community Group Contribution"),
    MERCHANT_SETTLEMENT("Merchant Payout Settlement"),
    FEE_CHARGE("System Fee")
}

enum class TransactionStatus {
    PENDING, SUCCESS, FAILED, REVERSED, REFUNDED
}

data class LedgerEntry(
    val entryId: String = UUID.randomUUID().toString(),
    val accountName: String,
    val debitAmount: Double = 0.0,
    val creditAmount: Double = 0.0,
    val description: String
)

data class TransactionRecord(
    val id: String = UUID.randomUUID().toString(),
    val referenceNumber: String = "WP" + (100000..999999).random(),
    val type: TransactionType,
    val amountKina: Double,
    val feeKina: Double = 0.50,
    val senderName: String,
    val senderAccount: String,
    val receiverName: String,
    val receiverAccount: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: TransactionStatus = TransactionStatus.SUCCESS,
    val description: String,
    val idempotencyKey: String = UUID.randomUUID().toString(),
    val ledgerEntries: List<LedgerEntry> = emptyList()
)

data class MerchantProfile(
    val merchantId: String = "MCH-" + (1000..9999).random(),
    val businessName: String,
    val category: String,
    val ownerName: String,
    val qrCodePayload: String,
    val dailySalesKina: Double = 0.0,
    val pendingSettlementKina: Double = 0.0,
    val staffList: List<String> = listOf("Lead Cashier", "Store Manager")
)

data class CommunityGroup(
    val groupId: String = "GRP-" + (100..999).random(),
    val groupName: String,
    val category: String, // School Fees, Church, Village Development, Family Request
    val adminName: String,
    val targetAmountKina: Double,
    val currentBalanceKina: Double,
    val contributorCount: Int,
    val isTransparentLedger: Boolean = true
)

data class AgentNode(
    val agentId: String = "AGT-" + (1000..9999).random(),
    val agentName: String,
    val locationName: String,
    val province: String,
    val floatBalanceKina: Double,
    val cashOnHandKina: Double,
    val isOpen: Boolean = true
)

data class BillerItem(
    val billerId: String,
    val providerName: String,
    val category: String, // Electricity, Water, Telecom, Internet
    val accountOrMeterNumberLabel: String,
    val sampleAccount: String
)

data class ScheduledPayment(
    val id: String = UUID.randomUUID().toString(),
    val billerName: String,
    val accountNumber: String,
    val amountKina: Double,
    val frequency: String, // Monthly, Weekly
    val nextDueDate: String,
    val isAutoPayEnabled: Boolean = true
)

data class InventoryItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val unitPriceKina: Double,
    val stockQuantity: Int,
    val category: String
)

data class BusinessInvoice(
    val invoiceId: String = "INV-" + (10000..99999).random(),
    val customerName: String,
    val amountKina: Double,
    val dueDate: String,
    val isPaid: Boolean = false
)

data class BookkeepingRecord(
    val id: String = UUID.randomUUID().toString(),
    val type: String, // INCOME or EXPENSE
    val category: String,
    val amountKina: Double,
    val date: String,
    val note: String
)

data class DisputeTicket(
    val ticketId: String = "DISP-" + (1000..9999).random(),
    val transactionRef: String,
    val reason: String,
    val status: String = "UNDER_REVIEW",
    val createdAt: String
)

data class SecuritySession(
    val deviceName: String,
    val ipAddress: String,
    val lastActive: String,
    val isCurrentDevice: Boolean
)

data class SystemModuleInfo(
    val id: Int,
    val component: String,
    val moduleName: String,
    val status: String = "ACTIVE & COMPLIANT"
)
