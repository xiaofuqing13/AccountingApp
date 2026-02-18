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
        LoveWord("📝", "给你写的", "这个APP是我专门为你写的，\n每一行代码里都藏着我对你的喜欢。\n\n你看到的每一个粉色，\n都是我心里你的颜色。"),
        LoveWord("🧸", "抱抱你", "今天份的拥抱已送达。\n\n不管你开心还是难过，\n我都想把你搂进怀里，\n轻轻拍拍你的背说：\n\n「有我在呢。」"),
        LoveWord("🍦", "甜甜的你", "你知道吗？\n\n自从遇见你，\n我觉得冰淇淋都不够甜了。\n\n因为你比世界上\n所有的甜都甜。"),
        LoveWord("🌈", "雨后彩虹", "吵架的时候我也在想你，\n生气的时候我也在爱你。\n\n每一次和好，\n都让我更确定：\n你就是我这辈子的人。"),
        LoveWord("🎵", "我们的歌", "每首情歌我都觉得\n是在唱我们的故事。\n\n副歌部分是你笑的样子，\n间奏是我偷看你的时候。"),
        LoveWord("📷", "偷拍你", "我手机相册里，\n你的照片比自拍多三倍。\n\n每一张都舍不得删，\n因为每一张你都好看。"),
        LoveWord("🧣", "冬天暖暖", "冬天最幸福的事，\n不是喝热奶茶，\n\n而是把冰凉的手\n伸进你的口袋里，\n然后被你握住。"),
        LoveWord("🌻", "向日葵", "你就是我的太阳，\n我永远朝着你的方向。\n\n不管你在哪里，\n我的心都跟着你转。"),
        LoveWord("🎪", "小小世界", "全世界有70亿人，\n但我的世界里只有你。\n\n一个你，\n就够我忙一辈子了。"),
        LoveWord("🍳", "明天的早餐", "等我学会做饭，\n每天早上叫你起床的\n不是闹钟，\n\n而是厨房飘来的香味，\n和一句「宝贝，吃早餐啦」。"),
        LoveWord("🎁", "每天的礼物", "认识你的每一天，\n都是我收到的最好的礼物。\n\n不用包装，不用丝带，\n你站在那里就够了。"),
        LoveWord("🐱", "如果我是猫", "如果我是一只猫，\n我一定每天赖在你腿上，\n蹭你的手心，\n咬你的手指。\n\n其实现在我也想。"),
        LoveWord("💫", "许愿", "看到流星的时候，\n别人许愿要发财，\n\n我只许了一个愿：\n下辈子还要遇见你。"),
        LoveWord("🎠", "旋转木马", "下次去游乐园，\n我要和你坐旋转木马。\n\n你骑白马，我骑旁边那匹，\n一圈一圈，永远不停。"),
        LoveWord("🌊", "海边的约定", "等有一天我们去看海，\n我要在沙滩上写你的名字。\n\n就算海浪冲走了字，\n我心里的也冲不走。"),
        LoveWord("🍰", "你的味道", "你身上有一种味道，\n不是香水，不是洗衣液，\n\n是让我闻到就安心的味道，\n叫做「家」。"),
        LoveWord("🎈", "气球", "如果思念可以变成气球，\n我想我已经可以\n飞到你身边了。\n\n不对，应该已经飞过去\n好几个来回了。"),
        LoveWord("🌸", "樱花树下", "等春天来了，\n我们去看樱花吧。\n\n花瓣落在你头发上的样子，\n一定比樱花还美。")
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
        "看到你就开心", "你是草莓味的", "想把你揣兜里带走",
        "你今天也好好看哦", "想和你一起发呆", "你的酒窝偷走了我的心",
        "想牵你的手逛超市", "你是我的人间理想", "今天的你也闪闪发光",
        "想和你一起看日落", "你比奶茶还让人上瘾", "想听你叫我的名字",
        "你是我见过最好的风景", "想和你一起变老"
    )

    // 特殊彩蛋
    val egg520 = LoveWord("💝", "¥520.00", "520 = 我爱你\n\n给你花的每一分钱，\n都是心甘情愿的。\n\n因为你值得全世界最好的。")
    val egg1314 = LoveWord("💞", "¥1314.00", "1314 = 一生一世\n\n这笔钱是我对你的承诺：\n\n一生一世，\n只爱你一个人。")
    val egg99 = LoveWord("🌷", "99朵郁金香", "99 = 长长久久\n\n给你买花不需要理由，\n但如果非要一个的话：\n\n因为你比花还好看。")
    val egg777 = LoveWord("🍀", "¥777.00", "777 = 三个幸运7\n\n遇见你就是我\n这辈子最大的幸运。\n\n比中彩票还开心。")
    val eggSalary = LoveWord("💼", "工资到啦", "发工资最开心的事，\n不是数字变多了，\n\n而是又可以带你\n去吃你想吃的东西了。\n\n我的钱就是你的钱。")
    val eggGift = LoveWord("🎀", "礼物时间", "给你买礼物的时候，\n我从来不看价格。\n\n只看你会不会笑，\n笑了就值了。")
    val eggFlower = LoveWord("💐", "今日份鲜花", "虽然花会枯萎，\n但我对你的爱不会。\n\n以后每周都给你买花，\n让家里永远有花香。")
    val eggDarkMode = LoveWord("🌙", "不用开深色模式", "因为有你在，\n我的世界永远是亮的。\n\n你就是我生命里的那束光。")
    val eggLock = LoveWord("🔐", "这个APP的密码是", "密码就是我们在一起的日子：\n\n0214\n\n因为从那天起，\n我的心就只为你上锁了。")
    val eggVersion = LoveWord("💌", "写给你的秘密", "这个APP从第一行代码开始，\n就是为你而写的。\n\n每一个按钮、每一种颜色，\n都是按照你喜欢的样子设计的。\n\n——爱你的我")
    val egg214 = LoveWord("🗓️", "2月14日", "这一天不开会，\n只想和你约会。\n\n在我的日程表里，\n每一天都写着你的名字。")
    val eggSearch = LoveWord("🔍", "找到了", "你搜索的答案只有一个：\n\n我爱你。\n\n不管搜多少次，\n答案都不会变。")
    val eggMiss = LoveWord("💭", "我也想你", "你在搜「想你」？\n\n巧了，我也在想你。\n现在，此刻，每一秒。")
    val eggForever = LoveWord("♾️", "永远有多远", "你搜「永远」？\n\n永远就是：\n从现在开始，\n到宇宙尽头，\n\n我都要和你在一起。")
    val eggHappy = LoveWord("🎊", "开心就好", "你搜「开心」？\n\n你开心就是我最大的心愿。\n\n如果不开心，\n就来找我，\n我负责逗你笑。")
    val eggLongPress = LoveWord("💍", "未来可期", "你按住这个按钮的时间，\n就像我想和你在一起的时间。\n\n永远不想松手。\n\n余生请多指教。")
    val eggSaveSuccess = LoveWord("🌟", "记录成功", "又多了一条我们的回忆。\n\n等我们老了，\n翻开这些记录，\n一定会笑着说：\n\n「那时候真好。」")
    val eggMeetingSave = LoveWord("💑", "两个人的会议", "会议纪要已保存。\n\n其实我最想开的会议是：\n\n议题：今晚吃什么\n参会人：你和我\n决议：你说了算")
    val eggDiarySave = LoveWord("📖", "今日份日记", "日记已保存。\n\n以后翻开这本日记，\n每一页都有你的影子。\n\n这就是我最珍贵的宝藏。")
    val eggEmptySearch = LoveWord("🔮", "什么都没搜到？", "没关系，\n因为你要找的人\n一直都在这里。\n\n就是正在看这段话的我。")
    val eggShake = LoveWord("🎐", "摇一摇", "你在摇手机？\n\n是不是想把我\n从手机里摇出来？\n\n等一下，\n我马上就到你身边。")
}
