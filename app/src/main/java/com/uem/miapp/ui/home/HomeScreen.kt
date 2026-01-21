package com.uem.miapp.ui.home

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
import androidx.compose.runtime.OptIn


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onSignOut: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    val context = LocalContext.current
    val contentResolver = context.contentResolver

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            viewModel.onImagePicked(uri, contentResolver)
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Home") },
                actions = {
                    TextButton(onClick = onSignOut) { Text("Salir") }
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
                        Text("Aún no hay imagen seleccionada")
                    }
                }
            }

            // Botones
            Button(
                onClick = {
                    pickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Elegir foto")
            }

            OutlinedButton(
                onClick = { viewModel.applySimpleFilterPlaceholder() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Aplicar filtro (placeholder)")
            }

            Button(
                onClick = { viewModel.uploadPlaceholder() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Subir (placeholder)")
            }

            // Mensajes
            state.error?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }

            if (state.uploadSuccess) {
                Text("✅ Subida simulada OK (luego Firebase)", color = MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}
