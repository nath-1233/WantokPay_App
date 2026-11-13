package com.example.wantokpay.repository

import com.example.wantokpay.data.*
import java.text.SimpleDateFormat
import java.util.*

object WantokPayRepository {

    // Current logged in user profile
    var currentUser: UserProfile = UserProfile(
        fullName = "Samuel Karani",
        phoneNumber = "+675 7123 4567",
        email = "samuel.karani@wantokpay.com.pg",
        role = UserRole.INDIVIDUAL,
        verificationTier = VerificationTier.TIER_2
    )

    // Main wallet balances (Double entry ledger total calculation)
    private var primaryWalletBalance: Double = 1450.75
    private var merchantWalletBalance: Double = 8920.00

    fun getWalletBalance(): Double = primaryWalletBalance
    fun getMerchantBalance(): Double = merchantWalletBalance

    // Transactions History List
    private val transactionsList = mutableListOf<TransactionRecord>()

    // Merchant Profiles
    private val merchantsList = mutableListOf<MerchantProfile>()

    // Community Groups
    private val communityGroupsList = mutableListOf<CommunityGroup>()

    // Agent Nodes
    private val agentsList = mutableListOf<AgentNode>()

    // Billers
    val billersList = listOf(
        BillerItem("B001", "PNG Power Ltd (EasyPay)", "Electricity", "Meter Number", "0714-8892-1209"),
        BillerItem("B002", "Water PNG / Eda Ranu", "Water Utility", "Account Number", "WP-WATER-9921"),
        BillerItem("B003", "Digicel PNG", "Mobile & Data", "Phone Number", "+675 7200 1122"),
        BillerItem("B004", "Telikom PNG / Bmobile", "Landline & Broadband", "Account ID", "TLK-771239"),
        BillerItem("B005", "PNG Air / Air Niugini", "Travel Payments", "Booking Reference", "PNG-FLY-882")
    )

    // Scheduled Payments
    val scheduledPayments = mutableListOf<ScheduledPayment>()

    // Business Plus Records
    val inventoryItems = mutableListOf<InventoryItem>()
    val businessInvoices = mutableListOf<BusinessInvoice>()
    val bookkeepingRecords = mutableListOf<BookkeepingRecord>()

    // Disputes
    val disputeTickets = mutableListOf<DisputeTicket>()

    // 47 Platform Modules Audit Info
    val platformModulesList = listOf(
        SystemModuleInfo(1, "A. Customer Wallet", "1. Registration and Account"),
        SystemModuleInfo(2, "A. Customer Wallet", "2. Wallet Dashboard"),
        SystemModuleInfo(3, "A. Customer Wallet", "3. Money Transfers"),
        SystemModuleInfo(4, "A. Customer Wallet", "4. Wallet Management"),
        SystemModuleInfo(5, "A. Customer Wallet", "5. Payments"),
        SystemModuleInfo(6, "B. Merchant", "6. Merchant Registration"),
        SystemModuleInfo(7, "B. Merchant", "7. Merchant Payment Acceptance"),
        SystemModuleInfo(8, "B. Merchant", "8. Sales and Business Management"),
        SystemModuleInfo(9, "B. Merchant", "9. Merchant Staff"),
        SystemModuleInfo(10, "B. Merchant", "10. Merchant Settlement"),
        SystemModuleInfo(11, "C. Community", "11. Group Accounts and Collections"),
        SystemModuleInfo(12, "C. Community", "12. Group Transparency"),
        SystemModuleInfo(13, "C. Community", "13. Fundraising"),
        SystemModuleInfo(14, "C. Community", "14. Family and Community Requests"),
        SystemModuleInfo(15, "D. Agent Network", "15. Agent Onboarding"),
        SystemModuleInfo(16, "D. Agent Network", "16. Agent Operations"),
        SystemModuleInfo(17, "D. Agent Network", "17. Agent Locator"),
        SystemModuleInfo(18, "E. Bills & Services", "18. Airtime and Data"),
        SystemModuleInfo(19, "E. Bills & Services", "19. Utility and Service Payments"),
        SystemModuleInfo(20, "E. Bills & Services", "20. Payment Scheduling"),
        SystemModuleInfo(21, "F. Business Plus", "21. Inventory and Purchasing"),
        SystemModuleInfo(22, "F. Business Plus", "22. Bookkeeping"),
        SystemModuleInfo(23, "F. Business Plus", "23. Business Analytics"),
        SystemModuleInfo(24, "F. Business Plus", "24. Business Invoices"),
        SystemModuleInfo(25, "G. Trust & Security", "25. Identity and Verification"),
        SystemModuleInfo(26, "G. Trust & Security", "26. Fraud Prevention"),
        SystemModuleInfo(27, "G. Trust & Security", "27. Disputes and Complaints"),
        SystemModuleInfo(28, "G. Trust & Security", "28. Security Controls"),
        SystemModuleInfo(29, "H. Administration", "29. Customer Administration"),
        SystemModuleInfo(30, "H. Administration", "30. Merchant and Agent Administration"),
        SystemModuleInfo(31, "H. Administration", "31. Financial Operations"),
        SystemModuleInfo(32, "H. Administration", "32. Risk and Compliance"),
        SystemModuleInfo(33, "H. Administration", "33. Customer Service"),
        SystemModuleInfo(34, "I. Platform Infra", "34. Payment Gateway"),
        SystemModuleInfo(35, "I. Platform Infra", "35. Provider Integration Hub"),
        SystemModuleInfo(36, "I. Platform Infra", "36. Interoperability Layer"),
        SystemModuleInfo(37, "I. Platform Infra", "37. Notification Engine"),
        SystemModuleInfo(38, "I. Platform Infra", "38. Reconciliation Engine"),
        SystemModuleInfo(39, "I. Platform Infra", "39. Fees and Commissions Engine"),
        SystemModuleInfo(40, "I. Platform Infra", "40. Offline Business Records"),
        SystemModuleInfo(41, "I. Platform Infra", "41. Reporting and Analytics"),
        SystemModuleInfo(42, "I. Platform Infra", "42. Developer API Portal"),
        SystemModuleInfo(43, "I. Platform Infra", "43. Data Privacy Management"),
        SystemModuleInfo(44, "I. Platform Infra", "44. Business Continuity"),
        SystemModuleInfo(45, "I. Platform Infra", "45. Multi-language and Accessibility"),
        SystemModuleInfo(46, "I. Platform Infra", "46. USSD/SMS Access"),
        SystemModuleInfo(47, "I. Platform Infra", "47. Partner Settlement and Treasury Operations")
    )

    init {
        seedInitialData()
    }

    private fun seedInitialData() {
        // Seed initial transactions
        transactionsList.addAll(
            listOf(
                TransactionRecord(
                    type = TransactionType.TRANSFER,
                    amountKina = 250.00,
                    feeKina = 0.50,
                    senderName = "Samuel Karani",
                    senderAccount = "+675 7123 4567",
                    receiverName = "Grace Kaupa",
                    receiverAccount = "+675 7288 3491",
                    description = "Family Support Transfer",
                    ledgerEntries = listOf(
                        LedgerEntry(accountName = "Customer Wallet Ledger", debitAmount = 250.50, description = "Transfer Debit"),
                        LedgerEntry(accountName = "Recipient Wallet Ledger", creditAmount = 250.00, description = "Transfer Credit"),
                        LedgerEntry(accountName = "WantokPay Fee Income Account", creditAmount = 0.50, description = "System Fee")
                    )
                ),
                TransactionRecord(
                    type = TransactionType.BILL_PAYMENT,
                    amountKina = 50.00,
                    feeKina = 0.00,
                    senderName = "Samuel Karani",
                    senderAccount = "+675 7123 4567",
                    receiverName = "PNG Power Ltd",
                    receiverAccount = "Meter 0714-8892",
                    description = "EasyPay Token Purchase",
                    ledgerEntries = listOf(
                        LedgerEntry(accountName = "Customer Wallet Ledger", debitAmount = 50.00, description = "Bill Payment"),
                        LedgerEntry(accountName = "PNG Power Settlement Escrow", creditAmount = 50.00, description = "Biller Clearing")
                    )
                ),
                TransactionRecord(
                    type = TransactionType.MERCHANT_PAYMENT,
                    amountKina = 120.00,
                    feeKina = 0.00,
                    senderName = "Samuel Karani",
                    senderAccount = "+675 7123 4567",
                    receiverName = "Boroko Fresh Produce",
                    receiverAccount = "MCH-8821",
                    description = "Grocery QR Payment",
                    ledgerEntries = listOf(
                        LedgerEntry(accountName = "Customer Wallet Ledger", debitAmount = 120.00, description = "Merchant Checkout"),
                        LedgerEntry(accountName = "Merchant Ledger", creditAmount = 120.00, description = "Merchant Revenue")
                    )
                ),
                TransactionRecord(
                    type = TransactionType.CASH_IN,
                    amountKina = 500.00,
                    feeKina = 0.00,
                    senderName = "Lae City Super Agent",
                    senderAccount = "AGT-9011",
                    receiverName = "Samuel Karani",
                    receiverAccount = "+675 7123 4567",
                    description = "Agent Cash Deposit",
                    ledgerEntries = listOf(
                        LedgerEntry(accountName = "Agent Float Ledger", debitAmount = 500.00, description = "Agent Cash Outflow"),
                        LedgerEntry(accountName = "Customer Wallet Ledger", creditAmount = 500.00, description = "Wallet Top Up")
                    )
                )
            )
        )

        // Seed merchants
        merchantsList.addAll(
            listOf(
                MerchantProfile(
                    businessName = "Koki Market Wholesale & Retail",
                    category = "Groceries & Food",
                    ownerName = "Samuel Karani",
                    qrCodePayload = "WANTOKPAY:MCH:KOKI-8812:PGK",
                    dailySalesKina = 1450.00,
                    pendingSettlementKina = 3200.00
                ),
                MerchantProfile(
                    businessName = "Waigani Tech & Electronics",
                    category = "Electronics & IT",
                    ownerName = "Peter Namaliu",
                    qrCodePayload = "WANTOKPAY:MCH:WAIGANI-9902:PGK",
                    dailySalesKina = 4800.00,
                    pendingSettlementKina = 9500.00
                )
            )
        )

        // Seed Community Groups
        communityGroupsList.addAll(
            listOf(
                CommunityGroup(
                    groupName = "Sogeri Primary School Fee Fund",
                    category = "Education & School Fees",
                    adminName = "Mary Kuman",
                    targetAmountKina = 10000.00,
                    currentBalanceKina = 6450.00,
                    contributorCount = 28
                ),
                CommunityGroup(
                    groupName = "Hanuabada Community Water Tank Drive",
                    category = "Village Infrastructure",
                    adminName = "John Tabar",
                    targetAmountKina = 5000.00,
                    currentBalanceKina = 3800.00,
                    contributorCount = 42
                ),
                CommunityGroup(
                    groupName = "Wabag Emergency Relief Support",
                    category = "Family & Community Request",
                    adminName = "David Yari",
                    targetAmountKina = 3000.00,
                    currentBalanceKina = 1950.00,
                    contributorCount = 19
                )
            )
        )

        // Seed Agents
        agentsList.addAll(
            listOf(
                AgentNode("AGT-101", "Vision City Kiosk Agent", "Waigani, Port Moresby", "NCD", 15000.00, 8500.00),
                AgentNode("AGT-102", "Lae Top Town Pharmacy Agent", "Top Town, Lae", "Morobe", 22000.00, 11000.00),
                AgentNode("AGT-103", "Hagen Central Trade Agent", "Main Market, Mount Hagen", "Western Highlands", 9500.00, 4300.00),
                AgentNode("AGT-104", "Kokopo Beachfront Agent", "Main Street, Kokopo", "East New Britain", 12000.00, 6700.00)
            )
        )

        // Seed Scheduled Payments
        scheduledPayments.add(
            ScheduledPayment(
                billerName = "PNG Power Ltd (EasyPay)",
                accountNumber = "0714-8892-1209",
                amountKina = 100.00,
                frequency = "Monthly",
                nextDueDate = "1st of next month",
                isAutoPayEnabled = true
            )
        )

        // Seed Business Plus items
        inventoryItems.addAll(
            listOf(
                InventoryItem(name = "Solar Lamp Kit", unitPriceKina = 85.00, stockQuantity = 35, category = "Hardware"),
                InventoryItem(name = "5kg Rice Bag", unitPriceKina = 24.50, stockQuantity = 120, category = "Foodstuff"),
                InventoryItem(name = "Tinned Fish Pack", unitPriceKina = 18.00, stockQuantity = 90, category = "Foodstuff")
            )
        )

        businessInvoices.add(
            BusinessInvoice(customerName = "Pacific Trade Co.", amountKina = 1250.00, dueDate = "2025-04-15", isPaid = false)
        )

        bookkeepingRecords.addAll(
            listOf(
                BookkeepingRecord(type = "INCOME", category = "Retail Sales", amountKina = 850.00, date = "Today", note = "Counter Cash & QR"),
                BookkeepingRecord(type = "EXPENSE", category = "Stock Restock", amountKina = 300.00, date = "Yesterday", note = "Wholesale Wharehouse")
            )
        )

        disputeTickets.add(
            DisputeTicket(transactionRef = "WP882190", reason = "Duplicate payment debit at store", status = "RESOLVED", createdAt = "2025-03-20")
        )
    }

    fun getTransactions(): List<TransactionRecord> = transactionsList.toList()
    fun getMerchants(): List<MerchantProfile> = merchantsList.toList()
    fun getCommunityGroups(): List<CommunityGroup> = communityGroupsList.toList()
    fun getAgents(): List<AgentNode> = agentsList.toList()

    // Execute Money Transfer (Double-entry accounting, limit check, fee calculation)
    fun processTransfer(
        recipientPhoneOrWallet: String,
        recipientName: String,
        amountKina: Double,
        note: String
    ): Result<TransactionRecord> {
        if (amountKina <= 0) return Result.failure(Exception("Amount must be greater than K0.00"))
        
        val totalDebit = amountKina + 0.50
        if (totalDebit > primaryWalletBalance) {
            return Result.failure(Exception("Insufficient wallet balance. Total required: K${String.format(Locale.US, "%.2f", totalDebit)} (includes K0.50 system fee)"))
        }

        if (amountKina > currentUser.verificationTier.dailyLimitKina) {
            return Result.failure(Exception("Transfer exceeds your daily limit of K${currentUser.verificationTier.dailyLimitKina} for ${currentUser.verificationTier.label}"))
        }

        primaryWalletBalance -= totalDebit

        val tx = TransactionRecord(
            type = TransactionType.TRANSFER,
            amountKina = amountKina,
            feeKina = 0.50,
            senderName = currentUser.fullName,
            senderAccount = currentUser.phoneNumber,
            receiverName = recipientName.ifEmpty { recipientPhoneOrWallet },
            receiverAccount = recipientPhoneOrWallet,
            description = note.ifEmpty { "Wallet Money Transfer" },
            ledgerEntries = listOf(
                LedgerEntry(accountName = "Sender Wallet Account", debitAmount = totalDebit, description = "Funds Transfer & Fee Debit"),
                LedgerEntry(accountName = "Receiver Wallet Account", creditAmount = amountKina, description = "Recipient Wallet Credit"),
                LedgerEntry(accountName = "WantokPay System Fee Account", creditAmount = 0.50, description = "Transaction Processing Fee")
            )
        )

        transactionsList.add(0, tx)
        return Result.success(tx)
    }

    // Process Merchant Payment
    fun processMerchantPayment(
        merchantCode: String,
        amountKina: Double
    ): Result<TransactionRecord> {
        if (amountKina <= 0) return Result.failure(Exception("Amount must be greater than K0.00"))
        if (amountKina > primaryWalletBalance) return Result.failure(Exception("Insufficient wallet funds."))

        primaryWalletBalance -= amountKina
        merchantWalletBalance += amountKina

        val tx = TransactionRecord(
            type = TransactionType.MERCHANT_PAYMENT,
            amountKina = amountKina,
            feeKina = 0.00,
            senderName = currentUser.fullName,
            senderAccount = currentUser.phoneNumber,
            receiverName = "Merchant Store ($merchantCode)",
            receiverAccount = merchantCode,
            description = "Store QR Payment",
            ledgerEntries = listOf(
                LedgerEntry(accountName = "Customer Wallet Ledger", debitAmount = amountKina, description = "Merchant Purchase Debit"),
                LedgerEntry(accountName = "Merchant Ledger Account", creditAmount = amountKina, description = "Merchant Store Revenue")
            )
        )

        transactionsList.add(0, tx)
        return Result.success(tx)
    }

    // Process Bill / Airtime Payment
    fun processBillPayment(
        billerName: String,
        accountOrMeter: String,
        amountKina: Double
    ): Result<TransactionRecord> {
        if (amountKina <= 0) return Result.failure(Exception("Amount must be greater than K0.00"))
        if (amountKina > primaryWalletBalance) return Result.failure(Exception("Insufficient wallet funds."))

        primaryWalletBalance -= amountKina

        val tx = TransactionRecord(
            type = TransactionType.BILL_PAYMENT,
            amountKina = amountKina,
            feeKina = 0.00,
            senderName = currentUser.fullName,
            senderAccount = currentUser.phoneNumber,
            receiverName = billerName,
            receiverAccount = accountOrMeter,
            description = "Utility / Airtime Payment",
            ledgerEntries = listOf(
                LedgerEntry(accountName = "Customer Wallet Ledger", debitAmount = amountKina, description = "Bill Settlement Debit"),
                LedgerEntry(accountName = "Biller Clearing Account", creditAmount = amountKina, description = "Biller Escrow Account")
            )
        )

        transactionsList.add(0, tx)
        return Result.success(tx)
    }

    // Process Cash-In / Cash-Out
    fun processAgentOperation(
        agentId: String,
        isCashIn: Boolean,
        amountKina: Double
    ): Result<TransactionRecord> {
        if (amountKina <= 0) return Result.failure(Exception("Amount must be greater than K0.00"))

        if (isCashIn) {
            primaryWalletBalance += amountKina
        } else {
            if (amountKina > primaryWalletBalance) return Result.failure(Exception("Insufficient funds for Cash-Out"))
            primaryWalletBalance -= amountKina
        }

        val tx = TransactionRecord(
            type = if (isCashIn) TransactionType.CASH_IN else TransactionType.CASH_OUT,
            amountKina = amountKina,
            feeKina = 0.00,
            senderName = if (isCashIn) "Agent ($agentId)" else currentUser.fullName,
            senderAccount = if (isCashIn) agentId else currentUser.phoneNumber,
            receiverName = if (isCashIn) currentUser.fullName else "Agent ($agentId)",
            receiverAccount = if (isCashIn) currentUser.phoneNumber else agentId,
            description = if (isCashIn) "Agent Cash Deposit" else "Agent Cash Withdrawal",
            ledgerEntries = listOf(
                LedgerEntry(accountName = "Agent Ledger", debitAmount = amountKina, description = "Agent Cash Transfer"),
                LedgerEntry(accountName = "Customer Wallet Ledger", creditAmount = amountKina, description = "Wallet Adjustment")
            )
        )

        transactionsList.add(0, tx)
        return Result.success(tx)
    }

    // Process Group Contribution
    fun processGroupContribution(
        groupId: String,
        amountKina: Double
    ): Result<TransactionRecord> {
        if (amountKina <= 0) return Result.failure(Exception("Amount must be greater than K0.00"))
        if (amountKina > primaryWalletBalance) return Result.failure(Exception("Insufficient wallet balance."))

        val group = communityGroupsList.find { it.groupId == groupId }
            ?: return Result.failure(Exception("Group not found."))

        primaryWalletBalance -= amountKina

        // Update group balance
        val index = communityGroupsList.indexOf(group)
        communityGroupsList[index] = group.copy(
            currentBalanceKina = group.currentBalanceKina + amountKina,
            contributorCount = group.contributorCount + 1
        )

        val tx = TransactionRecord(
            type = TransactionType.GROUP_COLLECTION,
            amountKina = amountKina,
            feeKina = 0.00,
            senderName = currentUser.fullName,
            senderAccount = currentUser.phoneNumber,
            receiverName = group.groupName,
            receiverAccount = group.groupId,
            description = "Community Group Contribution",
            ledgerEntries = listOf(
                LedgerEntry(accountName = "Customer Wallet Ledger", debitAmount = amountKina, description = "Group Contribution Debit"),
                LedgerEntry(accountName = "Community Group Fund Ledger", creditAmount = amountKina, description = "Transparent Group Credit")
            )
        )

        transactionsList.add(0, tx)
        return Result.success(tx)
    }

    // Add new inventory item
    fun addInventoryItem(name: String, price: Double, qty: Int, category: String) {
        inventoryItems.add(InventoryItem(name = name, unitPriceKina = price, stockQuantity = qty, category = category))
    }

    // Add new invoice
    fun addInvoice(customerName: String, amount: Double, dueDate: String) {
        businessInvoices.add(BusinessInvoice(customerName = customerName, amountKina = amount, dueDate = dueDate))
    }

    // Add dispute
    fun createDispute(txRef: String, reason: String): DisputeTicket {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val ticket = DisputeTicket(transactionRef = txRef, reason = reason, createdAt = dateFormat.format(Date()))
        disputeTickets.add(0, ticket)
        return ticket
    }
}
