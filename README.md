<div align="center">

# WearAI

轻量的第三方 AI 聊天客户端

</div>


# 介绍
这是一个**轻量**的 **AI 聊天客户端**，默认使用**智谱大模型**（可以不换，也可以换）。使用 **Kotlin + Jetpack Compose** 开发，遵循 **Material Design 3** 与 **莫奈取色（Monet 动态取色）** 设计标准，最低支持 **安卓6.0**。

- 项目名：`WearAI`
- 包名：`com.foggland.wearai`
- 最低兼容：**Android 6.0（API 23）**
- 默认模型（调用名）：**`glm-4.7-flash`**
- 开源协议：**GNU General Public License v3.0（GPL-3.0）**

本项目借鉴了**智谱官方 API 文档**与若干开源库（详见「开源致谢」）。除此之外与任何第三方 AI 聊天软件**无任何关系**。

1. 我们会尽量保证软件的轻量，不在里面塞太多东西，优先保证**可用性**与**流畅性**（但 LaTeX 公式渲染这种功能还是塞进去了，没办法）。
2. ~~我们尽量把代码写得好看~~ 现在的代码已经是一坨相当壮观的屎山了（在逐步清理）。
3. ~~项目的 API 解析逻辑十分甚至九分清晰~~（指直接一层一层拆 JSON）。
4. 依赖库少，~~可以快速嫁接到其他工程里~~（大嘘）。

>**品鉴此项目代码前请注意：此工程的某些部分存在复用、存在一些奇怪的写法，以及可能存在暗病和屎山！**
>
>#### 本项目可能包含：
>
> ~~神秘逻辑：~~
> ```kotlin
> // if (data.aaa == 1 ? true : false)
> // if (data.bbb == 1 ? true : false)
> // if (data.ccc == 1 ? true : false)
> ```
>
> ~~层层拆包：~~
> ```kotlin
> // if (all.has("xxx")) {
> //     val data = all.getJSONObject("xxx")
> //     if (data.has("xxx")) { ... }
> // }
> ```
>
> ~~以上问题正在逐渐改善，大概（QAQ）~~
>
> **此项目已用 Gson + 数据类重构了大部分解析逻辑，暗病正在减少——大概（**

> [!IMPORTANT]
> 项目展望：本项目将作为一个第三方 AI 客户端一直存在，可能不定期更新（？）

## 选型

> 既然只是个「选默认模型」的客户端，为啥默认智谱？
> - 智谱开放平台文档齐全、接口实现了四种调用方式（对话补全 / 流式 / 深度思考 / 异步图像视频）。
> - 其实**换成其它模型的 OpenAI 兼容接口**也不难，模型管理里新增一个模型、填上调用名和 Key 就行。

# 功能特性

| 功能 | 说明 |
| --- | --- |
| 流式输出开关 | 开启后通过 **SSE** 逐字显示回复；关闭后一次性吐完 |
| 深度思考开关 | 请求体注入 `thinking: {"type": "enabled"}`，展示模型的 `reasoning_content` 推理过程 |
| LaTeX 公式渲染 | 输出与思考过程实时渲染 `$...$`、`$$...$$` 数学公式 |
| Markdown 排版 | 标题、加粗、斜体、代码块（带语言标签与复制）、引用、列表、链接 |
| 自定义模型管理 | 增删改模型，每个模型可单独配调用名、API Key、接入地址 |
| 标题自动生成 | 首次回答后由 AI 后台自动总结会话标题 |
| 本地会话存储 | 多会话本地持久化，重启后记忆仍在，可切换 / 删除 |
| 深色模式 | 跟随系统或手动强制深色（莫奈动态取色，安卓12+） |
| 界面缩放 | 全局 `0.3× ~ 1.6×` 缩放，适配小屏设备 |
| 高级参数 | `temperature`、`top_p`、`max_tokens` 随便调 |

# 工程结构

```
WearAI/
├── core/    # 共享库：模型、网络（SSE/异步）、仓库、ViewModel
├── app/     # 应用 UI（Compose + Material 3 + 动态取色）
├── docs/    # 智谱 API 文档
└── gradle/  # 版本目录 libs.versions.toml 与 Gradle Wrapper
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

## 智谱 API 调用方法与核心参数

详细文档见 [docs/API.md](docs/API.md)。四类核心接口：

1. **对话补全**（本项目使用）：`POST https://open.bigmodel.cn/api/paas/v4/chat/completions`
2. **图像生成（异步）**：`POST https://open.bigmodel.cn/api/paas/v4/images/generations`
3. **视频生成（异步）**：`POST https://open.bigmodel.cn/api/paas/v4/videos/generations`
4. **查询异步结果**：`GET https://open.bigmodel.cn/api/paas/v4/async-result/{id}`

四类接口都实现了（`core/.../network/ZhipuClient.kt`），聊天界面用对话补全，其余供扩展。

## 默认配置

- 默认模型：`glm-4.7-flash`
- 默认接入地址：`https://open.bigmodel.cn/api/paas/v4/chat/completions`
- 内置演示 API Key：`74a49e6b944a456eaf089f6ea8593302.yhD4E7YQKnRbRp6L`

> 以上在「设置 → 模型管理」里都能改，改完即时生效、本地保存。

---

# 开源致谢

- **智谱 AI 开放平台**公开文档（[对话补全](https://docs.bigmodel.cn/api-reference/%E6%A8%A1%E5%9E%8B-api/%E5%AF%B9%E8%AF%9D%E8%A1%A5%E5%85%A8)、[核心参数](https://docs.bigmodel.cn/cn/guide/start/concept-param)、[深度思考](https://docs.bigmodel.cn/cn/guide/capabilities/thinking)、[流式](https://docs.bigmodel.cn/cn/guide/capabilities/streaming)、异步图像/视频生成），本项目据此实现 API 接入。
- [ru.noties/jlatexmath-android](https://github.com/noties/jlatexmath-android)：JLaTeXMath 的 Android 版，用于渲染数学公式。
- Jetpack Compose / Material 3 / OkHttp / Gson / Kotlinx Coroutines 等开源库。

# 开源许可

本项目基于 **GNU General Public License v3.0（GPL-3.0）** 开源。你可以自由使用、修改、复制、分发，但部分基于本项目的衍生作品也**必须**以相同的 GPL-3.0 协议开源。完整文本见 [LICENSE](LICENSE)，或访问 https://www.gnu.org/licenses/gpl-3.0.txt 。

### 免责声明

> 本项目仅用于学习与技术交流，不提供任何形式的担保或商用保证。使用各模型开放平台 API 时，请遵守对应平台的服务条款及当地法律法规；**API Key 请妥善保管，切勿提交到公开仓库**。项目内内置的默认 API Key 仅供演示，正式使用请换成你自己的。
