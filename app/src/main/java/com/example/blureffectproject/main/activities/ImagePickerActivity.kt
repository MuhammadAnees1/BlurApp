package com.example.blureffectproject.main.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ext.SdkExtensions
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresExtension
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.blureffectproject.databinding.ActivityImagePickerBinding

class ImagePickerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImagePickerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImagePickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.drawPaintTextView.setOnClickListener {
            checkGalleryPermissionAndOpenGallery()
        }
    }

    private val pickPhotoLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            navigateToBlurActivity(uri)
        } else {
            Toast.makeText(this, "No image selected!", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedImageUri: Uri? = result.data?.data
            if (selectedImageUri != null) {
                navigateToBlurActivity(selectedImageUri)
            } else {
                Toast.makeText(this, "No image selected!", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Image selection cancelled!", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestGalleryPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.entries.all { it.value }

            if (allGranted) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && SdkExtensions.getExtensionVersion(
                        Build.VERSION_CODES.R) >= 2) {
                    openGallery()
                }
            } else {
                Toast.makeText(this, "Gallery permission denied!", Toast.LENGTH_SHORT).show()
            }
        }

    private fun checkGalleryPermissionAndOpenGallery() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                if (SdkExtensions.getExtensionVersion(Build.VERSION_CODES.R) >= 2) {
                    openGallery()
                }
            }

            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && SdkExtensions.getExtensionVersion(
                        Build.VERSION_CODES.R) >= 2) {
                    openGallery()
                }
            }

            else -> requestGalleryPermissions()
        }
    }

    private fun requestGalleryPermissions() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                requestGalleryPermissionsLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VIDEO,
                        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                    )
                )
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                requestGalleryPermissionsLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VIDEO
                    )
                )
            }

            else -> {
                requestGalleryPermissionsLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                )
            }
        }
    }

    @RequiresExtension(extension = Build.VERSION_CODES.R, version = 2)
    private fun openGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (isPhotoPickerIntentAvailable()) {
                openPhotoPicker()
            } else {
                openLegacyGallery()
            }
        } else {
            openLegacyGallery()
        }
    }

    @RequiresExtension(extension = Build.VERSION_CODES.R, version = 2)
    private fun isPhotoPickerIntentAvailable(): Boolean {
        val intent = Intent(MediaStore.ACTION_PICK_IMAGES)
        return intent.resolveActivity(packageManager) != null
    }

    private fun openPhotoPicker() {
        pickPhotoLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    private fun openLegacyGallery() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        pickImageLauncher.launch(intent)
    }

    private fun navigateToBlurActivity(imageUri: Uri) {
        val intent = Intent(this, BlurActivity::class.java).apply {
            putExtra("imageUri", imageUri.toString())
        }
        startActivity(intent)
    }
}