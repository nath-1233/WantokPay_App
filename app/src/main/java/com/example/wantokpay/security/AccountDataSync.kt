package com.example.wantokpay.security

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.security.GeneralSecurityException
import java.util.concurrent.Executors

object AccountDataSync {
    private const val TAG = "WantokPayDataSync"
    private val executor = Executors.newSingleThreadExecutor()
    @Volatile
    private var applicationContext: Context? = null

    fun configure(context: Context) {
        applicationContext = context.applicationContext
    }

    fun load(onComplete: (Result<JSONObject>) -> Unit) {
        val context = applicationContext
            ?: return onComplete(Result.failure(IllegalStateException("Data sync has not been configured.")))
        executor.execute {
            val result = try {
                Result.success(AuthApi.get(context, "/auth/me/data").getJSONObject("data"))
            } catch (error: AuthApiException) {
                Result.failure(error)
            } catch (error: IOException) {
                Result.failure(IOException("Could not load your saved account data.", error))
            } catch (error: GeneralSecurityException) {
                Result.failure(error)
            } catch (error: JSONException) {
                Result.failure(IOException("The server returned invalid account data.", error))
            }
            onComplete(result)
        }
    }

    fun save(snapshot: JSONObject) {
        val context = applicationContext ?: return
        executor.execute {
            try {
                AuthApi.put(
                    context,
                    "/auth/me/data",
                    JSONObject().put("data", snapshot)
                )
            } catch (error: AuthApiException) {
                Log.e(TAG, "Server rejected account data sync.", error)
                showSyncError(context, "Account data was not saved. Please sign in again.")
            } catch (error: IOException) {
                Log.e(TAG, "Account data sync failed.", error)
                showSyncError(context, "Could not save your account data. Check your connection.")
            } catch (error: JSONException) {
                Log.e(TAG, "Account data snapshot could not be encoded.", error)
                showSyncError(context, "Could not save your account data.")
            } catch (error: GeneralSecurityException) {
                Log.e(TAG, "Account data sync failed.", error)
                showSyncError(context, "Could not securely read your sign-in token.")
            }
        }
    }

    private fun showSyncError(context: Context, message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
}
