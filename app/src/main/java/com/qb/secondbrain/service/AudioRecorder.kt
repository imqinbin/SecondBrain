package com.qb.secondbrain.service

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Process
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.resume

class AudioRecorder(
    private val cacheDir: File,
    private val maxDurationMs: Long = 60_000L,
    private val sampleRate: Int = 16_000
) {
    companion object {
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BYTES_PER_SAMPLE = 2 // 16-bit = 2 bytes
    }

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    @Volatile
    private var isRecording = false
    private var currentPcmFile: File? = null

    val currentlyRecording: Boolean
        get() = isRecording

    @Synchronized
    fun startRecording(outputFileName: String): File {
        require(!isRecording) { "Already recording" }

        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, CHANNEL_CONFIG, AUDIO_FORMAT)
        val pcmFile = File(cacheDir, outputFileName)
        currentPcmFile = pcmFile

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            minBufferSize * 2
        )

        audioRecord?.startRecording()
        isRecording = true

        recordingThread = Thread({
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            writePcmData(pcmFile, minBufferSize)
        }, "AudioRecorderThread").also {
            it.start()
        }

        return pcmFile
    }

    private fun writePcmData(pcmFile: File, bufferSize: Int) {
        val buffer = ByteArray(bufferSize)
        FileOutputStream(pcmFile).use { outputStream ->
            var totalRead = 0L
            val maxBytes = sampleRate * BYTES_PER_SAMPLE * maxDurationMs / 1000

            while (isRecording && totalRead < maxBytes) {
                val read = audioRecord?.read(buffer, 0, bufferSize) ?: -1
                if (read > 0) {
                    outputStream.write(buffer, 0, read)
                    totalRead += read
                }
            }
        }
    }

    suspend fun stopRecording(): File? = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            if (!isRecording) {
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            isRecording = false

            try {
                audioRecord?.stop()
            } catch (_: IllegalStateException) {
                // AudioRecord not in proper state
            }

            recordingThread?.join(2000)

            val pcmFile = currentPcmFile
            val wavFile = pcmFile?.let {
                File(it.parent, it.nameWithoutExtension + ".wav")
            }

            if (pcmFile != null && pcmFile.exists() && wavFile != null) {
                try {
                    convertPcmToWav(pcmFile, wavFile)
                    pcmFile.delete()
                    continuation.resume(wavFile)
                } catch (e: Exception) {
                    continuation.resume(pcmFile)
                }
            } else {
                continuation.resume(null)
            }
        }
    }

    private fun convertPcmToWav(pcmFile: File, wavFile: File) {
        val pcmData = pcmFile.readBytes()
        val dataLength = pcmData.size
        val totalLength = 44 + dataLength

        RandomAccessFile(wavFile, "rw").use { raf ->
            // RIFF header
            raf.writeBytes("RIFF")
            raf.write(intToLittleEndian(totalLength - 8)) // file size - 8
            raf.writeBytes("WAVE")

            // fmt sub-chunk
            raf.writeBytes("fmt ")
            raf.write(intToLittleEndian(16))              // sub-chunk size
            raf.write(shortToLittleEndian(1))             // audio format: PCM
            raf.write(shortToLittleEndian(1))             // channels: mono
            raf.write(intToLittleEndian(sampleRate))      // sample rate
            raf.write(intToLittleEndian(sampleRate * BYTES_PER_SAMPLE)) // byte rate
            raf.write(shortToLittleEndian(BYTES_PER_SAMPLE))            // block align
            raf.write(shortToLittleEndian(16))            // bits per sample

            // data sub-chunk
            raf.writeBytes("data")
            raf.write(intToLittleEndian(dataLength))
            raf.write(pcmData)
        }
    }

    private fun intToLittleEndian(value: Int): ByteArray {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()
    }

    private fun shortToLittleEndian(value: Short): ByteArray {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array()
    }

    fun release() {
        isRecording = false
        recordingThread?.join(2000)
        try {
            audioRecord?.stop()
        } catch (_: IllegalStateException) {
            // Ignore
        }
        audioRecord?.release()
        audioRecord = null
        recordingThread = null
    }
}
