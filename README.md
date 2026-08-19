# WearAI

基于**智谱 AI 大模型开放平台（BigModel）** 的原生 Android 聊天客户端，采用 **Kotlin + Jetpack Compose**，可在普通安卓设备（手机 / 平板）上运行，遵循 **Material Design 3** 与 **莫奈取色（Monet 动态取色）** 设计标准。

- 项目名：`WearAI`
- 包名：`com.foggland.wearai`
- 最低兼容：**Android 8.1（API 27）**
- 默认模型（调用名）：**`glm-4.7-flash`**

---

## 一、功能特性

| 功能 | 说明 |
| --- | --- |
| 流式输出开关 | 开启后通过 **SSE** 逐字显示回复；关闭后一次性返回完整内容 |
| 深度思考开关 | 请求体注入 `thinking: {"type": "enabled"}`，界面展示模型 `reasoning_content` 推理过程 |
| 自定义模型管理 | 预置多款模型，支持新增 / 编辑 / 删除，自由切换当前模型 |
| 高级参数 | 可调 `temperature`、`top_p`、`max_tokens` |
| 接口配置 | 自定义 API Key、接入地址、系统提示词 |
| 对话管理 | 本地持久化历史、新对话、停止生成、Token 用量统计 |

## 二、工程结构

```
WearAI/
├── core/    # 共享库：模型、网络（SSE/异步）、仓库、ViewModel
├── app/     # 应用 UI（Compose + Material 3 + 动态取色）
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

> 以上默认值在「设置 → 接口」中可修改，修改后即时生效并本地保存。
