package com.uem.miapp.ui.home

import android.graphics.Bitmap
import android.net.Uri

data class HomeUiState(
    val selectedImageUri: Uri? = null,
    val originalBitmap: Bitmap? = null,
    val filteredBitmap: Bitmap? = null,
    val filterName: String? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val uploadSuccess: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val imageLoading: Boolean = false
)
