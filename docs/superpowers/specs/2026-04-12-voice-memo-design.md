# VoiceMemo - Android 语音备忘录设计文档

## 概述

一款 Android 语音备忘录应用，通过音量上+下同时短按触发录音，利用语音转文字和大模型理解用户意图（添加/查询/修改/删除备忘录），并按需自动获取屏幕截图和 GPS 定位信息。同时支持手动添加/编辑备忘录。

## 核心理念

- **一步直达**：音量键触发，无需解锁、打开 App、点击按钮
- **AI 驱动**：语音→文字→意图理解→自动执行，全程无需手动操作
- **上下文感知**：自动获取截图和定位，用户说"记住这个"时自动关联
- **国内可用**：不依赖 Google 服务，所有组件在国内环境可用

## 1. 整体架构

```
┌─────────────────────────────────────────────────┐
│                  Android App                     │
├──────────────┬──────────────┬───────────────────┤
│ Accessibility│  Foreground  │     Main App      │
│   Service    │   Service    │   (UI 层)         │
│              │              │                   │
│ - 按键监听   │ - 录音控制   │ - 备忘录列表      │
│ - 屏幕截图   │ - ASR 调用   │ - 详情/编辑页     │
│              │ - LLM 调用   │ - 搜索页          │
│              │ - 意图路由   │ - 设置页          │
│              │ - 通知展示   │                   │
├──────────────┴──────────────┴───────────────────┤
│              Domain Layer (用例)                  │
│  AddMemoUseCase / QueryMemoUseCase / ...         │
├─────────────────────────────────────────────────┤
│              Data Layer                          │
│  Room DB (本地) / Repository (预留云端接口)       │
│  ASR Engine (云端优先→本地回退)                   │
│  LLM Client (云端优先→本地回退)                   │
│  LocationProvider / ScreenCaptureProvider         │
└─────────────────────────────────────────────────┘
```

### 模块职责

| 模块 | 职责 | 关键点 |
|------|------|--------|
| AccessibilityService | 监听音量上+下同时短按；按需截屏 | 只做两件事，保持轻量 |
| ForegroundService | 录音→ASR→LLM→意图路由→通知 | 核心处理链，前台保活 |
| Main App (UI) | 备忘录 CRUD、搜索、设置 | 通知点击跳转入口 |
| Domain Layer | 用例封装业务逻辑 | Add/Query/Update/Delete |
| Data Layer | Room + Repository 模式 | Repository 接口预留云端实现 |

### 触发流程

1. AccessibilityService 检测到音量上+下同时短按
2. 通过绑定/广播通知 ForegroundService
3. ForegroundService 启动录音（首次按下）或结束录音并处理（第二次按下）
4. 处理完成后以通知展示结果，通知可跳转到对应 App 页面

## 2. 核心处理链

```
音量键(第二次按下)
     ↓
┌─ ForegroundService ─────────────────────────────┐
│                                                  │
│  ① 录音文件 ──→ ② ASR 引擎 ──→ ③ 原始文本        │
│                                   ↓              │
│                          ④ LLM 意图识别           │
│                          ┌───┴───┐               │
│                     需要上下文?   不需要            │
│                          ↓        ↓              │
│                  ⑤ 获取截图/GPS   ⑥ 直接执行      │
│                          ↓        ↓              │
│                  ⑦ 带上下文再调LLM ──┘             │
│                                   ↓              │
│                          ⑧ 执行意图               │
│                     ┌────┬────┬────┐             │
│                   添加  查询 修改 删除             │
│                     ↓    ↓    ↓    ↓             │
│                   ⑨ 通知展示结果                   │
│                   (可跳转对应页面)                  │
└──────────────────────────────────────────────────┘
```

### 录音

- 格式：PCM/WAV，采样率 16kHz
- 存储：app 私有缓存目录，处理完即删
- 最大时长：60 秒，超时自动结束并处理

### ASR 引擎（混合方案）

- **在线优先**：科大讯飞/百度云端 API（用户可在设置中选择）
- **离线回退**：Whisper tiny 本地模型（ONNX 格式，不依赖 Google 服务）

### LLM 意图识别

返回结构化 JSON：

```json
{
  "intent": "add | query | update | delete",
  "needContext": {
    "screenshot": true,
    "location": true
  },
  "content": "这是海天公园的停车场二维码",
  "tags": ["停车", "二维码"],
  "reminderTime": "2024-04-13T15:00:00 | null",
  "queryKeywords": ["停车", "二维码"]
}
```

- **needContext** 判断是否需要获取截图/定位（如用户说"记住这个"→ screenshot=true）
- 在线优先调用 LLM API，离线时回退简单规则匹配（仅支持明确指令如"删除最后一条"/"查看今天的备忘录"等，通过关键词+正则匹配意图，不解析复杂语义）

### 上下文获取

- **截图**：AccessibilityService 的 `getRootInActiveWindow()` 截取当前屏幕
- **定位**：FusedLocationProviderClient 获取最近一次 GPS 位置
- 获取后作为附加信息再传给 LLM 做最终处理
- **二维码**：不解析，仅记录截图和位置

### 意图执行

- **添加**：存入 Room DB → 通知"已添加：XXX" → 点击跳转详情页
- **查询**：关键词搜索 Room DB → 通知展示匹配结果摘要 → 点击跳转搜索结果页
- **修改**：匹配目标备忘录 → 更新 → 通知确认
- **删除**：匹配目标备忘录 → 删除 → 通知确认

## 3. 数据模型

### Room 数据库表

```
┌──────────────────────────────────┐
│            memo 表                │
├──────────────────────────────────┤
│ id: Long (主键, 自增)             │
│ content: String (LLM整理后内容)   │
│ rawText: String (ASR原始文本)     │
│ tags: String (JSON数组)          │
│ imagePaths: String (JSON数组)    │
│   - 每项含 path + source         │
│   - source: voice_screenshot /   │
│     camera / gallery             │
│ latitude: Double?                │
│ longitude: Double?               │
│ address: String? (逆地理编码)    │
│ reminderTime: Long? (时间戳)     │
│ createdAt: Long (创建时间)       │
│ updatedAt: Long (更新时间)       │
│ isDeleted: Boolean (软删除)      │
└──────────────────────────────────┘
```

### Repository 接口

```kotlin
interface MemoRepository {
    // 本地实现: LocalMemoRepository (Room)
    // 云端实现: CloudMemoRepository (未来扩展)
    suspend fun addMemo(memo: Memo): Long
    suspend fun queryMemos(keywords: List<String>): List<Memo>
    suspend fun updateMemo(memo: Memo)
    suspend fun deleteMemo(id: Long)
    suspend fun getMemoById(id: Long): Memo?
}
```

## 4. 服务间通信

### 通信方式

AccessibilityService 通过 Binder 绑定 ForegroundService，通过方法调用传递指令，截图通过回调返回。

### ForegroundService 状态机

```
IDLE ──(音量键触发)──→ RECORDING ──(再次音量键/超时)──→ PROCESSING
  ↑                                                       │
  │                                           ┌── 失败 ───┤
  │                                           ↓           │
  └─────────────── NOTIFYING ←── 成功 ────────┘           │
                                     ↑                     │
                                     └── ERROR ────────────┘
```

- **IDLE**：空闲，等待触发
- **RECORDING**：录音中，通知栏显示"正在录音..."（带取消按钮）
- **PROCESSING**：ASR → LLM → 执行意图
- **NOTIFYING**：展示结果通知
- **ERROR**：处理失败，通知提示失败原因，回到 IDLE

## 5. 通知设计

### 通知渠道

| 渠道 | 重要级别 | 用途 |
|------|---------|------|
| channel_recording | IMPORTANCE_LOW | 录音状态 |
| channel_result | IMPORTANCE_HIGH | 结果通知，有声提示 |
| channel_error | IMPORTANCE_DEFAULT | 错误提示 |

### 通知样式

| 场景 | 内容 | 点击行为 |
|------|------|---------|
| 录音中 | "正在录音..." + 取消按钮 | 取消录音 |
| 添加成功 | "已添加：XXX" + 缩略图 | 跳转详情页 |
| 查询结果 | "找到 N 条：1.XXX 2.XXX" | 跳转搜索结果页 |
| 修改成功 | "已修改：XXX" | 跳转详情页 |
| 删除成功 | "已删除：XXX" + 撤销按钮(5秒) | 撤销则恢复 |
| 处理失败 | "处理失败：原因" | 跳转设置页 |

## 6. UI 界面

### 页面结构

```
MainActivity (单 Activity)
├── MemoListFragment      — 备忘录列表（首页）
├── MemoDetailFragment    — 备忘录详情
├── MemoEditFragment      — 添加/编辑备忘录
├── SearchFragment        — 搜索结果页
└── SettingsFragment      — 设置页
```

### MemoListFragment（首页）

- 顶部搜索栏（点击进入搜索页）
- 卡片式列表，每张卡片显示：内容摘要、标签 chips、位置图标+地址、缩略图、创建时间
- 左滑删除、长按多选
- 右下角 FAB 按钮（手动添加）
- 空状态提示："同时短按音量键开始记录"
- 无障碍服务未开启时顶部常驻提示条

### MemoDetailFragment（详情页）

- 完整文本内容
- 图片大图展示（点击全屏查看）
- 地图区块（位置标记）+ "导航到这里"按钮 → 高德地图（`amapuri://` scheme），未安装高德回退系统地图
- 标签列表
- 提醒时间
- 原始语音文本（折叠显示）
- 右上角编辑/删除菜单

### MemoEditFragment（添加/编辑页）

复用同一页面，参数区分新建/编辑模式：

- **文本输入**：多行编辑框
- **定位区块**：
  - 显示当前地址
  - "重新定位"按钮 → GPS 获取最新位置
  - "选择位置"按钮 → 高德地图 SDK 选点
  - "清除位置"按钮
- **图片区块**：
  - 缩略图网格（可删除单张）
  - "拍照"按钮 → 系统相机
  - "从相册选择"按钮 → 系统图片选择器
- **标签**：语音添加时 LLM 自动生成，手动添加时可输入
- **提醒时间**：可选，日期时间选择器
- 右上角保存按钮

### SearchFragment

- 搜索框 + 结果列表（复用 MemoList 卡片样式）
- 支持按标签筛选

### SettingsFragment

- ASR 引擎选择（科大讯飞/百度/离线 Whisper）
- LLM 配置（API 地址、API Key、模型选择）
- 录音时长上限
- 数据导出
- 关于

### 导航关系

```
首页列表 ──点击──→ 详情页 ──编辑──→ 编辑页
  │                                    ↑
  ├── + FAB ─────────────────────→ 新建页
  │
  ├──搜索──→ 搜索页 ──点击──→ 详情页
  │
  └──设置──→ 设置页
```

### 通知跳转映射

```
添加成功通知 → MemoDetailFragment(memoId=xxx)
查询结果通知 → SearchFragment(keywords=[xxx])
删除成功通知 → 无跳转（有撤销按钮）
```

## 7. 权限

| 权限 | 用途 |
|------|------|
| RECORD_AUDIO | 录音 |
| ACCESS_FINE_LOCATION | GPS 定位 |
| ACCESS_COARSE_LOCATION | 网络定位（回退） |
| INTERNET | 云端 ASR/LLM |
| FOREGROUND_SERVICE | 前台服务 |
| FOREGROUND_SERVICE_MEDIA_PLAYBACK / RECORD_AUDIO | Android 14+ 前台服务类型 |
| POST_NOTIFICATIONS | 通知 (Android 13+) |
| BIND_ACCESSIBILITY_SERVICE | 无障碍服务 |
| CAMERA | 拍照 |
| READ_MEDIA_IMAGES / READ_EXTERNAL_STORAGE | 相册选择 |

## 8. 技术选型

| 能力 | 选型 | 理由 |
|------|------|------|
| 语言 | Kotlin | 用户选择 |
| UI | Jetpack Compose | 现代 Android UI |
| 导航 | Navigation Compose | 单 Activity 架构 |
| 存储 | Room | 本地 SQLite ORM |
| 依赖注入 | Hilt | Jetpack 官方推荐 |
| 异步 | Kotlin Coroutines + Flow | 全链路异步 |
| 网络 | Retrofit + OkHttp | API 调用 |
| 图片加载 | Coil | Compose 友好 |
| 图片存储 | App 私有目录 + 压缩 | 无需存储权限 |
| ASR-在线 | 科大讯飞/百度 SDK | 国内可用 |
| ASR-离线 | Whisper tiny (ONNX) | 不依赖 Google |
| LLM-在线 | OpenAI 兼容 API | 用户自选模型 |
| LLM-离线 | 规则匹配 | 简单意图离线可用 |
| 地图 | 高德地图 SDK | 导航 + 选点 |
| 截图 | AccessibilityService API | 系统原生支持 |

## 9. 错误处理

| 场景 | 处理方式 |
|------|---------|
| 无网络 | ASR 离线回退，LLM 离线回退（规则匹配），通知"离线模式，功能受限" |
| ASR 识别失败 | 通知"语音识别失败，请重试"，不创建备忘录 |
| LLM 调用超时(10s) | 原始文本直接存为备忘录，标签留空，通知"AI 处理超时，已保存原文" |
| GPS 不可用 | 跳过定位，不附带位置 |
| 截图失败 | 跳过截图，不附带截图 |
| 录音超时(60s) | 自动结束并处理，通知"录音已自动结束" |
| 无障碍服务未开启 | 首页顶部常驻提示条，引导跳转系统设置 |
