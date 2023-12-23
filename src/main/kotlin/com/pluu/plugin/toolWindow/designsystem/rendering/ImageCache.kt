package com.pluu.plugin.toolWindow.designsystem.rendering

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.util.concurrency.EdtExecutorService
import com.intellij.util.ui.update.MergingUpdateQueue
import com.intellij.util.ui.update.Update
import com.pluu.plugin.toolWindow.designsystem.model.AssetKey
import com.pluu.plugin.toolWindow.designsystem.model.DesignSystemItem
import org.jetbrains.annotations.Async
import java.awt.image.BufferedImage
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import kotlin.math.pow

private val CACHE_WEIGHT_BYTES = (100 * 1024.0.pow(2)).toLong() // 100 MB

private data class CachedImage(val image: BufferedImage, val modificationStamp: Long) {
    val size: Int
        get() = image.raster.dataBuffer.size * Integer.BYTES
}

/**
 * Helper class that caches the result of a computation of [BufferedImage].
 *
 * The keys of the cache are strong references to let
 * @see Cache
 * @see CacheBuilder.softValues
 */
class ImageCache private constructor(mergingUpdateQueue: MergingUpdateQueue?,
                                     private val objectToImage: Cache<AssetKey, CachedImage>
) : Disposable {
    companion object {
        private val objectToImageCache by lazy {
            createObjectToImageCache(5, CACHE_WEIGHT_BYTES)
        }

        /**
         * Returns an ImageCache that uses an image pool of size [CACHE_WEIGHT_BYTES] to store previews for a given [Asset]
         *
         * @param parentDisposable Used to dispose of the returned [ImageCache], used as the parent disposable for the default
         * [MergingUpdateQueue] when the [mergingUpdateQueue] parameter is null.
         */
        fun createImageCache(
            parentDisposable: Disposable,
            mergingUpdateQueue: MergingUpdateQueue? = null
        ) = ImageCache(mergingUpdateQueue, objectToImageCache).apply { Disposer.register(parentDisposable, this) }
    }

    private val pendingFutures = HashMap<DesignSystemItem, CompletableFuture<*>?>()

    private val updateQueue = mergingUpdateQueue ?: MergingUpdateQueue("queue", 3000, true, MergingUpdateQueue.ANY_COMPONENT, this, null,
        false)

    @Async.Schedule
    private fun runOrQueue(asset: DesignSystemItem,
                           executeImmediately: Boolean = false,
                           runnable: () -> Unit) {
        // We map to null to mark that the computation for asset has started and avoid any new computation.
        // It will then be replaced by the computation future once it is created.
        pendingFutures[asset] = null
        if (executeImmediately) {
            runnable()
        }
        else {
            val update = Update.create(asset.name, runnable)
            updateQueue.queue(update)
        }
    }

    override fun dispose() {
        synchronized(pendingFutures) {
            pendingFutures.values.forEach { it?.cancel(true) }
        }
    }

    fun clear(asset: DesignSystemItem) {
        objectToImage.invalidate(asset.key)
    }

    fun clear() {
        objectToImage.invalidateAll()
    }

    /**
     * Return the value identified by [AssetKey] in the cache if it exists, otherwise returns the [placeholder] image
     * and gets the image from the [CompletableFuture] returned by [computationFutureProvider].
     *
     * If [forceComputation] is true, the [CompletableFuture] will be ran even if a value is present in the cache.
     *
     * Note that if a value is present in the cache and [forceComputation] is true, the returned [BufferedImage] will be the value from
     * the cache.
     *
     * Once the image is cached, [onImageCached] is invoked on [executor] (or the EDT if none is provided)
     */
    fun computeAndGet(@Async.Schedule asset: DesignSystemItem,
                      placeholder: BufferedImage,
                      forceComputation: Boolean,
                      onImageCached: () -> Unit = {},
                      executor: Executor = EdtExecutorService.getInstance(),
                      computationFutureProvider: (() -> CompletableFuture<out BufferedImage?>))
            : BufferedImage {
        val cachedImage = objectToImage.getIfPresent(asset.key)
        if ((cachedImage == null || cachedImage.modificationStamp != asset.modificationStamp || forceComputation)
            && !pendingFutures.containsKey(asset)) {
            val executeImmediately = cachedImage == null // If we don't have any image, no need to wait.
            runOrQueue(asset, executeImmediately) {
                startComputation(computationFutureProvider, asset, onImageCached, executor)
            }
        }
        return cachedImage?.image ?: placeholder
    }

    private fun startComputation(computationFutureProvider: () -> CompletableFuture<out BufferedImage?>,
                                 @Async.Execute asset: DesignSystemItem,
                                 onImageCached: () -> Unit,
                                 executor: Executor) {
        val future = computationFutureProvider()
            .thenAccept { image: BufferedImage? ->
                synchronized(pendingFutures) {
                    pendingFutures.remove(asset)
                }
                if (image != null) {
                    objectToImage.put(asset.key, CachedImage(image, asset.modificationStamp))
                    executor.execute(onImageCached)
                }
                else {
                    objectToImage.invalidate(asset.key)
                }
            }
        synchronized(pendingFutures) {
            if (!future.isDone) {
                pendingFutures[asset] = future
            }
        }
    }
}

private fun createObjectToImageCache(duration: Long, size: Long): Cache<AssetKey, CachedImage> =
    CacheBuilder.newBuilder()
        .expireAfterAccess(duration, TimeUnit.MINUTES)
        .softValues()
        .weigher<AssetKey, CachedImage> { _, image -> image.size }
        .maximumWeight(size)
        .build<AssetKey, CachedImage>()