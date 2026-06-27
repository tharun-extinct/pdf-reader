import com.pdfreader.app.domain.repository.PdfTextBox
import com.pdfreader.app.domain.tts.TtsState

/**
 * Represents the entire state of the PDF Reader screen.
 */
data class PdfReaderState(
    val isAnnotationSettingsOpen: Boolean = false,
    val ttsState: TtsState = TtsState.Idle
)

/**
 * Represents the intents that can be sent to the PdfReader.
 */
sealed class PdfReaderIntent {
    data class AddTextAnnotation(val pageIndex: Int, val position: androidx.compose.ui.geometry.Offset) : PdfReaderIntent()
    data class UpdateTextAnnotation(val annotationId: Long, val text: String) : PdfReaderIntent()
    data class PlayTts(val text: String) : PdfReaderIntent()
    object PauseTts : PdfReaderIntent()
    object ResumeTts : PdfReaderIntent()
    object StopTts : PdfReaderIntent()
}