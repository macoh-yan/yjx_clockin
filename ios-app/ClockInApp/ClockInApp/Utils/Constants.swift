import Foundation

struct Constants {
    static let BASE_URL = "http://117.36.73.158:5000"
    
    static let PREFS_NAME = "app_prefs"
    
    static let KEY_TOKEN = "token"
    static let KEY_EMP_ID = "emp_id"
    static let KEY_EMP_NAME = "emp_name"
    static let KEY_COOKIE = "cookie"
    
    static let KEY_DEVICE_BOUND = "device_bound"
    static let KEY_DEVICE_ID = "device_id"
    
    static let KEY_REMEMBER_PASSWORD = "remember_password"
    static let KEY_SAVED_EMP_ID = "saved_emp_id"
    static let KEY_SAVED_PASSWORD = "saved_password"
    
    static let LOCATION_PERMISSION_REQUEST_CODE = 100
    static let CAMERA_PERMISSION_REQUEST_CODE = 101
    
    struct ApiEndpoints {
        static let LOGIN = "/api/employee/login"
        static let EMPLOYEES = "/api/employees"
        static let BIND_DEVICE = "/api/bind_device"
        static let CURRENT_USER = "/api/current_user"
        static let PUNCH_POINTS = "/api/emp/punch_points"
        static let TIME_RULE = "/api/emp/time_rule"
        static let UPLOAD_CHECK = "/api/upload_check"
        static let DAILY_RECORD = "/api/get_daily_record"
        static let CHANGE_PASSWORD = "/api/employee/change_password"
        static let LEAVE_MY = "/api/leave/my"
        static let EXCHANGE_MY = "/api/exchange/my"
        static let EXPENSE_MY = "/api/expense/my"
        static let LEAVE_WAIT_COUNT = "/api/leave/wait_count"
        static let EXCHANGE_WAIT_COUNT = "/api/exchange/wait_count"
        static let EXPENSE_WAIT_COUNT = "/api/expense/wait_count"
        static let OUT_APPLY_WAIT_COUNT = "/api/out_apply/wait_count"
        static let PURCHASE_WAIT_COUNT = "/api/purchase/wait_count"
        static let MY_SCHEDULE_TODAY = "/api/my_schedule_today"
    }
    
    static let EXTRA_URL = "url"
    static let EXTRA_TITLE = "title"
    
    static let WEBVIEW_TIMEOUT_MS: TimeInterval = 30000
    static let LOCATION_TIMEOUT_MS: TimeInterval = 10000
}
