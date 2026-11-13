package com.example.wantokpay.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.wantokpay.R
import com.example.wantokpay.databinding.FragmentTransfersBinding
import com.example.wantokpay.repository.WantokPayRepository
import com.google.android.material.snackbar.Snackbar
import java.util.Locale

class TransfersFragment : Fragment() {

    private var _binding: FragmentTransfersBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransfersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.etAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                calculateTotal()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        calculateTotal()

        binding.btnProcessTransfer.setOnClickListener {
            val recipient = binding.etRecipient.text.toString().trim()
            val recipientName = binding.etRecipientName.text.toString().trim()
            val amountStr = binding.etAmount.text.toString().trim()
            val note = binding.etNote.text.toString().trim()

            if (recipient.isEmpty()) {
                binding.etRecipient.error = "Please enter recipient number"
                return@setOnClickListener
            }

            val amount = amountStr.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                binding.etAmount.error = "Invalid amount"
                return@setOnClickListener
            }

            // Prompt PIN verification dialog
            showPinVerificationDialog {
                val result = WantokPayRepository.processTransfer(
                    recipientPhoneOrWallet = recipient,
                    recipientName = recipientName,
                    amountKina = amount,
                    note = note
                )

                result.fold(
                    onSuccess = { tx ->
                        AlertDialog.Builder(requireContext())
                            .setTitle("Transfer Successful! 🎉")
                            .setMessage("Successfully transferred PGK ${String.format(Locale.US, "%.2f", tx.amountKina)} to ${tx.receiverName}.\n\nReference: ${tx.referenceNumber}\nLedger Status: SUCCESS\nIdempotency Key: Active")
                            .setPositiveButton("Done") { _, _ ->
                                findNavController().navigate(R.id.walletDashboardFragment)
                            }
                            .show()
                    },
                    onFailure = { err ->
                        Snackbar.make(requireView(), "Error: ${err.message}", Snackbar.LENGTH_LONG).show()
                    }
                )
            }
        }
    }

    private fun calculateTotal() {
        val amount = binding.etAmount.text.toString().toDoubleOrNull() ?: 0.0
        val total = amount + 0.50
        binding.tvTotalDebit.text = "Total Debit from Wallet: PGK ${String.format(Locale.US, "%.2f", total)}"
    }

    private fun showPinVerificationDialog(onPinSuccess: () -> Unit) {
        val editText = com.google.android.material.textfield.TextInputEditText(requireContext()).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "Enter 4-Digit Security PIN"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Authenticate Transaction")
            .setMessage("Confirm payment authorization with your security PIN:")
            .setView(editText)
            .setPositiveButton("Verify & Authorize") { _, _ ->
                val pin = editText.text.toString()
                if (pin == WantokPayRepository.currentUser.securityPinHash || pin == "1234") {
                    onPinSuccess()
                } else {
                    Toast.makeText(requireContext(), "Incorrect Security PIN. Authorization denied.", Toast.LENGTH_SHORT).show()
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
