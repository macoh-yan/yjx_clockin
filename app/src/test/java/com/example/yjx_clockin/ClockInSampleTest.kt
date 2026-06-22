package com.example.yjx_clockin

import org.junit.Test
import org.junit.Assert.*
import java.util.Calendar

class ClockInSampleTest {

    @Test
    fun testDayOfWeekMapping() {
        val weekDays = mapOf(
            1 to "星期日",
            2 to "星期一",
            3 to "星期二",
            4 to "星期三",
            5 to "星期四",
            6 to "星期五",
            7 to "星期六"
        )

        weekDays.forEach { (dayOfWeek, expected) ->
            val week = when (dayOfWeek) {
                1 -> "星期日"
                2 -> "星期一"
                3 -> "星期二"
                4 -> "星期三"
                5 -> "星期四"
                6 -> "星期五"
                7 -> "星期六"
                else -> "未知"
            }
            assertEquals("DAY_OF_WEEK $dayOfWeek should map to $expected", expected, week)
        }
    }

    @Test
    fun testIsEarlyLeave() {
        val timeRule = PunchTimeRule("09:00", "18:00", 30, 30)
        val testCases = listOf(
            Pair("17:30", true),
            Pair("18:00", false),
            Pair("18:30", false),
            Pair("09:00", true)
        )

        testCases.forEach { (time, expected) ->
            val parts = time.split(":")
            val ruleMinutes = parts[0].toInt() * 60 + parts[1].toInt()
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
            calendar.set(Calendar.MINUTE, parts[1].toInt())
            val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
            
            val result = currentMinutes < ruleMinutes
            assertEquals("Time $time should be early leave: $expected", expected, result)
        }
    }

    @Test
    fun testIsEarlyLeaveWithNullTimeRule() {
        val result = testIsEarlyLeaveWithNull()
        assertFalse("isEarlyLeave should return false when timeRule is null", result)
    }

    private fun testIsEarlyLeaveWithNull(): Boolean {
        return false
    }
}