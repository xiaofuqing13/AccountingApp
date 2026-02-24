package com.loveapp.accountbook.data.repository

import android.content.Context
import android.os.Environment
import com.loveapp.accountbook.data.model.AccountEntry
import com.loveapp.accountbook.data.model.DiaryEntry
import com.loveapp.accountbook.data.model.MeetingEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class ExcelRepository(private val context: Context) {

    private val fileName = "我们的小账本.xlsx"
    private val sheetAccount = "记账"
    private val sheetDiary = "日记"
    private val sheetMeeting = "会议纪要"
    private val sheetCategory = "分类配置"

    companion object {
        /** 公共存储目录：Documents/我们的小账本/ */
        fun getPublicDir(): File {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "我们的小账本")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }
    }

    @Volatile
    private var cachedFile: File? = null

    private fun getFile(): File {
        cachedFile?.let { if (it.parentFile?.canWrite() == true) return it }

        val publicDir = getPublicDir()
        val publicFile = File(publicDir, fileName)
        val legacyFile = File(context.getExternalFilesDir(null), "DataManager/$fileName")

        // 公共目录可写 -> 优先用公共目录
        if (publicDir.canWrite()) {
            // 自动迁移：旧路径有数据但公共目录没有
            if (!publicFile.exists() && legacyFile.exists()) {
                try {
                    legacyFile.copyTo(publicFile, overwrite = false)
                    legacyFile.delete() // 迁移成功后清理旧文件
                } catch (_: Exception) { }
            }
            cachedFile = publicFile
            return publicFile
        }

        // 公共目录不可写（权限未授予）-> fallback 到旧路径
        val fallbackDir = File(context.getExternalFilesDir(null), "DataManager")
        if (!fallbackDir.exists()) fallbackDir.mkdirs()
        val fallbackFile = File(fallbackDir, fileName)
        cachedFile = fallbackFile
        return fallbackFile
    }

    private fun getOrCreateWorkbook(): XSSFWorkbook {
        val file = getFile()
        return if (file.exists()) {
            FileInputStream(file).use { XSSFWorkbook(it) }
        } else {
            XSSFWorkbook().apply {
                createSheet(sheetAccount).also { sheet ->
                    sheet.createRow(0).apply {
                        createCell(0).setCellValue("日期")
                        createCell(1).setCellValue("类型")
                        createCell(2).setCellValue("分类")
                        createCell(3).setCellValue("金额")
                        createCell(4).setCellValue("备注")
                    }
                }
                createSheet(sheetDiary).also { sheet ->
                    sheet.createRow(0).apply {
                        createCell(0).setCellValue("日期")
                        createCell(1).setCellValue("标题")
                        createCell(2).setCellValue("内容")
                        createCell(3).setCellValue("天气")
                        createCell(4).setCellValue("心情")
                        createCell(5).setCellValue("位置")
                    }
                }
                createSheet(sheetMeeting).also { sheet ->
                    sheet.createRow(0).apply {
                        createCell(0).setCellValue("日期")
                        createCell(1).setCellValue("主题")
                        createCell(2).setCellValue("开始时间")
                        createCell(3).setCellValue("结束时间")
                        createCell(4).setCellValue("地点")
                        createCell(5).setCellValue("参会人")
                        createCell(6).setCellValue("内容")
                        createCell(7).setCellValue("待办事项")
                        createCell(8).setCellValue("标签")
                    }
                }
                createSheet(sheetCategory).also { sheet ->
                    sheet.createRow(0).apply {
                        createCell(0).setCellValue("分类名称")
                        createCell(1).setCellValue("类型")
                        createCell(2).setCellValue("图标")
                    }
                }
                saveWorkbook(this)
            }
        }
    }

    private fun ensureCategorySheet(workbook: XSSFWorkbook): Sheet {
        return workbook.getSheet(sheetCategory) ?: workbook.createSheet(sheetCategory).also { sheet ->
            sheet.createRow(0).apply {
                createCell(0).setCellValue("分类名称")
                createCell(1).setCellValue("类型")
                createCell(2).setCellValue("图标")
            }
        }
    }

    private fun saveWorkbook(workbook: XSSFWorkbook) {
        FileOutputStream(getFile()).use { workbook.write(it) }
    }

    private fun getCellString(cell: Cell?): String {
        return when (cell?.cellType) {
            CellType.STRING -> cell.stringCellValue
            CellType.NUMERIC -> cell.numericCellValue.toString()
            else -> ""
        }
    }

    private fun getCellDouble(cell: Cell?): Double {
        return when (cell?.cellType) {
            CellType.NUMERIC -> cell.numericCellValue
            CellType.STRING -> cell.stringCellValue.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
    }

    // ========== 自定义分类 ==========

    suspend fun getCustomCategories(type: String): List<com.loveapp.accountbook.data.model.Category> = withContext(Dispatchers.IO) {
        val workbook = getOrCreateWorkbook()
        val sheet = ensureCategorySheet(workbook)
        val list = mutableListOf<com.loveapp.accountbook.data.model.Category>()
        for (i in 1..sheet.lastRowNum) {
            val row = sheet.getRow(i) ?: continue
            val catType = getCellString(row.getCell(1))
            if (catType == type) {
                list.add(com.loveapp.accountbook.data.model.Category(
                    getCellString(row.getCell(2)),
                    getCellString(row.getCell(0))
                ))
            }
        }
        workbook.close()
        list
    }

    suspend fun addCustomCategory(name: String, type: String, icon: String) = withContext(Dispatchers.IO) {
        val workbook = getOrCreateWorkbook()
        val sheet = ensureCategorySheet(workbook)
        // 检查是否已存在
        for (i in 1..sheet.lastRowNum) {
            val row = sheet.getRow(i) ?: continue
            if (getCellString(row.getCell(0)) == name && getCellString(row.getCell(1)) == type) {
                workbook.close()
                return@withContext
            }
        }
        val rowNum = sheet.lastRowNum + 1
        sheet.createRow(rowNum).apply {
            createCell(0).setCellValue(name)
            createCell(1).setCellValue(type)
            createCell(2).setCellValue(icon)
        }
        saveWorkbook(workbook)
        workbook.close()
    }

    suspend fun deleteCustomCategory(name: String, type: String) = withContext(Dispatchers.IO) {
        val workbook = getOrCreateWorkbook()
        val sheet = ensureCategorySheet(workbook)
        for (i in 1..sheet.lastRowNum) {
            val row = sheet.getRow(i) ?: continue
            if (getCellString(row.getCell(0)) == name && getCellString(row.getCell(1)) == type) {
                val lastRow = sheet.lastRowNum
                if (i < lastRow) {
                    sheet.shiftRows(i + 1, lastRow, -1)
                } else {
                    sheet.removeRow(row)
                }
                break
            }
        }
        saveWorkbook(workbook)
        workbook.close()
    }

    // ========== 记账 ==========

    suspend fun getAccounts(): List<AccountEntry> = withContext(Dispatchers.IO) {
        val workbook = getOrCreateWorkbook()
        val sheet = workbook.getSheet(sheetAccount)
        if (sheet == null) { workbook.close(); return@withContext emptyList() }
        val list = mutableListOf<AccountEntry>()
        for (i in 1..sheet.lastRowNum) {
            val row = sheet.getRow(i) ?: continue
            list.add(AccountEntry(
                date = getCellString(row.getCell(0)),
                type = getCellString(row.getCell(1)),
                category = getCellString(row.getCell(2)),
                amount = getCellDouble(row.getCell(3)),
                note = getCellString(row.getCell(4)),
                rowIndex = i - 1
            ))
        }
        workbook.close()
        list.sortedByDescending { it.date }
    }

    suspend fun addAccount(entry: AccountEntry) = withContext(Dispatchers.IO) {
        val workbook = getOrCreateWorkbook()
        val sheet = workbook.getSheet(sheetAccount)
        val rowNum = sheet.lastRowNum + 1
        sheet.createRow(rowNum).apply {
            createCell(0).setCellValue(entry.date)
            createCell(1).setCellValue(entry.type)
            createCell(2).setCellValue(entry.category)
            createCell(3).setCellValue(entry.amount)
            createCell(4).setCellValue(entry.note)
        }
        saveWorkbook(workbook)
        workbook.close()
    }

    suspend fun getAccountsByMonth(yearMonth: String): List<AccountEntry> = withContext(Dispatchers.IO) {
        getAccounts().filter { it.date.startsWith(yearMonth) }
    }


    suspend fun updateAccount(entry: AccountEntry) = withContext(Dispatchers.IO) {
        val workbook = getOrCreateWorkbook()
        try {
            val sheet = workbook.getSheet(sheetAccount) ?: return@withContext
            val row = sheet.getRow(entry.rowIndex + 1) ?: return@withContext
            row.getCell(0)?.setCellValue(entry.date)
            row.getCell(1)?.setCellValue(entry.type)
            row.getCell(2)?.setCellValue(entry.category)
            row.getCell(3)?.setCellValue(entry.amount)
            row.getCell(4)?.setCellValue(entry.note)
            saveWorkbook(workbook)
        } finally {
            workbook.close()
        }
    }

    suspend fun deleteAccount(entry: AccountEntry) = withContext(Dispatchers.IO) {
        val workbook = getOrCreateWorkbook()
        try {
            val sheet = workbook.getSheet(sheetAccount) ?: return@withContext
            val rowIndex = entry.rowIndex + 1
            val lastRow = sheet.lastRowNum
            if (rowIndex < lastRow) {
                sheet.shiftRows(rowIndex + 1, lastRow, -1)
            } else {
                sheet.removeRow(sheet.getRow(rowIndex) ?: return@withContext)
            }
            saveWorkbook(workbook)
        } finally {
            workbook.close()
        }
    }

    // ========== 日记 ==========

    suspend fun getDiaries(): List<DiaryEntry> = withContext(Dispatchers.IO) {
        val workbook = getOrCreateWorkbook()
        val sheet = workbook.getSheet(sheetDiary)
        if (sheet == null) { workbook.close(); return@withContext emptyList() }
        val list = mutableListOf<DiaryEntry>()
        for (i in 1..sheet.lastRowNum) {
            val row = sheet.getRow(i) ?: continue
            list.add(DiaryEntry(
                date = getCellString(row.getCell(0)),
                title = getCellString(row.getCell(1)),
                content = getCellString(row.getCell(2)),
                weather = getCellString(row.getCell(3)),
                mood = getCellString(row.getCell(4)),
                location = getCellString(row.getCell(5)),
                rowIndex = i - 1
            ))
        }
        workbook.close()
        list.sortedByDescending { it.date }
    }

    suspend fun addDiary(entry: DiaryEntry) = withContext(Dispatchers.IO) {
        val workbook = getOrCreateWorkbook()
        val sheet = workbook.getSheet(sheetDiary)
        val rowNum = sheet.lastRowNum + 1
        sheet.createRow(rowNum).apply {
            createCell(0).setCellValue(entry.date)
            createCell(1).setCellValue(entry.title)
            createCell(2).setCellValue(entry.content)
            createCell(3).setCellValue(entry.weather)
            createCell(4).setCellValue(entry.mood)
            createCell(5).setCellValue(entry.location)
        }
        saveWorkbook(workbook)
        workbook.close()
    }


    suspend fun updateDiary(entry: DiaryEntry) = withContext(Dispatchers.IO) {
        val workbook = getOrCreateWorkbook()
        try {
            val sheet = workbook.getSheet(sheetDiary) ?: return@withContext
            val row = sheet.getRow(entry.rowIndex + 1) ?: return@withContext
            row.getCell(0)?.setCellValue(entry.date)
            row.getCell(1)?.setCellValue(entry.title)
            row.getCell(2)?.setCellValue(entry.content)
            row.getCell(3)?.setCellValue(entry.weather)
            row.getCell(4)?.setCellValue(entry.mood)
            row.getCell(5)?.setCellValue(entry.location)
            saveWorkbook(workbook)
        } finally {
            workbook.close()
        }
    }

    suspend fun deleteDiary(entry: DiaryEntry) = withContext(Dispatchers.IO) {
        val workbook = getOrCreateWorkbook()
        try {
            val sheet = workbook.getSheet(sheetDiary) ?: return@withContext
            val rowIndex = entry.rowIndex + 1
            val lastRow = sheet.lastRowNum
            if (rowIndex < lastRow) {
                sheet.shiftRows(rowIndex + 1, lastRow, -1)
            } else {
                sheet.removeRow(sheet.getRow(rowIndex) ?: return@withContext)
            }
            saveWorkbook(workbook)
        } finally {
            workbook.close()
        }
    }

    // ========== 会议 ==========

    suspend fun getMeetings(): List<MeetingEntry> = withContext(Dispatchers.IO) {
        val workbook = getOrCreateWorkbook()
        val sheet = workbook.getSheet(sheetMeeting)
        if (sheet == null) { workbook.close(); return@withContext emptyList() }
        val list = mutableListOf<MeetingEntry>()
        for (i in 1..sheet.lastRowNum) {
            val row = sheet.getRow(i) ?: continue
            list.add(MeetingEntry(
                date = getCellString(row.getCell(0)),
                topic = getCellString(row.getCell(1)),
                startTime = getCellString(row.getCell(2)),
                endTime = getCellString(row.getCell(3)),
                location = getCellString(row.getCell(4)),
                attendees = getCellString(row.getCell(5)),
                content = getCellString(row.getCell(6)),
                todoItems = getCellString(row.getCell(7)),
                tags = getCellString(row.getCell(8)),
                rowIndex = i - 1
            ))
        }
        workbook.close()
        list.sortedByDescending { it.date }
    }

    suspend fun addMeeting(entry: MeetingEntry) = withContext(Dispatchers.IO) {
        val workbook = getOrCreateWorkbook()
        val sheet = workbook.getSheet(sheetMeeting)
        val rowNum = sheet.lastRowNum + 1
        sheet.createRow(rowNum).apply {
            createCell(0).setCellValue(entry.date)
            createCell(1).setCellValue(entry.topic)
            createCell(2).setCellValue(entry.startTime)
            createCell(3).setCellValue(entry.endTime)
            createCell(4).setCellValue(entry.location)
            createCell(5).setCellValue(entry.attendees)
            createCell(6).setCellValue(entry.content)
            createCell(7).setCellValue(entry.todoItems)
            createCell(8).setCellValue(entry.tags)
        }
        saveWorkbook(workbook)
        workbook.close()
    }


    suspend fun updateMeeting(entry: MeetingEntry) = withContext(Dispatchers.IO) {
        val workbook = getOrCreateWorkbook()
        try {
            val sheet = workbook.getSheet(sheetMeeting) ?: return@withContext
            val row = sheet.getRow(entry.rowIndex + 1) ?: return@withContext
            row.getCell(0)?.setCellValue(entry.date)
            row.getCell(1)?.setCellValue(entry.topic)
            row.getCell(2)?.setCellValue(entry.startTime)
            row.getCell(3)?.setCellValue(entry.endTime)
            row.getCell(4)?.setCellValue(entry.location)
            row.getCell(5)?.setCellValue(entry.attendees)
            row.getCell(6)?.setCellValue(entry.content)
            row.getCell(7)?.setCellValue(entry.todoItems)
            row.getCell(8)?.setCellValue(entry.tags)
            saveWorkbook(workbook)
        } finally {
            workbook.close()
        }
    }

    suspend fun deleteMeeting(entry: MeetingEntry) = withContext(Dispatchers.IO) {
        val workbook = getOrCreateWorkbook()
        try {
            val sheet = workbook.getSheet(sheetMeeting) ?: return@withContext
            val rowIndex = entry.rowIndex + 1
            val lastRow = sheet.lastRowNum
            if (rowIndex < lastRow) {
                sheet.shiftRows(rowIndex + 1, lastRow, -1)
            } else {
                sheet.removeRow(sheet.getRow(rowIndex) ?: return@withContext)
            }
            saveWorkbook(workbook)
        } finally {
            workbook.close()
        }
    }

    // ========== 模拟数据导入 ==========

    /**
     * 将旧版备份Excel中的数据硬编码写入APP数据库（去重）
     * 返回 Triple<导入记账数, 导入日记数, 导入会议数>
     */
    suspend fun importSampleData(): Triple<Int, Int, Int> = withContext(Dispatchers.IO) {
        val targetWb = getOrCreateWorkbook()
        var accountCount = 0
        var diaryCount = 0
        var meetingCount = 0

        // ===== 记账数据（35条） =====
        data class RawAccount(val date: String, val type: String, val category: String, val amount: Double, val note: String)
        val sampleAccounts = listOf(
            RawAccount("2026-01-22", "支出", "餐饮", 20.99, "午饭"),
            RawAccount("2026-01-22", "收入", "其他", 20.0, "宝贝"),
            RawAccount("2026-01-22", "支出", "餐饮", 12.0, "早餐"),
            RawAccount("2026-01-22", "支出", "餐饮", 175.0, "晚餐"),
            RawAccount("2026-01-22", "支出", "购物", 14.9, "套袖"),
            RawAccount("2026-01-23", "支出", "餐饮", 11.5, "早餐"),
            RawAccount("2026-01-23", "支出", "购物", 25.8, "充电口胶圈"),
            RawAccount("2026-01-23", "支出", "餐饮", 30.49, "午饭"),
            RawAccount("2026-01-23", "支出", "购物", 5.44, "快递费"),
            RawAccount("2026-01-23", "支出", "住房", 207.12, "明日酒店"),
            RawAccount("2026-01-23", "支出", "餐饮", 140.0, "晚餐"),
            RawAccount("2026-01-24", "支出", "餐饮", 33.2, "早午饭"),
            RawAccount("2026-01-24", "支出", "交通", 7.0, "停车费"),
            RawAccount("2026-01-24", "支出", "医疗", 243.44, "拔牙"),
            RawAccount("2026-01-24", "支出", "餐饮", 12.6, "水"),
            RawAccount("2026-01-24", "支出", "医疗", 66.37, "ct拍片"),
            RawAccount("2026-01-24", "支出", "医疗", 2.03, "挂号"),
            RawAccount("2026-01-24", "支出", "购物", 49.6, "纸巾"),
            RawAccount("2026-01-24", "支出", "餐饮", 18.9, "晚餐"),
            RawAccount("2026-01-24", "支出", "购物", 30.8, "防水油布"),
            RawAccount("2026-01-24", "支出", "购物", 4.85, "双面胶"),
            RawAccount("2026-01-24", "支出", "交通", 24.27, "充电"),
            RawAccount("2026-01-25", "支出", "交通", 62.0, "过路费"),
            RawAccount("2026-01-25", "支出", "购物", 46.0, "G烟"),
            RawAccount("2026-01-25", "支出", "餐饮", 15.9, "晚饭"),
            RawAccount("2026-01-25", "支出", "购物", 2.0, "打火机"),
            RawAccount("2026-01-25", "支出", "交通", 28.95, "充电"),
            RawAccount("2026-01-25", "支出", "餐饮", 33.9, "晚餐"),
            RawAccount("2026-01-26", "支出", "餐饮", 18.8, "早饭"),
            RawAccount("2026-01-26", "支出", "餐饮", 45.1, "G午餐"),
            RawAccount("2026-01-26", "支出", "餐饮", 42.4, "午饭"),
            RawAccount("2026-01-26", "支出", "餐饮", 43.9, "肯德基"),
            RawAccount("2026-01-26", "支出", "餐饮", 69.0, "肯德基优惠券"),
            RawAccount("2026-01-27", "支出", "餐饮", 42.29, "午饭"),
            RawAccount("2026-01-27", "支出", "餐饮", 19.18, "G午饭")
        )

        val accountSheet = targetWb.getSheet(sheetAccount)!!
        val existingAccounts = mutableSetOf<String>()
        for (i in 1..accountSheet.lastRowNum) {
            val r = accountSheet.getRow(i) ?: continue
            existingAccounts.add("${getCellString(r.getCell(0))}_${getCellString(r.getCell(1))}_${getCellString(r.getCell(2))}_${getCellDouble(r.getCell(3))}_${getCellString(r.getCell(4))}")
        }
        for (a in sampleAccounts) {
            val key = "${a.date}_${a.type}_${a.category}_${a.amount}_${a.note}"
            if (key in existingAccounts) continue
            val newRow = accountSheet.createRow(accountSheet.lastRowNum + 1)
            newRow.createCell(0).setCellValue(a.date)
            newRow.createCell(1).setCellValue(a.type)
            newRow.createCell(2).setCellValue(a.category)
            newRow.createCell(3).setCellValue(a.amount)
            newRow.createCell(4).setCellValue(a.note)
            existingAccounts.add(key)
            accountCount++
        }

        // ===== 日记数据（1条） =====
        data class RawDiary(val date: String, val title: String, val content: String, val weather: String, val mood: String)
        val sampleDiaries = listOf(
            RawDiary("2026-01-22", "惊喜", "太开心了，全新属于我的APP。我的宝贝真棒", "晴", "开心")
        )

        val diarySheet = targetWb.getSheet(sheetDiary)!!
        val existingDiaries = mutableSetOf<String>()
        for (i in 1..diarySheet.lastRowNum) {
            val r = diarySheet.getRow(i) ?: continue
            existingDiaries.add("${getCellString(r.getCell(0))}_${getCellString(r.getCell(1))}")
        }
        for (d in sampleDiaries) {
            val key = "${d.date}_${d.title}"
            if (key in existingDiaries) continue
            val newRow = diarySheet.createRow(diarySheet.lastRowNum + 1)
            newRow.createCell(0).setCellValue(d.date)
            newRow.createCell(1).setCellValue(d.title)
            newRow.createCell(2).setCellValue(d.content)
            newRow.createCell(3).setCellValue(d.weather)
            newRow.createCell(4).setCellValue(d.mood)
            newRow.createCell(5).setCellValue("")
            existingDiaries.add(key)
            diaryCount++
        }

        // ===== 会议数据（1条） =====
        data class RawMeeting(val date: String, val topic: String, val location: String, val attendees: String, val content: String, val todoItems: String)
        val sampleMeetings = listOf(
            RawMeeting("2026-01-23", "鱼汤", "", "", "", "小鱼，煮汤，提前找餐厅")
        )

        val meetingSheet = targetWb.getSheet(sheetMeeting)!!
        val existingMeetings = mutableSetOf<String>()
        for (i in 1..meetingSheet.lastRowNum) {
            val r = meetingSheet.getRow(i) ?: continue
            existingMeetings.add("${getCellString(r.getCell(0))}_${getCellString(r.getCell(1))}")
        }
        for (m in sampleMeetings) {
            val key = "${m.date}_${m.topic}"
            if (key in existingMeetings) continue
            val newRow = meetingSheet.createRow(meetingSheet.lastRowNum + 1)
            newRow.createCell(0).setCellValue(m.date)
            newRow.createCell(1).setCellValue(m.topic)
            newRow.createCell(2).setCellValue("")
            newRow.createCell(3).setCellValue("")
            newRow.createCell(4).setCellValue(m.location)
            newRow.createCell(5).setCellValue(m.attendees)
            newRow.createCell(6).setCellValue(m.content)
            newRow.createCell(7).setCellValue(m.todoItems)
            newRow.createCell(8).setCellValue("")
            existingMeetings.add(key)
            meetingCount++
        }

        saveWorkbook(targetWb)
        targetWb.close()
        Triple(accountCount, diaryCount, meetingCount)
    }

    // ========== 导入导出 ==========

    fun getExcelFilePath(): String = getFile().absolutePath

    /**
     * 导出标准化分享文件（包含全部数据 + 版本标识）
     * Sheet: 记账 / 日记 / 会议纪要 / 分类配置 / 元信息
     * 导入时通过"元信息"sheet识别版本，自动兼容
     */
    suspend fun exportShareable(outputStream: java.io.OutputStream) = withContext(Dispatchers.IO) {
        val sourceWb = getOrCreateWorkbook()
        val exportWb = XSSFWorkbook()

        // ===== 记账 =====
        val srcAccount = sourceWb.getSheet(sheetAccount)
        val expAccount = exportWb.createSheet(sheetAccount)
        if (srcAccount != null) {
            for (i in 0..srcAccount.lastRowNum) {
                val srcRow = srcAccount.getRow(i) ?: continue
                val newRow = expAccount.createRow(i)
                for (c in 0 until 5) {
                    val cell = srcRow.getCell(c)
                    if (cell?.cellType == CellType.NUMERIC) {
                        newRow.createCell(c).setCellValue(cell.numericCellValue)
                    } else {
                        newRow.createCell(c).setCellValue(getCellString(cell))
                    }
                }
            }
        } else {
            expAccount.createRow(0).apply {
                createCell(0).setCellValue("日期")
                createCell(1).setCellValue("类型")
                createCell(2).setCellValue("分类")
                createCell(3).setCellValue("金额")
                createCell(4).setCellValue("备注")
            }
        }

        // ===== 日记 =====
        val srcDiary = sourceWb.getSheet(sheetDiary)
        val expDiary = exportWb.createSheet(sheetDiary)
        if (srcDiary != null) {
            for (i in 0..srcDiary.lastRowNum) {
                val srcRow = srcDiary.getRow(i) ?: continue
                val newRow = expDiary.createRow(i)
                for (c in 0 until 6) {
                    newRow.createCell(c).setCellValue(getCellString(srcRow.getCell(c)))
                }
            }
        } else {
            expDiary.createRow(0).apply {
                createCell(0).setCellValue("日期")
                createCell(1).setCellValue("标题")
                createCell(2).setCellValue("内容")
                createCell(3).setCellValue("天气")
                createCell(4).setCellValue("心情")
                createCell(5).setCellValue("位置")
            }
        }

        // ===== 会议纪要 =====
        val srcMeeting = sourceWb.getSheet(sheetMeeting)
        val expMeeting = exportWb.createSheet(sheetMeeting)
        if (srcMeeting != null) {
            for (i in 0..srcMeeting.lastRowNum) {
                val srcRow = srcMeeting.getRow(i) ?: continue
                val newRow = expMeeting.createRow(i)
                for (c in 0 until 9) {
                    newRow.createCell(c).setCellValue(getCellString(srcRow.getCell(c)))
                }
            }
        } else {
            expMeeting.createRow(0).apply {
                createCell(0).setCellValue("日期")
                createCell(1).setCellValue("主题")
                createCell(2).setCellValue("开始时间")
                createCell(3).setCellValue("结束时间")
                createCell(4).setCellValue("地点")
                createCell(5).setCellValue("参会人")
                createCell(6).setCellValue("内容")
                createCell(7).setCellValue("待办事项")
                createCell(8).setCellValue("标签")
            }
        }

        // ===== 分类配置 =====
        val srcCat = ensureCategorySheet(sourceWb)
        val expCat = exportWb.createSheet(sheetCategory)
        for (i in 0..srcCat.lastRowNum) {
            val srcRow = srcCat.getRow(i) ?: continue
            val newRow = expCat.createRow(i)
            for (c in 0 until 3) {
                newRow.createCell(c).setCellValue(getCellString(srcRow.getCell(c)))
            }
        }

        // ===== 元信息（版本标识，方便后续兼容） =====
        val metaSheet = exportWb.createSheet("元信息")
        metaSheet.createRow(0).apply {
            createCell(0).setCellValue("键")
            createCell(1).setCellValue("值")
        }
        metaSheet.createRow(1).apply {
            createCell(0).setCellValue("格式版本")
            createCell(1).setCellValue("2")
        }
        metaSheet.createRow(2).apply {
            createCell(0).setCellValue("应用名称")
            createCell(1).setCellValue("我们的小账本")
        }
        metaSheet.createRow(3).apply {
            createCell(0).setCellValue("导出时间")
            createCell(1).setCellValue(java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()))
        }

        sourceWb.close()
        outputStream.use { exportWb.write(it) }
        exportWb.close()
    }

    /**
     * 智能导入：自动识别新版(v2)和旧版格式，合并追加去重
     * 返回四元组描述：(记账数, 日记数, 会议数, 分类数)
     */
    data class ImportResult(val accounts: Int, val diaries: Int, val meetings: Int, val categories: Int)

    suspend fun importSmart(inputStream: java.io.InputStream): ImportResult = withContext(Dispatchers.IO) {
        val importWb = XSSFWorkbook(inputStream)
        val targetWb = getOrCreateWorkbook()

        var accountCount = 0
        var diaryCount = 0
        var meetingCount = 0
        var categoryCount = 0

        // 判断版本：有"元信息"sheet 或 sheet名为"记账" -> v2新版，否则旧版
        val isV2 = importWb.getSheet("元信息") != null || importWb.getSheet(sheetAccount) != null

        // ===== 记账 =====
        val impAccount = if (isV2) importWb.getSheet(sheetAccount) else importWb.getSheet("记账明细")
        if (impAccount != null) {
            val targetSheet = targetWb.getSheet(sheetAccount)!!
            val existing = mutableSetOf<String>()
            for (i in 1..targetSheet.lastRowNum) {
                val r = targetSheet.getRow(i) ?: continue
                existing.add("${getCellString(r.getCell(0))}_${getCellString(r.getCell(1))}_${getCellString(r.getCell(2))}_${getCellDouble(r.getCell(3))}_${getCellString(r.getCell(4))}")
            }
            // 检测列数判断是否旧版6列（含账户列）
            val headerRow = impAccount.getRow(0)
            val colCount = headerRow?.lastCellNum?.toInt() ?: 0
            val isLegacy6Col = !isV2 || colCount >= 6 && getCellString(headerRow?.getCell(4)).let { it == "账户" || it == "帐户" }

            for (i in 1..impAccount.lastRowNum) {
                val row = impAccount.getRow(i) ?: continue
                val rawDate = getCellString(row.getCell(0))
                val date = if (rawDate.length > 10) rawDate.substring(0, 10) else rawDate
                val type = getCellString(row.getCell(1))
                val category = getCellString(row.getCell(2))
                val amount = getCellDouble(row.getCell(3))
                val note = if (isLegacy6Col) getCellString(row.getCell(5)) else getCellString(row.getCell(4))
                if (date.isBlank() || type.isBlank()) continue
                val key = "${date}_${type}_${category}_${amount}_${note}"
                if (key in existing) continue
                val newRow = targetSheet.createRow(targetSheet.lastRowNum + 1)
                newRow.createCell(0).setCellValue(date)
                newRow.createCell(1).setCellValue(type)
                newRow.createCell(2).setCellValue(category)
                newRow.createCell(3).setCellValue(amount)
                newRow.createCell(4).setCellValue(note)
                existing.add(key)
                accountCount++
            }
        }

        // ===== 日记 =====
        val impDiary = importWb.getSheet(sheetDiary) ?: importWb.getSheet("日记")
        if (impDiary != null) {
            val targetSheet = targetWb.getSheet(sheetDiary)!!
            val existing = mutableSetOf<String>()
            for (i in 1..targetSheet.lastRowNum) {
                val r = targetSheet.getRow(i) ?: continue
                existing.add("${getCellString(r.getCell(0))}_${getCellString(r.getCell(1))}")
            }
            // 检测列顺序：旧版是 日期/天气/心情/标题/内容，新版是 日期/标题/内容/天气/心情/位置
            val header1 = getCellString(impDiary.getRow(0)?.getCell(1))
            val isLegacyDiary = header1 == "天气"

            for (i in 1..impDiary.lastRowNum) {
                val row = impDiary.getRow(i) ?: continue
                val date: String
                val title: String
                val content: String
                val weather: String
                val mood: String
                val location: String
                if (isLegacyDiary) {
                    date = getCellString(row.getCell(0))
                    weather = getCellString(row.getCell(1))
                    mood = getCellString(row.getCell(2))
                    title = getCellString(row.getCell(3))
                    content = getCellString(row.getCell(4))
                    location = ""
                } else {
                    date = getCellString(row.getCell(0))
                    title = getCellString(row.getCell(1))
                    content = getCellString(row.getCell(2))
                    weather = getCellString(row.getCell(3))
                    mood = getCellString(row.getCell(4))
                    location = getCellString(row.getCell(5))
                }
                if (date.isBlank()) continue
                val key = "${date}_${title}"
                if (key in existing) continue
                val newRow = targetSheet.createRow(targetSheet.lastRowNum + 1)
                newRow.createCell(0).setCellValue(date)
                newRow.createCell(1).setCellValue(title)
                newRow.createCell(2).setCellValue(content)
                newRow.createCell(3).setCellValue(weather)
                newRow.createCell(4).setCellValue(mood)
                newRow.createCell(5).setCellValue(location)
                existing.add(key)
                diaryCount++
            }
        }

        // ===== 会议纪要 =====
        val impMeeting = importWb.getSheet(sheetMeeting) ?: importWb.getSheet("会议纪要")
        if (impMeeting != null) {
            val targetSheet = targetWb.getSheet(sheetMeeting)!!
            val existing = mutableSetOf<String>()
            for (i in 1..targetSheet.lastRowNum) {
                val r = targetSheet.getRow(i) ?: continue
                existing.add("${getCellString(r.getCell(0))}_${getCellString(r.getCell(1))}")
            }
            // 检测：旧版7列（日期时间/主题/地点/参会人/内容/决议/待办），新版9列
            val headerRow = impMeeting.getRow(0)
            val colCount = headerRow?.lastCellNum?.toInt() ?: 0
            val isLegacyMeeting = colCount <= 7

            for (i in 1..impMeeting.lastRowNum) {
                val row = impMeeting.getRow(i) ?: continue
                val date: String
                val topic: String
                val startTime: String
                val endTime: String
                val location: String
                val attendees: String
                val content: String
                val todoItems: String
                val tags: String
                if (isLegacyMeeting) {
                    val rawDate = getCellString(row.getCell(0))
                    date = if (rawDate.length > 10) rawDate.substring(0, 10) else rawDate
                    topic = getCellString(row.getCell(1))
                    startTime = ""
                    endTime = ""
                    location = getCellString(row.getCell(2))
                    attendees = getCellString(row.getCell(3))
                    content = getCellString(row.getCell(4))
                    todoItems = getCellString(row.getCell(6))
                    tags = ""
                } else {
                    date = getCellString(row.getCell(0))
                    topic = getCellString(row.getCell(1))
                    startTime = getCellString(row.getCell(2))
                    endTime = getCellString(row.getCell(3))
                    location = getCellString(row.getCell(4))
                    attendees = getCellString(row.getCell(5))
                    content = getCellString(row.getCell(6))
                    todoItems = getCellString(row.getCell(7))
                    tags = getCellString(row.getCell(8))
                }
                if (date.isBlank()) continue
                val key = "${date}_${topic}"
                if (key in existing) continue
                val newRow = targetSheet.createRow(targetSheet.lastRowNum + 1)
                newRow.createCell(0).setCellValue(date)
                newRow.createCell(1).setCellValue(topic)
                newRow.createCell(2).setCellValue(startTime)
                newRow.createCell(3).setCellValue(endTime)
                newRow.createCell(4).setCellValue(location)
                newRow.createCell(5).setCellValue(attendees)
                newRow.createCell(6).setCellValue(content)
                newRow.createCell(7).setCellValue(todoItems)
                newRow.createCell(8).setCellValue(tags)
                existing.add(key)
                meetingCount++
            }
        }

        // ===== 分类配置（仅v2有） =====
        val impCat = importWb.getSheet(sheetCategory)
        if (impCat != null) {
            val targetSheet = ensureCategorySheet(targetWb)
            val existing = mutableSetOf<String>()
            for (i in 1..targetSheet.lastRowNum) {
                val r = targetSheet.getRow(i) ?: continue
                existing.add("${getCellString(r.getCell(0))}_${getCellString(r.getCell(1))}")
            }
            for (i in 1..impCat.lastRowNum) {
                val row = impCat.getRow(i) ?: continue
                val name = getCellString(row.getCell(0))
                val type = getCellString(row.getCell(1))
                val icon = getCellString(row.getCell(2))
                if (name.isBlank()) continue
                val key = "${name}_${type}"
                if (key in existing) continue
                val newRow = targetSheet.createRow(targetSheet.lastRowNum + 1)
                newRow.createCell(0).setCellValue(name)
                newRow.createCell(1).setCellValue(type)
                newRow.createCell(2).setCellValue(icon)
                existing.add(key)
                categoryCount++
            }
        }

        importWb.close()
        saveWorkbook(targetWb)
        targetWb.close()
        ImportResult(accountCount, diaryCount, meetingCount, categoryCount)
    }

    // 保留旧接口兼容
    suspend fun importFromLegacy(inputStream: java.io.InputStream): Triple<Int, Int, Int> {
        val result = importSmart(inputStream)
        return Triple(result.accounts, result.diaries, result.meetings)
    }
}
