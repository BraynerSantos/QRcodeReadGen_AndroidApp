package com.brayner.qrscanner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.brayner.qrscanner.databinding.FragmentOpenQrBinding
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

class OpenQRFragment : Fragment() {

    private var _binding: FragmentOpenQrBinding? = null
    private val binding get() = _binding!!

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: android.net.Uri? ->
        uri?.let { processImage(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOpenQrBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSelectImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
    }

    private fun processImage(uri: android.net.Uri) {
        try {
            val image = InputImage.fromFilePath(requireContext(), uri)
            val scanner = BarcodeScanning.getClient()
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isEmpty()) {
                        Toast.makeText(requireContext(), "No QR code found", Toast.LENGTH_SHORT).show()
                    } else if (barcodes.size == 1) {
                        showResult(barcodes[0].rawValue ?: "")
                    } else {
                        // Multiple QR codes found
                        showMultipleResultsDialog(barcodes)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to scan image", Toast.LENGTH_SHORT).show()
                }
        } catch (e: java.io.IOException) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error loading image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showMultipleResultsDialog(barcodes: List<com.google.mlkit.vision.barcode.common.Barcode>) {
        val items = barcodes.mapIndexed { index, barcode ->
            val content = barcode.rawValue ?: "Unknown"
            val shortContent = if (content.length > 30) content.take(27) + "..." else content
            "QR #${index + 1}: $shortContent"
        }.toTypedArray()

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Multiple QR Codes Found")
            .setItems(items) { _, which ->
                showResult(barcodes[which].rawValue ?: "")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showResult(rawValue: String) {
        if (childFragmentManager.findFragmentByTag("ResultBottomSheet") == null) {
            val bottomSheet = ResultBottomSheet()
            bottomSheet.setContent(rawValue)
            bottomSheet.show(childFragmentManager, "ResultBottomSheet")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
