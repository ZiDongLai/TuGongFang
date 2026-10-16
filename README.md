<div align="center">

# 图工坊 · TuGongFang

**Android 图片编辑应用 · 单图编辑 / 多图拼图 / 自由文字 / 影视信息模板**

![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4)
![Version](https://img.shields.io/badge/Stable-v1.0.43-2563EB)
![Status](https://img.shields.io/badge/Status-Private%20Development-64748B)

**[功能介绍](#-核心功能) · [运行项目](#-开发环境与本地运行) · [版本记录](#-版本记录) · [注意事项](#-项目状态与使用声明)**

</div>

---

## 📖 项目简介

**图工坊（TuGongFang）** 是一款以 Android 手机为主要使用场景的图片编辑应用，目标是在统一的编辑工作区中完成图片整理、拼图、文字标注、信息模板填写和图片导出，减少频繁切换工具的操作。

项目包含面向日常图片编辑的功能，也提供用于影视服装、造型、接戏照等工作场景的结构化文字信息模板。

> **当前稳定基线：v1.0.43**。已通过 Android Studio 编译和华为 Mate60 Pro 真机功能测试。本仓库用于个人开发、代码管理与稳定版本备份；不代表应用已在应用商店正式发布。

## ✨ 核心功能

| 模块 | 功能概览 |
| --- | --- |
| **单图编辑** | 图片导入、裁切、调色、画布背景、边框、间距、替换照片 |
| **多图拼图** | 模板拼图、自由拼图、画布比例、图片位置与尺寸调整 |
| **拼图模板** | 共 **87 款模板**，根据照片数量提供对应布局 |
| **文字编辑** | 自由文字框、字体样式、字号、文字排版、颜色、旋转与缩放 |
| **信息模板** | 影视接戏照等信息卡片；日期、角色、场次、描述/备注等字段 |
| **图形与标记** | 图形、符号、涂鸦、马赛克 |
| **颜色系统** | 预设颜色、自定义色轮、HEX 色值、饱和度和明度调节 |
| **图片变换** | 裁切、旋转90°、水平翻转、垂直翻转；部分对象支持自由旋转和缩放 |
| **项目管理** | 编辑中自动保存、最近项目、重新打开继续编辑 |
| **图片导出** | PNG / JPG，支持在导出图中保留文字、信息模板和已应用的图片效果 |

### 单图与拼图

- **单图模式**：围绕一张照片完成裁切、背景、边框、颜色和文字等编辑。
- **模板拼图**：按所选图片数量匹配布局，支持切换模板和调整单张照片。
- **自由拼图**：在画布内移动、缩放、旋转图片，配合裁切与图层操作完成自由排版。

### 影视信息模板

信息卡片可用于保存工作照的结构化说明，例如：

```text
影视接戏照
日期：2026-10-16
角色：示例角色
场次：示例场次
描述/备注：造型记录
```

实际字段和版式以所选模板及用户输入为准。支持信息卡片叠加到图片中，或在图片顶部 / 底部展示。v1.0.43 已优化导出时的日期标签与文字可读性。

## 🖼️ 界面截图

当前仓库未约定固定截图文件地址，因此暂不引用不存在的图片，避免 README 出现失效图像。

后续可以将实际真机截图放入仓库 `screenshots/` 文件夹，并在这里添加：**首页、单图编辑、模板拼图、自由拼图、信息模板、导出示例**。上传前请检查图片是否含私人照片、通知或敏感资料。

## 🛠️ 技术栈

| 项目 | 说明 |
| --- | --- |
| 开发语言 | Kotlin |
| UI | Jetpack Compose、Material 3 |
| 工程管理 | Android Studio、Gradle（Kotlin DSL） |
| Android 包名 | `com.tugongfang.editor` |
| 最低系统版本 | Android 8.0（API 26） |
| 编译 / 目标 SDK | 以仓库中的 `app/build.gradle.kts` 为准（当前配置为 API 37） |
| 稳定版配置 | `versionCode = 43`，`versionName = "1.0.43"` |

## 🚀 开发环境与本地运行

1. 安装 **Android Studio**，以及工程配置所需的 Android SDK、JDK 和 Gradle 组件。
2. 克隆或下载本仓库源码。私有仓库需要具备访问权限。
3. 在 Android Studio 中选择 **Open**，打开包含 `settings.gradle.kts` 的工程根目录。
4. 等待 **Gradle Sync** 完成。若 Android Studio 提示安装 SDK 组件，按提示安装。
5. 连接已开启开发者选项和 USB 调试的 Android 设备，选择 `app` 运行配置并点击 **Run**。

也可以在项目根目录使用 Gradle Wrapper 构建 Debug APK：

**Windows PowerShell**

```powershell
.\gradlew.bat :app:assembleDebug
```

成功后一般可在以下位置找到安装包：

```text
app/build/outputs/apk/debug/app-debug.apk
```

> **注意：** Debug APK 适合个人开发和测试；面向正式分发的安装包应使用规范签名流程。不要将签名私钥、密码、`local.properties` 或个人配置上传到 GitHub。

### 测试

工程包含 `app/src/test/` 下的单元测试和 `app/src/androidTest/` 下的 Android 设备测试。可根据开发环境执行：

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebugAndroidTest
```

Android 仪器测试通常需要已连接并配置正常的真机或模拟器才能运行。测试代码是否全部通过，应以当次构建与执行结果为准。

## 🗂️ 项目结构

```text
TuGongFang/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/tugongfang/editor/
│   │   │   │   ├── camera/          # 拍照与图片输入相关
│   │   │   │   ├── editor/          # 图片处理与导出逻辑
│   │   │   │   ├── model/           # 画布、图层、拼图等数据模型
│   │   │   │   ├── project/         # 本地项目存储与备份
│   │   │   │   ├── ui/              # Compose 界面与组件
│   │   │   │   └── MainActivity.kt
│   │   │   └── res/                # Android 资源
│   │   ├── test/                   # 单元测试
│   │   └── androidTest/            # 设备测试
│   └── build.gradle.kts
├── gradle/                         # Gradle 配置及 Wrapper
├── build.gradle.kts
├── settings.gradle.kts
├── .gitignore
└── README.md
```

目录说明只涵盖主要结构；具体文件以当前仓库为准。

## 🏷️ 版本记录

| 版本 | 状态 | 说明 |
| --- | --- | --- |
| **v1.0.43** | **Stable** | 修复信息模板导出日期名称及导出文字大小；通过 Android Studio 和真机验收 |
| v1.0.42 | 历史开发阶段 | 单图边框、色轮、拖动交互、拼图编辑、旋转工具及角部操作图标优化 |
| v1.0.41 | 历史稳定基线 | 拼图模板扩充至 87 款，并优化模板选择面板 |

版本源码与发布说明请查看仓库 [Releases](https://github.com/ZiDongLai/TuGongFang/releases)。

> 版本标签是固定时间点的源码快照。后续对 `main` 分支提交 README 或测试文件，不会自动更新已有 `v1.0.43` 标签。

## 🔒 项目状态与使用声明

- **当前是个人开发项目和私有源码仓库**，不代表已公开发布、开放注册或提供商业服务。
- 仓库中未声明适用于整个项目的开源许可证。**请勿将“能看到源码”理解为获得复制、修改或再分发的授权。**
- 项目包含的图标、图片、字体及其他素材，可能受各自的权利与许可约束。未经确认，不应将其作为可自由再分发的素材。
- 使用 Debug 安装包、重新签名或后续公开发布前，应完成必要的兼容性、隐私和合规检查。
- 未来功能计划不等于当前版本已经实现；请以实际源码、应用界面和对应版本记录为准。

## 📌 开发维护建议

- 已通过验收的稳定版本保留 Git 标签与完整源码备份。
- 每次更新递增版本号，并在 GitHub Desktop 中检查变更文件后再提交与推送。
- `.gradle/`、`build/`、`local.properties`、密钥和签名文件等不应进入代码仓库。
- 下一功能版本拟从 **v1.0.44** 继续；目前暂不新增功能。

---

<div align="center">

**图工坊 TuGongFang · 专注于高效、清晰的移动图片编辑体验**

`v1.0.43 Stable` · Android · Kotlin · Jetpack Compose

</div>
