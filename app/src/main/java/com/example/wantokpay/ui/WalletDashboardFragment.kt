package com.example.wantokpay.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.wantokpay.R
import com.example.wantokpay.data.TransactionRecord
import com.example.wantokpay.data.UserRole
import com.example.wantokpay.databinding.FragmentWalletDashboardBinding
import com.example.wantokpay.repository.WantokPayRepository
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*

class WalletDashboardFragment : Fragment() {

    private var _binding: FragmentWalletDashboardBinding? = null
    private val binding get() = _binding!!

    private var isBalanceVisible = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWalletDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateHeaderUI()
        updateBalanceDisplay()
        renderTransactions()

        // Toggle balance visibility
        binding.btnToggleBalance.setOnClickListener {
            isBalanceVisible = !isBalanceVisible
            updateBalanceDisplay()
        }

        // Role Switcher
        binding.btnSwitchRole.setOnClickListener {
            showRoleSwitcherDialog()
        }

        // Quick Actions
        binding.btnActionSend.setOnClickListener {
            findNavController().navigate(R.id.transfersFragment)
        }

        binding.btnActionQr.setOnClickListener {
            showQrScannerDialog()
        }

        binding.btnActionCashin.setOnClickListener {
            findNavController().navigate(R.id.agentNetworkFragment)
        }

        binding.btnActionCashout.setOnClickListener {
            findNavController().navigate(R.id.agentNetworkFragment)
        }

        binding.cardServiceBills.setOnClickListener {
            findNavController().navigate(R.id.billsServicesFragment)
        }

        binding.cardServiceCommunity.setOnClickListener {
            findNavController().navigate(R.id.communityFragment)
        }

        binding.tvViewAllTx.setOnClickListener {
            Snackbar.make(requireView(), "Showing all double-entry ledger records.", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun updateHeaderUI() {
        val user = WantokPayRepository.currentUser
        binding.tvUserName.text = user.fullName
        binding.tvVerificationBadge.text = "Verified ${user.verificationTier.name} (K${String.format(Locale.US, "%.0f", user.verificationTier.dailyLimitKina)}/day limit)"
        binding.btnSwitchRole.text = "Role: ${user.role.displayName}"
    }

    private fun updateBalanceDisplay() {
        if (isBalanceVisible) {
            val balance = WantokPayRepository.getWalletBalance()
            binding.tvWalletBalance.text = "PGK ${String.format(Locale.US, "%.2f", balance)}"
            binding.btnToggleBalance.setImageResource(android.R.drawable.ic_menu_view)
        } else {
            binding.tvWalletBalance.text = "PGK ••••••"
            binding.btnToggleBalance.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
        }
    }

    private fun showRoleSwitcherDialog() {
        val roles = UserRole.values()
        val roleNames = roles.map { it.displayName }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Switch Active Role")
            .setItems(roleNames) { _, which ->
                val selectedRole = roles[which]
                WantokPayRepository.currentUser = WantokPayRepository.currentUser.copy(role = selectedRole)
                updateHeaderUI()
                Toast.makeText(requireContext(), "Role updated to: ${selectedRole.displayName}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showQrScannerDialog() {
        val dialogView = layoutInflater.inflate(R.layout.fragment_transfers, null)
        AlertDialog.Builder(requireContext())
            .setTitle("Scan WantokPay Merchant QR")
            .setMessage("Simulating QR Camera Scan...\nMerchant Detected: Boroko Fresh Produce (MCH-8821)")
            .setPositiveButton("Pay K50.00") { _, _ ->
                val result = WantokPayRepository.processMerchantPayment("MCH-8821", 50.00)
                result.fold(
                    onSuccess = { tx ->
                        updateBalanceDisplay()
                        renderTransactions()
                        Snackbar.make(requireView(), "Merchant Paid PGK 50.00! Ref: ${tx.referenceNumber}", Snackbar.LENGTH_LONG).show()
                    },
                    onFailure = { err ->
                        Toast.makeText(requireContext(), err.message, Toast.LENGTH_LONG).show()
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun renderTransactions() {
        val container = binding.layoutTransactionsContainer
        container.removeAllViews()

        val transactions = WantokPayRepository.getTransactions()
        val dateFormat = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.US)

        for (tx in transactions) {
            val card = MaterialCardView(requireContext()).apply {
                radius = 12f * resources.displayMetrics.density
                cardElevation = 1f * resources.displayMetrics.density
                strokeWidth = 0
                setCardBackgroundColor(Color.WHITE)
                useCompatPadding = true
            }

            val layout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(32, 24, 32, 24)
                gravity = android.view.Gravity.CENTER_VERTICAL
            }

            val textLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val titleTv = TextView(requireContext()).apply {
                text = "${tx.type.label} - ${tx.receiverName}"
                setTextColor(Color.parseColor("#1A1D20"))
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            val subtitleTv = TextView(requireContext()).apply {
                text = "${dateFormat.format(Date(tx.timestamp))} • Ref: ${tx.referenceNumber}"
                setTextColor(Color.parseColor("#6C757D"))
                textSize = 12f
            }

            textLayout.addView(titleTv)
            textLayout.addView(subtitleTv)

            val amountLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.END
            }

            val amountTv = TextView(requireContext()).apply {
                text = "PGK ${String.format(Locale.US, "%.2f", tx.amountKina)}"
                setTextColor(Color.parseColor("#0D47A1"))
                textSize = 15f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            val statusTv = TextView(requireContext()).apply {
                text = tx.status.name
                setTextColor(Color.parseColor("#2E7D32"))
                textSize = 11f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            amountLayout.addView(amountTv)
            amountLayout.addView(statusTv)

            layout.addView(textLayout)
            layout.addView(amountLayout)
            card.addView(layout)

            card.setOnClickListener {
                showTransactionLedgerDialog(tx)
            }

            container.addView(card)
        }
    }

    private fun showTransactionLedgerDialog(tx: TransactionRecord) {
        val ledgerText = StringBuilder()
        ledgerText.append("Transaction Ref: ${tx.referenceNumber}\n")
        ledgerText.append("Type: ${tx.type.label}\n")
        ledgerText.append("Amount: PGK ${String.format(Locale.US, "%.2f", tx.amountKina)}\n")
        ledgerText.append("System Fee: PGK ${String.format(Locale.US, "%.2f", tx.feeKina)}\n")
        ledgerText.append("Status: ${tx.status.name}\n\n")
        ledgerText.append("--- DOUBLE-ENTRY LEDGER BREAKDOWN ---\n")
        tx.ledgerEntries.forEach { entry ->
            if (entry.debitAmount > 0) {
                ledgerText.append("• DEBIT: ${entry.accountName} -> K${String.format(Locale.US, "%.2f", entry.debitAmount)} (${entry.description})\n")
            }
            if (entry.creditAmount > 0) {
                ledgerText.append("• CREDIT: ${entry.accountName} -> K${String.format(Locale.US, "%.2f", entry.creditAmount)} (${entry.description})\n")
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Financial Audit & Ledger Trail")
            .setMessage(ledgerText.toString())
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
