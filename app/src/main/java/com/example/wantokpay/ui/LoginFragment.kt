package com.example.wantokpay.ui

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.wantokpay.R
import com.example.wantokpay.repository.WantokPayRepository
import com.example.wantokpay.security.AccountDataSync
import com.example.wantokpay.security.AuthApi
import com.example.wantokpay.security.AuthApiException
import com.example.wantokpay.security.AuthTokenStore
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.JsonParseException
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.security.GeneralSecurityException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class LoginFragment : Fragment(R.layout.fragment_login) {
    private var authExecutor: ExecutorService? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        authExecutor = Executors.newSingleThreadExecutor()

        view.findViewById<Button>(R.id.btn_login).setOnClickListener { loginButton ->
            val identifier = view.findViewById<TextInputEditText>(R.id.input_identifier).text?.toString()?.trim().orEmpty()
            val password = view.findViewById<TextInputEditText>(R.id.input_password).text?.toString().orEmpty()

            when {
                identifier.isBlank() -> showMessage("Enter your username.")
                password.isBlank() -> showMessage("Enter your password.")
                else -> post(
                    "/auth/login",
                    JSONObject().put("username", identifier).put("password", password),
                    loginButton
                ) { response ->
                    val token = response.optString("access_token")
                    if (token.isBlank()) {
                        showMessage("The server did not return a sign-in token.")
                        return@post
                    }
                    try {
                        AuthTokenStore.save(requireContext(), token)
                        AccountDataSync.configure(requireContext())
                        val userId = response.optString("user_id")
                        if (userId.isBlank()) {
                            AuthTokenStore.clear(requireContext())
                            showMessage("The server did not return an account ID.")
                            return@post
                        }
                        AccountDataSync.load { accountData ->
                            activity?.runOnUiThread {
                                if (!isAdded || getView() == null) return@runOnUiThread
                                accountData.fold(
                                    onSuccess = { data ->
                                        try {
                                            WantokPayRepository.restoreAccountData(
                                                userId,
                                                response.optString("username", identifier),
                                                data.toString()
                                            )
                                            findNavController().navigate(R.id.action_loginFragment_to_walletDashboardFragment)
                                        } catch (error: JsonParseException) {
                                            AuthTokenStore.clear(requireContext())
                                            showMessage("Could not load your saved account data.")
                                        }
                                    },
                                    onFailure = { error ->
                                        AuthTokenStore.clear(requireContext())
                                        showMessage(error.message ?: "Could not load your saved account data.")
                                    }
                                )
                            }
                        }
                    } catch (_: GeneralSecurityException) {
                        showMessage("Could not securely save the sign-in token.")
                    }
                }
            }
        }

        view.findViewById<TextView>(R.id.btn_forgot_password).setOnClickListener {
            showMessage("Password recovery is not connected yet.")
        }

        view.findViewById<Button>(R.id.btn_create_account).setOnClickListener {
            showRegistrationDialog()
        }
    }

    override fun onDestroyView() {
        authExecutor?.shutdownNow()
        authExecutor = null
        super.onDestroyView()
    }

    private fun showRegistrationDialog() {
        val context = requireContext()
        val usernameInput = EditText(context).apply {
            hint = "Username"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PERSON_NAME
        }
        val passwordInput = EditText(context).apply {
            hint = "Password (12+ characters)"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val confirmPasswordInput = EditText(context).apply {
            hint = "Confirm password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val form = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (24 * resources.displayMetrics.density).toInt()
            setPadding(padding, 0, padding, 0)
            addView(usernameInput)
            addView(passwordInput)
            addView(confirmPasswordInput)
        }

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle("Create your account")
            .setView(form)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Create account", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener { button ->
                val username = usernameInput.text.toString().trim()
                val password = passwordInput.text.toString()
                val confirmPassword = confirmPasswordInput.text.toString()
                when {
                    username.isBlank() || password.isBlank() -> showMessage("Enter a username and password.")
                    password != confirmPassword -> showMessage("The passwords do not match.")
                    else -> post(
                        "/auth/register",
                        JSONObject().put("username", username).put("password", password),
                        button
                    ) {
                        dialog.dismiss()
                        view?.findViewById<TextInputEditText>(R.id.input_identifier)?.setText(username)
                        showMessage("Account created. Sign in with your new credentials.")
                    }
                }
            }
        }
        dialog.show()
    }

    private fun post(
        endpoint: String,
        payload: JSONObject,
        button: View,
        onSuccess: (JSONObject) -> Unit
    ) {
        val textView = button as? TextView
        val originalText = textView?.text
        button.isEnabled = false
        textView?.text = "Please wait..."
        val executor = authExecutor
        if (executor == null) {
            button.isEnabled = true
            if (originalText != null) textView?.text = originalText
            showMessage("The sign-in screen is no longer available.")
            return
        }

        executor.execute {
            val result = try {
                Result.success(AuthApi.post(endpoint, payload))
            } catch (error: AuthApiException) {
                Result.failure(error)
            } catch (error: IOException) {
                Result.failure(IOException("Cannot reach the WantokPay server. Check that it is running.", error))
            } catch (error: JSONException) {
                Result.failure(IOException("The server returned an invalid response.", error))
            }

            activity?.runOnUiThread {
                if (!isAdded || view == null) return@runOnUiThread
                button.isEnabled = true
                if (originalText != null) textView?.text = originalText
                result.fold(
                    onSuccess = onSuccess,
                    onFailure = { error ->
                        showMessage(error.message ?: "The request could not be completed.")
                    }
                )
            }
        }
    }

    private fun showMessage(message: String) {
        view?.let { Snackbar.make(it, message, Snackbar.LENGTH_LONG).show() }
    }
}
