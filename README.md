<div align="center">

# WearAI

轻量、优雅的 **智谱大模型（BigModel）Android 聊天客户端**

</div>

# 介绍

**WearAI** 是一个基于**智谱 AI 大模型开放平台（BigModel）** 的原生 Android 聊天客户端，使用 **Kotlin + Jetpack Compose** 开发，可在普通安卓设备（手机 / 平板）上流畅运行，遵循 **Material Design 3** 与 **莫奈取色（Monet 动态取色）** 设计标准。

- 项目名：`WearAI`
- 包名：`com.foggland.wearai`
- 最低兼容：**Android 6.0（API 23）**
- 默认模型（调用名）：**`glm-4.7-flash`**
- 开源协议：**GNU General Public License v3.0（GPL-3.0）**

本项目借鉴了**智谱官方 API 文档**及若干开源库（详见「开源致谢」），除此之外与其他第三方 AI 聊天软件无任何关系。

> [!IMPORTANT]
> 项目展望：本项目将持续迭代，作为开源的学习与交流项目存在，欢迎提交 Issue 与 PR。

### 特性一览

1. 尽量保证软件的轻量与流畅，优先保证**可用性**与**稳定性**，依赖库少、启动快。
2. 架构清晰，数据层（`core`）与界面层（`app`）分离，便于扩展与维护。
3. 完整覆盖智谱 API 的核心调用方式（对话补全、流式输出、深度思考、异步图像/视频生成等）。

> **品鉴此项目代码前请注意：** 本项目某些部分可能存在复用、以及一些不那么常规的写法，若遇到问题欢迎提 Issue 反馈。

---

## 一、功能特性

| 功能 | 说明 |
| --- | --- |
| 流式输出开关 | 开启后通过 **SSE** 逐字显示回复；关闭后一次性返回完整内容 |
| 深度思考开关 | 请求体注入 `thinking: {"type": "enabled"}`，界面展示模型 `reasoning_content` 推理过程 |
| LaTeX 公式渲染 | AI 输出（含思考过程）支持实时渲染行内 `$...$` 与块级 `$$...$$` 数学公式 |
| Markdown 排版 | 支持标题、加粗、斜体、代码块（含语言标签与一键复制）、引用、列表、链接 |
| 自定义模型管理 | 可新增 / 编辑 / 删除模型，每个模型可单独配置调用名、API Key 与接入地址 |
| 标题自动生成 | 首次回答后由 AI 在后台为会话自动总结标题 |
| 本地会话存储 | 多会话本地持久化，重启后长期保留对话记忆，可随时切换/删除 |
| 深色模式 | 支持跟随系统或手动强制深色（莫奈动态取色，Android 12+） |
| 全局面板缩放 | 界面尺寸可全局缩放（`0.3× ~ 1.6×`），适配小屏设备 |
| 高级参数 | 可调 `temperature`、`top_p`、`max_tokens` |
| 接口配置 | 自定义系统提示词（API Key 与接入地址已并入模型管理） |

## 二、工程结构

```
WearAI/
├── core/    # 共享库：模型、网络（SSE/异步）、仓库、ViewModel
├── app/     # 应用 UI（Compose + Material 3 + 动态取色）
├── docs/    # 智谱 API 文档
└── gradle/  # 版本目录 libs.versions.toml 与 Gradle Wrapper
```

## 三、构建运行

1. 使用 **Android Studio** 打开项目根目录（`WearAI/`）。
2. 确认 `local.properties` 中 `sdk.dir` 指向本机 Android SDK（或由 IDE 自动生成）。
3. 运行 `app` 模块到手机 / 模拟器。
4. 命令行构建：

```bash
# Windows
gradlew.bat :app:assembleDebug
```

> 首次运行会从 Google Maven / Maven Central 拉取依赖，需要联网。

---

## 四、智谱大模型 API 调用方法与核心参数

详细文档见 [docs/API.md](docs/API.md)。四类核心接口：

1. **对话补全**（本项目使用）：`POST https://open.bigmodel.cn/api/paas/v4/chat/completions`
2. **图像生成（异步）**：`POST https://open.bigmodel.cn/api/paas/v4/images/generations`
3. **视频生成（异步）**：`POST https://open.bigmodel.cn/api/paas/v4/videos/generations`
4. **查询异步结果**：`GET https://open.bigmodel.cn/api/paas/v4/async-result/{id}`

代码中四类接口均已实现（`core/.../network/ZhipuClient.kt`），聊天界面使用对话补全接口，其余接口供扩展。

## 五、默认配置

- API Key：`74a49e6b944a456eaf089f6ea8593302.yhD4E7YQKnRbRp6L`
- 接入地址：`https://open.bigmodel.cn/api/paas/v4/chat/completions`
- 默认模型：`glm-4.7-flash`

> 以上默认值在「设置 → 模型管理」中可修改，修改后即时生效并本地保存。

---

## 六、开源致谢

- 智谱 AI 开放平台「[对话补全](https://docs.bigmodel.cn/api-reference/%E6%A8%A1%E5%9E%8B-api/%E5%AF%B9%E8%AF%9D%E8%A1%A5%E5%85%A8)、[核心参数](https://docs.bigmodel.cn/cn/guide/start/concept-param)、[深度思考](https://docs.bigmodel.cn/cn/guide/capabilities/thinking)、[流式消息](https://docs.bigmodel.cn/cn/guide/capabilities/streaming)、异步图像/视频生成」等公开文档，本项目据此实现 API 接入。
- [ru.noties/jlatexmath-android](https://github.com/noties/jlatexmath-android)：JLaTeXMath 的 Android 版本，用于渲染 LaTeX 数学公式。
- Jetpack Compose / Material 3 / OkHttp / Gson / Kotlinx Coroutines 等开源库。

## 七、开源许可

本项目基于 **GNU General Public License v3.0（GPL-3.0）** 开源。

你可以自由地使用、修改、复制、分发本软件，但任何基于本软件的衍生作品也**必须**以相同的 GPL-3.0 协议开源。完整许可文本见 [LICENSE](LICENSE) 文件，或访问 https://www.gnu.org/licenses/gpl-3.0.txt 。

### 免责声明

> 本项目仅用于学习与技术交流，不提供任何形式的商业用途担保。使用智谱 AI 开放平台 API 时，请遵守[智谱开放平台服务条款](https://open.bigmodel.cn)及当地法律法规；API Key 请妥善保管，**切勿提交到公开仓库**。项目中内置的默认 API Key 仅供演示，正式使用请替换为你自己的 Key。
