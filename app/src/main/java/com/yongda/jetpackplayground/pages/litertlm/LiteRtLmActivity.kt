package com.yongda.jetpackplayground.pages.litertlm

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.InputData
import com.google.ai.edge.litertlm.ResponseCallback
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.Session
import com.google.ai.edge.litertlm.SessionConfig
import com.yongda.jetpackplayground.ui.theme.JetpackPlaygroundTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

// ─────────────────────────────────────────────────────────
//  Activity
// ─────────────────────────────────────────────────────────

/**
 * LiteRT-LM 推理示例页面。
 * 可输入设备上的模型路径进行推理交互。
 */
class LiteRtLmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JetpackPlaygroundTheme {
                val viewModel: LiteRtLmViewModel = viewModel()
                LiteRtLmScreen(viewModel = viewModel)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
//  ViewModel
// ─────────────────────────────────────────────────────────

/** 聊天消息数据类，语义更清晰地替换 Pair<String, String> */
data class ChatMessage(val question: String, val answer: String)

class LiteRtLmViewModel : ViewModel() {

    /** 页面 UI 状态 */
    sealed class UiState {
        data object Idle : UiState()
        data object LoadingModel : UiState()
        data object Ready : UiState()
        data object Generating : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatHistory: StateFlow<List<ChatMessage>> = _chatHistory.asStateFlow()

    private val _currentPrompt = MutableStateFlow("")
    val currentPrompt: StateFlow<String> = _currentPrompt.asStateFlow()

    private val _currentResponse = MutableStateFlow("")
    val currentResponse: StateFlow<String> = _currentResponse.asStateFlow()

    private var engine: Engine? = null
    private var session: Session? = null

    // ── 公开操作 ──────────────────────────────────────────

    fun loadModel(context: Context, modelPath: String, useGpu: Boolean = true) {
        viewModelScope.launch {
            _uiState.value = UiState.LoadingModel
            resetConversation()
            try {
                withContext(Dispatchers.IO) {
                    val resolvedPath = resolveModelPath(context, modelPath)
                    initEngine(resolvedPath, useGpu)
                }
                _uiState.value = UiState.Ready
            } catch (e: Exception) {
                Log.e(TAG, "Init failed", e)
                _uiState.value = UiState.Error(e.message ?: "初始化模型失败")
            }
        }
    }

    fun generateText(prompt: String) {
        if (session == null || _uiState.value == UiState.Generating) return

        viewModelScope.launch {
            _uiState.value = UiState.Generating
            _currentPrompt.value = prompt
            _currentResponse.value = ""
            try {
                withContext(Dispatchers.IO) {
                    val formattedPrompt = buildChatPrompt(prompt)
                    session?.generateContentStream(
                        listOf(InputData.Text(formattedPrompt)),
                        StreamResponseCallback()
                    )
                }
                _chatHistory.value += ChatMessage(prompt, _currentResponse.value)
                _currentPrompt.value = ""
                _currentResponse.value = ""
                _uiState.value = UiState.Ready
            } catch (e: Exception) {
                Log.e(TAG, "Generation failed", e)
                _uiState.value = UiState.Error(e.message ?: "生成失败")
                _chatHistory.value += ChatMessage(prompt, "错误: ${e.message}")
                _currentPrompt.value = ""
                _currentResponse.value = ""
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        session?.close()
        engine?.close()
    }

    // ── 私有辅助 ──────────────────────────────────────────

    private fun resetConversation() {
        _chatHistory.value = emptyList()
        _currentPrompt.value = ""
        _currentResponse.value = ""
    }

    /** 解析模型路径：如果文件系统不存在，则尝试从 assets 复制到 cacheDir */
    private fun resolveModelPath(context: Context, modelPath: String): String {
        val file = File(modelPath)
        if (file.exists()) return modelPath

        return try {
            val cached = context.cacheDir.resolve(modelPath.substringAfterLast("/"))
            context.assets.open(modelPath).use { input ->
                FileOutputStream(cached).use { output -> input.copyTo(output) }
            }
            cached.absolutePath
        } catch (e: Exception) {
            throw IllegalArgumentException("模型文件不存在且无法从 assets 加载: $modelPath", e)
        }
    }

    /** 初始化 Engine & Session，自动清理旧实例 */
    private fun initEngine(modelPath: String, useGpu: Boolean) {
        val backend = if (useGpu) Backend.GPU() else Backend.CPU()
        val config = EngineConfig(
            modelPath = modelPath,
            backend = backend,
            maxNumTokens = 1024,
            cacheDir = null
        )
        val newEngine = Engine(config).also { it.initialize() }
        val newSession = newEngine.createSession(
            SessionConfig(SamplerConfig(40, 0.9, 0.8, 0))
        )

        engine?.close()
        session?.close()
        engine = newEngine
        session = newSession
    }

    /** 构建 Qwen ChatML 格式的 prompt */
    private fun buildChatPrompt(prompt: String): String = buildString {
        append("<|im_start|>system\nYou are a helpful assistant.<|im_end|>\n")
        _chatHistory.value.forEach { msg ->
            append("<|im_start|>user\n${msg.question}<|im_end|>\n")
            append("<|im_start|>assistant\n${msg.answer}<|im_end|>\n")
        }
        append("<|im_start|>user\n$prompt<|im_end|>\n")
        append("<|im_start|>assistant\n")
    }

    /** 流式回调：将 token 追加到 currentResponse */
    private inner class StreamResponseCallback : ResponseCallback {
        override fun onNext(response: String) {
//            Log.d(TAG, "response: $response")
            _currentResponse.value += response
        }

        override fun onDone() { /* completed */
        }

        override fun onError(error: Throwable) {
            _currentResponse.value += "\n(发生错误: ${error.message})"
        }
    }

    companion object {
        private const val TAG = "LiteRtLmViewModel"
    }
}

// ─────────────────────────────────────────────────────────
//  UI – 主屏幕
// ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiteRtLmScreen(viewModel: LiteRtLmViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val chatHistory by viewModel.chatHistory.collectAsState()
    val currentPromptState by viewModel.currentPrompt.collectAsState()
    val currentResponse by viewModel.currentResponse.collectAsState()

    val context = LocalContext.current
    var modelPath by remember { mutableStateOf("Qwen2.5-1.5.litertlm") }
    var prompt by remember { mutableStateOf("") }
    var useGpu by remember { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()

    // 自动滚动到底部：计算总条目数后滚到末尾
    val totalItems = chatHistory.size * 2 + if (currentResponse.isNotEmpty() || uiState == LiteRtLmViewModel.UiState.Generating) 2 else 0
    LaunchedEffect(totalItems, currentResponse.length) {
        if (totalItems > 0) lazyListState.animateScrollToItem(totalItems - 1)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("LiteRT-LM 实验") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ModelConfigSection(
                modelPath = modelPath,
                onModelPathChange = { modelPath = it },
                useGpu = useGpu,
                onUseGpuChange = { useGpu = it },
                uiState = uiState,
                onLoadModel = { viewModel.loadModel(context, modelPath, useGpu) }
            )

            StatusBar(uiState = uiState)

            ChatArea(
                chatHistory = chatHistory,
                currentPrompt = currentPromptState,
                currentResponse = currentResponse,
                lazyListState = lazyListState,
                modifier = Modifier.weight(1f)
            )

            PromptInputBar(
                prompt = prompt,
                onPromptChange = { prompt = it },
                enabled = uiState == LiteRtLmViewModel.UiState.Ready
                        || uiState is LiteRtLmViewModel.UiState.Error,
                onSend = {
                    viewModel.generateText(prompt)
                    prompt = ""
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────
//  UI – 模型配置区
// ─────────────────────────────────────────────────────────

@Composable
private fun ModelConfigSection(
    modelPath: String,
    onModelPathChange: (String) -> Unit,
    useGpu: Boolean,
    onUseGpuChange: (Boolean) -> Unit,
    uiState: LiteRtLmViewModel.UiState,
    onLoadModel: () -> Unit
) {
    val isLoading = uiState == LiteRtLmViewModel.UiState.LoadingModel
    val isGenerating = uiState == LiteRtLmViewModel.UiState.Generating

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("1. 模型配置", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = modelPath,
                onValueChange = onModelPathChange,
                label = { Text("Model Path (*.tflite)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = useGpu, onCheckedChange = onUseGpuChange)
                Text("使用 GPU", style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = onLoadModel,
                    enabled = !isLoading && !isGenerating
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (isLoading) "加载中..." else "加载模型")
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
//  UI – 状态栏
// ─────────────────────────────────────────────────────────

@Composable
private fun StatusBar(uiState: LiteRtLmViewModel.UiState) {
    val (text, isError) = uiState.toStatusDisplay()
    Text(
        text = "系统状态: $text",
        style = MaterialTheme.typography.bodySmall,
        color = if (isError) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** 将 UiState 映射为可读文本和是否为错误状态 */
private fun LiteRtLmViewModel.UiState.toStatusDisplay(): Pair<String, Boolean> = when (this) {
    is LiteRtLmViewModel.UiState.Idle -> "等待加载..." to false
    is LiteRtLmViewModel.UiState.LoadingModel -> "模型加载中..." to false
    is LiteRtLmViewModel.UiState.Ready -> "就绪，请输入提示词" to false
    is LiteRtLmViewModel.UiState.Generating -> "生成中..." to false
    is LiteRtLmViewModel.UiState.Error -> "错误: $message" to true
}

// ─────────────────────────────────────────────────────────
//  UI – 聊天区域
// ─────────────────────────────────────────────────────────

@Composable
private fun ChatArea(
    chatHistory: List<ChatMessage>,
    currentPrompt: String,
    currentResponse: String,
    lazyListState: LazyListState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = lazyListState,
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 历史消息：每条 ChatMessage 拆成两个 item，key 保证重组稳定
        items(chatHistory, key = { it.hashCode() }) { msg ->
            if (msg.question.isNotBlank()) {
                ChatBubble(label = "User:", text = msg.question, isUser = true)
            }
            if (msg.answer.isNotBlank()) {
                ChatBubble(label = "Model:", text = msg.answer, isUser = false)
            }
        }

        // 流式生成区块：以 currentPrompt 是否非空作为显示条件，
        // 避免 prompt 清空而 uiState 尚未切为 Ready 时渲染出空气泡
        if (currentPrompt.isNotBlank()) {
            item(key = "streaming_user") {
                ChatBubble(label = "User:", text = currentPrompt, isUser = true)
            }
            item(key = "streaming_model") {
                ChatBubble(label = "Model:", text = currentResponse, isUser = false)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
//  UI – 聊天气泡（消除历史 / 当前消息的重复代码）
// ─────────────────────────────────────────────────────────

@Composable
private fun ChatBubble(label: String, text: String, isUser: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isUser) Modifier.padding(end = 32.dp)
                else Modifier.padding(start = 32.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isUser) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (isUser) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// ─────────────────────────────────────────────────────────
//  UI – 输入栏
// ─────────────────────────────────────────────────────────

@Composable
private fun PromptInputBar(
    prompt: String,
    onPromptChange: (String) -> Unit,
    enabled: Boolean,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            label = { Text("输入聊天内容") },
            modifier = Modifier.weight(1f),
            enabled = enabled,
            maxLines = 3
        )
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = onSend,
            enabled = prompt.isNotBlank() && enabled
        ) {
            Text("发送")
        }
    }
}
