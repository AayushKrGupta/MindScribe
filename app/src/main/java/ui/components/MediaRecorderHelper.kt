
package ui.components

import android.content.Context
import android.media.MediaRecorder
import android.os.Environment
import android.widget.Toast
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MediaRecorderHelper(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: String? = null
    private var isRecording = false

    fun startRecording(): String? {
        if (isRecording) {
            return null
        }

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Recording_$timeStamp.3gp"
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)

        if (storageDir == null) {
            Toast.makeText(context, "Cannot access storage", Toast.LENGTH_SHORT).show()
            return null
        }

        outputFile = File(storageDir, fileName).absolutePath

        try {
            recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(outputFile)
                prepare()
                start()
            }
            isRecording = true
            Toast.makeText(context, "Recording started", Toast.LENGTH_SHORT).show()
            return outputFile
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            Toast.makeText(context, "Recording failed: ${e.message}", Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, "Recording failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
        return null
    }

    fun stopRecording(): String? {
        return try {
            recorder?.apply {
                stop()
                release()
            }
            Toast.makeText(context, "Recording saved", Toast.LENGTH_SHORT).show()
            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error stopping recording: ${e.message}", Toast.LENGTH_LONG).show()
            null
        } finally {
            recorder = null
            isRecording = false
            // Add a small delay to ensure file is fully written
            Thread.sleep(200)
        }
    }

    fun isRecording(): Boolean = isRecording

    fun getCurrentOutputFile(): String? = outputFile
}