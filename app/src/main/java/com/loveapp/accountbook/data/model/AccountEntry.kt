package com.loveapp.accountbook.data.model

import com.loveapp.accountbook.R

data class AccountEntry(
    val date: String,
    val type: String,
    val category: String,
    val amount: Double,
    val note: String = "",
    val location: String = "",
    val rowIndex: Int = -1
) {
    val isIncome: Boolean get() = type == "收入"
    val isExpense: Boolean get() = type == "支出"

    companion object {
        val EXPENSE_CATEGORIES = listOf(
            Category(R.drawable.ic_cat_food, "餐饮"),
            Category(R.drawable.ic_cat_transport, "交通"),
            Category(R.drawable.ic_cat_shopping, "购物"),
            Category(R.drawable.ic_cat_house, "住房"),
            Category(R.drawable.ic_cat_phone, "通讯"),
            Category(R.drawable.ic_cat_game, "娱乐"),
            Category(R.drawable.ic_cat_medical, "医疗"),
            Category(R.drawable.ic_cat_education, "教育"),
            Category(R.drawable.ic_cat_clothes, "服饰"),
            Category(R.drawable.ic_cat_more, "更多")
        )
        val INCOME_CATEGORIES = listOf(
            Category(R.drawable.ic_cat_salary, "工资"),
            Category(R.drawable.ic_cat_bonus, "奖金"),
            Category(R.drawable.ic_cat_invest, "理财"),
            Category(R.drawable.ic_cat_parttime, "兼职"),
            Category(R.drawable.ic_cat_other, "其他")
        )
    }
}

data class Category(val icon: String, val name: String) {
    constructor(iconRes: Int, name: String) : this(iconRes.toString(), name)

    val iconRes: Int? get() = icon.toIntOrNull()
}
