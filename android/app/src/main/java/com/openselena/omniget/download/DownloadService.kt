package com.openselena.omniget.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.openselena.omniget.MainActivity
import com.openselena.omniget.OmniGetApplication
import com.openselena.omniget.R
import com.openselena.omniget.data.DownloadRepository
import com.openselena.omniget.data.DownloadStatus
import com.openselena.omniget.data.DownloadTask
import com.openselena.omniget.data.SettingsRepository
import com.openselena.omniget.util.ProgressParser
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class DownloadService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var downloads: DownloadRepository
    private lateinit var settings: SettingsRepository
    private lateinit var notificationManager: NotificationManager
    private var loopJob: Job? = null

    @Volatile
    private var activeTaskId: String? = null

    override fun onCreate() {
        super.onCreate()
        val application = application as OmniGetApplication
        downloads = application.downloads
        settings = application.settings
        notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> {
                val id = intent.getStringExtra(EXTRA_TASK_ID) ?: return START_NOT_STICKY
                downloads.markPaused(id)
                DownloadEngine.cancel(id)
            }

            ACTION_CANCEL -> {
                val id = intent.getStringExtra(EXTRA_TASK_ID) ?: return START_NOT_STICKY
                downloads.removeFromService(id)
                DownloadEngine.cancel(id)
                FileDelivery.deleteStagingFiles(applicationContext, id)
            }

            ACTION_START, null -> ensureLoop()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        activeTaskId?.let { id ->
            if (downloads.get(id)?.status == DownloadStatus.DOWNLOADING) {
                downloads.markPaused(id)
            }
            DownloadEngine.cancel(id)
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun ensureLoop() {
        if (loopJob?.isActive == true) return
        startInForeground(
            title = getString(R.string.app_name),
            text = getString(R.string.notification_preparing),
            progress = null,
        )
        loopJob = serviceScope.launch {
            try {
                DownloadEngine.initialize(applicationContext)
                while (isActive) {
                    val task = downloads.nextQueued()
                    if (task == null) {
                        delay(350)
                        if (downloads.nextQueued() == null) break else continue
                    } else {
                        process(task)
                    }
                }
            } catch (error: Throwable) {
                if (error !is CancellationException) {
                    downloads.nextQueued()?.let {
                        downloads.markFailed(it.id, friendlyError(error))
                    }
                }
            } finally {
                loopJob = null
                if (downloads.nextQueued() != null) {
                    ensureLoop()
                } else {
                    ServiceCompat.stopForeground(this@DownloadService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
    }

    private suspend fun process(task: DownloadTask) {
        activeTaskId = task.id
        downloads.markDownloading(task.id)
        var lastNotificationAt = 0L
        var lastPersistAt = 0L

        try {
            val result = DownloadEngine.download(applicationContext, task, settings.settings.value) { progress, eta, line ->
                val parsed = ProgressParser.parse(line, progress, eta)
                val now = System.currentTimeMillis()
                if (now - lastPersistAt >= 500L || parsed.percent >= 100f) {
                    lastPersistAt = now
                    downloads.updateProgress(
                        id = task.id,
                        progress = parsed.percent,
                        speedBps = parsed.speedBps,
                        etaSeconds = parsed.etaSeconds,
                        downloadedBytes = parsed.downloadedBytes,
                        totalBytes = parsed.totalBytes,
                        statusLine = parsed.detail,
                    )
                }
                if (now - lastNotificationAt >= 900L) {
                    lastNotificationAt = now
                    postProgress(task, parsed.percent, parsed.processing, parsed.detail)
                }
            }

            val current = downloads.get(task.id) ?: return
            if (current.status == DownloadStatus.PAUSED) return

            val rawFiles = result.filePaths.map(::File).filter(File::isFile)
            if (rawFiles.isEmpty()) {
                downloads.markFailed(task.id, "The engine finished without producing a file.")
                FileDelivery.deleteStagingFiles(applicationContext, task.id)
                postFailure(task)
                return
            }

            val totalBytes = rawFiles.sumOf(File::length)
            downloads.updateProgress(task.id, 100f, 0L, 0L, totalBytes, totalBytes, "")
            val delivered = FileDelivery.deliverAll(applicationContext, rawFiles)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && delivered.allPublished) {
                FileDelivery.deleteStagingFiles(applicationContext, task.id)
            }
            downloads.markCompleted(
                task.id,
                delivered.primary.localPath,
                delivered.primary.uri,
            )
            postCompleted(task, Uri.parse(delivered.primary.uri))
        } catch (_: YoutubeDL.CanceledException) {
            when (downloads.get(task.id)?.status) {
                DownloadStatus.PAUSED, null -> Unit
                else -> {
                    downloads.markFailed(task.id, "Download was interrupted.")
                    FileDelivery.deleteStagingFiles(applicationContext, task.id)
                    postFailure(task)
                }
            }
        } catch (error: Throwable) {
            val current = downloads.get(task.id) ?: return
            if (current.status == DownloadStatus.PAUSED) return
            downloads.markFailed(task.id, friendlyError(error))
            FileDelivery.deleteStagingFiles(applicationContext, task.id)
            postFailure(task)
        } finally {
            activeTaskId = null
            notificationManager.cancel(FOREGROUND_NOTIFICATION_ID)
        }
    }

    private fun postProgress(task: DownloadTask, progress: Float, processing: Boolean, detail: String) {
        val knownProgress = progress > 0f && !processing
        val notification = baseNotification()
            .setContentTitle(task.title)
            .setContentText(
                if (processing) getString(R.string.processing)
                else detail.ifBlank { getString(R.string.status_downloading) },
            )
            .setProgress(100, progress.toInt().coerceIn(0, 100), !knownProgress)
            .addAction(0, getString(R.string.notification_pause), serviceIntent(ACTION_PAUSE, task.id, 11))
            .addAction(0, getString(R.string.notification_cancel), serviceIntent(ACTION_CANCEL, task.id, 12))
            .setContentIntent(openAppIntent())
            .setOngoing(true)
            .build()
        notificationManager.notify(FOREGROUND_NOTIFICATION_ID, notification)
    }

    private fun postCompleted(task: DownloadTask, uri: Uri) {
        val openIntent = PendingIntent.getActivity(
            this,
            task.id.hashCode(),
            FileDelivery.openIntent(this, uri),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = completionNotification(task)
            .setContentText(getString(R.string.notification_complete, task.title))
            .setContentIntent(openIntent)
            .addAction(0, getString(R.string.notification_open), openIntent)
            .build()
        notificationManager.notify(completionId(task.id), notification)
    }

    private fun postFailure(task: DownloadTask) {
        val notification = completionNotification(task)
            .setContentText(getString(R.string.notification_failed))
            .setContentIntent(openAppIntent())
            .build()
        notificationManager.notify(completionId(task.id), notification)
    }

    private fun startInForeground(title: String, text: String, progress: Int?) {
        val notification = baseNotification()
            .setContentTitle(title)
            .setContentText(text)
            .setProgress(100, progress ?: 0, progress == null)
            .setOngoing(true)
            .setContentIntent(openAppIntent())
            .build()
        ServiceCompat.startForeground(
            this,
            FOREGROUND_NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0,
        )
    }

    private fun baseNotification(): NotificationCompat.Builder =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_download)
            .setColor(ContextCompat.getColor(this, R.color.ic_launcher_background))
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)

    private fun completionNotification(task: DownloadTask): NotificationCompat.Builder =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_download)
            .setColor(ContextCompat.getColor(this, R.color.ic_launcher_background))
            .setContentTitle(task.title)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

    private fun openAppIntent(): PendingIntent =
        PendingIntent.getActivity(
            this,
            7,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_OPEN_DOWNLOADS, true)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun serviceIntent(action: String, taskId: String, requestBase: Int): PendingIntent =
        PendingIntent.getService(
            this,
            (taskId.hashCode() * 31) xor requestBase,
            Intent(this, DownloadService::class.java).apply {
                this.action = action
                putExtra(EXTRA_TASK_ID, taskId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notification_channel_description)
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun completionId(taskId: String) = taskId.hashCode().let {
        if (it == FOREGROUND_NOTIFICATION_ID) it + 1 else it
    }

    private fun friendlyError(error: Throwable): String {
        val raw = error.message.orEmpty()
        val lower = raw.lowercase()
        return when {
            lower.contains("sign in") || lower.contains("login") || lower.contains("cookies") ->
                getString(R.string.error_cookie_hint)
            else -> raw.lineSequence()
                .map(String::trim)
                .lastOrNull { it.isNotBlank() }
                ?.removePrefix("ERROR:")
                ?.trim()
                ?.take(500)
                ?.ifBlank { error.javaClass.simpleName }
                ?: error.javaClass.simpleName
        }
    }

    companion object {
        private const val CHANNEL_ID = "omniget.downloads"
        private const val FOREGROUND_NOTIFICATION_ID = 0x4F47
        private const val ACTION_START = "com.openselena.omniget.action.START"
        private const val ACTION_PAUSE = "com.openselena.omniget.action.PAUSE"
        private const val ACTION_CANCEL = "com.openselena.omniget.action.CANCEL"
        private const val EXTRA_TASK_ID = "task_id"

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, DownloadService::class.java).setAction(ACTION_START),
            )
        }

        fun pause(context: Context, taskId: String) {
            runCatching {
                context.startService(
                    Intent(context, DownloadService::class.java).apply {
                        action = ACTION_PAUSE
                        putExtra(EXTRA_TASK_ID, taskId)
                    },
                )
            }
        }

        fun cancel(context: Context, taskId: String) {
            DownloadEngine.cancel(taskId)
            runCatching {
                context.startService(
                    Intent(context, DownloadService::class.java).apply {
                        action = ACTION_CANCEL
                        putExtra(EXTRA_TASK_ID, taskId)
                    },
                )
            }
        }
    }
}
