package com.example.photoeditor

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toFile
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.example.photoeditor.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.net.URI
import java.util.*

class MainActivity : AppCompatActivity() {
    lateinit var activityMainBinding: ActivityMainBinding
    lateinit var permisssionLauncher: ActivityResultLauncher<Array<String>>
    lateinit var currentBitmap: Bitmap
    lateinit var contract: ActivityResultLauncher<String>
    lateinit var filename: String
    val SELECT_IMAGE = 1
    val SAVE_IMAGE = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)

        val viewModel: PhotoEditViewModel by viewModels()

        contract = registerForActivityResult(ActivityResultContracts.GetContent()) {
            it?.let { uri ->
                activityMainBinding.apply {
                    selectImage.visibility = View.GONE
                    imageSection.visibility = View.VISIBLE
                    val inputStream = contentResolver.openInputStream(uri)
                    currentBitmap = BitmapFactory.decodeStream(inputStream)
                    viewModel.setBitmap(currentBitmap)
                    filename = getNameFromUri(uri)
                    imageName.text = filename
                }
            }
        }

        permisssionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                if (permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true) {
                    showDialogPromt(
                        "Photo Editor",
                        "Please Select the Image",
                        "Open files",
                        SELECT_IMAGE, null
                    )
                }
            }

        requestPermission()

        viewModel.mbitmap.observe(this, Observer {
            activityMainBinding.imageView.setImageBitmap(it)
        })

        activityMainBinding.selectImage.setOnClickListener {
            if (hasWritePremission(this))
                showDialogPromt(
                    "Photo Editor",
                    "Please Select the Image",
                    "Open files",
                    SELECT_IMAGE, null
                )
            else {
                requestPermission()
            }
        }

        activityMainBinding.discard.setOnClickListener {
            activityMainBinding.apply {
                viewModel.setBitmap(currentBitmap)
            }
        }

        activityMainBinding.flipX.setOnClickListener {
            viewModel.setBitmap(viewModel.filpImage(-1f, 1f))
        }
        activityMainBinding.flipY.setOnClickListener {
            viewModel.setBitmap(viewModel.filpImage(1f, -1f))
        }
        activityMainBinding.rotateLeft.setOnClickListener {
            viewModel.setBitmap(viewModel.rotateRight())
        }
        activityMainBinding.rotateRight.setOnClickListener {
            viewModel.setBitmap(viewModel.rotateLeft())
        }
        activityMainBinding.squareCrop.setOnClickListener {
            viewModel.setBitmap(viewModel.centerCrop())
        }

        activityMainBinding.saveImage.setOnClickListener {
            showDialogPromt(
                "Photo Editor",
                "Do you want save image",
                "yes",
                SAVE_IMAGE,
                viewModel.getBitmap()
            )
        }
    }

    private fun requestPermission() {
        var permissionList = mutableListOf<String>()

        if (!(hasWritePremission(this) || MinSdk29()))
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if (!(hasWritePremission(this)))
            permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE)

        if (permissionList.isNotEmpty())
            permisssionLauncher.launch(permissionList.toTypedArray())

        permissionList.clear()
    }

    private suspend fun saveToExternalStorage(filename: String, bitmap: Bitmap): Boolean {

        return withContext(Dispatchers.IO) {

            val imageCollection = sdk29AndUp {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } ?: MediaStore.Images.Media.EXTERNAL_CONTENT_URI

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$filename.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.WIDTH, bitmap.width)
                put(MediaStore.Images.Media.HEIGHT, bitmap.height)
            }

            try {
                contentResolver.insert(imageCollection, contentValues)?.also { uri ->
                    contentResolver.openOutputStream(uri).use { outputStream ->
                        if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream))
                            throw IOException("Could not save Bitmap")
                    }
                } ?: throw IOException("Could not create MediaStore Entry")
                true
            } catch (e: IOException) {
                e.printStackTrace()
                false
            }
        }
    }

    @SuppressLint("Range")
    fun getNameFromUri(uri: Uri): String {
        var name: String = ""
        if (uri.scheme.equals("content")) {
            val cursor = application.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    Log.d(
                        "asass",
                        cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                    )
                    name = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            } finally {
                cursor?.let {
                    cursor.close()
                }
            }
        }
        return name
    }

    fun showDialogPromt(
        title: String,
        message: String,
        action: String,
        event: Int,
        bitmap: Bitmap?
    ) {
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setNegativeButton(action) { dialog, where ->

                when (event) {
                    SAVE_IMAGE -> {
                        lifecycleScope.launch {
                            saveToExternalStorage(UUID.randomUUID().toString(), bitmap!!)
                        }
                    }
                    SELECT_IMAGE -> {
                        contract.launch("image/*")
                    }
                }

                dialog.dismiss()
            }
            .setNeutralButton("Cancel") { dialog, where ->
                dialog.dismiss()
            }
            .show()
    }

}