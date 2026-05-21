package cz.jancejka.spayddecoder.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cz.jancejka.spayddecoder.SpaydUiState
import cz.jancejka.spayddecoder.SpaydViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaydApp(viewModel: SpaydViewModel) {
    val state by viewModel.state.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) viewModel.handleImage(context, uri) }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("SPAYD dekodér") }) },
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                SpaydUiState.Idle -> IdleScreen(
                    hasCameraPermission = hasCameraPermission,
                    onRequestCameraPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onScanned = viewModel::handleScannedText,
                    onPickImage = { imagePicker.launch("image/*") },
                    contentPadding = PaddingValues(16.dp),
                )
                is SpaydUiState.Result -> ResultView(
                    parsed = s.parsed,
                    snackbarHost = snackbarHost,
                    onReset = viewModel::reset,
                )
                is SpaydUiState.Error -> ErrorView(
                    message = s.message,
                    onReset = viewModel::reset,
                )
            }
        }
    }
}

@Composable
private fun IdleScreen(
    hasCameraPermission: Boolean,
    onRequestCameraPermission: () -> Unit,
    onScanned: (String) -> Unit,
    onPickImage: () -> Unit,
    contentPadding: PaddingValues,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (hasCameraPermission) {
            CameraScanner(
                modifier = Modifier.fillMaxSize(),
                onScanned = onScanned,
                onPickImage = onPickImage,
            )
        } else {
            NoCameraPermission(
                onRequest = onRequestCameraPermission,
                onPickImage = onPickImage,
            )
        }
    }
}
