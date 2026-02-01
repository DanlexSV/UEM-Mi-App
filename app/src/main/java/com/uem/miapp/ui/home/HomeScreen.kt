package com.uem.miapp.ui.home

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.ExperimentalMaterial3Api
import com.uem.miapp.util.GPUImageUtils
import com.uem.miapp.util.LocationUtils


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onSignOut: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    if (state.loading) {
        AlertDialog(
            onDismissRequest = { },
            confirmButton = {},
            title = { Text("Subiendo imagen") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator()
                    Spacer(Modifier.width(16.dp))
                    Text("Subiendo a Firebase…")
                }
            }
        )
    }

    val context = LocalContext.current
    val contentResolver = context.contentResolver

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            LocationUtils.requestCurrentLocation(context) { lat, lng ->
                viewModel.onImagePickedWithLocation(uri, contentResolver, lat, lng)
            }
        }
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
        onResult = { bitmap ->
            LocationUtils.requestCurrentLocation(context) { lat, lng ->
                viewModel.onCapturedImage(bitmap, lat, lng)
            }
        }
    )

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (!isGranted) {
                Log.e("PERMISSIONS", "Ubicación denegada!")
            }
        }
    )

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                cameraLauncher.launch(null)
            } else {
                Log.e("PERMISSIONS", "Cámara denegada!")
            }
        }
    )

    LaunchedEffect(Unit) {
        if (!LocationUtils.hasLocationPermission(context)) {
            locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (android.os.Build.VERSION.SDK_INT >= 23 &&
            context.checkSelfPermission(android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Home") },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.resetHome()
                            onSignOut()
                        }
                    ) { Text("Salir") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Subir fotografía",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )

            // Preview
            val preview = state.filteredBitmap ?: state.originalBitmap
            if (preview != null) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        bitmap = preview.asImageBitmap(),
                        contentDescription = "Foto seleccionada",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                    )
                }

                state.filterName?.let {
                    Text("Filtro aplicado: $it", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.imageLoading) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(12.dp))
                                Text("Cargando foto...")
                            }
                        } else {
                            Text("Aún no hay imagen seleccionada")
                        }
                    }
                }
            }

            // Botones
            Button(
                onClick = {
                    viewModel.setImageLoading(true)
                    pickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Elegir foto")
            }

            Button(
                onClick = {
                    viewModel.setImageLoading(true)
                    val granted = context.checkSelfPermission(android.Manifest.permission.CAMERA) ==
                            android.content.pm.PackageManager.PERMISSION_GRANTED

                    if (granted) {
                        cameraLauncher.launch(null)
                    } else {
                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Tomar foto")
            }

            val disabled = state.imageLoading || state.loading
            var showFilterMenu by remember { mutableStateOf(false) }

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { showFilterMenu = true },
                    enabled = !disabled,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Aplicar filtros")
                }

                DropdownMenu(
                    expanded = showFilterMenu,
                    onDismissRequest = { showFilterMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Sepia") },
                        onClick = {
                            viewModel.applyGPUFilter(context, GPUImageUtils.FilterType.SEPIA)
                            showFilterMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Escala de grises") },
                        onClick = {
                            viewModel.applyGPUFilter(context, GPUImageUtils.FilterType.GRAYSCALE)
                            showFilterMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Boceto") },
                        onClick = {
                            viewModel.applyGPUFilter(context, GPUImageUtils.FilterType.SKETCH)
                            showFilterMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Cómic (Toon)") },
                        onClick = {
                            viewModel.applyGPUFilter(context, GPUImageUtils.FilterType.TOON)
                            showFilterMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Sin filtro") },
                        onClick = {
                            viewModel.applyGPUFilter(context, GPUImageUtils.FilterType.NONE)
                            showFilterMenu = false
                        }
                    )
                }
            }

            Button(
                onClick = { viewModel.uploadToFirebase(context) },
                enabled = !disabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Subir imagen a Firebase")
            }

            // Mensajes
            state.error?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }

            if (state.uploadSuccess) {
                Text("Imagen subida correctamente", color = MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}
