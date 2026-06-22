# 亿杰智企打卡系统 (yjx_clockin) - 代码文档

## 项目概述

| 属性 | 值 |
|------|-----|
| 包名 | `com.example.yjx_clockin` |
| 版本 | 1.0 (versionCode: 1) |
| 最低SDK | API 24 (Android 7.0) |
| 目标SDK | API 36 |
| 语言 | Kotlin |
| UI框架 | Jetpack Compose + ViewBinding 混合 |
| 地图SDK | 高德地图 (AMap) 3DMap + Search + Location |
| 网络库 | OkHttp 4.12.0 |
| 后端地址 | `http://117.36.73.158:5000` |

---

## 目录结构

```
yjx_clockin/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/yjx_clockin/
│   │   │   ├── model/
│   │   │   │   └── MenuButton.kt          # 数据模型
│   │   │   ├── ui/theme/                  # Compose 主题
│   │   │   │   ├── Color.kt
│   │   │   │   ├── Theme.kt
│   │   │   │   └── Type.kt
│   │   │   ├── utils/                     # 工具类
│   │   │   │   ├── ApiService.kt          # API 封装
│   │   │   │   ├── DialogUtils.kt         # 对话框工具
│   │   │   │   └── WorkbenchAdapter.kt    # 工作台列表适配器
│   │   │   ├── LoginActivity.kt           # 登录页
│   │   │   ├── ClockInSample.kt           # 打卡 Fragment
│   │   │   ├── ClockInSamplePage.kt       # 打卡主页 (含 Tab)
│   │   │   ├── FragmentAdapter.kt         # ViewPager 适配器
│   │   │   ├── WorkbenchContent.kt        # 工作台 Fragment
│   │   │   ├── ProfileContent.kt          # 个人中心 Fragment
│   │   │   └── WebViewActivity.kt         # WebView 容器
│   │   ├── res/                           # 资源文件
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── gradle/                                 # Gradle Wrapper
├── build.gradle.kts                        # 根构建配置
├── settings.gradle.kts
├── gradle.properties
└── yjx_clockin_release.jks                # 签名文件
```

---

## 核心模块

### 1. 登录模块 - LoginActivity

**文件**: `app/src/main/java/com/example/yjx_clockin/LoginActivity.kt`

**功能**:
- 员工编号 + 密码登录
- 记住密码功能 (SharedPreferences)
- 设备绑定验证 (防止账号在多设备登录)
- 登录成功后跳转 ClockInSamplePage

**核心流程**:
```
用户输入 → 验证 → 检查设备绑定 →
  ├─ 未绑定 → 自动绑定设备 → 登录
  ├─ 已绑定本设备 → 直接登录
  └─ 已绑定其他设备 → 拒绝登录
```

**关键方法**:
| 方法 | 说明 |
|------|------|
| `loadSavedCredentials()` | 加载本地保存的账号密码 |
| `saveCredentials()` | 保存/清除记住的密码 |
| `checkAndBindDevice()` | 检查并执行设备绑定 |
| `performLogin()` | 执行登录请求 |

**SharedPreferences Key**:
| Key | 类型 | 说明 |
|-----|------|------|
| `saved_emp_id` | String | 保存的员工编号 |
| `saved_password` | String | 保存的密码 |
| `remember_password` | Boolean | 是否记住密码 |
| `token` | String | 登录 Token |
| `emp_id` | String | 当前员工编号 |
| `emp_name` | String | 当前员工姓名 |
| `device_bound` | Boolean | 设备绑定状态 |
| `device_id` | String | 绑定的设备ID |

---

### 2. 打卡模块 - ClockInSample

**文件**: `app/src/main/java/com/example/yjx_clockin/ClockInSample.kt`

**功能**:
- 高德地图展示打卡区域围栏
- GPS 定位获取当前位置
- 上班打卡 / 下班打卡
- 人脸识别验证 (部分打卡点需开启)
- 虚拟定位检测 (禁止模拟位置打卡)
- 早退确认提示

**打卡点数据结构** (从服务器获取):
```kotlin
data class PunchTimeRule(
    checkInDeadline: String,   // 上班打卡截止时间
    checkOutStart: String,     // 下班打卡开始时间
    lateMinutes: Int,          // 迟到分钟数
    earlyLeaveMinutes: Int     // 早退分钟数
)
```

**核心流程**:
```
点击打卡按钮 → 检查设备绑定 → 获取当前位置 →
├─ 在打卡范围内
│   ├─ 需要人脸验证 → 打开相机 → 拍照 → 提交打卡
│   └─ 不需要人脸 → 直接提交打卡
└─ 不在范围内 → 提示不在打卡范围
```

**API 接口**:
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/emp/punch_points` | GET | 获取打卡点列表及围栏 |
| `/api/emp/time_rule` | GET | 获取打卡时间规则 |
| `/api/upload_check` | POST | 提交打卡记录 |
| `/api/get_daily_record` | POST | 获取当日打卡记录 |

**防作弊机制**:
- `isMock` 检测: 禁止虚拟定位
- 设备绑定验证: 防止换设备打卡
- 围栏范围校验: 必须在指定范围内

---

### 3. 工作台模块 - WorkbenchContent

**文件**: `app/src/main/java/com/example/yjx_clockin/WorkbenchContent.kt`

**功能**:
- 展示个人记录入口 (报销、请假、调休、外出、采购、加班、工资条)
- 展示申请入口 (各类申请表单)
- 展示审批入口 (根据用户角色动态显示)
- 下拉刷新
- WebView 打开各类表单页面

**角色权限对照**:

| 审批类型 | 有权限的角色 |
|----------|-------------|
| 请假/调休/外出/加班审批 | admin, manager, supervisor, general_manager, tech_supervisor, hr_supervisor |
| 报销审批 | admin, finance, accountant, general_manager, supervisor, manager, tech_supervisor |
| 采购审批 | admin, manager, supervisor, general_manager, tech_supervisor, purchaser, storekeeper |
| 费用审批 | admin, finance, accountant, general_manager, supervisor, tech_supervisor |

**个人记录项**:
| 名称 | 图标 | 路径 |
|------|------|------|
| 报销记录 | ic_expense | /employee/expense_list |
| 请假记录 | ic_leave | /employee/leave_list |
| 调休记录 | ic_exchange | /employee/exchange_list |
| 外出记录 | ic_out_apply | /employee/out_list |
| 采购记录 | ic_purchase | /employee/purchase_list |
| 加班记录 | ic_overtime | /employee/overtime_list |
| 费用申请记录 | ic_cost | /employee/cost_list |
| 工资条 | ic_salary | /employee/salary |

---

### 4. 个人中心 - ProfileContent

**文件**: `app/src/main/java/com/example/yjx_clockin/ProfileContent.kt`

**功能**:
- 显示员工头像、姓名、部门、工号、联系方式、入职日期
- 修改密码
- 显示关于信息
- 退出登录

**头像加载**: 支持 Base64 字符串解码显示

---

### 5. WebView 容器 - WebViewActivity

**文件**: `app/src/main/java/com/example/yjx_clockin/WorkbenchContent.kt` (内联类)

**功能**:
- 加载 H5 表单页面
- 保持登录态 Cookie
- Token 参数自动注入
- 附件下载支持 (Excel/PDF 等)
- 自动清除页面顶部 Header
- 表单导出拦截 (为导出链接添加 Token)

**JavaScript 注入**:
```javascript
// 为附件链接添加 Token
document.querySelectorAll('a[href*="attachment"], a[href*="export"]')

// 拦截表单提交
document.querySelectorAll('form[action*="export"], form[action*="excel"]')
```

**文件下载处理**:
- 支持 Content-Disposition 文件名解析
- 支持 RFC 5987 (filename*)
- 自动修正文件扩展名 (.bin → .xlsx 等)
- 根据 MIME 类型生成默认文件名

---

### 6. API 服务 - ApiService

**文件**: `app/src/main/java/com/example/yjx_clockin/utils/ApiService.kt`

**单例对象**, 封装所有网络请求。

**Base URL**: `http://117.36.73.158:5000`

**认证方式**: Bearer Token (登录后通过 `setToken()` 设置)

**主要接口**:

| 接口路径 | 方法 | 说明 |
|----------|------|------|
| `/api/employee/login` | POST | 员工登录 |
| `/api/employees` | GET | 获取员工列表 |
| `/api/employees?keyword=xxx` | GET | 搜索员工 |
| `/api/current_user` | GET | 获取当前用户信息 |
| `/api/bind_device` | POST | 绑定设备 |
| `/api/leave/my` | GET | 我的请假记录 |
| `/api/exchange/my` | GET | 我的调休记录 |
| `/api/expense/my` | GET | 我的报销记录 |
| `/api/leave/wait_count` | GET | 请假待审批数量 |
| `/api/employee/change_password` | POST | 修改密码 |
| `/employee/mobile` | GET | 获取移动端菜单 |

---

### 7. 对话框工具 - DialogUtils

**文件**: `app/src/main/java/com/example/yjx_clockin/utils/DialogUtils.kt`

**单例对象**, 使用 Material Design AlertDialog 封装统一风格的对话框。

```kotlin
DialogUtils.showCustomDialog(
    context = context,
    title = "标题",
    message = "内容",
    iconRes = R.drawable.ic_dialog_info,
    positiveText = "确定",
    negativeText = "取消",
    onPositive = { /* 确定回调 */ },
    onNegative = { /* 取消回调 */ }
)
```

---

### 8. 工作台适配器 - WorkbenchAdapter

**文件**: `app/src/main/java/com/example/yjx_clockin/utils/WorkbenchAdapter.kt`

RecyclerView 多类型适配器, 支持 6 种 ViewType:

| Type | 说明 |
|------|------|
| `TYPE_WELCOME` | 欢迎卡片 |
| `TYPE_PERSONAL_GRID` | 个人记录九宫格 |
| `TYPE_APPLY_HEADER` | "申请管理" 分组标题 |
| `TYPE_APPLY_GRID` | 申请入口网格 |
| `TYPE_APPROVAL_HEADER` | "审批管理" 分组标题 |
| `TYPE_APPROVAL_GRID` | 审批入口网格 |

---

## 权限清单

| 权限 | 用途 |
|------|------|
| `INTERNET` | 网络请求 |
| `ACCESS_FINE_LOCATION` | GPS 精确定位 |
| `ACCESS_COARSE_LOCATION` | 网络定位辅助 |
| `ACCESS_NETWORK_STATE` | 检测网络状态 |
| `ACCESS_WIFI_STATE` | WiFi 定位辅助 |
| `CHANGE_WIFI_STATE` | WiFi 状态监控 |
| `ACCESS_LOCATION_EXTRA_COMMANDS` | 加速 GPS 定位 |
| `WRITE_SETTINGS` | 系统设置写入 |
| `WRITE_EXTERNAL_STORAGE` | 地图缓存 (maxSdk=28) |
| `READ_EXTERNAL_STORAGE` | 文件读取 (maxSdk=32) |
| `CAMERA` | 人脸拍照 |

---

## 第三方库依赖

| 库 | 版本 | 用途 |
|----|------|------|
| OkHttp | 4.12.0 | HTTP 客户端 |
| Retrofit | 2.9.0 | REST 客户端 (已引入但未使用) |
| Gson Converter | 2.9.0 | JSON 解析 (已引入但未使用) |
| Glide | 4.15.0 | 图片加载 (已引入但未使用) |
| CircleImageView | 3.1.0 | 圆形头像 |
| Material Components | 1.9.0 | Material Design UI |
| AMap 3DMap | 11.1.200 | 3D 地图展示 |
| AMap Search | 9.7.4 | 地图搜索服务 |
| AMap Location | 11.1.200 | 定位服务 |
| Kotlinx Coroutines | 1.6.4 | 协程支持 |
| SwipeRefreshLayout | 1.1.0 | 下拉刷新 |
| ConstraintLayout | 2.1.4 | 复杂布局 |

---

## 数据流

```
登录 → Token 存储 SharedPreferences
    ↓
ClockInSamplePage (ViewPager + TabLayout)
    ├── Tab 0: ClockInSample (打卡)
    │   ├── 获取打卡围栏 /api/emp/punch_points
    │   ├── 定位 (高德 SDK)
    │   └── 提交打卡 /api/upload_check
    │
    ├── Tab 1: WorkbenchContent (工作台)
    │   ├── 获取当前用户 /api/current_user
    │   ├── 个人记录列表 (本地配置)
    │   ├── 申请菜单 (本地配置)
    │   └── 审批菜单 (根据角色过滤)
    │
    └── Tab 2: ProfileContent (我的)
        ├── 获取员工详情 /api/employees?keyword=xxx
        ├── 修改密码 /api/employee/change_password
        └── 退出登录 (清除 Token)
```

---

## 布局文件对照

| 文件 | 对应 Activity/Fragment |
|------|------------------------|
| `activity_login.xml` | LoginActivity |
| `activity_clock_in_sample_page.xml` | ClockInSamplePage |
| `activity_clock_in_sample.xml` | ClockInSample (Fragment) |
| `activity_workbench_content.xml` | WorkbenchContent (Fragment) |
| `activity_profile_content.xml` | ProfileContent (Fragment) |
| `activity_webview.xml` | WebViewActivity |
| `fragment_workbench.xml` | WorkbenchContent 布局 |
| `item_welcome_card.xml` | 工作台欢迎卡片 |
| `item_personal_grid.xml` | 个人记录九宫格容器 |
| `item_grid_menu.xml` | 申请/审批菜单网格容器 |
| `dialog_change_password.xml` | 修改密码弹窗 |
| `dialog_custom_layout.xml` | 通用对话框布局 |

---

## 主题配置

**文件**: `app/src/main/res/values/themes.xml`

主要使用 `AppTheme` 和 `Theme.Yjx_clockin`, 支持:
- 沉浸式状态栏 (透明)
- 导航栏沉浸
- Material Design 组件风格

---

## 构建配置

**Gradle Version**: 8.7
**Android Gradle Plugin**: 8.5.0+
**Kotlin**: 2.0.0 (via kotlin.compose compiler plugin)

**NDK ABI Filter**: `arm64-v8a`, `armeabi-v7a`

**ProGuard**: 未启用 (release minifyEnabled = false)

---

## 版本信息

- **APP Name**: 亿杰智企
- **签名**: `yjx_clockin_release.jks`
- **版本**: 1.0.0
