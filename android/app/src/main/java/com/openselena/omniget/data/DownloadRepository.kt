package com.openselena.omniget.data

import android.content.Context
import android.net.Uri
import com.openselena.omniget.download.DownloadService
import com.openselena.omniget.download.FileDelivery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class DownloadRepository(context: Context) {
    private val appContext = context.applicationContext
    private val stateFile = File(appContext.filesDir, "downloads.json")
    private val lock = Any()
    private val _tasks = MutableStateFlow(restore())

    val tasks = _tasks.asStateFlow()

    fun enqueue(task: DownloadTask): DownloadTask {
        synchronized(lock) {
            val current = _tasks.value.toMutableList()
            current.add(0, task)
            publish(current)
        }
        DownloadService.start(appContext)
        return task
    }

    fun get(id: String): DownloadTask? = _tasks.value.firstOrNull { it.id == id }

    fun nextQueued(): DownloadTask? = _tasks.value
        .filter { it.status == DownloadStatus.QUEUED }
        .minByOrNull { it.createdAt }

    fun markDownloading(id: String) = change(id) {
        it.copy(
            status = DownloadStatus.DOWNLOADING,
            progress = 0f,
            speedBps = 0L,
            etaSeconds = -1L,
            downloadedBytes = 0L,
            totalBytes = 0L,
            statusLine = "",
            error = null,
        )
    }

    fun updateProgress(
        id: String,
        progress: Float,
        speedBps: Long,
        etaSeconds: Long,
        downloadedBytes: Long,
        totalBytes: Long,
        statusLine: String,
    ) = change(id, persist = false) {
        it.copy(
            progress = progress.coerceIn(0f, 100f),
            speedBps = speedBps,
            etaSeconds = etaSeconds,
            downloadedBytes = downloadedBytes,
            totalBytes = totalBytes,
            statusLine = statusLine,
        )
    }

    fun markCompleted(id: String, filePath: String?, fileUri: String?) = change(id) {
        it.copy(
            status = DownloadStatus.COMPLETED,
            progress = 100f,
            speedBps = 0L,
            etaSeconds = 0L,
            filePath = filePath,
            fileUri = fileUri,
            statusLine = "",
            error = null,
        )
    }

    fun markFailed(id: String, message: String) = change(id) {
        it.copy(
            status = DownloadStatus.FAILED,
            speedBps = 0L,
            etaSeconds = -1L,
            error = message,
            statusLine = "",
        )
    }

    fun markPaused(id: String) = change(id) {
        if (it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED) {
            it.copy(status = DownloadStatus.PAUSED, speedBps = 0L, etaSeconds = -1L)
        } else {
            it
        }
    }

    fun queue(id: String) = change(id) {
        it.copy(
            status = DownloadStatus.QUEUED,
            progress = 0f,
            speedBps = 0L,
            etaSeconds = -1L,
            downloadedBytes = 0L,
            statusLine = "",
            error = null,
        )
    }

    fun pause(id: String) {
        markPaused(id)
        DownloadService.pause(appContext, id)
    }

    fun resume(id: String) {
        queue(id)
        DownloadService.start(appContext)
    }

    fun retry(id: String) {
        queue(id)
        DownloadService.start(appContext)
    }

    fun cancel(id: String) {
        val task = get(id) ?: return
        synchronized(lock) {
            publish(_tasks.value.filterNot { it.id == id })
        }
        DownloadService.cancel(appContext, id)
        FileDelivery.deleteStagingFiles(appContext, id)
        task.filePath?.let { runCatching { File(it).delete() } }
    }

    fun remove(id: String, deleteFile: Boolean = false) {
        val task = get(id) ?: return
        if (task.status == DownloadStatus.DOWNLOADING) {
            DownloadService.cancel(appContext, id)
        }
        synchronized(lock) {
            publish(_tasks.value.filterNot { it.id == id })
        }
        if (deleteFile) {
            task.filePath?.let { runCatching { File(it).delete() } }
        }
    }

    fun removeFromService(id: String, deleteFile: Boolean = false) {
        val task = get(id) ?: return
        synchronized(lock) {
            publish(_tasks.value.filterNot { it.id == id })
        }
        if (deleteFile) {
            task.filePath?.let { runCatching { File(it).delete() } }
        }
    }

    fun clearFinished() {
        synchronized(lock) {
            publish(_tasks.value.filterNot {
                it.status == DownloadStatus.COMPLETED || it.status == DownloadStatus.FAILED
            })
        }
    }

    fun pauseAll() {
        val active = _tasks.value.filter { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED }
        active.forEach { markPaused(it.id) }
        active.forEach { DownloadService.pause(appContext, it.id) }
    }

    fun hasActiveDownloads(): Boolean = _tasks.value.any {
        it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED
    }

    fun contentUriFor(task: DownloadTask): Uri? = task.fileUri?.let(Uri::parse)

    private fun change(
        id: String,
        persist: Boolean = true,
        transform: (DownloadTask) -> DownloadTask,
    ) {
        synchronized(lock) {
            val next = _tasks.value.map {
                if (it.id == id) transform(it).copy(updatedAt = System.currentTimeMillis()) else it
            }
            _tasks.value = next
            if (persist) persist(next)
        }
    }

    private fun publish(next: List<DownloadTask>) {
        _tasks.value = next
        persist(next)
    }

    private fun persist(tasks: List<DownloadTask>) {
        runCatching {
            stateFile.parentFile?.mkdirs()
            val temporary = File(stateFile.parentFile, "${stateFile.name}.tmp")
            temporary.writeText(DownloadTask.listToJson(tasks))
            if (!temporary.renameTo(stateFile)) {
                temporary.copyTo(stateFile, overwrite = true)
                temporary.delete()
            }
        }
    }

    private fun restore(): List<DownloadTask> {
        val raw = runCatching { stateFile.takeIf(File::exists)?.readText() }.getOrNull()
        val loaded = raw?.let(DownloadTask::listFromJson).orEmpty()
        return loaded.map {
            if (it.status == DownloadStatus.DOWNLOADING) {
                it.copy(status = DownloadStatus.PAUSED, speedBps = 0L, etaSeconds = -1L)
            } else {
                it
            }
        }
    }
}
