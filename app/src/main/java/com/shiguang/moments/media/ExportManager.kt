package com.shiguang.moments.media

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.shiguang.moments.data.models.MomentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

/** 导出为自包含 HTML（图片内嵌 base64），打开即是一本「美好之书」 */
object ExportManager {

    private val dayFmt = SimpleDateFormat("yyyy年M月d日 EEEE", Locale.CHINA)
    private val timeFmt = SimpleDateFormat("HH:mm", Locale.CHINA)

    private fun dayKey(ts: Long) = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date(ts))

    suspend fun exportHtml(ctx: Context, target: Uri, moments: List<MomentEntity>) {
        withContext(Dispatchers.IO) {
            val sb = StringBuilder(1 shl 16)
            sb.append("""<!doctype html><html lang="zh"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<style>
:root{--bg:#FFFBF5;--card:#FFFFFF;--ink:#3D2E2E;--sub:#9A8578;--rose:#C15570;--gold:#C98A3B}
*{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--ink);font-family:"Songti SC","Noto Serif SC",Georgia,serif;line-height:1.7}
.wrap{max-width:680px;margin:0 auto;padding:32px 20px 80px}
h1{font-size:28px;letter-spacing:2px;color:var(--rose);margin:8px 0 4px}
.sub{color:var(--sub);font-size:13px;margin-bottom:28px}
.day{margin-top:34px}
.day h2{font-size:16px;color:var(--gold);border-bottom:1px solid #F0E2DA;padding-bottom:6px;margin-bottom:14px}
.m{background:var(--card);border-radius:16px;padding:16px 18px;margin:12px 0;box-shadow:0 2px 10px rgba(80,40,40,.05);border:1px solid #F5E9E3}
.m .who{font-size:13px;color:var(--sub);margin-bottom:6px}
.m .txt{white-space:pre-wrap;font-size:15.5px}
.m img{width:100%;max-height:420px;object-fit:cover;border-radius:12px;margin-top:10px}
.m .quote{color:var(--rose);font-style:italic;border-left:3px solid var(--rose);padding-left:10px;margin-top:8px}
.m .tags{color:var(--sub);font-size:12px;margin-top:8px}
</style></head><body><div class="wrap"><h1>拾光 · 美好之书</h1><div class="sub">共 ${moments.size} 段被记住的美好瞬间</div>""")

            val sorted = moments.sortedByDescending { it.capturedAt }
            var lastDay: String? = null
            for ((i, m) in sorted.withIndex()) {
                val d = dayKey(m.capturedAt)
                if (d != lastDay) {
                    lastDay = d
                    sb.append("""<div class="day"><h2>${esc(dayFmt.format(Date(m.capturedAt)))}</h2></div>""")
                }
                sb.append("""<div class="m"><div class="who">${esc(m.sender)} · ${timeFmt.format(Date(m.capturedAt))}${if (m.starred) " · ★" else ""}</div>""")
                if (m.text.isNotBlank()) sb.append("""<div class="txt">${esc(m.text)}</div>""")
                if (!m.imagePath.isNullOrBlank()) {
                    val b64 = imageToBase64(m.imagePath!!)
                    if (b64 != null) sb.append("""<img src="data:image/jpeg;base64,$b64" alt="瞬间 ${i + 1}">""")
                }
                if (!m.quote.isNullOrBlank()) sb.append("""<div class="quote">「${esc(m.quote!!)}」</div>""")
                if (m.themeTags.isNotBlank()) sb.append("""<div class="tags">${esc(m.themeTags)}</div>""")
                sb.append("""</div>""")
            }
            sb.append("</div></body></html>")

            ctx.contentResolver.openOutputStream(target, "w")?.use { out ->
                out.write(sb.toString().toByteArray(Charsets.UTF_8))
            }
        }
    }

    suspend fun exportJson(ctx: Context, target: Uri, moments: List<MomentEntity>) {
        withContext(Dispatchers.IO) {
            val arr = JSONArray()
            for (m in moments.sortedByDescending { it.capturedAt }) {
                val jo = JSONObject()
                    .put("capturedAt", m.capturedAt).put("createdAt", m.createdAt)
                    .put("app", m.app).put("sender", m.sender)
                    .put("type", m.type.name).put("text", m.text)
                    .put("score", m.score).put("starred", m.starred)
                    .put("quote", m.quote ?: "").put("note", m.note ?: "")
                    .put("themeTags", m.themeTags).put("source", m.source)
                if (!m.imagePath.isNullOrBlank()) {
                    imageToBase64(m.imagePath!!)?.let { jo.put("imageBase64", it) }
                }
                arr.put(jo)
            }
            ctx.contentResolver.openOutputStream(target, "w")?.use { out ->
                out.write(arr.toString(2).toByteArray(Charsets.UTF_8))
            }
        }
    }

    private fun imageToBase64(path: String): String? {
        return try {
            val bytes = File(path).readBytes()
            if (bytes.size > 6 * 1024 * 1024) { null } else { Base64.encodeToString(bytes, Base64.NO_WRAP) }
        } catch (_: Throwable) { null }
    }

    private fun esc(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&#39;")
}