package com.example.wantokpay.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.wantokpay.databinding.FragmentMerchantHubBinding
import com.example.wantokpay.repository.WantokPayRepository
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import java.util.Locale

class MerchantHubFragment : Fragment() {

    private var _binding: FragmentMerchantHubBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMerchantHubBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        renderMerchantDetails()
        renderInventory()
        renderInvoices()

        binding.btnRequestSettlement.setOnClickListener {
            val merchant = WantokPayRepository.getMerchants().firstOrNull()
            if (merchant != null && merchant.pendingSettlementKina > 0) {
                AlertDialog.Builder(requireContext())
                    .setTitle("Confirm Settlement Payout")
                    .setMessage("Transfer PGK ${String.format(Locale.US, "%.2f", merchant.pendingSettlementKina)} to registered BSP Bank account ending 9921?")
                    .setPositiveButton("Confirm Payout") { _, _ ->
                        Snackbar.make(requireView(), "Settlement Payout request submitted to Finance Operations! Reference: SETTL-9901", Snackbar.LENGTH_LONG).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                Toast.makeText(requireContext(), "No pending settlement balance available.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnAddInventory.setOnClickListener {
            showAddInventoryDialog()
        }

        binding.btnCreateInvoice.setOnClickListener {
            showCreateInvoiceDialog()
        }
    }

    private fun renderMerchantDetails() {
        val merchant = WantokPayRepository.getMerchants().firstOrNull() ?: return
        binding.tvMerchantName.text = merchant.businessName
        binding.tvDailySales.text = "PGK ${String.format(Locale.US, "%.2f", merchant.dailySalesKina)}"
        binding.tvPendingSettlement.text = "PGK ${String.format(Locale.US, "%.2f", merchant.pendingSettlementKina)}"
    }

    private fun renderInventory() {
        val container = binding.layoutInventoryList
        container.removeAllViews()

        for (item in WantokPayRepository.inventoryItems) {
            val card = MaterialCardView(requireContext()).apply {
                radius = 8f * resources.displayMetrics.density
                strokeWidth = 0
                setCardBackgroundColor(Color.parseColor("#F4F6F9"))
                useCompatPadding = true
            }

            val layout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(24, 16, 24, 16)
                gravity = android.view.Gravity.CENTER_VERTICAL
            }

            val textTv = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = "${item.name} (${item.category})"
                setTextColor(Color.parseColor("#1A1D20"))
                textSize = 13f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            val priceTv = TextView(requireContext()).apply {
                text = "K${String.format(Locale.US, "%.2f", item.unitPriceKina)} • Qty: ${item.stockQuantity}"
                setTextColor(Color.parseColor("#0D47A1"))
                textSize = 13f
            }

            layout.addView(textTv)
            layout.addView(priceTv)
            card.addView(layout)
            container.addView(card)
        }
    }

    private fun renderInvoices() {
        val container = binding.layoutInvoicesList
        container.removeAllViews()

        for (inv in WantokPayRepository.businessInvoices) {
            val card = MaterialCardView(requireContext()).apply {
                radius = 8f * resources.displayMetrics.density
                strokeWidth = 0
                setCardBackgroundColor(Color.parseColor("#F4F6F9"))
                useCompatPadding = true
            }

            val layout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(24, 16, 24, 16)
                gravity = android.view.Gravity.CENTER_VERTICAL
            }

            val textTv = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = "Invoice ${inv.invoiceId} - ${inv.customerName}"
                setTextColor(Color.parseColor("#1A1D20"))
                textSize = 13f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            val statusTv = TextView(requireContext()).apply {
                text = "K${String.format(Locale.US, "%.2f", inv.amountKina)} (${if (inv.isPaid) "PAID" else "UNPAID"})"
                setTextColor(if (inv.isPaid) Color.parseColor("#2E7D32") else Color.parseColor("#C62828"))
                textSize = 13f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            layout.addView(textTv)
            layout.addView(statusTv)
            card.addView(layout)
            container.addView(card)
        }
    }

    private fun showAddInventoryDialog() {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 16)
        }

        val nameEt = EditText(requireContext()).apply { hint = "Item Name" }
        val priceEt = EditText(requireContext()).apply { hint = "Unit Price (PGK)"; inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL }
        val qtyEt = EditText(requireContext()).apply { hint = "Stock Quantity"; inputType = android.text.InputType.TYPE_CLASS_NUMBER }

        layout.addView(nameEt)
        layout.addView(priceEt)
        layout.addView(qtyEt)

        AlertDialog.Builder(requireContext())
            .setTitle("Add Inventory Item")
            .setView(layout)
            .setPositiveButton("Save Item") { _, _ ->
                val name = nameEt.text.toString().trim()
                val price = priceEt.text.toString().toDoubleOrNull() ?: 0.0
                val qty = qtyEt.text.toString().toIntOrNull() ?: 0
                if (name.isNotEmpty() && price > 0) {
                    WantokPayRepository.addInventoryItem(name, price, qty, "General")
                    renderInventory()
                    Toast.makeText(requireContext(), "Inventory Item Added!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCreateInvoiceDialog() {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 16)
        }

        val custEt = EditText(requireContext()).apply { hint = "Customer Name" }
        val amtEt = EditText(requireContext()).apply { hint = "Invoice Amount (PGK)"; inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL }

        layout.addView(custEt)
        layout.addView(amtEt)

        AlertDialog.Builder(requireContext())
            .setTitle("Create Business Invoice")
            .setView(layout)
            .setPositiveButton("Issue Invoice") { _, _ ->
                val cust = custEt.text.toString().trim()
                val amt = amtEt.text.toString().toDoubleOrNull() ?: 0.0
                if (cust.isNotEmpty() && amt > 0) {
                    WantokPayRepository.addInvoice(cust, amt, "2025-05-01")
                    renderInvoices()
                    Toast.makeText(requireContext(), "Invoice Issued Successfully!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
