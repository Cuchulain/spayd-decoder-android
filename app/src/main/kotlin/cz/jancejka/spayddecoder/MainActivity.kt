package cz.jancejka.spayddecoder

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import cz.jancejka.spayddecoder.ui.SpaydApp
import cz.jancejka.spayddecoder.ui.theme.SpaydDecoderTheme

class MainActivity : ComponentActivity() {

    private val viewModel: SpaydViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            SpaydDecoderTheme {
                SpaydApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent ?: return
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val type = intent.type ?: return
                when {
                    type == "text/plain" -> {
                        viewModel.handleText(intent.getStringExtra(Intent.EXTRA_TEXT))
                    }
                    type.startsWith("image/") -> {
                        val uri = intent.extraStreamCompat()
                        if (uri != null) viewModel.handleImage(this, uri)
                    }
                }
            }
            Intent.ACTION_VIEW -> {
                intent.dataString?.let { viewModel.handleText(it) }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun Intent.extraStreamCompat(): Uri? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            getParcelableExtra(Intent.EXTRA_STREAM)
        }
}
