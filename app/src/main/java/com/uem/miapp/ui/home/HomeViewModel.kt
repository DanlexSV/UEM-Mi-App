package com.uem.miapp.ui.home

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.uem.miapp.util.GPUImageUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.util.*

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

    fun applyGPUFilter(context: Context, filter: GPUImageUtils.FilterType) {
        val original = _uiState.value.originalBitmap ?: run {
            _uiState.update { it.copy(error = "Primero selecciona una imagen.") }
            return
        }

        val filtered = GPUImageUtils.applyFilter(context, original, filter)
        _uiState.update {
            it.copy(
                filteredBitmap = filtered,
                filterName = filter.name.lowercase(),
                error = null
            )
        }
    }

    fun uploadToFirebase(context: Context) {
        val bitmap = _uiState.value.filteredBitmap ?: _uiState.value.originalBitmap
        if (bitmap == null) {
            _uiState.update { it.copy(error = "Primero selecciona una imagen.") }
            return
        }

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            _uiState.update { it.copy(error = "Usuario no autenticado.") }
            return
        }

        val lat = _uiState.value.latitude
        val lng = _uiState.value.longitude
        val filter = _uiState.value.filterName ?: "none"

        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos)
        val imageData = baos.toByteArray()

        val filename = "images/${user.uid}_${System.currentTimeMillis()}.jpg"
        val storageRef = FirebaseStorage.getInstance().reference.child(filename)

        _uiState.update { it.copy(loading = true, error = null) }

        storageRef.putBytes(imageData)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    val metadata = hashMapOf(
                        "uid" to user.uid,
                        "url" to downloadUrl.toString(),
                        "lat" to lat,
                        "lng" to lng,
                        "filter" to filter,
                        "timestamp" to com.google.firebase.Timestamp.now()
                    )

                    FirebaseFirestore.getInstance()
                        .collection("photos")
                        .add(metadata)
                        .addOnSuccessListener {
                            _uiState.update {
                                it.copy(uploadSuccess = true, loading = false, error = null)
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("FIRESTORE", "Error metadata: ${e.message}")
                            _uiState.update {
                                it.copy(error = "Error al guardar metadata.", loading = false)
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("FIREBASE", "Error subida: ${e.message}")
                _uiState.update {
                    it.copy(error = "Error al subir imagen: ${e.message}", loading = false)
                }
            }
    }

    fun onImagePickedWithLocation(uri: Uri?, contentResolver: ContentResolver, lat: Double?, lng: Double?) {
        if (uri == null) return

        val inputStream = contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)

        _uiState.update {
            it.copy(
                originalBitmap = bitmap,
                latitude = lat,
                longitude = lng,
                uploadSuccess = false,
                error = null
            )
        }
    }

    fun onCapturedImage(bitmap: Bitmap?, lat: Double?, lng: Double?) {
        if (bitmap == null) {
            _uiState.update { it.copy(error = "No se pudo capturar la imagen.") }
            return
        }

        _uiState.update {
            it.copy(
                selectedImageUri = null,
                originalBitmap = bitmap,
                filteredBitmap = null,
                filterName = null,
                uploadSuccess = false,
                error = null,
                latitude = lat,
                longitude = lng
            )
        }
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
