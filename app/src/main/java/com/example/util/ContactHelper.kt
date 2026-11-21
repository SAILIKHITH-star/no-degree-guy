package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.text.NumberFormat
import java.util.Locale

data class ContactNumberEntry(
    val label: String,
    val number: String,
    val isMain: Boolean
)

object ContactHelper {

    fun formatInr(amount: Long): String {
        val formatter = NumberFormat.getNumberInstance(Locale("en", "IN"))
        return "₹${formatter.format(amount)}"
    }

    fun normalizeIndianPhone(raw: String): String {
        val digits = raw.filter(Char::isDigit)
        return when {
            digits.length == 10 -> "91$digits"
            digits.length == 12 && digits.startsWith("91") -> digits
            digits.length == 11 && digits.startsWith("0") -> "91${digits.drop(1)}"
            else -> digits
        }
    }

    fun formatPhoneDisplay(raw: String): String {
        val digits = raw.filter(Char::isDigit)
        return if (digits.length == 10) {
            "+91 ${digits.substring(0, 5)} ${digits.substring(5)}"
        } else {
            raw.trim()
        }
    }

    /**
     * Opens a direct 1-tap WhatsApp chat with the specified phone number.
     * Uses wa.me HTTPS deep link which opens WhatsApp directly if installed or web fallback safely.
     */
    fun openWhatsAppChat(
        context: Context,
        rawPhone: String,
        prefilledMessage: String = "Hello Rupa IIT / NEET Study Circle, I would like to enquire about batches and admissions."
    ): Boolean {
        val normalized = normalizeIndianPhone(rawPhone)
        if (normalized.length < 10) return false

        val encodedMessage = Uri.encode(prefilledMessage)
        val waUri = Uri.parse("https://wa.me/$normalized?text=$encodedMessage")

        // Prefer opening directly in WhatsApp if installed
        val directWhatsApp = Intent(Intent.ACTION_VIEW, waUri).apply {
            setPackage("com.whatsapp")
        }
        if (runCatching { context.startActivity(directWhatsApp); true }.getOrDefault(false)) {
            return true
        }

        // Try WhatsApp Business
        val businessWhatsApp = Intent(Intent.ACTION_VIEW, waUri).apply {
            setPackage("com.whatsapp.w4b")
        }
        if (runCatching { context.startActivity(businessWhatsApp); true }.getOrDefault(false)) {
            return true
        }

        // Fallback to standard browser/handler for wa.me
        return runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, waUri))
            true
        }.getOrDefault(false)
    }

    /**
     * Opens the device phone dialer pre-filled with the number for instant calling.
     */
    fun dialPhone(context: Context, rawPhone: String): Boolean {
        val normalized = normalizeIndianPhone(rawPhone)
        if (normalized.length < 10) return false
        val telUri = Uri.parse("tel:+$normalized")
        return runCatching {
            context.startActivity(Intent(Intent.ACTION_DIAL, telUri))
            true
        }.getOrDefault(false)
    }

    fun isSafeExternalUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val parsed = runCatching { Uri.parse(url.trim()) }.getOrNull() ?: return false
        val scheme = parsed.scheme?.lowercase(Locale.US)
        return (scheme == "https" || scheme == "http") && !parsed.host.isNullOrBlank()
    }

    fun openSafeUrl(context: Context, url: String): Boolean {
        if (!isSafeExternalUrl(url)) return false
        return runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url.trim())))
            true
        }.getOrDefault(false)
    }
}
