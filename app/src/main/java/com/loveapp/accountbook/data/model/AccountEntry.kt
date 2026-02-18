package com.loveapp.accountbook.data.model

data class AccountEntry(
    val date: String,
    val type: String,
    val category: String,
    val amount: Double,
    val note: String = "",
    val rowIndex: Int = -1
) {
    val isIncome: Boolean get() = type == "收入"
    val isExpense: Boolean get() = type == "支出"

    companion object {
        val EXPENSE_CATEGORIES = listOf(
            Category("🍔", "餐饮"), Category("🚗", "交通"), Category("🛒", "购物"),
            Category("🏠", "住房"), Category("📱", "通讯"), Category("🎮", "娱乐"),
            Category("💊", "医疗"), Category("📚", "教育"), Category("👗", "服饰"),
            Category("📌", "更多")
        )
        val INCOME_CATEGORIES = listOf(
            Category("💼", "工资"), Category("🎁", "奖金"), Category("💰", "理财"),
            Category("🤝", "兼职"), Category("💵", "其他")
        )
    }
}

data class Category(val icon: String, val name: String)