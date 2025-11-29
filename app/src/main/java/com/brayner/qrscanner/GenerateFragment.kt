package com.brayner.qrscanner

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.brayner.qrscanner.databinding.FragmentGenerateBinding
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix

class GenerateFragment : Fragment() {

    private var _binding: FragmentGenerateBinding? = null
    private val binding get() = _binding!!

    private var currentBitmap: Bitmap? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGenerateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnGenerate.setOnClickListener {
            val text = binding.editInput.text.toString()
            if (text.isNotBlank()) {
                // 1. Validate Input
                val validationError = SafetyValidator.validateInputForGeneration(text)
                if (validationError != null) {
                    Toast.makeText(requireContext(), "Error: $validationError", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                // 2. Check for Sensitive Data
                if (SafetyValidator.checkForSensitiveData(text)) {
                    android.app.AlertDialog.Builder(requireContext())
                        .setTitle("Sensitive Data Warning")
                        .setMessage("This QR code contains sensitive data (e.g., password, key). Anyone who scans it can see this information.\n\nDo you want to continue?")
                        .setPositiveButton("Generate") { _, _ ->
                            performGeneration(text)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                } else {
                    performGeneration(text)
                }
            } else {
                Toast.makeText(requireContext(), "Please enter text", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnShare.setOnClickListener {
            currentBitmap?.let { bitmap ->
                shareImage(bitmap)
            }
        }

        binding.btnSave.setOnClickListener {
            currentBitmap?.let { bitmap ->
                saveImageToGallery(bitmap)
            }
        }
    }

    private fun performGeneration(text: String) {
        try {
            currentBitmap = generateQRCode(text)
            binding.imageQr.setImageBitmap(currentBitmap)
            binding.layoutActions.visibility = View.VISIBLE
        } catch (e: WriterException) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error generating QR code", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareImage(bitmap: Bitmap) {
        try {
            val cachePath = java.io.File(requireContext().cacheDir, "images")
            cachePath.mkdirs()
            val stream = java.io.FileOutputStream("$cachePath/image.png")
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val imagePath = java.io.File(requireContext().cacheDir, "images")
            val newFile = java.io.File(imagePath, "image.png")
            val contentUri = androidx.core.content.FileProvider.getUriForFile(
                requireContext(),
                "com.example.newqrcode.fileprovider",
                newFile
            )

            if (contentUri != null) {
                val shareIntent = android.content.Intent()
                shareIntent.action = android.content.Intent.ACTION_SEND
                shareIntent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                shareIntent.setDataAndType(contentUri, requireContext().contentResolver.getType(contentUri))
                shareIntent.putExtra(android.content.Intent.EXTRA_STREAM, contentUri)
                startActivity(android.content.Intent.createChooser(shareIntent, "Share QR Code"))
            }
        } catch (e: java.io.IOException) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error sharing image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveImageToGallery(bitmap: Bitmap) {
        val filename = "QR_${System.currentTimeMillis()}.png"
        var fos: java.io.OutputStream?
        var imageUri: android.net.Uri? = null

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val resolver = requireContext().contentResolver
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES)
                }
                imageUri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                fos = imageUri?.let { resolver.openOutputStream(it) }
            } else {
                val imagesDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES)
                val image = java.io.File(imagesDir, filename)
                fos = java.io.FileOutputStream(image)
            }

            fos?.use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                Toast.makeText(requireContext(), "QR Code saved to Gallery", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error saving image", Toast.LENGTH_SHORT).show()
        }
    }

    @Throws(WriterException::class)
    private fun generateQRCode(text: String): Bitmap {
        val width = 500
        val height = 500
        val hints = java.util.EnumMap<com.google.zxing.EncodeHintType, Any>(com.google.zxing.EncodeHintType::class.java)
        hints[com.google.zxing.EncodeHintType.CHARACTER_SET] = "UTF-8"
        
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, width, height, hints)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
