# 智谱大模型（BigModel）API 调用方法与核心参数

> 参考官方文档：https://docs.bigmodel.cn
> 基础地址（Base）：`https://open.bigmodel.cn/api/paas/v4`

## 1. 鉴权

所有接口使用 **Bearer Token** 鉴权：

```
Authorization: Bearer <你的 API Key>
Content-Type: application/json
```

API Key 形如 `{id}.{secret}`（本项目的默认 Key 已内置在设置中）。

---

## 2. 对话补全（本项目核心接口）

**接口地址**：`POST https://open.bigmodel.cn/api/paas/v4/chat/completions`

### 2.1 请求体（核心参数）

| 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `model` | string | ✅ | - | 模型**调用名**，如 `glm-4.7-flash` |
| `messages` | array | ✅ | - | 对话消息数组，`role` ∈ `system`/`user`/`assistant`/`tool` |
| `stream` | boolean | ❌ | `false` | 是否流式输出 |
| `temperature` | number | ❌ | `0.95` | 采样温度，范围 `0~2`，越大越随机 |
| `top_p` | number | ❌ | `0.7` | 核采样，范围 `0~1` |
| `max_tokens` | integer | ❌ | - | 最大输出 token 数 |
| `stop` | array | ❌ | - | 停止序列 |
| `thinking` | object | ❌ | - | 深度思考开关：`{"type": "enabled"}` 或 `{"type": "disabled"}`，部分模型支持 `budget_tokens` 控制思考预算 |
| `tools` / `tool_choice` | array / string | ❌ | - | 函数调用（工具调用）配置 |
| `do_sample` | boolean | ❌ | - | 是否采样 |
| `presence_penalty` / `frequency_penalty` | number | ❌ | - | 重复惩罚 |
| `seed` | integer | ❌ | - | 随机种子（固定输出） |
| `response_format` | object | ❌ | - | 输出格式（如 `{"type":"json_object"}`） |

示例：

```json
{
  "model": "glm-4.7-flash",
  "messages": [
    {"role": "system", "content": "你是 AI 助手"},
    {"role": "user", "content": "你好"}
  ],
  "stream": true,
  "temperature": 0.95,
  "top_p": 0.7,
  "max_tokens": 4096,
  "thinking": {"type": "enabled"}
}
```

### 2.2 非流式响应

```json
{
  "id": "chatcmpl-xxx",
  "object": "chat.completion",
  "created": 1710000000,
  "model": "glm-4.7-flash",
  "choices": [
    {
      "index": 0,
      "finish_reason": "stop",
      "message": {
        "role": "assistant",
        "content": "你好！",
        "reasoning_content": "（开启深度思考后返回的推理过程）"
      }
    }
  ],
  "usage": {
    "prompt_tokens": 12,
    "completion_tokens": 8,
    "total_tokens": 20
  }
}
```

### 2.3 流式响应（SSE）

服务端以 `text/event-stream` 返回，每行一个 `data:` 事件，结束时发送 `data: [DONE]`：

```
data: {"id":"...","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"role":"assistant","content":"你"},"finish_reason":null}]}

data: {"choices":[{"index":0,"delta":{"content":"好"},"finish_reason":null}]}

data: {"choices":[{"index":0,"delta":{},"finish_reason":"stop"}],"usage":{"prompt_tokens":12,"completion_tokens":8,"total_tokens":20}}

data: [DONE]
```

开启深度思考时，`delta` 中会先出现 `reasoning_content` 增量，随后才是 `content` 增量。

### 2.4 深度思考（Deep Thinking）

- 开启：请求体加 `"thinking": {"type": "enabled"}`。
- 模型返回的思考内容位于 `message.reasoning_content`（非流式）或 `delta.reasoning_content`（流式）。
- 为了保持多轮上下文连续，应将上一轮 assistant 的 `reasoning_content` 回传。

---

## 3. 图像生成（异步）

**接口地址**：`POST https://open.bigmodel.cn/api/paas/v4/images/generations`

常用模型：`cogview-4`、`cogview-3-flash`、`cogview-3-plus`。

```json
{ "model": "cogview-4", "prompt": "一只戴着帽子的猫", "size": "1024x1024" }
```

返回任务 ID（异步处理）：

```json
{ "id": "task-xxx", "model": "cogview-4", "task_status": "PROCESSING" }
```

---

## 4. 视频生成（异步）

**接口地址**：`POST https://open.bigmodel.cn/api/paas/v4/videos/generations`

常用模型：`cogvideox-2`、`cogvideox-flash`、`cogvideox-2-turbo`。

```json
{
  "model": "cogvideox-2",
  "prompt": "下雨的街道，霓虹灯",
  "image_url": "可选首帧图",
  "duration": 5,
  "with_audio": true
}
```

返回任务 ID：

```json
{ "id": "task-xxx", "model": "cogvideox-2", "task_status": "PROCESSING" }
```

---

## 5. 查询异步结果

**接口地址**：`GET https://open.bigmodel.cn/api/paas/v4/async-result/{id}`

```json
{
  "id": "task-xxx",
  "task_status": "SUCCESS",
  "data": [{"url": "https://.../result.png"}],
  "video_result": [{"url": "https://.../result.mp4", "cover_image_url": "https://.../cover.png"}]
}
```

`task_status` 取值：`PROCESSING`（处理中）、`SUCCESS`（成功）、`FAIL`（失败）。

---

## 6. 本项目的实现位置

- 对话补全 + SSE 解析：`core/src/main/java/com/foggland/wearai/core/network/ZhipuClient.kt`
- 图像/视频/异步结果：同上文件中的 `createImage` / `createVideo` / `queryAsyncResult`
- 请求/响应 DTO：`core/src/main/java/com/foggland/wearai/core/network/dto/`
- 参数组装（含 thinking / stream / temperature 等）：`core/.../repository/ChatRepository.kt`
