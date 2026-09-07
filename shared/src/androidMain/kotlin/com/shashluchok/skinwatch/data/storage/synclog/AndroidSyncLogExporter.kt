package com.shashluchok.skinwatch.data.storage.synclog

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.shashluchok.skinwatch.domain.synclog.SyncLogExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val SHARED_DIRECTORY_NAME = "shared-logs"
private const val SHARED_FILE_NAME = "price-sync-log.txt"
private const val FILE_PROVIDER_SUFFIX = ".fileprovider"
private const val MIME_TYPE = "text/plain"

/**
 * Writes the rendered log into the app's cache and hands it to the system share sheet.
 *
 * Started from an application context, so the chooser needs its own task -- there is no activity in
 * the call chain when the export is triggered from a bottom sheet's ViewModel.
 */
internal class AndroidSyncLogExporter(
    private val context: Context,
) : SyncLogExporter {
    override val isSupported: Boolean = true

    override suspend fun export(text: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val directory = File(context.cacheDir, SHARED_DIRECTORY_NAME).apply { mkdirs() }
            val file = File(directory, SHARED_FILE_NAME).apply { writeText(text) }
            val uri = FileProvider.getUriForFile(
                context,
                context.packageName + FILE_PROVIDER_SUFFIX,
                file,
            )
            val send = Intent(Intent.ACTION_SEND).apply {
                type = MIME_TYPE
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, SHARED_FILE_NAME)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(send, SHARED_FILE_NAME).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        }.isSuccess
    }
}
