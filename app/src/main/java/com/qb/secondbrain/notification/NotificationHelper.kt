package com.qb.secondbrain.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.qb.secondbrain.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        private const val CHANNEL_RECORDING = "channel_recording"
        private const val CHANNEL_RESULT = "channel_result"
        private const val CHANNEL_ERROR = "channel_error"

        private const val CHANNEL_RECORDING_NAME = "录音通知"
        private const val CHANNEL_RESULT_NAME = "处理结果"
        private const val CHANNEL_ERROR_NAME = "错误通知"

        private const val ID_RECORDING = 1001
        private const val ID_ADD_RESULT = 2001
        private const val ID_QUERY_RESULT = 2002
        private const val ID_UPDATE_RESULT = 2003
        private const val ID_DELETE_RESULT = 2004
        private const val ID_ERROR = 3001

        private val requestCodeGenerator = AtomicInteger(0)

        private fun nextRequestCode(): Int = requestCodeGenerator.getAndIncrement()
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_RECORDING,
                    CHANNEL_RECORDING_NAME,
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "录音进行中的通知"
                    setShowBadge(false)
                },
                NotificationChannel(
                    CHANNEL_RESULT,
                    CHANNEL_RESULT_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "语音处理结果通知"
                },
                NotificationChannel(
                    CHANNEL_ERROR,
                    CHANNEL_ERROR_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "处理错误通知"
                }
            )
            notificationManager.createNotificationChannels(channels)
        }
    }

    private fun baseBuilder(channelId: String): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
    }

    private fun createMainActivityPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            nextRequestCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun recordingNotification(): Notification {
        val cancelIntent = PendingIntent.getBroadcast(
            context,
            nextRequestCode(),
            Intent("com.qb.secondbrain.ACTION_CANCEL_RECORDING"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return baseBuilder(CHANNEL_RECORDING)
            .setContentTitle("正在录音...")
            .setOngoing(true)
            .addAction(android.R.drawable.ic_delete, "取消", cancelIntent)
            .build()
    }

    fun addResultNotification(memoId: Long, content: String): Notification {
        val truncatedContent = if (content.length > 50) content.substring(0, 50) + "..." else content
        return baseBuilder(CHANNEL_RESULT)
            .setContentTitle("已添加")
            .setContentText(truncatedContent)
            .setContentIntent(createMainActivityPendingIntent())
            .setAutoCancel(true)
            .build()
    }

    fun queryResultNotification(keywords: List<String>, results: List<String>): Notification {
        val count = results.size
        val keywordText = keywords.joinToString(", ")
        val bigText = results.joinToString("\n")
        return baseBuilder(CHANNEL_RESULT)
            .setContentTitle("找到 $count 条")
            .setContentText("关键词: $keywordText")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setContentIntent(createMainActivityPendingIntent())
            .setAutoCancel(true)
            .build()
    }

    fun updateResultNotification(memoId: Long, content: String): Notification {
        val truncatedContent = if (content.length > 50) content.substring(0, 50) + "..." else content
        return baseBuilder(CHANNEL_RESULT)
            .setContentTitle("已修改")
            .setContentText(truncatedContent)
            .setContentIntent(createMainActivityPendingIntent())
            .setAutoCancel(true)
            .build()
    }

    fun deleteResultNotification(content: String): Notification {
        val truncatedContent = if (content.length > 50) content.substring(0, 50) + "..." else content
        val undoIntent = PendingIntent.getBroadcast(
            context,
            nextRequestCode(),
            Intent("com.qb.secondbrain.ACTION_UNDO_DELETE"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return baseBuilder(CHANNEL_RESULT)
            .setContentTitle("已删除")
            .setContentText(truncatedContent)
            .addAction(android.R.drawable.ic_menu_revert, "撤销", undoIntent)
            .setTimeoutAfter(5000)
            .setAutoCancel(true)
            .build()
    }

    fun errorNotification(message: String): Notification {
        return baseBuilder(CHANNEL_ERROR)
            .setContentTitle("处理失败")
            .setContentText(message)
            .setAutoCancel(true)
            .build()
    }

    fun getNotificationId(type: NotificationType): Int {
        return when (type) {
            NotificationType.RECORDING -> ID_RECORDING
            NotificationType.ADD_RESULT -> ID_ADD_RESULT
            NotificationType.QUERY_RESULT -> ID_QUERY_RESULT
            NotificationType.UPDATE_RESULT -> ID_UPDATE_RESULT
            NotificationType.DELETE_RESULT -> ID_DELETE_RESULT
            NotificationType.ERROR -> ID_ERROR
        }
    }

    enum class NotificationType {
        RECORDING, ADD_RESULT, QUERY_RESULT, UPDATE_RESULT, DELETE_RESULT, ERROR
    }
}
