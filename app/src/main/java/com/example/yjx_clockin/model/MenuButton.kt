package com.example.yjx_clockin.model

data class MenuButton(
    val name: String,
    val icon: String,   // FontAwesome 类名，如 "fa-user"
    val url: String
)

// PersonalRecord.kt (用于展示个人记录)
sealed class PersonalRecord {
    data class Leave(val type: String, val startTime: String, val status: Int) : PersonalRecord()
    data class Exchange(val type: String, val startTime: String, val status: Int) : PersonalRecord()
    data class Expense(val amount: Double, val applyDate: String, val status: Int) : PersonalRecord()
}

// 分组数据
data class MenuGroup(val title: String, val items: List<MenuButton>)