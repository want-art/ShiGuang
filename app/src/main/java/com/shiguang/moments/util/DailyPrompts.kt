package com.shiguang.moments.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** 每天一条温柔的提示，把「记录」变得轻；按 dayOfYear 取一条稳定的，不重复一周 */
object DailyPrompts {
    private val POOL = listOf(
        "今天谁让你笑过？哪怕只是一句话",
        "试着把最近听到最暖的那句话存下来",
        "拍一张今天让你停下脚步的画面",
        "把今天感谢过的人写下来",
        "此刻窗外是什么？雨还是阳光？",
        "今天被谁治愈过？记一句话就好",
        "想做但还没做的事，是哪一件？",
        "今天学到了什么？哪怕只是个小知识",
        "把一件让你烦的小事，换成它好笑的一面",
        "今天和某个人说过最长的一句对话是什么？",
        "记录一个最近常浮现的画面",
        "你最想和谁说一声「谢谢」？",
        "今天吃了什么让你舒服的？",
        "写一件今天自己做得好的小事",
        "今天看见的小细节（光/声音/气味）",
        "你最近读到的/听到的最温暖的一句话",
        "把今天让你想家的一刻记下来",
        "今天想推荐给朋友的一部剧/一首歌",
        "给今年的自己写一句鼓励",
        "今天你想被人记住的一件事是什么？",
        "把最近哭/笑的瞬间记一笔",
        "今天你最不想做什么？为什么？",
        "把手机里一张旧照片翻出来，说说它",
        "今天有什么让你心跳加速？",
        "记录一个你想念却联系不上的人",
        "今天最想感谢的小事",
        "今晚想用什么结束这一天？",
        "今天和某人的一小段对话",
        "你想对自己说的一句心里话",
        "今天什么让你觉得活着真好？",
    )

    fun promptForToday(): String = POOL[dayOfYear() % POOL.size]

    private fun dayOfYear(): Int {
        val now = Calendar.getInstance()
        return now.get(Calendar.DAY_OF_YEAR)
    }

    fun displayDate(): String =
        SimpleDateFormat("M月d日 EEEE", Locale.CHINA).format(Date())
}