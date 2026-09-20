# 心迹 MoodNotes

![Platform](https://img.shields.io/badge/platform-Android%2014%2B-3DDC84?logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-7F52FF?logo=kotlin)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203%20Expressive-4285F4?logo=jetpackcompose)
![License](https://img.shields.io/badge/license-NCAL%20v1.0-orange)

一款本地优先的心情记录 + 日记应用。记一笔心情，写几行字，回头翻日历看看自己是怎么过来的。

数据只存在你自己的手机里 —— 不联网、不上传、不要账号、没有第三方 SDK。

---

## 功能

**今日** — 选一个心情，拖动强度（5 级），随手加两句备注，当天的日记卡片就在下方。

**日历** — 月视图上每天带心情标记，点开任意一天可以补记心情，或跳到那天的日记。

**日记** — 按月分组，支持插入图片和视频，全屏编辑器，写一半可以存草稿。点击日记默认是**查看模式**：图片可双指缩放放大，视频调起系统播放器，右上角再进编辑。正文支持 Markdown：标题、加粗、斜体、列表、引用、行内代码。

**统计** — 连续记录天数、本月与累计条数、心情分布、平均强度、本周回顾。

**隐私锁** — 4 位 PIN，可选生物识别解锁，另配 3 个保密问题用于找回。连续输错 5 次进入递增冷却（30 / 60 / 300 / 600 秒）。PIN 与答案只保存 SHA-256 加盐哈希，不存明文。

**每日提醒** — 基于 WorkManager 的定时任务，默认 21:00 提醒你记一笔，时间可自定义。

**个性化** — 6 套主题色，外加主题册可收藏自定义种子色；5 款渐变背景、4 档字号、深色模式（跟随系统 / 浅色 / 深色）、减少动画开关、周起始日（周一 / 周日）。

**多语言** — 简体中文 / English，使用系统 per-app locale，可在应用内独立切换，不必跟随系统语言。

---

<!--
截图位：建议放到 docs/screenshots/ 下，然后取消下面的注释
<div align="center">
  <img src="docs/screenshots/today.png" width="200" alt="今日"/>
  <img src="docs/screenshots/calendar.png" width="200" alt="日历"/>
  <img src="docs/screenshots/diary.png" width="200" alt="日记"/>
  <img src="docs/screenshots/stats.png" width="200" alt="统计"/>
</div>
-->

## 隐私

立信根本：

- **没有网络权限。** 应用不发起任何网络请求，也不包含任何分析、广告或崩溃上报 SDK。
- **数据全在本地。** 心情与日记保存在设备上的 Room 数据库（`moodnotes.db`）中，随应用卸载一并清除。
- **只申请两个权限。** `POST_NOTIFICATIONS` 用于每日提醒，`USE_BIOMETRIC` 用于指纹 / 人脸解锁。两者都只在对应功能开启时才需要。
- **锁屏凭据只存哈希。** 隐私锁的 PIN 与保密问题答案经 SHA-256 加随机盐处理后保存，应用本身也不知道你的明文 PIN。

**代价是：目前没有导出与云同步功能，但是以后应该会添加自定义WebDAV。**

---

## 获取

- **下载 APK** — 前往本仓库的 Releases 页面获取签名好的安装包。
- **自行构建** —按下面的步骤从源码编译。

### 系统要求

- Android 14（API 34）及以上
- 构建环境：JDK 21、Android SDK（compileSdk 37）、Gradle 9.7.0（wrapper 自带，无需手动安装）

### 构建步骤

```bash
# 1. 配置 SDK 路径：在项目根目录创建 local.properties
sdk.dir=/path/to/your/Android/Sdk

# 2. 构建
./gradlew assembleDebug      # 调试版
./gradlew assembleRelease    # 正式版（需自备签名配置）
```

Windows 用 `gradlew.bat`，并确保 `JAVA_HOME` 指向 JDK 21：

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-21'
.\gradlew.bat assembleRelease
```

产物位于 `app/build/outputs/apk/{debug,release}/`。安装到设备：

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

> **关于签名**：仓库自带的签名配置仅供参考。请创建自己的 keystore，并在 `app/build.gradle.kts` 中替换。覆盖安装必须使用同一签名，keystore 一旦丢失，已安装的应用将无法升级、数据也无法读取。

---

## 技术栈

|     |     |
| --- | --- |
| 语言  | Kotlin 2.4.10 |
| UI  | Jetpack Compose（BOM 2026.06.01）+ Material 3 Expressive |
| 架构  | 单模块 + Repository，ViewModel + StateFlow |
| 数据库 | Room 2.8.4（KSP，schema v4） |
| 后台任务 | WorkManager 2.10.0 |
| 构建  | AGP 9.3.1 · Gradle 9.7.0 · JDK 21 |

`minSdk 34` · `targetSdk 36` · `compileSdk 37` · 包名 `com.moodnotes.app`

---

## 项目结构

```
app/src/main/java/com/moodnotes/app/
├── MainActivity.kt          入口（隐私锁状态机 / 首次启动分流）
├── data/                    Room 实体、DAO、Repository、设置持久化
│   ├── MoodRecord.kt        每日心情
│   ├── DiaryEntry.kt        日记（图片 / 视频 / 草稿标记）
│   ├── CustomMood.kt        自定义心情
│   ├── SavedTheme.kt        主题册收藏
│   └── AppDatabase.kt       schema v4 + 迁移链
├── ui/
│   ├── MoodNotesApp.kt      导航 + 底部栏
│   ├── today/ calendar/ diary/ stats/
│   ├── settings/            设置页 + 主题册
│   ├── lock/                隐私锁（PIN / 保密问题 / 生物识别）
│   ├── welcome/             首次启动引导 + 许可证
│   ├── mood/                心情选择器与视觉组件
│   ├── theme/               Material 3 主题
│   └── components/          动画与通用组件
├── util/                    日期 / 图片 / 安全哈希
└── work/                    每日提醒 Worker
```

资源文件：`res/values/` 为中文（默认），`res/values-en/` 为英文。新增界面文案时两边都要补，不要硬编码字符串。

### 开发约定

- 依赖版本统一维护在 `gradle/libs.versions.toml`，不要在 `build.gradle.kts` 里写死版本号
- 版本号只在 `app/build.gradle.kts` 的 `defaultConfig` 中修改
- 数据库结构变更时，schema 版本号 +1 并**追加**新的 Migration，不要改动已发布的迁移
- 源码与配置文件统一 UTF-8 无 BOM
- `gradle.properties` 中的 `android.overridePathCheck=true` 请勿删除 —— 项目路径含非 ASCII 字符时 AGP 默认会拒绝构建

---

## 参与

欢迎提 Issue 反馈问题或建议。提交 PR 前请注意：

- 界面文案同时补充 `values/strings.xml` 与 `values-en/strings.xml`
  
- 保持 Kotlin 官方代码风格（`kotlin.code.style=official`）
  

---

## 许可证

本项目采用 **非商业署名许可证（NCAL）v1.0**：

- ✅ 允许个人使用、学习、修改与分享
- ❌ 禁止任何形式的商业使用
- 📝 转载、分发或二次创作需保留署名

完整法律文本见 [`许可证/`](许可证/) 目录，含中英文两个版本。

---

Made by 荣荣
