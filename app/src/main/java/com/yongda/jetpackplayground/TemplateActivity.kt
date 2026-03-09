package com.yongda.jetpackplayground

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.yongda.jetpackplayground.ui.theme.JetpackPlaygroundTheme

/**
 * 通用模板 Activity，作为所有新实验功能的起点。
 * 复制此文件并重命名来实验新功能，然后在 [FEATURE_REGISTRY] 中注册即可。
 */
class TemplateActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JetpackPlaygroundTheme {
                TemplateScreen()
            }
        }
    }
}

@Composable
private fun TemplateScreen() {
    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "🧪 在这里开始实验！",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
