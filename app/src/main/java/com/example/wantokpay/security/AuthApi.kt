package com.example.wantokpay.security

import android.content.Context
import com.example.wantokpay.BuildConfig
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class AuthApiException(message: String) : IOException(message)

object AuthApi {
    fun post(endpoint: String, body: JSONObject): JSONObject {
        return request("POST", endpoint, body)
    }

    fun get(context: Context, endpoint: String): JSONObject {
        val token = AuthTokenStore.read(context)
            ?: throw AuthApiException("You are signed out. Sign in again to continue.")
        return request("GET", endpoint, accessToken = token)
    }

    fun put(context: Context, endpoint: String, body: JSONObject): JSONObject {
        val token = AuthTokenStore.read(context)
            ?: throw AuthApiException("You are signed out. Sign in again to continue.")
        return request("PUT", endpoint, body, token)
    }

    private fun request(
        method: String,
        endpoint: String,
        body: JSONObject? = null,
        accessToken: String? = null
    ): JSONObject {
        val baseUrl = BuildConfig.WANTOKPAY_API_BASE_URL.trimEnd('/')
        val connection = URL("$baseUrl$endpoint").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Accept", "application/json")
            if (accessToken != null) {
                connection.setRequestProperty("Authorization", "Bearer $accessToken")
            }
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body.toString()) }
            }

            val statusCode = connection.responseCode
            val responseStream = if (statusCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val responseText = responseStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val response = try {
                JSONObject(responseText)
            } catch (error: JSONException) {
                if (statusCode !in 200..299) {
                    throw AuthApiException("The server rejected the request (HTTP $statusCode).")
                }
                throw error
            }

            if (statusCode !in 200..299) {
                val detail = response.optString("detail")
                throw AuthApiException(detail.ifBlank { "The server rejected the request (HTTP $statusCode)." })
            }
            return response
        } finally {
            connection.disconnect()
        }
    }
}
