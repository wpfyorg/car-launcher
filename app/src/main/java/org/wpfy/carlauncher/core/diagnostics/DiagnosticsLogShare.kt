package org.wpfy.carlauncher.core.diagnostics

import android.app.Activity
import android.content.Context
import android.content.Intent
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DiagnosticsLogShare {
    suspend fun share(context: Context, diagnostics: String): Result<Unit> = runCatching {
        val report = withContext(Dispatchers.IO) {
            AppLog.i(Tag, "diagnostics log share requested")
            buildDiagnosticsReport(
                diagnostics = diagnostics,
                logs = AppLog.snapshot(),
                generatedAt = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            )
        }
        withContext(Dispatchers.Main) {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, report)
            }
            val chooser = Intent.createChooser(sendIntent, "Share Car Launcher diagnostics")
            if (context !is Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        }
    }

    private const val Tag = "DiagnosticsLogShare"
}
