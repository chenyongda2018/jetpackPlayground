package com.yongda.jetpackplayground

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yongda.jetpackplayground.ui.theme.JetpackPlaygroundTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JetpackPlaygroundTheme {
                FeatureListScreen()
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// 每个卡片的渐变配色方案（双色对）
// ──────────────────────────────────────────────────────────────────────────────
private val CARD_GRADIENTS: List<Pair<Color, Color>> = listOf(
    Color(0xFF6C63FF) to Color(0xFF9C94FF),
    Color(0xFFFF6584) to Color(0xFFFF8FA3),
    Color(0xFF43C6AC) to Color(0xFF56D6C2),
    Color(0xFFFF9966) to Color(0xFFFFC17A),
    Color(0xFF36D1DC) to Color(0xFF5B86E5),
    Color(0xFFF7971E) to Color(0xFFFFD200),
    Color(0xFF11998E) to Color(0xFF38EF7D),
    Color(0xFFDA22FF) to Color(0xFF9733EE),
)

@Preview
@Composable
private fun FeatureListScreen() {
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF0F0F1A),
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize()) {
            // ── 顶部标题区 ──────────────────────────────────────────────
            FeatureListHeader(
                topPadding = innerPadding.calculateTopPadding()
            )

            // ── 功能列表 ─────────────────────────────────────────────────
            FeatureList(
                features = FEATURE_REGISTRY,
                bottomPadding = innerPadding.calculateBottomPadding(),
                onItemClick = { item ->
                    context.startActivity(Intent(context, item.activityClass.java))
                }
            )
        }
    }
}

@Composable
private fun FeatureListHeader(topPadding: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF6C63FF), Color(0xFF36D1DC)),
                )
            )
            .padding(top = topPadding)
            .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
        Column {
            Text(
                text = "Jetpack Playground",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 22.sp,
                ),
            )
            Text(
                text = "选择一个实验功能开始探索",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.75f),
                ),
            )
        }
    }
}

@Composable
private fun FeatureList(
    features: List<FeatureItem>,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onItemClick: (FeatureItem) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = bottomPadding + 12.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        itemsIndexed(
            items = features,
            key = { _, item -> item.title },
        ) { index, item ->
            FeatureCard(
                item = item,
                gradientColors = CARD_GRADIENTS[index % CARD_GRADIENTS.size],
                onClick = { onItemClick(item) }
            )
        }
    }
}

@Composable
private fun FeatureCard(
    item: FeatureItem,
    gradientColors: Pair<Color, Color>,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "card_scale",
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(
                elevation = if (isPressed) 2.dp else 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = gradientColors.first.copy(alpha = 0.4f),
                spotColor = gradientColors.first.copy(alpha = 0.4f),
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 渐变圆形图标背景
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(gradientColors.first, gradientColors.second),
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = item.emoji,
                    fontSize = 24.sp,
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 标题 + 描述
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    ),
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.55f),
                    ),
                    maxLines = 1,
                )
            }

//            // 右箭头
//            Box(
//                modifier = Modifier
//                    .size(30.dp)
//                    .clip(CircleShape)
//                    .background(gradientColors.first.copy(alpha = 0.15f)),
//                contentAlignment = Alignment.Center,
//            ) {
//                Icon(
//                    imageVector = Icons
//                    contentDescription = null,
//                    tint = gradientColors.first,
//                    modifier = Modifier.size(18.dp),
//                )
//            }
        }
    }
}