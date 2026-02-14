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
                note = getCellString(row.getCell(4))
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
                location = getCellString(row.getCell(5))
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
                tags = getCellString(row.getCell(8))
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
}
