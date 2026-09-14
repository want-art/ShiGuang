package com.shiguang.moments.data.repo

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File

/** 把相册/手动选择的图片复制到 App 私有目录（无需存储权限写，Coil 可直接加载绝对路径） */
object ImageStore {
    private const val TAG = "ImageStore"

    fun copyToLocal(ctx: Context, uri: Uri): String? {
        return try {
            val dir = File(ctx.getExternalFilesDir(null), "images").apply { mkdirs() }
            val out = File(dir, "img_${System.currentTimeMillis()}_${kotlin.random.Random.nextInt(1000)}.img")
            val n = ctx.contentResolver.openInputStream(uri)?.use { input ->
                out.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            if (n <= 0) { out.delete(); return null }
            out.absolutePath
        } catch (t: Throwable) {
            Log.w(TAG, "copy failed", t)
            null
        }
    }
}