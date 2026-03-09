package com.yongda.jetpackplayground

import android.app.Activity
import com.yongda.jetpackplayground.pages.litertlm.LiteRtLmActivity
import kotlin.reflect.KClass

/**
 * 代表首页列表中一个可点击的实验功能入口。
 *
 * @param title       功能标题
 * @param description 功能简短描述
 * @param emoji       代表该功能的 emoji 图标
 * @param activityClass 点击后要打开的 Activity
 */
data class FeatureItem(
    val title: String,
    val description: String,
    val emoji: String,
    val activityClass: KClass<out Activity>,
)

/**
 * 所有实验功能的注册表。
 * 每次新建实验 Activity，只需在这里追加一个 [FeatureItem] 即可，主页列表会自动更新。
 */
val FEATURE_REGISTRY: List<FeatureItem> = listOf(
    FeatureItem(
        title = "Template Demo",
        description = "空白模板，用于实验新功能 / 新 API",
        emoji = "🧪",
        activityClass = TemplateActivity::class,
    ),
    FeatureItem(
        title = "LiteLm",
        description = "Google AI Edge",
        emoji = "😄",
        activityClass = LiteRtLmActivity::class,
    ),
    // 在此处继续追加新的实验功能 ↓
)
