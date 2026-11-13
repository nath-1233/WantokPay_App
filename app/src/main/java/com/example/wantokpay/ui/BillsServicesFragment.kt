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
import com.example.wantokpay.databinding.FragmentBillsServicesBinding
import com.example.wantokpay.repository.WantokPayRepository
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import java.util.Locale

class BillsServicesFragment : Fragment() {

    private var _binding: FragmentBillsServicesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBillsServicesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.etBillerProvider.setOnClickListener {
            showProviderSelectorDialog()
        }

        binding.btnPayBill.setOnClickListener {
            val provider = binding.etBillerProvider.text.toString().trim()
            val meter = binding.etMeterNumber.text.toString().trim()
            val amountStr = binding.etBillAmount.text.toString().trim()

            val amount = amountStr.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                binding.etBillAmount.error = "Invalid bill amount"
                return@setOnClickListener
            }

            val result = WantokPayRepository.processBillPayment(provider, meter, amount)
            result.fold(
                onSuccess = { tx ->
                    val token = (1000..9999).random().toString() + "-" + (1000..9999).random().toString() + "-" + (1000..9999).random().toString()
                    AlertDialog.Builder(requireContext())
                        .setTitle("Payment Successful! ⚡")
                        .setMessage("Biller: $provider\nMeter/Account: $meter\nAmount: PGK ${String.format(Locale.US, "%.2f", amount)}\n\nGENERATED EASYPAY TOKEN / RECEIPT:\n👉 $token 👈")
                        .setPositiveButton("Copy Token & Close", null)
                        .show()
                },
                onFailure = { err ->
                    Snackbar.make(requireView(), "Error: ${err.message}", Snackbar.LENGTH_LONG).show()
                }
            )
        }

        renderScheduledPayments()
    }

    private fun showProviderSelectorDialog() {
        val providers = WantokPayRepository.billersList.map { "${it.providerName} (${it.category})" }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle("Select Utility Provider")
            .setItems(providers) { _, which ->
                val selected = WantokPayRepository.billersList[which]
                binding.etBillerProvider.setText(selected.providerName)
                binding.etMeterNumber.setText(selected.sampleAccount)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun renderScheduledPayments() {
        val container = binding.layoutScheduledContainer
        container.removeAllViews()

        for (sch in WantokPayRepository.scheduledPayments) {
            val card = MaterialCardView(requireContext()).apply {
                radius = 12f * resources.displayMetrics.density
                elevation = 1f * resources.displayMetrics.density
                strokeWidth = 0
                setCardBackgroundColor(Color.WHITE)
                useCompatPadding = true
            }

            val layout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(24, 20, 24, 20)
            }

            val titleTv = TextView(requireContext()).apply {
                text = "${sch.billerName} • Account: ${sch.accountNumber}"
                setTextColor(Color.parseColor("#0D47A1"))
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            val detailsTv = TextView(requireContext()).apply {
                text = "Auto-Pay: PGK ${String.format(Locale.US, "%.2f", sch.amountKina)} (${sch.frequency}) • Next Due: ${sch.nextDueDate}"
                setTextColor(Color.parseColor("#6C757D"))
                textSize = 12f
            }

            layout.addView(titleTv)
            layout.addView(detailsTv)
            card.addView(layout)
            container.addView(card)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
