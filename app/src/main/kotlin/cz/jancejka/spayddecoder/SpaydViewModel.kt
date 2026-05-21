package cz.jancejka.spayddecoder

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jancejka.spayddecoder.data.ParsedSpayd
import cz.jancejka.spayddecoder.data.SpaydParser
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed interface SpaydUiState {
    data object Idle : SpaydUiState
    data class Result(val parsed: ParsedSpayd) : SpaydUiState
    data class Error(val message: String) : SpaydUiState
}

class SpaydViewModel : ViewModel() {

    private val _state = MutableStateFlow<SpaydUiState>(SpaydUiState.Idle)
    val state: StateFlow<SpaydUiState> = _state.asStateFlow()

    fun reset() {
        _state.value = SpaydUiState.Idle
    }

    fun handleText(text: String?) {
        if (text.isNullOrBlank()) {
            _state.value = SpaydUiState.Error("Sdílený text je prázdný.")
            return
        }
        parseAndPublish(text)
    }

    fun handleImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val image = InputImage.fromFilePath(context, uri)
                val barcode = scanForQr(image)
                val raw = barcode?.rawValue
                if (raw.isNullOrBlank()) {
                    _state.value = SpaydUiState.Error("V obrázku se nepodařilo najít QR kód.")
                } else {
                    parseAndPublish(raw)
                }
            } catch (e: Exception) {
                _state.value = SpaydUiState.Error("Chyba při čtení obrázku: ${e.message}")
            }
        }
    }

    fun handleScannedText(text: String) {
        parseAndPublish(text)
    }

    private fun parseAndPublish(text: String) {
        _state.value = try {
            SpaydUiState.Result(SpaydParser.parse(text))
        } catch (e: SpaydParser.SpaydException) {
            SpaydUiState.Error("${e.message}\n\nObsah: $text")
        } catch (e: Exception) {
            SpaydUiState.Error("Neznámá chyba: ${e.message}")
        }
    }

    private suspend fun scanForQr(image: InputImage): Barcode? =
        suspendCancellableCoroutine { cont ->
            val scanner = BarcodeScanning.getClient()
            scanner.process(image)
                .addOnSuccessListener { list -> cont.resume(list.firstOrNull()) }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
                .addOnCompleteListener { scanner.close() }
        }
}
