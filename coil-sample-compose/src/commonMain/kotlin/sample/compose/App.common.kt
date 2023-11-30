package sample.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import coil.compose.AsyncImage
import coil.compose.LocalPlatformContext
import coil.memory.MemoryCache
import coil.request.ImageRequest
import sample.common.AssetType
import sample.common.Image
import sample.common.MainViewModel
import sample.common.NUM_COLUMNS
import sample.common.Screen
import sample.common.calculateScaledSize
import sample.common.next

@Composable
fun App(viewModel: MainViewModel) {
    MaterialTheme(
        colors = lightColors(
            primary = Color.White,
            onPrimary = Color.Black,
        ),
    ) {
        val screen by viewModel.screen.collectAsState()
        val isDetail = screen is Screen.Detail
        Scaffold(
            topBar = {
                Toolbar(
                    assetType = viewModel.assetType.collectAsState().value,
                    backEnabled = isDetail,
                    onAssetTypeChange = { viewModel.assetType.value = it },
                    onBackPressed = { viewModel.onBackPressed() },
                )
            },
            content = { padding ->
                Box(
                    modifier = Modifier.padding(padding),
                ) {
                    ScaffoldContent(
                        screen = screen,
                        onScreenChange = { viewModel.screen.value = it },
                        images = viewModel.images.collectAsState().value,
                    )
                }
            },
        )
        BackHandler(enabled = isDetail) {
            viewModel.onBackPressed()
        }
    }
}

@Composable
private fun Toolbar(
    assetType: AssetType,
    backEnabled: Boolean,
    onAssetTypeChange: (AssetType) -> Unit,
    onBackPressed: () -> Unit,
) {
    TopAppBar(
        title = { Text("Coil") },
        navigationIcon = if (backEnabled) {
            { BackIconButton(onBackPressed) }
        } else {
            null
        },
        actions = {
            AssetTypeButton(
                assetType = assetType,
                onAssetTypeChange = onAssetTypeChange,
            )
        },
        modifier = Modifier.statusBarsPadding(),
    )
}

@Composable
private fun BackIconButton(
    onBackPressed: () -> Unit,
) {
    IconButton(
        onClick = onBackPressed,
        content = {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = null,
            )
        },
    )
}

@Composable
private fun AssetTypeButton(
    assetType: AssetType,
    onAssetTypeChange: (AssetType) -> Unit,
) {
    IconButton(
        onClick = { onAssetTypeChange(assetType.next()) },
        content = { Text(assetType.name) },
    )
}

@Composable
private fun ScaffoldContent(
    screen: Screen,
    onScreenChange: (Screen) -> Unit,
    images: List<Image>,
) {
    // Reset the scroll position when the image list changes.
    // Preserve the scroll position when navigating to/from the detail screen.
    val gridState = rememberSaveable(images, saver = LazyStaggeredGridState.Saver) {
        LazyStaggeredGridState()
    }

    when (screen) {
        is Screen.Detail -> {
            DetailScreen(screen)
        }
        is Screen.List -> {
            ListScreen(
                gridState = gridState,
                images = images,
                onImageClick = { image, placeholder ->
                    onScreenChange(Screen.Detail(image, placeholder))
                },
            )
        }
    }
}

@Composable
private fun DetailScreen(screen: Screen.Detail) {
    AsyncImage(
        model = ImageRequest.Builder(LocalPlatformContext.current)
            .data(screen.image.uri)
            .placeholderMemoryCacheKey(screen.placeholder)
            .apply { extras.setAll(screen.image.extras) }
            .build(),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun ListScreen(
    gridState: LazyStaggeredGridState,
    images: List<Image>,
    onImageClick: (Image, MemoryCache.Key?) -> Unit,
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(NUM_COLUMNS),
        state = gridState,
        modifier = Modifier.testTag("list"),
    ) {
        items(images) { image ->
            // Scale the image to fit the width of a column.
            val size = with(LocalDensity.current) {
                val (width, height) = image.calculateScaledSize(containerSize().width)
                DpSize(width.toDp(), height.toDp())
            }

            // Intentionally not a state object to avoid recomposition.
            var placeholder: MemoryCache.Key? = null

            AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current)
                    .data(image.uri)
                    .apply { extras.setAll(image.extras) }
                    .build(),
                contentDescription = null,
                placeholder = ColorPainter(Color(image.color)),
                error = ColorPainter(Color.Red),
                onSuccess = { placeholder = it.result.memoryCacheKey },
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clickable { onImageClick(image, placeholder) },
            )
        }
    }
}

@Composable
expect fun containerSize(): IntSize

@Composable
expect fun BackHandler(
    enabled: Boolean = true,
    onBack: () -> Unit,
)
