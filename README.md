<div align="center">
  
![logo](https://raw.githubusercontent.com/roaming555/WearAI/refs/heads/main/ic_launcher-showmd.png)

![logo](https://raw.githubusercontent.com/roaming555/WearAI/refs/heads/main/ic_launcher-showmd.png)
# WearAI

轻量的第三方 AI 聊天客户端

</div>

# 介绍

一个**轻量**的 **AI 聊天客户端**，默认使用**智谱大模型**（也支持换别的）。使用 **Kotlin + Jetpack Compose** 开发，遵循 **Material Design 3** 与 **莫奈取色（Android 12及以上）** 设计标准，最低支持 **安卓5.0**。

- 项目名：`WearAI`
- 包名：`com.foggland.wearai`
- 当前版本：**1.1**
- 最低兼容：**Android 5.0（API 21）**
- 默认模型（调用名）：**`glm-4.7-flash`**
- 开源协议：**GNU General Public License v3.0（GPL-3.0）**

本项目使用了**智谱官方API**与若干开源库（详见「致谢名单」），除此之外与其他第三方 AI 聊天软件无任何关系。

1. 尽量保证软件的轻量，不在里面塞太多东西，优先保证**可用性**与**流畅性**。
2. 数据层（`core`）与界面层（`app`）分离，理论上挺好维护的（？
3. 依赖库少，启动快。

> **品鉴此项目代码前请注意：** 本项目某些部分可能存在复用、存在一些不那么常规的写法，若遇到问题欢迎提 Issue。

> [!IMPORTANT]
> 项目展望：本人为初中牲，项目将作为第三方 AI 客户端持续存在，可能不定期更新。

# 1.1 更新内容

这一版主要动了界面，功能是顺手补的。

### 界面重做

- **悬浮胶囊栏**：顶栏与输入栏不再各占一条，改成浮在内容之上的胶囊，消息可以滚到胶囊下面。高度固定 56dp，圆角拉到 28dp 以上也不会变形。
- **可调参数**：胶囊圆角 `8–28dp`、描边 `1dp`（深色模式下自动变浅）、投影 `18dp`、栏透明度 `35–95%`。
- **通栏模式**：关掉开关就回到普通顶栏 + 圆润描边输入框，功能不变。
- **气泡与卡片**统一用 `12dp` 小圆角，只有栏是胶囊，不会满屏大圆角。

### 三色印象派配色

- **7 套预设**：晨雾蓝 / 薰衣草暮 / 青瓷 / 麦浪 / 玫瑰灰 / 鼠尾草 / 石墨。
- 每套只有三个颜色：**工具栏 / 消息 / 输入区**，同一色相附近取色，饱和度压低，自带灰雾感。
- 支持**动态取色**（Android 12+ 跟随壁纸）、**自定义主色**（RGB 滑块 + 十六进制输入 + 明度滑块）。
- 深色模式是另一套明度更低的值，文字颜色按背景亮度自动选深或浅。

### 动效

- **页面切换**：淡入 220ms + 横向 16dp，退出 140ms，可中途打断。
- **消息气泡入场**：淡入 260ms + 上滑 12dp。
- **发送键**：发送/停止图标交叉淡入，进入带轻微回弹。
- **内容高度**：`animateContentSize`，流式输出时气泡平滑长高。
- 全部是弹簧或短 tween，新目标值从当前值接着插值，不等前一段播完。

### 多模态图片

- 输入栏左侧加图片按钮，可选**相册**或**拍照**（拍照走 FileProvider，不申请相机权限）。
- 原图超过 **4MB** 直接拒绝；通过后压到最长边 **1024px**、JPEG 85，再以 base64 拼进 `content` 数组。
- 模型级开关：只有勾了「多模态模型」的模型才会出现图片按钮，其它模型仍发纯文本请求。
- 气泡内上图下文；删除对话时会把对应图片文件一起删掉。

### 其它

- **Markdown**：新增**表格**（支持 `:--` / `--:` / `:-:` 三种对齐，列多可横向滚，单元格内可用行内格式）与**加粗斜体**（`***文本***`）。
- **界面缩放**：新增「自动适配屏幕」，按 `smallestScreenWidthDp` 计算（手表 284dp → 0.72，手机 360dp → 0.92），也可手动 `0.3× ~ 1.6×`。
- **标题命名**：标题模型可选「无」，选中后不再调用 AI，直接用「x月x日x时的对话」命名（取对话创建时间）。
- **背景图片**：自定义背景 + 蒙层浓度。
- **弹窗缩放**：Dialog 用独立窗口，页面级 `LocalDensity` 传不进去，改为在弹窗内部重新提供；小屏弹窗字段区可滚动、按钮固定底部。
- **键盘适配**：输入栏与消息列表吃同一份 IME inset，弹起时一起让位。
- **输入框**：长文本自动增高到 6 行，超过才内部滚动。

### 已知限制

- **毛玻璃**依赖系统的 `RenderEffect`，**Android 12 以下无效**，低于 12 时只能退化为纯半透明，设置里会直接标注「此设备不支持」。
- **液态玻璃**（折射 + 色散）试过 [Backdrop](https://github.com/Kyant0/AndroidLiquidGlass) 库，它要求 Kotlin 2.4 或 minSdk 23，与当前构建链冲突，暂时搁置。
- **沉浸式隐藏导航栏**已移除：Android 5 上系统会把隐藏标志清掉，反复重设也不稳，改为靠内边距避让。

# 功能特性

| 功能 | 说明 |
| --- | --- |
| 流式输出开关 | 开启后通过 **SSE** 逐字显示回复；关闭后一次性返回 |
| 深度思考开关 | 请求体注入 `thinking: {"type": "enabled"}`，展示 `reasoning_content` 推理过程 |
| LaTeX 公式渲染 | 输出与思考过程实时渲染 `$...$`、`$$...$$` 数学公式 ，不显示的原因可能为不支持部分算式或AI未使用可识别格式 |
| Markdown 排版 | 标题、加粗、斜体、加粗斜体、代码块（带语言标签与复制）、引用、列表、链接、**表格** |
| 多模态图片 | 相册 / 拍照上传，4MB 上限，自动压缩至 1024px，以 base64 发送 |
| 自定义模型管理 | 增删改模型，每个模型可单独配调用名、API Key、接入地址、是否多模态 |
| 标题自动生成 | 首次回答后由 AI 后台自动总结会话标题；也可选「无」用日期命名 |
| 本地会话存储 | 多会话本地持久化，重启后记忆仍在，可切换 / 删除 |
| 悬浮胶囊栏 | 顶栏与输入栏浮在内容之上，圆角 / 描边 / 投影 / 半透明可调 |
| 三色配色 | 7 套预设 + 动态取色 + 自定义 RGB / 十六进制 / 明度 |
| 背景图片 | 自定义背景图 + 蒙层浓度调节 |
| 深色模式 | 跟随系统或手动强制深色（莫奈动态取色，安卓12+） |
| 界面缩放 | 自动按屏幕宽度适配，或手动 `0.3× ~ 1.6×` |
| 高级参数 | `temperature`、`top_p`、`max_tokens` 可调 |

# 工程结构

```
WearAI/
├── core/    # 共享库：模型、网络（SSE/异步）、仓库、ViewModel
├── app/     # 应用 UI（Compose + Material 3 + 动态取色）
└── gradle/  # 版本目录 libs.versions.toml 与 Gradle Wrapper
```

关键文件：

```
app/src/main/java/com/foggland/wearai/
├── ui/theme/        配色（三色派生）、Shape、背景图、缩放
├── ui/components/   胶囊栏、Markdown、公式、图标、动画工具
├── ui/chat/         聊天页、气泡、图片导入
├── ui/settings/     设置、模型管理
└── ui/conversations/对话记录

core/src/main/java/com/foggland/wearai/core/
├── model/           AppSettings / Conversation / ChatMessage / ModelConfig
├── repository/      SettingsRepository / ChatRepository / ConversationStore
├── network/         ZhipuClient（SSE）、TlsCompat（低版本 CA 兜底）
└── vm/              ChatViewModel
```

# 构建运行

1. 使用 **Android Studio** 打开项目根目录（`WearAI/`）。
2. 确认 `local.properties` 里 `sdk.dir` 指向本机 SDK（或由 IDE 自动生成）。
3. 运行 `app` 模块到手机 / 模拟器。
4. 命令行构建：

```bash
# Windows
gradlew.bat :app:assembleDebug
```

> 首次构建会从 Google Maven / Maven Central 拉依赖，需联网。

---

# 致谢名单

- Deepseek V4.1-flash (?
- [ru.noties/jlatexmath-android](https://github.com/noties/jlatexmath-android)：JLaTeXMath 的 Android 版，用于渲染数学公式。
- Jetpack Compose / Material 3 / OkHttp / Gson / Kotlinx Coroutines 等开源库。

# 开源许可

本项目基于 **GNU General Public License v3.0（GPL-3.0）** 开源。你可以自由使用、修改、复制、分发，但部分基于本项目的衍生作品也**必须**以相同的 GPL-3.0 协议开源。完整文本见 [LICENSE](LICENSE)，或访问 https://www.gnu.org/licenses/gpl-3.0.txt 

### 免责声明

> 本项目仅用于学习与技术交流，不提供任何形式的担保或商用保证。使用各模型开放平台 API 时，请遵守对应平台的服务条款及法律法规；**API Key 请妥善保管，切勿提交到公开仓库**。项目内内置的默认 API Key 仅供测试，正式使用请换成你自己的，~~本人敢把key放代码里的底气就是余额为0~~
