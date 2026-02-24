package com.loveapp.accountbook.util

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import com.loveapp.accountbook.R

data class LoveWord(val tag: String, val title: String, val text: String)

object EasterEggManager {

    val loveWords = listOf(
        LoveWord("heart", "牵你的手", "从牵你手的那一刻起，\n我就决定这辈子都不放开了。\n\n每多一天，\n就多爱你一点。"),
        LoveWord("moon", "晚安情话", "我对世界说晚安，\n唯独对你说喜欢。\n\n今晚的月亮很美，\n但不及你万分之一。"),
        LoveWord("sun", "早安情话", "每天早上醒来，\n第一件事就是看你发没发消息。\n\n你是我睁眼后，\n最想见到的人。"),
        LoveWord("rain", "下雨天想你", "下雨了我会想你，\n天晴了我也想你。\n\n不管什么天气，\n反正我都在想你。"),
        LoveWord("cake", "小小心愿", "如果可以许一个愿望，\n我希望你每天都开开心心的。\n\n如果可以许两个，\n那第二个是永远在一起。"),
        LoveWord("shine", "遇见你之后", "遇见你之前，\n我没想过结婚。\n遇见你之后，\n我没想过别人。"),
        LoveWord("home", "关于未来", "等攒够了钱，\n我们就买一个小房子。\n\n养一只猫，\n周末一起做饭。\n你负责吃，我负责洗碗。"),
        LoveWord("diary", "给你写的", "这个APP是我专门为你写的，\n每一行代码里都藏着我对你的喜欢。\n\n你看到的每一个粉色，\n都是我心里你的颜色。"),
        LoveWord("bear", "抱抱你", "今天份的拥抱已送达。\n\n不管你开心还是难过，\n我都想把你搂进怀里，\n轻轻拍拍你的背说：\n\n「有我在呢。」"),
        LoveWord("food", "甜甜的你", "你知道吗？\n\n自从遇见你，\n我觉得冰淇淋都不够甜了。\n\n因为你比世界上\n所有的甜都甜。"),
        LoveWord("rainbow", "雨后彩虹", "吵架的时候我也在想你，\n生气的时候我也在爱你。\n\n每一次和好，\n都让我更确定：\n你就是我这辈子的人。"),
        LoveWord("music", "我们的歌", "每首情歌我都觉得\n是在唱我们的故事。\n\n副歌部分是你笑的样子，\n间奏是我偷看你的时候。"),
        LoveWord("camera", "偷拍你", "我手机相册里，\n你的照片比自拍多三倍。\n\n每一张都舍不得删，\n因为每一张你都好看。"),
        LoveWord("scarf", "冬天暖暖", "冬天最幸福的事，\n不是喝热奶茶，\n\n而是把冰凉的手\n伸进你的口袋里，\n然后被你握住。"),
        LoveWord("flower", "向日葵", "你就是我的太阳，\n我永远朝着你的方向。\n\n不管你在哪里，\n我的心都跟着你转。"),
        LoveWord("world", "小小世界", "全世界有70亿人，\n但我的世界里只有你。\n\n一个你，\n就够我忙一辈子了。"),
        LoveWord("food", "明天的早餐", "等我学会做饭，\n每天早上叫你起床的\n不是闹钟，\n\n而是厨房飘来的香味，\n和一句「宝贝，吃早餐啦」。"),
        LoveWord("gift", "每天的礼物", "认识你的每一天，\n都是我收到的最好的礼物。\n\n不用包装，不用丝带，\n你站在那里就够了。"),
        LoveWord("cat", "如果我是猫", "如果我是一只猫，\n我一定每天赖在你腿上，\n蹭你的手心，\n咬你的手指。\n\n其实现在我也想。"),
        LoveWord("shine", "许愿", "看到流星的时候，\n别人许愿要发财，\n\n我只许了一个愿：\n下辈子还要遇见你。"),
        LoveWord("carousel", "旋转木马", "下次去游乐园，\n我要和你坐旋转木马。\n\n你骑白马，我骑旁边那匹，\n一圈一圈，永远不停。"),
        LoveWord("sea", "海边的约定", "等有一天我们去看海，\n我要在沙滩上写你的名字。\n\n就算海浪冲走了字，\n我心里的也冲不走。"),
        LoveWord("food", "你的味道", "你身上有一种味道，\n不是香水，不是洗衣液，\n\n是让我闻到就安心的味道，\n叫做「家」。"),
        LoveWord("balloon", "气球", "如果思念可以变成气球，\n我想我已经可以\n飞到你身边了。\n\n不对，应该已经飞过去\n好几个来回了。"),
        LoveWord("cherry", "樱花树下", "等春天来了，\n我们去看樱花吧。\n\n花瓣落在你头发上的样子，\n一定比樱花还美。"),

        // —— 四季情话 ——
        LoveWord("tulip", "春天来了", "春风吹过的时候，\n我闻到了花香，\n也闻到了想你的味道。\n\n春天真好，\n因为又可以和你一起赏花了。"),
        LoveWord("sprout", "发芽的心", "春天万物复苏，\n我对你的爱也在疯狂生长。\n\n从一颗种子，\n长成了参天大树。"),
        LoveWord("sun", "夏日冰饮", "夏天太热了，\n但和你在一起，\n心里却凉凉的、甜甜的。\n\n像喝了一杯冰柠檬水。"),
        LoveWord("watermelon", "西瓜味的夏天", "夏天就该吃西瓜，\n最中间最甜的那一口，\n永远留给你。\n\n这是我的夏日仪式感。"),
        LoveWord("autumn", "秋天的第一杯奶茶", "秋天的第一杯奶茶，\n当然要和你一起喝。\n\n三分糖就够了，\n因为有你已经很甜。"),
        LoveWord("leaf", "落叶归根", "秋天的落叶飘啊飘，\n最后都落在了地上。\n\n就像我兜兜转转，\n最后还是回到你身边。"),
        LoveWord("snowman", "冬日暖阳", "冬天最想做的事，\n就是和你窝在沙发上，\n盖一条毯子，\n看一部老电影。\n\n外面再冷，心里都是暖的。"),
        LoveWord("gloves", "手套给你", "冬天出门忘了戴手套？\n没关系，\n把手给我，\n我帮你捂热。\n\n我的手永远为你留着温度。"),

        // —— 节日情话 ——
        LoveWord("christmas", "圣诞快乐", "圣诞老人问我想要什么礼物，\n我说不用了，\n\n因为最好的礼物，\n已经在我身边了。"),
        LoveWord("fireworks", "新年快乐", "新年的钟声响起时，\n我许的愿望和去年一样：\n\n和你在一起，\n一年又一年。"),
        LoveWord("lantern", "元宵节", "元宵节的灯笼再亮，\n也没有你的眼睛亮。\n\n猜灯谜的答案只有一个：\n我爱你。"),
        LoveWord("qixi", "七夕", "牛郎织女一年见一次，\n而我每天都想见你。\n\n所以我比牛郎幸福多了。"),
        LoveWord("cake", "生日快乐", "生日蛋糕上的蜡烛，\n我帮你吹。\n\n许愿的时候别说出来，\n但我猜一定和我有关吧。"),

        // —— 美食情话 ——
        LoveWord("food", "一碗热汤面", "天冷的时候，\n最想给你煮一碗热汤面。\n\n加花，\n满满都是爱你的味道。"),
        LoveWord("food", "甜品时间", "你就像提拉米苏，\n一层一层都是甜蜜。\n\n每一口都让我上瘾，\n怎么吃都吃不够。"),
        LoveWord("coffee", "咖啡与你", "你就像我的咖啡，\n苦的时候有你就变甜了。\n\n每天一杯，\n已经成了习惯。"),
        LoveWord("food", "一起吃披萨", "披萨要趁热吃，\n就像爱你这件事，\n一刻都不想等。\n\n芝士拉丝的瞬间，\n像极了我们分不开的样子。"),

        // —— 旅行情话 ——
        LoveWord("mountain", "一起爬山", "和你一起爬山，\n不在乎山顶的风景，\n\n在乎的是你气喘吁吁\n还要牵着我的手。"),
        LoveWord("sunrise", "看日出", "想和你一起看日出，\n在山顶，在海边，\n在任何有你的地方。\n\n第一缕阳光照在你脸上，\n那就是我见过最美的画。"),
        LoveWord("plane", "下一站", "我的旅行清单上，\n每一个目的地旁边，\n都写着你的名字。\n\n因为没有你的旅行，\n不算旅行。"),
        LoveWord("star", "看星星", "想和你去没有灯光的地方，\n躺在草地上看星星。\n\n数到第一百颗的时候，\n转头看你，\n你比星星还耀眼。"),

        // —— 日常甜蜜 ——
        LoveWord("home", "赖床", "周末最幸福的事，\n就是赖在床上不起来，\n\n然后你凑过来说：\n「再睡五分钟。」\n\n五分钟又五分钟。"),
        LoveWord("car", "接你下班", "在公司楼下等你的时候，\n看到你从门口走出来，\n\n那一刻觉得，\n等再久都值得。"),
        LoveWord("shopping", "逛超市", "和你逛超市，\n你往购物车里放零食，\n我往购物车里放你。\n\n这就是我最想要的日常。"),
        LoveWord("phone", "等你回消息", "你回消息慢的时候，\n我会反复看聊天记录。\n\n看着看着就笑了，\n因为每一条都好甜。"),
        LoveWord("game", "一起打游戏", "和你打游戏，\n赢了我开心，\n输了你开心。\n\n怎么算都是开心的。"),

        // —— 电影/音乐情话 ——
        LoveWord("movie", "一起看电影", "看恐怖片的时候，\n你躲在我怀里。\n\n其实我也害怕，\n但有你在，\n就什么都不怕了。"),
        LoveWord("music", "单曲循环", "最近单曲循环一首歌，\n因为歌词写的就是我们。\n\n每一句都像是\n我想对你说的话。"),
        LoveWord("piano", "弹给你听", "如果我会弹钢琴，\n一定给你弹一首情歌。\n\n不会也没关系，\n我用心跳给你打节拍。"),

        // —— 未来畅想 ——
        LoveWord("ring", "嫁给我吧", "等到那一天，\n我会单膝跪地，\n把戒指戴在你手上。\n\n然后说：\n余生请多指教。"),
        LoveWord("dog", "养一只狗", "以后我们养一只狗吧，\n你遛狗，我遛你。\n\n三个人一起散步，\n想想就觉得幸福。"),
        LoveWord("plant", "阳台花园", "以后我们的阳台上，\n要种满花花草草。\n\n你浇水，我施肥，\n一起看它们开花。"),
        LoveWord("letter", "写给未来的信", "亲爱的未来的你：\n\n不管过了多少年，\n我还是会像现在一样爱你。\n\n这是我最认真的承诺。"),
        LoveWord("ferris", "游乐园之约", "下次去游乐园，\n我要和你坐摩天轮。\n\n在最高点的时候亲你一下，\n这是摩天轮的规矩。")
    )

    fun showLovePopup(context: Context, loveWord: LoveWord) {
        val builder = AlertDialog.Builder(context)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_love_popup, null)
        view.findViewById<ImageView>(R.id.iv_emoji).setImageResource(iconResForTag(loveWord.tag))
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

    fun iconResForTag(tag: String): Int {
        return when (tag) {
            "sun", "shine", "star" -> R.drawable.ic_love_shine
            "diary" -> R.drawable.ic_nav_diary
            "food", "coffee", "watermelon" -> R.drawable.ic_export
            "moon", "rain" -> R.drawable.ic_love_heart
            "rainbow" -> R.drawable.ic_love_shine
            "cherry", "tulip", "flower", "sprout", "plant" -> R.drawable.ic_love_heart
            "music", "piano", "movie" -> R.drawable.ic_love_heart
            "ring", "letter", "heart" -> R.drawable.ic_love_heart
            "gift", "balloon", "fireworks", "christmas", "lantern" -> R.drawable.ic_love_shine
            else -> R.drawable.ic_love_heart
        }
    }

    val moodWords = listOf(
        "你笑起来真好看", "想捏捏你的脸", "今天也超喜欢你",
        "你是我的小太阳", "全世界你最可爱", "想rua你的头发",
        "看到你就开心", "你是草莓味的", "想把你揣兜里带走",
        "你今天也好好看哦", "想和你一起发呆", "你的酒窝偷走了我的心",
        "想牵你的手逛超市", "你是我的人间理想", "今天的你也闪闪发光",
        "想和你一起看日落", "你比奶茶还让人上瘾", "想听你叫我的名字",
        "你是我见过最好的风景", "想和你一起变老",
        "你是我的独家记忆", "想偷走你的影子", "你的声音是最好的闹钟",
        "想给你织一条围巾", "你比星星还闪", "想和你一起淋雨",
        "你是限量版的宝贝", "想把月亮摘给你", "你的眼睛里有星辰大海",
        "想和你一起数星星", "你是我的心动开关", "想给你唱一首歌",
        "你比春天还温柔", "想和你一起踩落叶", "你是我的小确幸",
        "想和你分享每一天", "你是我最甜的梦", "想和你一起看烟花",
        "你是我的专属天使", "想和你一起去流浪", "你比糖果还甜",
        "想和你一起堆雪人", "你是我的心头好", "想和你一起看彩虹"
    )

    // 特殊彩蛋
    val egg520 = LoveWord("heart", "¥520.00", "520 = 我爱你\n\n给你花的每一分钱，\n都是心甘情愿的。\n\n因为你值得全世界最好的。")
    val egg1314 = LoveWord("heart", "¥1314.00", "1314 = 一生一世\n\n这笔钱是我对你的承诺：\n\n一生一世，\n只爱你一个人。")
    val egg99 = LoveWord("tulip", "99朵郁金香", "99 = 长长久久\n\n给你买花不需要理由，\n但如果非要一个的话：\n\n因为你比花还好看。")
    val egg777 = LoveWord("shine", "¥777.00", "777 = 三个幸运7\n\n遇见你就是我\n这辈子最大的幸运。\n\n比中彩票还开心。")
    val eggSalary = LoveWord("heart", "工资到啦", "发工资最开心的事，\n不是数字变多了，\n\n而是又可以带你\n去吃你想吃的东西了。\n\n我的钱就是你的钱。")
    val eggGift = LoveWord("gift", "礼物时间", "给你买礼物的时候，\n我从来不看价格。\n\n只看你会不会笑，\n笑了就值了。")
    val eggFlower = LoveWord("flower", "今日份鲜花", "虽然花会枯萎，\n但我对你的爱不会。\n\n以后每周都给你买花，\n让家里永远有花香。")
    val eggDarkMode = LoveWord("moon", "不用开深色模式", "因为有你在，\n我的世界永远是亮的。\n\n你就是我生命里的那束光。")
    val eggLock = LoveWord("heart", "这个APP的密码是", "密码就是我们在一起的日子：\n\n0928\n\n因为从那天起，\n我的心就只为你上锁了。")
    val eggVersion = LoveWord("letter", "写给你的秘密", "这个APP从第一行代码开始，\n就是为你而写的。\n\n每一个按钮、每一种颜色，\n都是按照你喜欢的样子设计的。\n\n——爱你的我")
    val egg214 = LoveWord("heart", "2月14日", "这一天不开会，\n只想和你约会。\n\n在我的日程表里，\n每一天都写着你的名字。")
    val eggSearch = LoveWord("heart", "找到了", "你搜索的答案只有一个：\n\n我爱你。\n\n不管搜多少次，\n答案都不会变。")
    val eggMiss = LoveWord("heart", "我也想你", "你在搜「想你」？\n\n巧了，我也在想你。\n现在，此刻，每一秒。")
    val eggForever = LoveWord("heart", "永远有多远", "你搜「永远」？\n\n永远就是：\n从现在开始，\n到宇宙尽头，\n\n我都要和你在一起。")
    val eggHappy = LoveWord("shine", "开心就好", "你搜「开心」？\n\n你开心就是我最大的心愿。\n\n如果不开心，\n就来找我，\n我负责逗你笑。")
    val eggLongPress = LoveWord("ring", "未来可期", "你按住这个按钮的时间，\n就像我想和你在一起的时间。\n\n永远不想松手。\n\n余生请多指教。")
    val eggSaveSuccess = LoveWord("shine", "记录成功", "又多了一条我们的回忆。\n\n等我们老了，\n翻开这些记录，\n一定会笑着说：\n\n「那时候真好。」")
    val eggMeetingSave = LoveWord("heart", "两个人的会议", "会议纪要已保存。\n\n其实我最想开的会议是：\n\n议题：今晚吃什么\n参会人：你和我\n决议：你说了算")
    val eggDiarySave = LoveWord("diary", "今日份日记", "日记已保存。\n\n以后翻开这本日记，\n每一页都有你的影子。\n\n这就是我最珍贵的宝藏。")
    val egg666 = LoveWord("shine", "666大顺", "666 = 一切顺利\n\n有你在身边，\n做什么都顺顺利利。\n\n你就是我的幸运符。")
    val egg888 = LoveWord("gift", "发发发", "888 = 发发发\n\n不用发大财，\n只要有你就是最大的财富。\n\n你比金子还珍贵。")
    val egg233 = LoveWord("shine", "笑死我了", "233 = 哈哈哈\n\n和你在一起的每一天，\n都充满了欢笑。\n\n你是我的快乐源泉。")
    val egg521 = LoveWord("heart", "521", "521 = 我爱你\n\n不止521，\n365天每一天都爱你。\n\n爱你是我最擅长的事。")
    val eggMorning = LoveWord("sunrise", "早安宝贝", "新的一天开始了，\n第一件事就是想你。\n\n希望你今天也元气满满，\n记得吃早餐哦。\n\n早安，我最爱的人。")
    val eggNight = LoveWord("moon", "晚安宝贝", "忙碌的一天结束了，\n辛苦了，宝贝。\n\n今晚做个好梦，\n梦里有我就更好了。\n\n晚安，明天见。")
    val eggWeekend = LoveWord("ferris", "周末快乐", "终于到周末了！\n\n想和你一起赖床，\n一起看电影，\n一起吃好吃的。\n\n周末的意义就是和你在一起。")
    val eggRain = LoveWord("rain", "下雨天", "外面下雨了，\n你带伞了吗？\n\n如果没有，\n就在原地等我，\n我去接你。")

    /** 根据日期返回每日情话（每天固定一条） */
    fun dailyLoveWord(): LoveWord {
        val dayOfYear = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
        return loveWords[dayOfYear % loveWords.size]
    }

    /** 根据季节返回应景情话 */
    fun seasonalLoveWord(): LoveWord {
        val month = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1
        val seasonWords = when (month) {
            3, 4, 5 -> loveWords.filter { it.tag in listOf("cherry", "flower", "rainbow", "tulip") }
            6, 7, 8 -> loveWords.filter { it.tag in listOf("sun", "food", "sea", "world") }
            9, 10, 11 -> loveWords.filter { it.tag in listOf("autumn", "scarf", "music", "food") }
            else -> loveWords.filter { it.tag in listOf("scarf", "moon", "coffee", "christmas") }
        }
        return if (seasonWords.isNotEmpty()) seasonWords.random() else loveWords.random()
    }

    /** 根据时间段返回情话 */
    fun timeLoveWord(): LoveWord {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when {
            hour in 5..8 -> eggMorning
            hour in 9..11 -> loveWords.random()
            hour in 12..13 -> LoveWord("food", "午餐时间", "中午了，记得吃饭哦。\n\n不管吃什么，\n想到你也在吃饭，\n就觉得很幸福。")
            hour in 14..17 -> loveWords.random()
            hour in 18..20 -> LoveWord("sun", "下班啦", "下班了！\n\n今天辛苦了，\n回家的路上注意安全。\n\n到家告诉我一声哦。")
            hour in 21..23 -> eggNight
            else -> LoveWord("moon", "深夜情话", "这么晚了还没睡？\n\n快去睡觉，\n不然明天会有黑眼圈的。\n\n做个好梦，梦里有我。")
        }
    }

    /** 获取所有情话的分类 */
    fun getLoveWordCategories(): Map<String, List<LoveWord>> {
        return mapOf(
            "日常甜蜜" to loveWords.filter { it.tag in listOf("heart", "sun", "bear", "food", "gift", "shine", "balloon") },
            "四季情话" to loveWords.filter { it.tag in listOf("cherry", "flower", "autumn", "scarf", "sea", "sun") },
            "美食情话" to loveWords.filter { it.tag in listOf("food", "coffee") },
            "浪漫时刻" to loveWords.filter { it.tag in listOf("moon", "shine", "music", "camera", "carousel", "rainbow") },
            "未来畅想" to loveWords.filter { it.tag in listOf("home", "ring", "cat", "world", "letter") },
            "全部情话" to loveWords
        )
    }
}
