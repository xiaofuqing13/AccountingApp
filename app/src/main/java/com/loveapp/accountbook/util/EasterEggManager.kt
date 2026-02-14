package com.loveapp.accountbook.util

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import com.loveapp.accountbook.R

data class LoveWord(val emoji: String, val title: String, val text: String)

object EasterEggManager {

    val loveWords = listOf(
        LoveWord("💕", "第365天", "从第一天牵你的手开始，\n我就决定这辈子都不放开了。\n\n谢谢你愿意和我一起走过\n这365个日日夜夜。"),
        LoveWord("🌙", "晚安情话", "我对世界说晚安，\n唯独对你说喜欢。\n\n今晚的月亮很美，\n但不及你万分之一。"),
        LoveWord("☀️", "早安情话", "每天早上醒来，\n第一件事就是看你发没发消息。\n\n你是我睁眼后，\n最想见到的人。"),
        LoveWord("🌧️", "下雨天想你", "下雨了我会想你，\n天晴了我也想你。\n\n不管什么天气，\n反正我都在想你。"),
        LoveWord("🎂", "小小心愿", "如果可以许一个愿望，\n我希望你每天都开开心心的。\n\n如果可以许两个，\n那第二个是永远在一起。"),
        LoveWord("✨", "遇见你之后", "遇见你之前，\n我没想过结婚。\n遇见你之后，\n我没想过别人。"),
        LoveWord("🏠", "关于未来", "等攒够了钱，\n我们就买一个小房子。\n\n养一只猫，\n周末一起做饭。\n你负责吃，我负责洗碗。"),
        LoveWord("📝", "给你写的", "这个APP是我专门为你写的，\n每一行代码里都藏着我对你的喜欢。\n\n你看到的每一个粉色，\n都是我心里你的颜色。")
    )

    fun showLovePopup(context: Context, loveWord: LoveWord) {
        val builder = AlertDialog.Builder(context)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_love_popup, null)
        view.findViewById<TextView>(R.id.tv_emoji).text = loveWord.emoji
        view.findViewById<TextView>(R.id.tv_title).text = loveWord.title
        view.findViewById<TextView>(R.id.tv_text).text = loveWord.text

        val dialog = builder.setView(view).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        view.findViewById<TextView>(R.id.btn_close).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    fun showRandomLovePopup(context: Context) {
        showLovePopup(context, loveWords.random())
    }

    val moodEmojis = listOf("🥰", "😘", "💋", "😍", "🤗", "💕", "❤️", "💗", "✨", "🌸")
    val moodWords = listOf(
        "你笑起来真好看", "想捏捏你的脸", "今天也超喜欢你",
        "你是我的小太阳", "全世界你最可爱", "想rua你的头发",
        "看到你就开心", "你是草莓味的"
    )

    // 特殊彩蛋
    val egg520 = LoveWord("💝", "¥520.00", "520 = 我爱你\n\n给你花的每一分钱，\n都是心甘情愿的。\n\n因为你值得全世界最好的。")
    val egg99 = LoveWord("🌷", "99朵郁金香", "99 = 长长久久\n\n给你买花不需要理由，\n但如果非要一个的话：\n\n因为你比花还好看。")
    val eggSalary = LoveWord("💼", "工资到啦", "发工资最开心的事，\n不是数字变多了，\n\n而是又可以带你\n去吃你想吃的东西了。\n\n我的钱就是你的钱。")
    val eggDarkMode = LoveWord("🌙", "不用开深色模式", "因为有你在，\n我的世界永远是亮的。\n\n你就是我生命里的那束光。")
    val eggLock = LoveWord("🔐", "这个APP的密码是", "密码就是我们在一起的日子：\n\n0214\n\n因为从那天起，\n我的心就只为你上锁了。")
    val eggVersion = LoveWord("💌", "写给你的秘密", "这个APP从第一行代码开始，\n就是为你而写的。\n\n每一个按钮、每一种颜色，\n都是按照你喜欢的样子设计的。\n\n——爱你的我")
    val egg214 = LoveWord("🗓️", "2月14日", "这一天不开会，\n只想和你约会。\n\n在我的日程表里，\n每一天都写着你的名字。")
    val eggSearch = LoveWord("🔍", "找到了", "你搜索的答案只有一个：\n\n我爱你。\n\n不管搜多少次，\n答案都不会变。")
    val eggMiss = LoveWord("💭", "我也想你", "你在搜「想你」？\n\n巧了，我也在想你。\n现在，此刻，每一秒。")
    val eggLongPress = LoveWord("💍", "未来可期", "你按住这个按钮的时间，\n就像我想和你在一起的时间。\n\n永远不想松手。\n\n余生请多指教。")
}
