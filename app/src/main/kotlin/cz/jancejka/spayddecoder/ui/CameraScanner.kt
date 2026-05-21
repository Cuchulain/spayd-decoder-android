package cz.jancejka.spayddecoder.ui

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import zxingcpp.BarcodeReader
import java.util.concurrent.Executors

@Composable
fun CameraScanner(
    modifier: Modifier = Modifier,
    onScanned: (String) -> Unit,
    onPickImage: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val reader = remember {
        BarcodeReader(
            BarcodeReader.Options(
                formats = setOf(BarcodeReader.Format.QR_CODE),
                tryHarder = true,
                tryRotate = true,
                tryDownscale = true,
            )
        )
    }
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            analysisExecutor.shutdown()
        }
    }

    var fired = remember { false }

    LaunchedEffect(Unit) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { ia ->
                    ia.setAnalyzer(analysisExecutor) { proxy ->
                        if (fired) {
                            proxy.close()
                            return@setAnalyzer
                        }
                        try {
                            val results = reader.read(proxy)
                            results.firstOrNull()?.text?.let { value ->
                                if (!fired) {
                                    fired = true
                                    onScanned(value)
                                }
                            }
                        } catch (_: Exception) {
                        } finally {
                            proxy.close()
                        }
                    }
                }
            try {
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis,
                )
            } catch (_: Exception) {
            }
        }, ContextCompat.getMainExecutor(context))
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
        ) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize(),
            )
        }
        Text(
            "Namiř kameru na platební QR kód, nebo:",
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        OutlinedButton(
            onClick = onPickImage,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Vybrat obrázek ze zařízení") }
    }
}

@Composable
fun NoCameraPermission(
    onRequest: () -> Unit,
    onPickImage: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text("Aplikace potřebuje přístup ke kameře pro skenování QR kódu, nebo můžeš vybrat obrázek.")
        Button(onClick = onRequest, modifier = Modifier.fillMaxWidth()) {
            Text("Povolit přístup ke kameře")
        }
        OutlinedButton(onClick = onPickImage, modifier = Modifier.fillMaxWidth()) {
            Text("Vybrat obrázek ze zařízení")
        }
    }
}
