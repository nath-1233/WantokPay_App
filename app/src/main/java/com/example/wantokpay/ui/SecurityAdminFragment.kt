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
import com.example.wantokpay.data.VerificationTier
import com.example.wantokpay.databinding.FragmentSecurityAdminBinding
import com.example.wantokpay.repository.WantokPayRepository
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar

class SecurityAdminFragment : Fragment() {

    private var _binding: FragmentSecurityAdminBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSecurityAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateKycUI()

        binding.btnUpgradeKyc.setOnClickListener {
            WantokPayRepository.currentUser = WantokPayRepository.currentUser.copy(verificationTier = VerificationTier.TIER_3)
            updateKycUI()
            Snackbar.make(requireView(), "KYC Tier upgraded to Tier 3 Corporate (Limit: K100,000/day)", Snackbar.LENGTH_LONG).show()
        }

        binding.btnSubmitDispute.setOnClickListener {
            val ref = binding.etDisputeTxRef.text.toString().trim()
            val reason = binding.etDisputeReason.text.toString().trim()

            if (ref.isEmpty() || reason.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in dispute details", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val ticket = WantokPayRepository.createDispute(ref, reason)
            AlertDialog.Builder(requireContext())
                .setTitle("Dispute Ticket Submitted 🛡️")
                .setMessage("Ticket ID: ${ticket.ticketId}\nTransaction Ref: ${ticket.transactionRef}\nStatus: ${ticket.status}\n\nOur Risk & Compliance team will review within 24 hours.")
                .setPositiveButton("OK", null)
                .show()
        }

        render47Modules()
    }

    private fun updateKycUI() {
        val tier = WantokPayRepository.currentUser.verificationTier
        binding.tvKycStatus.text = "Current Status: ${tier.label} • Limit: K${String.format(java.util.Locale.US, "%.0f", tier.dailyLimitKina)}/day"
    }

    private fun render47Modules() {
        val container = binding.layoutModulesContainer
        container.removeAllViews()

        val modules = WantokPayRepository.platformModulesList

        for (mod in modules) {
            val card = MaterialCardView(requireContext()).apply {
                radius = 8f * resources.displayMetrics.density
                strokeWidth = 0
                setCardBackgroundColor(Color.parseColor("#FFFFFF"))
                useCompatPadding = true
            }

            val layout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(24, 16, 24, 16)
                gravity = android.view.Gravity.CENTER_VERTICAL
            }

            val textLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val titleTv = TextView(requireContext()).apply {
                text = "Module ${mod.id}: ${mod.moduleName}"
                setTextColor(Color.parseColor("#1A1D20"))
                textSize = 13f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            val catTv = TextView(requireContext()).apply {
                text = "Category: ${mod.component}"
                setTextColor(Color.parseColor("#6C757D"))
                textSize = 11f
            }

            textLayout.addView(titleTv)
            textLayout.addView(catTv)

            val statusTv = TextView(requireContext()).apply {
                text = mod.status
                setTextColor(Color.parseColor("#2E7D32"))
                textSize = 11f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            layout.addView(textLayout)
            layout.addView(statusTv)
            card.addView(layout)

            card.setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("Module ${mod.id}: ${mod.moduleName}")
                    .setMessage("Component Area: ${mod.component}\nStatus: ${mod.status}\n\nThis module is fully integrated into the WantokPay financial service architecture for Papua New Guinea.")
                    .setPositiveButton("OK", null)
                    .show()
            }

            container.addView(card)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
