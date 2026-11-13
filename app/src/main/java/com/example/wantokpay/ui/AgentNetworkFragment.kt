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
import com.example.wantokpay.databinding.FragmentAgentNetworkBinding
import com.example.wantokpay.repository.WantokPayRepository
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import java.util.Locale

class AgentNetworkFragment : Fragment() {

    private var _binding: FragmentAgentNetworkBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAgentNetworkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnAgentCashIn.setOnClickListener {
            handleAgentOperation(isCashIn = true)
        }

        binding.btnAgentCashOut.setOnClickListener {
            handleAgentOperation(isCashIn = false)
        }

        renderAgents()
    }

    private fun handleAgentOperation(isCashIn: Boolean) {
        val agentCode = binding.etAgentCode.text.toString().trim()
        val amountStr = binding.etAgentAmount.text.toString().trim()

        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            binding.etAgentAmount.error = "Invalid amount"
            return
        }

        val result = WantokPayRepository.processAgentOperation(agentCode, isCashIn, amount)
        result.fold(
            onSuccess = { tx ->
                val opName = if (isCashIn) "Cash-In Deposit" else "Cash-Out Withdrawal"
                AlertDialog.Builder(requireContext())
                    .setTitle("Agent $opName Successful! 🏧")
                    .setMessage("Agent ID: $agentCode\nAmount: PGK ${String.format(Locale.US, "%.2f", amount)}\nReference: ${tx.referenceNumber}\nStatus: SUCCESS\nOTP Verified")
                    .setPositiveButton("Done", null)
                    .show()
            },
            onFailure = { err ->
                Snackbar.make(requireView(), "Error: ${err.message}", Snackbar.LENGTH_LONG).show()
            }
        )
    }

    private fun renderAgents() {
        val container = binding.layoutAgentsContainer
        container.removeAllViews()

        for (agent in WantokPayRepository.getAgents()) {
            val card = MaterialCardView(requireContext()).apply {
                radius = 12f * resources.displayMetrics.density
                elevation = 1f * resources.displayMetrics.density
                strokeWidth = 0
                setCardBackgroundColor(Color.WHITE)
                useCompatPadding = true
            }

            val layout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(24, 20, 24, 20)
                gravity = android.view.Gravity.CENTER_VERTICAL
            }

            val textLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val titleTv = TextView(requireContext()).apply {
                text = "${agent.agentName} (${agent.agentId})"
                setTextColor(Color.parseColor("#0D47A1"))
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            val subtitleTv = TextView(requireContext()).apply {
                text = "${agent.locationName}, ${agent.province} • Float: K${String.format(Locale.US, "%.0f", agent.floatBalanceKina)}"
                setTextColor(Color.parseColor("#6C757D"))
                textSize = 12f
            }

            textLayout.addView(titleTv)
            textLayout.addView(subtitleTv)

            val statusTv = TextView(requireContext()).apply {
                text = if (agent.isOpen) "OPEN" else "CLOSED"
                setTextColor(if (agent.isOpen) Color.parseColor("#2E7D32") else Color.parseColor("#C62828"))
                textSize = 12f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            layout.addView(textLayout)
            layout.addView(statusTv)
            card.addView(layout)

            card.setOnClickListener {
                binding.etAgentCode.setText(agent.agentId)
                Toast.makeText(requireContext(), "Selected Agent ${agent.agentName}", Toast.LENGTH_SHORT).show()
            }

            container.addView(card)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
