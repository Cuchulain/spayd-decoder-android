package cz.jancejka.spayddecoder

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jancejka.spayddecoder.data.ParsedSpayd
import cz.jancejka.spayddecoder.data.SpaydParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zxingcpp.BarcodeReader

sealed interface SpaydUiState {
    data object Idle : SpaydUiState
    data class Result(val parsed: ParsedSpayd) : SpaydUiState
    data class Error(val message: String) : SpaydUiState
}

class SpaydViewModel : ViewModel() {

    private val _state = MutableStateFlow<SpaydUiState>(SpaydUiState.Idle)
    val state: StateFlow<SpaydUiState> = _state.asStateFlow()

    private val reader = BarcodeReader(
        BarcodeReader.Options(
            formats = setOf(BarcodeReader.Format.QR_CODE),
            tryHarder = true,
            tryRotate = true,
            tryInvert = true,
            tryDownscale = true,
        )
    )

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
                val raw = withContext(Dispatchers.IO) {
                    val bitmap = context.contentResolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it)
                    } ?: return@withContext null
                    reader.read(bitmap).firstOrNull()?.text
                }
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
}
