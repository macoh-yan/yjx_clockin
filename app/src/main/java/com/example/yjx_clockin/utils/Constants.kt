package com.example.yjx_clockin.utils

/**
 * 统一管理应用级常量
 * 包括 SharedPreferences keys、API 配置、请求码等
 */
object Constants {

    // ==================== API 配置 ====================
    const val BASE_URL = "http://117.36.73.158:5000"

    // ==================== SharedPreferences ====================
    const val PREFS_NAME = "app_prefs"

    // Auth & User
    const val KEY_TOKEN = "token"
    const val KEY_EMP_ID = "emp_id"
    const val KEY_EMP_NAME = "emp_name"
    const val KEY_COOKIE = "cookie"

    // Device Binding
    const val KEY_DEVICE_BOUND = "device_bound"
    const val KEY_DEVICE_ID = "device_id"

    // Remember Password
    const val KEY_REMEMBER_PASSWORD = "remember_password"
    const val KEY_SAVED_EMP_ID = "saved_emp_id"
    const val KEY_SAVED_PASSWORD = "saved_password"

    // ==================== Permission Request Codes ====================
    const val LOCATION_PERMISSION_REQUEST_CODE = 100
    const val CAMERA_PERMISSION_REQUEST_CODE = 101

    // ==================== API Endpoints ====================
    object ApiEndpoints {
        const val LOGIN = "/api/employee/login"
        const val EMPLOYEES = "/api/employees"
        const val BIND_DEVICE = "/api/bind_device"
        const val CURRENT_USER = "/api/current_user"
        const val PUNCH_POINTS = "/api/emp/punch_points"
        const val TIME_RULE = "/api/emp/time_rule"
        const val UPLOAD_CHECK = "/api/upload_check"
        const val DAILY_RECORD = "/api/get_daily_record"
        const val CHANGE_PASSWORD = "/api/employee/change_password"
        const val LEAVE_MY = "/api/leave/my"
        const val EXCHANGE_MY = "/api/exchange/my"
        const val EXPENSE_MY = "/api/expense/my"
        const val LEAVE_WAIT_COUNT = "/api/leave/wait_count"
        const val EXCHANGE_WAIT_COUNT = "/api/exchange/wait_count"
        const val EXPENSE_WAIT_COUNT = "/api/expense/wait_count"
        const val OUT_APPLY_WAIT_COUNT = "/api/out_apply/wait_count"
        const val PURCHASE_WAIT_COUNT = "/api/purchase/wait_count"
        const val MY_SCHEDULE_TODAY = "/api/my_schedule_today"
    }

    // ==================== Intent Extras ====================
    const val EXTRA_URL = "url"
    const val EXTRA_TITLE = "title"

    // ==================== WebView ====================
    const val WEBVIEW_TIMEOUT_MS = 30000L
    const val LOCATION_TIMEOUT_MS = 10000L
}
