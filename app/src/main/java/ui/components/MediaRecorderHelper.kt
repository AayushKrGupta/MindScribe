package ui.components

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.widget.Toast
import java.io.File
import java.io.IOException

class MediaRecorderHelper(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: String = ""

    fun startRecording() {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        if (dir == null) {
            Toast.makeText(context, "Storage access error", Toast.LENGTH_SHORT).show()
            return
        }
        outputFile = File(dir, "recorded_note.3gp").absolutePath

        try {
            recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setOutputFile(outputFile)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                prepare()
                start()
            }
            Toast.makeText(context, "Recording Started", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Recording Failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            recorder?.release()
            recorder = null
        }
    }

    fun stopRecording() {
        try {
            recorder?.apply {
                stop()
                release()
                Toast.makeText(context, "Recording Saved: $outputFile", Toast.LENGTH_SHORT).show()
            }
        } catch (e: RuntimeException) {
            e.printStackTrace()
            Toast.makeText(context, "Recording Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        } finally {
            recorder = null
        }
    }
}
