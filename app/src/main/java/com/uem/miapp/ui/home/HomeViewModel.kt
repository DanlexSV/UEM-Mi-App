package com.uem.miapp.ui.home

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    fun onImagePicked(uri: Uri?, contentResolver: ContentResolver) {
        if (uri == null) return

        val bitmap = uriToBitmap(uri, contentResolver)
        if (bitmap == null) {
            _uiState.update { it.copy(error = "No se pudo leer la imagen.", selectedImageUri = uri) }
            return
        }

        _uiState.update {
            it.copy(
                selectedImageUri = uri,
                originalBitmap = bitmap,
                filteredBitmap = null,
                filterName = null,
                error = null,
                uploadSuccess = false
            )
        }
    }

    fun applySimpleFilterPlaceholder() {
        // Placeholder de filtro SIN OpenCV (para comprobar flujo).
        // Luego lo cambiamos por OpenCV.
        val original = _uiState.value.originalBitmap ?: run {
            _uiState.update { it.copy(error = "Primero selecciona una imagen.") }
            return
        }

        val filtered = original.copy(Bitmap.Config.ARGB_8888, true) // (sin cambios)
        _uiState.update { it.copy(filteredBitmap = filtered, filterName = "placeholder", error = null) }
    }

    fun uploadPlaceholder() {
        val bitmapToUpload = _uiState.value.filteredBitmap ?: _uiState.value.originalBitmap
        if (bitmapToUpload == null) {
            _uiState.update { it.copy(error = "Primero selecciona una imagen.") }
            return
        }

        // Placeholder: luego aquí subimos a Firebase Storage + guardamos metadata en Firestore + GPS
        _uiState.update { it.copy(uploadSuccess = true, error = null) }
    }

    private fun uriToBitmap(uri: Uri, contentResolver: ContentResolver): Bitmap? {
        return try {
            contentResolver.openInputStream(uri).use { input ->
                if (input == null) null else BitmapFactory.decodeStream(input)
            }
        } catch (_: Exception) {
            null
        }
    }
}
