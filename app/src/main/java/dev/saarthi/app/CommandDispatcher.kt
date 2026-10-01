package dev.saarthi.app

import android.content.Context
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri

enum class DispatchResult { Requested, Unavailable }

class CommandDispatcher(private val context: Context) {
    fun dispatch(action: ExternalAction): DispatchResult {
        val intent = Intent(action.action, action.uri?.let(Uri::parse)).apply {
            action.packageName?.let(::setPackage)
            action.extras.forEach { (key, value) -> putExtra(key, value) }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            DispatchResult.Requested
        } catch (_: ActivityNotFoundException) {
            DispatchResult.Unavailable
        } catch (_: SecurityException) {
            DispatchResult.Unavailable
        }
    }
}
