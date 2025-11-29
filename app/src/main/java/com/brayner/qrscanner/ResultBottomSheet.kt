package com.brayner.qrscanner

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import com.brayner.qrscanner.databinding.DialogScanResultBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ResultBottomSheet : BottomSheetDialogFragment() {

    private var _binding: DialogScanResultBinding? = null
    private val binding get() = _binding!!

    private var scanContent: String = ""
    private var onDismissListener: (() -> Unit)? = null

    fun setContent(content: String) {
        this.scanContent = content
    }

    fun setOnDismissListener(listener: () -> Unit) {
        this.onDismissListener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogScanResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val validation = SafetyValidator.validateScannedContent(scanContent)

        binding.textContent.text = scanContent
        binding.textType.text = validation.actionType.name

        if (validation.isSafe) {
            binding.layoutWarning.visibility = View.GONE
            binding.iconSafety.setImageResource(R.drawable.ic_check_circle)
            binding.textSafetyStatus.text = getString(R.string.status_safe)
            binding.textSafetyStatus.setTextColor(requireContext().getColor(android.R.color.holo_green_dark))
        } else {
            binding.layoutWarning.visibility = View.VISIBLE
            binding.textWarning.text = validation.warningMessage
            binding.iconSafety.setImageResource(R.drawable.ic_warning)
            binding.textSafetyStatus.text = getString(R.string.status_risk)
            binding.textSafetyStatus.setTextColor(requireContext().getColor(android.R.color.holo_red_dark))
        }

        // Action Buttons
        binding.btnCopy.setOnClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("QR Content", scanContent)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(requireContext(), getString(R.string.msg_copied), Toast.LENGTH_SHORT).show()
        }

        binding.btnShare.setOnClickListener {
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, scanContent)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, null)
            startActivity(shareIntent)
        }

        binding.btnOpen.setOnClickListener {
            try {
                val intent = when (validation.actionType) {
                    SafetyValidator.ActionType.URL -> Intent(Intent.ACTION_VIEW, scanContent.toUri())
                    SafetyValidator.ActionType.TEL -> Intent(Intent.ACTION_DIAL, scanContent.toUri())
                    SafetyValidator.ActionType.SMS -> Intent(Intent.ACTION_SENDTO, scanContent.toUri())
                    SafetyValidator.ActionType.EMAIL -> Intent(Intent.ACTION_SENDTO, scanContent.toUri())
                    SafetyValidator.ActionType.WIFI -> Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)
                    SafetyValidator.ActionType.TEXT -> {
                        Intent(Intent.ACTION_WEB_SEARCH).apply {
                            putExtra(android.app.SearchManager.QUERY, scanContent)
                        }
                    }
                    else -> null
                }

                if (intent != null) {
                    startActivity(intent)
                } else {
                    Toast.makeText(requireContext(), getString(R.string.error_no_app), Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(requireContext(), getString(R.string.error_handle_action), Toast.LENGTH_SHORT).show()
            }
        }
        
        // Configure button based on type
        when (validation.actionType) {
            SafetyValidator.ActionType.DANGEROUS_SCRIPT -> {
                binding.btnOpen.visibility = View.GONE
            }
            SafetyValidator.ActionType.TEXT -> {
                binding.btnOpen.visibility = View.VISIBLE
                binding.btnOpen.text = getString(R.string.btn_search)
            }
            else -> {
                binding.btnOpen.visibility = View.VISIBLE
                binding.btnOpen.text = getString(R.string.btn_open)
            }
        }
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        onDismissListener?.invoke()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
