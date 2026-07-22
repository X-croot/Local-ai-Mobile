package com.localai.mobile.system

import android.content.Context
import android.content.Intent
import com.topjohnwu.superuser.Shell

/**
 * Optional system integration. Everything here is GRACEFUL: if the device has
 * no root and no Termux, nothing crashes — the feature simply reports that it
 * is unavailable.
 *
 *  - Root: via libsu (topjohnwu). Runs `su` only if genuinely available.
 *  - Termux: via the RUN_COMMAND intent. If Termux isn't installed, we detect
 *    it and return a friendly message instead of throwing.
 */
object SystemTools {

    data class ExecResult(val ok: Boolean, val output: String, val method: String)

    /** True only if the device is actually rooted and grants su. */
    fun hasRoot(): Boolean = try { Shell.getShell().isRoot } catch (e: Throwable) { false }

    /** Run a command with root (libsu). Never throws. */
    fun runAsRoot(cmd: String): ExecResult {
        if (!hasRoot()) return ExecResult(false, "Root not available on this device.", "root")
        return try {
            val res = Shell.cmd(cmd).exec()
            ExecResult(res.isSuccess, (res.out + res.err).joinToString("\n"), "root")
        } catch (e: Throwable) {
            ExecResult(false, "Root exec failed: ${e.message}", "root")
        }
    }

    fun isTermuxInstalled(ctx: Context): Boolean = try {
        ctx.packageManager.getPackageInfo("com.termux", 0); true
    } catch (e: Exception) { false }

    /**
     * Send a command to Termux via RUN_COMMAND. Requires the user to have
     * enabled `allow-external-apps=true` in Termux. If Termux is missing we
     * return gracefully so the app never crashes.
     */
    fun runInTermux(ctx: Context, command: String): ExecResult {
        if (!isTermuxInstalled(ctx)) {
            return ExecResult(false, "Termux is not installed. Install it to run commands.", "termux")
        }
        return try {
            val intent = Intent().apply {
                setClassName("com.termux", "com.termux.app.RunCommandService")
                action = "com.termux.RUN_COMMAND"
                putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash")
                putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-c", command))
                putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
            }
            ctx.startForegroundService(intent)
            ExecResult(true, "Command dispatched to Termux.", "termux")
        } catch (e: Throwable) {
            ExecResult(false, "Termux exec failed (enable allow-external-apps): ${e.message}", "termux")
        }
    }
}
