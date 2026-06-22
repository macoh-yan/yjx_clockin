# 亿杰智企综合管理平台 - 移动端

[![Build Status](https://img.shields.io/badge/build-passing-green.svg)](https://github.com/example/yjx_clockin)
[![Platform](https://img.shields.io/badge/platform-Android-blue.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/language-Kotlin-orange.svg)](https://kotlinlang.org/)

## 项目简介

亿杰智企综合管理平台移动端是一款基于 Android 平台的企业考勤打卡应用，提供员工考勤打卡、设备绑定、人脸验证、审批流程等核心功能。

## 核心功能

- **考勤打卡**：支持上班/下班打卡，包含定位围栏检测和人脸验证
- **设备绑定**：账号与设备绑定，确保安全性
- **审批管理**：支持请假、调休、报销、外出、加班、采购等审批流程
- **个人中心**：个人信息展示、密码修改、设备状态管理
- **工作台**：集成个人记录、申请菜单、审批菜单

## 技术栈

| 分类 | 技术 | 版本 |
|------|------|------|
| 语言 | Kotlin | - |
| 框架 | Android Jetpack | - |
| UI | Material Design 3 | - |
| 地图 | 高德地图 SDK | 11.1.200 |
| 网络 | OkHttp | 4.12.0 |
| 图片加载 | Glide | 4.15.0 |
| 协程 | Kotlin Coroutines | 1.6.4 |

## 环境要求

- **Android SDK**：API 36（compileSdk）
- **最小支持版本**：API 24（Android 7.0）
- **构建工具**：Gradle 8.x
- **Kotlin版本**：与Compose兼容的版本

## 快速开始

### 构建命令

```bash
# 构建Debug版本
./gradlew assembleDebug

# 构建Release版本
./gradlew assembleRelease

# 运行测试
./gradlew test
```

### 配置说明

1. **高德地图API Key**：在 `AndroidManifest.xml` 中配置
2. **签名配置**：使用 `yjx_clockin_release.jks` 进行Release签名
3. **服务器地址**：`http://117.36.73.158:5000`

## 项目结构

```
app/
├── libs/                          # 第三方库（高德地图SDK）
├── src/main/
│   ├── java/com/example/yjx_clockin/
│   │   ├── model/                 # 数据模型
│   │   ├── ui/theme/              # Compose主题配置
│   │   ├── utils/                 # 工具类
│   │   ├── ClockInSample.kt       # 打卡Fragment
│   │   ├── ClockInSamplePage.kt   # 主页Activity
│   │   ├── LoginActivity.kt       # 登录Activity
│   │   ├── ProfileContent.kt      # 个人中心Fragment
│   │   └── WorkbenchContent.kt    # 工作台Fragment
│   └── res/                       # 资源文件
├── build.gradle.kts               # 模块构建配置
└── proguard-rules.pro             # 混淆规则
```

## 安全特性

- 设备绑定验证
- 虚拟定位检测
- Token认证机制
- 防重复打卡
- 密码本地加密

## 许可证

MIT License

## 文档

详细文档请参考：[项目代码Wiki.md](项目代码Wiki.md)