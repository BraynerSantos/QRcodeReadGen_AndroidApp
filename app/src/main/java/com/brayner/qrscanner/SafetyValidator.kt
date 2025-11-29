package com.brayner.qrscanner

import android.util.Patterns
import java.util.Locale

object SafetyValidator {

    data class ValidationResult(
        val isSafe: Boolean,
        val warningMessage: String? = null,
        val actionType: ActionType = ActionType.TEXT
    )

    enum class ActionType {
        URL, WIFI, TEL, SMS, EMAIL, TEXT, DANGEROUS_SCRIPT
    }

    private val RISKY_KEYWORDS = listOf(
        "login", "verify", "reward", "free", "credential", "token", "update", "security", "confirm"
    )

    private val DANGEROUS_SCHEMES = listOf(
        "javascript:", "data:", "vbscript:", "file:"
    )

    fun validateScannedContent(content: String): ValidationResult {
        val lowerContent = content.lowercase(Locale.getDefault())

        // 1. Check for Dangerous Scripts/Payloads
        if (DANGEROUS_SCHEMES.any { lowerContent.startsWith(it) }) {
            return ValidationResult(
                isSafe = false,
                warningMessage = "DANGEROUS: Contains executable script or unsafe data scheme.",
                actionType = ActionType.DANGEROUS_SCRIPT
            )
        }

        // 2. Check for URLs
        if (Patterns.WEB_URL.matcher(content).matches() || lowerContent.startsWith("http")) {
            val isHttp = lowerContent.startsWith("http://")
            
            var warning: String? = null
            
            if (isHttp) {
                warning = "Unsecured Connection (HTTP). Traffic can be intercepted."
            }

            if (RISKY_KEYWORDS.any { lowerContent.contains(it) }) {
                val keyword = RISKY_KEYWORDS.first { lowerContent.contains(it) }
                warning = (warning ?: "") + "\nSuspicious keyword detected: '$keyword'."
            }

            if (content.length > 200) {
                 warning = (warning ?: "") + "\nURL is unusually long, which can hide malicious intent."
            }

            return ValidationResult(
                isSafe = warning == null,
                warningMessage = warning,
                actionType = ActionType.URL
            )
        }

        // 3. Check for Specific URI Schemes
        if (lowerContent.startsWith("wifi:")) {
            return ValidationResult(false, "Connects to a Wi-Fi network. Verify the network name.", ActionType.WIFI)
        }
        if (lowerContent.startsWith("tel:") || Patterns.PHONE.matcher(content).matches()) {
            return ValidationResult(false, "Initiates a phone call.", ActionType.TEL)
        }
        if (lowerContent.startsWith("smsto:") || lowerContent.startsWith("sms:")) {
            return ValidationResult(false, "Sends an SMS message. Check destination and body.", ActionType.SMS)
        }
        if (lowerContent.startsWith("mailto:") || Patterns.EMAIL_ADDRESS.matcher(content).matches()) {
            return ValidationResult(true, null, ActionType.EMAIL)
        }

        // Default Text
        return ValidationResult(true, null, ActionType.TEXT)
    }

    fun validateInputForGeneration(input: String): String? {
        val lowerInput = input.lowercase(Locale.getDefault())
        
        // 1. Sanitize: Reject dangerous schemes
        if (DANGEROUS_SCHEMES.any { lowerInput.startsWith(it) }) {
            return "Input contains unsafe scheme (javascript, data, etc)."
        }

        // 2. Check for HTML/Script tags (Basic check)
        if (lowerInput.contains("<script") || lowerInput.contains("<html>")) {
            return "Input contains HTML or Script tags."
        }

        return null // Valid
    }

    fun checkForSensitiveData(input: String): Boolean {
        val lowerInput = input.lowercase(Locale.getDefault())
        val sensitiveKeywords = listOf("password", "key", "secret", "wifi", "token", "auth")
        return sensitiveKeywords.any { lowerInput.contains(it) }
    }
}
