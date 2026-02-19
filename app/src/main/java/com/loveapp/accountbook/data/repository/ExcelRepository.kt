package com.loveapp.accountbook.data.repository

import android.content.Context
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

    private fun getFile(): File {
        val dir = File(context.getExternalFilesDir(null), "DataManager")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, fileName)
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
                saveWorkbook(this)
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

    // ========== 记账 ==========

    suspend fun getAccounts(): List<AccountEntry> = withContext(Dispatchers.IO) {
        val workbook = getOrCreateWorkbook()
        val sheet = workbook.getSheet(sheetAccount) ?: return@withContext emptyList()
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
        val sheet = workbook.getSheet(sheetAccount) ?: return@withContext
        val row = sheet.getRow(entry.rowIndex + 1) ?: return@withContext
        row.getCell(0)?.setCellValue(entry.date)
        row.getCell(1)?.setCellValue(entry.type)
        row.getCell(2)?.setCellValue(entry.category)
        row.getCell(3)?.setCellValue(entry.amount)
        row.getCell(4)?.setCellValue(entry.note)
        saveWorkbook(workbook)
        workbook.close()
    }

    suspend fun deleteAccount(entry: AccountEntry) = withContext(Dispatchers.IO) {
        val workbook = getOrCreateWorkbook()
        val sheet = workbook.getSheet(sheetAccount) ?: return@withContext
        val rowIndex = entry.rowIndex + 1
        val lastRow = sheet.lastRowNum
        if (rowIndex < lastRow) {
            sheet.shiftRows(rowIndex + 1, lastRow, -1)
        } else {
            sheet.removeRow(sheet.getRow(rowIndex) ?: return@withContext)
        }
        saveWorkbook(workbook)
        workbook.close()
    }

    // ========== 日记 ==========

    suspend fun getDiaries(): List<DiaryEntry> = withContext(Dispatchers.IO) {
        val workbook = getOrCreateWorkbook()
        val sheet = workbook.getSheet(sheetDiary) ?: return@withContext emptyList()
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
        val sheet = workbook.getSheet(sheetDiary) ?: return@withContext
        val row = sheet.getRow(entry.rowIndex + 1) ?: return@withContext
        row.getCell(0)?.setCellValue(entry.date)
        row.getCell(1)?.setCellValue(entry.title)
        row.getCell(2)?.setCellValue(entry.content)
        row.getCell(3)?.setCellValue(entry.weather)
        row.getCell(4)?.setCellValue(entry.mood)
        row.getCell(5)?.setCellValue(entry.location)
        saveWorkbook(workbook)
        workbook.close()
    }

    suspend fun deleteDiary(entry: DiaryEntry) = withContext(Dispatchers.IO) {
        val workbook = getOrCreateWorkbook()
        val sheet = workbook.getSheet(sheetDiary) ?: return@withContext
        val rowIndex = entry.rowIndex + 1
        val lastRow = sheet.lastRowNum
        if (rowIndex < lastRow) {
            sheet.shiftRows(rowIndex + 1, lastRow, -1)
        } else {
            sheet.removeRow(sheet.getRow(rowIndex) ?: return@withContext)
        }
        saveWorkbook(workbook)
        workbook.close()
    }

    // ========== 会议 ==========

    suspend fun getMeetings(): List<MeetingEntry> = withContext(Dispatchers.IO) {
        val workbook = getOrCreateWorkbook()
        val sheet = workbook.getSheet(sheetMeeting) ?: return@withContext emptyList()
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
        workbook.close()
    }

    suspend fun deleteMeeting(entry: MeetingEntry) = withContext(Dispatchers.IO) {
        val workbook = getOrCreateWorkbook()
        val sheet = workbook.getSheet(sheetMeeting) ?: return@withContext
        val rowIndex = entry.rowIndex + 1
        val lastRow = sheet.lastRowNum
        if (rowIndex < lastRow) {
            sheet.shiftRows(rowIndex + 1, lastRow, -1)
        } else {
            sheet.removeRow(sheet.getRow(rowIndex) ?: return@withContext)
        }
        saveWorkbook(workbook)
        workbook.close()
    }

    // ========== 导入导出 ==========

    fun getExcelFilePath(): String = getFile().absolutePath

    suspend fun exportToPath(targetPath: String) = withContext(Dispatchers.IO) {
        val source = getFile()
        if (source.exists()) {
            source.copyTo(File(targetPath), overwrite = true)
        }
    }

    suspend fun importFromPath(sourcePath: String) = withContext(Dispatchers.IO) {
        val source = File(sourcePath)
        if (source.exists()) {
            source.copyTo(getFile(), overwrite = true)
        }
    }

    /**
     * 从旧版备份文件合并导入数据（不覆盖，追加去重）
     * 支持旧版 sheet 名：记账明细、日记、会议纪要
     * 返回三元组：(导入记账数, 导入日记数, 导入会议数)
     */
    suspend fun importFromLegacy(inputStream: java.io.InputStream): Triple<Int, Int, Int> = withContext(Dispatchers.IO) {
        val legacyWb = XSSFWorkbook(inputStream)
        val targetWb = getOrCreateWorkbook()

        var accountCount = 0
        var diaryCount = 0
        var meetingCount = 0

        // ===== 记账明细 =====
        // 旧版列：日期(含时间) / 类型 / 分类 / 金额 / 账户 / 备注
        // 新版列：日期(yyyy-MM-dd) / 类型 / 分类 / 金额 / 备注
        val legacyAccount = legacyWb.getSheet("记账明细")
        if (legacyAccount != null) {
            val targetSheet = targetWb.getSheet(sheetAccount)!!
            // 收集已有记录的去重 key
            val existing = mutableSetOf<String>()
            for (i in 1..targetSheet.lastRowNum) {
                val r = targetSheet.getRow(i) ?: continue
                existing.add("${getCellString(r.getCell(0))}_${getCellString(r.getCell(1))}_${getCellString(r.getCell(2))}_${getCellDouble(r.getCell(3))}_${getCellString(r.getCell(4))}")
            }
            for (i in 1..legacyAccount.lastRowNum) {
                val row = legacyAccount.getRow(i) ?: continue
                val rawDate = getCellString(row.getCell(0))
                val date = if (rawDate.length > 10) rawDate.substring(0, 10) else rawDate
                val type = getCellString(row.getCell(1))
                val category = getCellString(row.getCell(2))
                val amount = getCellDouble(row.getCell(3))
                // 旧版第4列是账户，第5列是备注
                val note = getCellString(row.getCell(5))
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
        // 旧版列：日期 / 天气 / 心情 / 标题 / 内容
        // 新版列：日期 / 标题 / 内容 / 天气 / 心情 / 位置
        val legacyDiary = legacyWb.getSheet("日记")
        if (legacyDiary != null) {
            val targetSheet = targetWb.getSheet(sheetDiary)!!
            val existing = mutableSetOf<String>()
            for (i in 1..targetSheet.lastRowNum) {
                val r = targetSheet.getRow(i) ?: continue
                existing.add("${getCellString(r.getCell(0))}_${getCellString(r.getCell(1))}")
            }
            for (i in 1..legacyDiary.lastRowNum) {
                val row = legacyDiary.getRow(i) ?: continue
                val date = getCellString(row.getCell(0))
                val weather = getCellString(row.getCell(1))
                val mood = getCellString(row.getCell(2))
                val title = getCellString(row.getCell(3))
                val content = getCellString(row.getCell(4))
                val key = "${date}_${title}"
                if (key in existing) continue
                val newRow = targetSheet.createRow(targetSheet.lastRowNum + 1)
                newRow.createCell(0).setCellValue(date)
                newRow.createCell(1).setCellValue(title)
                newRow.createCell(2).setCellValue(content)
                newRow.createCell(3).setCellValue(weather)
                newRow.createCell(4).setCellValue(mood)
                newRow.createCell(5).setCellValue("")
                existing.add(key)
                diaryCount++
            }
        }

        // ===== 会议纪要 =====
        // 旧版列：日期时间 / 会议主题 / 地点 / 参会人员 / 会议内容 / 决议事项 / 待办事项
        // 新版列：日期 / 主题 / 开始时间 / 结束时间 / 地点 / 参会人 / 内容 / 待办事项 / 标签
        val legacyMeeting = legacyWb.getSheet("会议纪要")
        if (legacyMeeting != null) {
            val targetSheet = targetWb.getSheet(sheetMeeting)!!
            val existing = mutableSetOf<String>()
            for (i in 1..targetSheet.lastRowNum) {
                val r = targetSheet.getRow(i) ?: continue
                existing.add("${getCellString(r.getCell(0))}_${getCellString(r.getCell(1))}")
            }
            for (i in 1..legacyMeeting.lastRowNum) {
                val row = legacyMeeting.getRow(i) ?: continue
                val rawDate = getCellString(row.getCell(0))
                val date = if (rawDate.length > 10) rawDate.substring(0, 10) else rawDate
                val topic = getCellString(row.getCell(1))
                val location = getCellString(row.getCell(2))
                val attendees = getCellString(row.getCell(3))
                val content = getCellString(row.getCell(4))
                val todoItems = getCellString(row.getCell(6))
                val key = "${date}_${topic}"
                if (key in existing) continue
                val newRow = targetSheet.createRow(targetSheet.lastRowNum + 1)
                newRow.createCell(0).setCellValue(date)
                newRow.createCell(1).setCellValue(topic)
                newRow.createCell(2).setCellValue("")
                newRow.createCell(3).setCellValue("")
                newRow.createCell(4).setCellValue(location)
                newRow.createCell(5).setCellValue(attendees)
                newRow.createCell(6).setCellValue(content)
                newRow.createCell(7).setCellValue(todoItems)
                newRow.createCell(8).setCellValue("")
                existing.add(key)
                meetingCount++
            }
        }

        legacyWb.close()
        saveWorkbook(targetWb)
        targetWb.close()
        Triple(accountCount, diaryCount, meetingCount)
    }
}
