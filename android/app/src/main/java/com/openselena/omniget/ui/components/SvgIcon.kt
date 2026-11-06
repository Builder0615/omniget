package com.openselena.omniget.ui.components

import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest

enum class AppIcon(val fileName: String) {
    HOME("home"),
    DOWNLOAD("download"),
    SETTINGS("settings"),
    HISTORY("history"),
    LINK("link"),
    ARROW_FORWARD("arrow-forward"),
    CHECK_CIRCLE("check-circle"),
    ERROR("error"),
    REFRESH("refresh"),
    PAUSE("pause"),
    PLAY("play"),
    CLOSE("close"),
    MOVIE("movie"),
    MUSIC("music"),
    OPEN_EXTERNAL("open-external"),
    SHARE("share"),
    DELETE("delete"),
    DELETE_SWEEP("delete-sweep"),
    PALETTE("palette"),
    SPARKLES("sparkles"),
    SYNC("sync"),
    STORAGE("storage"),
    FOLDER("folder"),
    COOKIE("cookie"),
    SLIDERS("sliders"),
    INFO("info"),
    EXPAND("expand"),
    CONTRACT("contract"),
    CHEVRON_DOWN("chevron-down"),
    MORE("more"),
    YOUTUBE("brand-youtube"),
    BILIBILI("brand-bilibili"),
    TIKTOK("brand-tiktok"),
    INSTAGRAM("brand-instagram"),
    X("brand-x"),
    REDDIT("brand-reddit"),
    VIMEO("brand-vimeo"),
}

@Composable
fun SvgIcon(
    icon: AppIcon,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    val context = LocalContext.current
    val request = remember(context, icon) {
        ImageRequest.Builder(context)
            .data("file:///android_asset/icons/${icon.fileName}.svg")
            .decoderFactory(SvgDecoder.Factory())
            .crossfade(false)
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(tint),
    )
}
