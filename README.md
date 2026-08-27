# DSH — DeepSeek Harness 移动客户端

[![Android](https://img.shields.io/badge/Android-8.0%2B-34A853?style=flat-square&logo=android)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-Compose-7F52FF?style=flat-square&logo=kotlin)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](#许可)

**DSH** 是 [DeepSeek Harness](https://github.com/LingRonghui/DSHAPP)（自部署 AI 工作站）的官方 Android 客户端：以原生 WebView 壳承载网页端，配合深度的移动端适配与「深海声呐」视觉体系，把属于你自己的 AI 工作台装进口袋。

## 特性

- **自带服务器，数据私有** — 连接任意 DeepSeek Harness 服务器（内网 / 公网），所有会话与数据始终保存在你自己的服务器上，客户端不留存任何内容。
- **一次登录，长久在线** — 登录状态由服务器与全局 Cookie 管理，冷启动自动恢复上次连接的服务器，直达工作台，无需重复登录。
- **随时切换服务器** — 主界面右缘拉手呼出断开面板，一键断开当前服务器并切换到其他实例；历史连接以卡片形式管理，标注「上次」连接。
- **深海声呐视觉体系** — 深渊蓝声呐涟漪启动动画、雾蓝渐变连接页、冰雾蓝主界面底色，浅色 / 深色双主题自适应，简约大气。
- **移动端深度适配** — 注入式 CSS 适配层（`mobile_adapt.css`）持续打磨：抽屉导航、触屏交互去气泡、模态层级修复、安全区适配，WebView 里的网页端获得接近原生的手感。

## 界面预览

| 启动动画 | 连接页 | 工作台 |
| :---: | :---: | :---: |
| 声呐涟漪 + 鲸鱼入场 | 雾蓝渐变 + 历史卡片 | 冰雾蓝底 + 右缘拉手 |

## 快速开始

### 环境要求

- JDK 17
- Android SDK（compileSdk 34）
- Android 8.0（API 26）及以上设备或模拟器

### 构建

```bash
git clone https://github.com/LingRonghui/DSH.git
cd DSH
./gradlew assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 使用

1. 启动 DSH，在连接页填入你的 DeepSeek Harness 服务器地址（如 `http://192.168.1.10:3080`）
2. 点击「连接服务器」，在网页端完成登录
3. 之后每次打开 APP 将自动恢复连接；需要切换服务器时，从主界面右缘拉手呼出面板断开即可

## 技术架构

```
app/src/main/
├── java/com/dsh/app/
│   └── MainActivity.kt      # 全部原生层：启动幕动画、连接页、WebView 壳、
│                            # 断开面板、ServerStore（服务器历史持久化）
├── assets/
│   └── mobile_adapt.css     # 注入 WebView 的移动端适配层（随 onPageFinished 注入）
└── res/                     # 深海声呐主题（浅色 / 深色）、鲸鱼图标、启动色
```

- **UI**：Jetpack Compose（Material 3），单 Activity 架构，无第三方 UI 依赖
- **WebView**：DOM Storage 开启、文件 / 内容访问关闭、混合内容兼容模式、深浅色跟随系统
- **适配层**：`mobile_adapt.css` 通过 `document-start` 脚本与 `onPageFinished` 双阶段注入，版本化演进（当前 v2.6）

## 许可

本项目以 MIT 许可开源，详见 [LICENSE](LICENSE)。
