package com.instatrace

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Starts Meta's official Instagram OAuth page. This deliberately never asks for or stores
 * an Instagram password. An app ID must be configured by the developer first.
 */
object InstagramOAuth {
    fun start(context: Context): Boolean {
        val appId = BuildConfig.INSTAGRAM_APP_ID.trim()
        val redirectUri = BuildConfig.INSTAGRAM_REDIRECT_URI.trim()
        if (appId.isEmpty() || redirectUri.isEmpty()) {
            Toast.makeText(
                context,
                "Instagram connection needs a Meta developer App ID first.",
                Toast.LENGTH_LONG
            ).show()
            return false
        }

        // Instagram Login is for eligible professional accounts. The callback URI must
        // be a real HTTPS URL registered exactly in the Meta developer dashboard.
        val authUri = Uri.parse("https://www.instagram.com/oauth/authorize").buildUpon()
            .appendQueryParameter("client_id", appId)
            .appendQueryParameter("redirect_uri", redirectUri)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", "instagram_business_basic,instagram_business_manage_insights")
            .appendQueryParameter("enable_fb_login", "0")
            .appendQueryParameter("force_authentication", "1")
            .build()

        return try {
            context.startActivity(Intent(Intent.ACTION_VIEW, authUri))
            true
        } catch (_: Exception) {
            Toast.makeText(context, "Could not open Instagram authorization.", Toast.LENGTH_LONG).show()
            false
        }
    }
}
