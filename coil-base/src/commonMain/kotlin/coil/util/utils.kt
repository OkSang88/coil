@file:JvmName("-utils")

package coil.util

import android.app.ActivityManager
import android.content.ContentResolver.SCHEME_FILE
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.ImageView.ScaleType.CENTER_INSIDE
import android.widget.ImageView.ScaleType.FIT_CENTER
import android.widget.ImageView.ScaleType.FIT_END
import android.widget.ImageView.ScaleType.FIT_START
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import coil.ComponentRegistry
import coil.EventListener
import coil.base.R
import coil.decode.DataSource
import coil.decode.Decoder
import coil.disk.DiskCache
import coil.fetch.Fetcher
import coil.intercept.Interceptor
import coil.intercept.RealInterceptorChain
import coil.memory.MemoryCache
import coil.request.Parameters
import coil.request.Tags
import coil.request.ViewTargetRequestManager
import coil.size.Dimension
import coil.size.Scale
import coil.size.Size
import coil.size.isOriginal
import coil.size.pxOrElse
import coil.transform.Transformation
import java.io.File
import kotlin.jvm.JvmName
import kotlin.reflect.KClass
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import okhttp3.Headers
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Closeable

internal inline fun Logger.log(tag: String, level: Logger.Level, message: () -> String) {
    if (minLevel <= level) {
        log(tag, level, message(), null)
    }
}

internal fun Logger.log(tag: String, throwable: Throwable) {
    if (minLevel <= Logger.Level.Error) {
        log(tag, Logger.Level.Error, null, throwable)
    }
}

internal val DataSource.emoji: String
    get() = when (this) {
        DataSource.MEMORY_CACHE,
        DataSource.MEMORY -> "🧠"
        DataSource.DISK -> "💾"
        DataSource.NETWORK -> "☁️"
    }

internal val View.requestManager: ViewTargetRequestManager
    get() {
        var manager = getTag(R.id.coil_request_manager) as? ViewTargetRequestManager
        if (manager == null) {
            manager = synchronized(this) {
                // Check again in case coil_request_manager was just set.
                (getTag(R.id.coil_request_manager) as? ViewTargetRequestManager)
                    ?.let { return@synchronized it }

                ViewTargetRequestManager(this).apply {
                    addOnAttachStateChangeListener(this)
                    setTag(R.id.coil_request_manager, this)
                }
            }
        }
        return manager
    }

internal val Drawable.width: Int
    get() = (this as? BitmapDrawable)?.bitmap?.width ?: intrinsicWidth

internal val Drawable.height: Int
    get() = (this as? BitmapDrawable)?.bitmap?.height ?: intrinsicHeight

internal val Drawable.isVector: Boolean
    get() = this is VectorDrawable || this is VectorDrawableCompat

internal fun Closeable.closeQuietly() {
    try {
        close()
    } catch (e: RuntimeException) {
        throw e
    } catch (_: Exception) {}
}

internal val ImageView.scale: Scale
    get() = when (scaleType) {
        FIT_START, FIT_CENTER, FIT_END, CENTER_INSIDE -> Scale.FIT
        else -> Scale.FILL
    }

internal val Uri.firstPathSegment: String?
    get() = pathSegments.firstOrNull()

internal val Configuration.nightMode: Int
    get() = uiMode and Configuration.UI_MODE_NIGHT_MASK

/**
 * An allowlist of valid bitmap configs for the input and output bitmaps of
 * [Transformation.transform].
 */
internal val VALID_TRANSFORMATION_CONFIGS = if (SDK_INT >= 26) {
    arrayOf(Bitmap.Config.ARGB_8888, Bitmap.Config.RGBA_F16)
} else {
    arrayOf(Bitmap.Config.ARGB_8888)
}

/**
 * Prefer hardware bitmaps on API 26 and above since they are optimized for drawing without
 * transformations.
 */
internal val DEFAULT_BITMAP_CONFIG = if (SDK_INT >= 26) {
    Bitmap.Config.HARDWARE
} else {
    Bitmap.Config.ARGB_8888
}

/** Required for compatibility with API 25 and below. */
internal val NULL_COLOR_SPACE: ColorSpace? = null

internal val EMPTY_HEADERS = Headers.Builder().build()

internal fun Headers?.orEmpty() = this ?: EMPTY_HEADERS

internal fun Tags?.orEmpty() = this ?: Tags.EMPTY

internal fun Parameters?.orEmpty() = this ?: Parameters.EMPTY

internal fun isMainThread() = Looper.myLooper() == Looper.getMainLooper()

@OptIn(ExperimentalCoroutinesApi::class)
internal fun <T> Deferred<T>.getCompletedOrNull(): T? {
    return try {
        getCompleted()
    } catch (_: Throwable) {
        null
    }
}

internal inline operator fun MemoryCache.get(key: MemoryCache.Key?) = key?.let(::get)

/** https://github.com/coil-kt/coil/issues/675 */
internal val Context.safeCacheDir: File
    get() {
        val cacheDir = checkNotNull(cacheDir) { "cacheDir == null" }
        return cacheDir.apply { mkdirs() }
    }

internal inline fun ComponentRegistry.Builder.addFirst(
    pair: Pair<Fetcher.Factory<*>, KClass<*>>?
) = apply { if (pair != null) fetcherFactories.add(0, pair) }

internal inline fun ComponentRegistry.Builder.addFirst(
    factory: Decoder.Factory?
) = apply { if (factory != null) decoderFactories.add(0, factory) }

internal fun String.toNonNegativeInt(defaultValue: Int): Int {
    val value = toLongOrNull() ?: return defaultValue
    return when {
        value > Int.MAX_VALUE -> Int.MAX_VALUE
        value < 0 -> 0
        else -> value.toInt()
    }
}

internal fun DiskCache.Editor.abortQuietly() {
    try {
        abort()
    } catch (_: Exception) {}
}

internal const val MIME_TYPE_JPEG = "image/jpeg"
internal const val MIME_TYPE_WEBP = "image/webp"
internal const val MIME_TYPE_HEIC = "image/heic"
internal const val MIME_TYPE_HEIF = "image/heif"

internal val Interceptor.Chain.isPlaceholderCached: Boolean
    get() = this is RealInterceptorChain && isPlaceholderCached

internal val Interceptor.Chain.eventListener: EventListener
    get() = if (this is RealInterceptorChain) eventListener else EventListener.NONE

internal fun Int.isMinOrMax() = this == Int.MIN_VALUE || this == Int.MAX_VALUE

internal inline fun Size.widthPx(scale: Scale, original: () -> Int): Int {
    return if (isOriginal) original() else width.toPx(scale)
}

internal inline fun Size.heightPx(scale: Scale, original: () -> Int): Int {
    return if (isOriginal) original() else height.toPx(scale)
}

internal fun Dimension.toPx(scale: Scale) = pxOrElse {
    when (scale) {
        Scale.FILL -> Int.MIN_VALUE
        Scale.FIT -> Int.MAX_VALUE
    }
}

internal fun unsupported(): Nothing = error("Unsupported")

internal const val ASSET_FILE_PATH_ROOT = "android_asset"

internal fun isAssetUri(uri: Uri): Boolean {
    return uri.scheme == SCHEME_FILE && uri.firstPathSegment == ASSET_FILE_PATH_ROOT
}

/** Modified from [Headers.Builder.add] */
internal fun Headers.Builder.addUnsafeNonAscii(line: String) = apply {
    val index = line.indexOf(':')
    require(index != -1) { "Unexpected header: $line" }
    addUnsafeNonAscii(line.substring(0, index).trim(), line.substring(index + 1))
}

internal fun Response.requireBody(): ResponseBody {
    return checkNotNull(body) { "response body == null" }
}

private const val STANDARD_MEMORY_MULTIPLIER = 0.2
private const val LOW_MEMORY_MULTIPLIER = 0.15

/** Return the default percent of the application's total memory to use for the memory cache. */
internal fun defaultMemoryCacheSizePercent(context: Context): Double {
    return try {
        val activityManager: ActivityManager = context.requireSystemService()
        if (activityManager.isLowRamDevice) LOW_MEMORY_MULTIPLIER else STANDARD_MEMORY_MULTIPLIER
    } catch (_: Exception) {
        STANDARD_MEMORY_MULTIPLIER
    }
}

private const val DEFAULT_MEMORY_CLASS_MEGABYTES = 256

/** Return a [percent] of the application's total memory in bytes. */
internal fun calculateMemoryCacheSize(context: Context, percent: Double): Long {
    val memoryClassMegabytes = try {
        val activityManager: ActivityManager = context.requireSystemService()
        val isLargeHeap = (context.applicationInfo.flags and ApplicationInfo.FLAG_LARGE_HEAP) != 0
        if (isLargeHeap) activityManager.largeMemoryClass else activityManager.memoryClass
    } catch (_: Exception) {
        DEFAULT_MEMORY_CLASS_MEGABYTES
    }
    return (percent * memoryClassMegabytes * 1024 * 1024).toLong()
}
