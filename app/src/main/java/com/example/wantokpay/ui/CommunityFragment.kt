package com.example.wantokpay.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.wantokpay.data.CommunityGroup
import com.example.wantokpay.databinding.FragmentCommunityBinding
import com.example.wantokpay.repository.WantokPayRepository
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import java.util.Locale

class CommunityFragment : Fragment() {

    private var _binding: FragmentCommunityBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCommunityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        renderCommunityGroups()
    }

    private fun renderCommunityGroups() {
        val container = binding.layoutCommunityGroupsContainer
        container.removeAllViews()

        val groups = WantokPayRepository.getCommunityGroups()

        for (group in groups) {
            val card = MaterialCardView(requireContext()).apply {
                radius = 16f * resources.displayMetrics.density
                elevation = 2f * resources.displayMetrics.density
                strokeWidth = 0
                setCardBackgroundColor(Color.WHITE)
                useCompatPadding = true
            }

            val layout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(24, 24, 24, 24)
            }

            val titleTv = TextView(requireContext()).apply {
                text = group.groupName
                setTextColor(Color.parseColor("#0D47A1"))
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            val categoryTv = TextView(requireContext()).apply {
                text = "${group.category} • Admin: ${group.adminName}"
                setTextColor(Color.parseColor("#6C757D"))
                textSize = 12f
            }

            val progress = ((group.currentBalanceKina / group.targetAmountKina) * 100).toInt().coerceAtMost(100)

            val progressBar = ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 24).apply {
                    setMargins(0, 16, 0, 12)
                }
                max = 100
                setProgress(progress)
            }

            val statsTv = TextView(requireContext()).apply {
                text = "Raised: PGK ${String.format(Locale.US, "%.2f", group.currentBalanceKina)} of K${String.format(Locale.US, "%.2f", group.targetAmountKina)} (${group.contributorCount} Contributors)"
                setTextColor(Color.parseColor("#1A1D20"))
                textSize = 13f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            val contributeBtn = Button(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 16, 0, 0)
                }
                text = "Contribute to Group Fund"
                setBackgroundColor(Color.parseColor("#0D47A1"))
                setTextColor(Color.WHITE)
            }

            contributeBtn.setOnClickListener {
                showContributionDialog(group)
            }

            layout.addView(titleTv)
            layout.addView(categoryTv)
            layout.addView(progressBar)
            layout.addView(statsTv)
            layout.addView(contributeBtn)

            card.addView(layout)
            container.addView(card)
        }
    }

    private fun showContributionDialog(group: CommunityGroup) {
        val editText = EditText(requireContext()).apply {
            hint = "Contribution Amount (PGK)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText("50.00")
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Contribute to ${group.groupName}")
            .setMessage("Your contribution will be recorded on the transparent WantokPay group public ledger.")
            .setView(editText)
            .setPositiveButton("Confirm Contribution") { _, _ ->
                val amount = editText.text.toString().toDoubleOrNull() ?: 0.0
                val result = WantokPayRepository.processGroupContribution(group.groupId, amount)
                result.fold(
                    onSuccess = { tx ->
                        renderCommunityGroups()
                        Snackbar.make(requireView(), "Contributed PGK ${String.format(Locale.US, "%.2f", amount)}! Ref: ${tx.referenceNumber}", Snackbar.LENGTH_LONG).show()
                    },
                    onFailure = { err ->
                        Toast.makeText(requireContext(), err.message, Toast.LENGTH_LONG).show()
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
