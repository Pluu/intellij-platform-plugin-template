package com.pluu.plugin.utils

import com.android.tools.idea.concurrency.AndroidCoroutineScope
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/** Returns a coroutine scope that is tied to the [com.intellij.openapi.application.Application]'s lifecycle. */
fun applicationCoroutineScope(context: CoroutineContext = EmptyCoroutineContext): CoroutineScope =
    AndroidCoroutineScope(service<ApplicationCoroutineScopeDisposable>(), context)

/**
 * This application level service is used to ensure all Disposables created by
 * [CoroutineScope.scopeDisposable] get disposed when the application is disposed.
 * [Job.invokeOnCompletion] does not provide thread guarantees (cf.
 * https://github.com/Kotlin/kotlinx.coroutines/issues/3505) and we had some race conditions where
 * the UndisposedAndroidObjectsCheckerRule#checkUndisposedAndroidRelatedObjects leak check would run
 * before the call to [Disposable.dispose] inside the [Job.invokeOnCompletion]. This created some
 * errors in some tests, for example: b/328290264. By using this application level service as a
 * parent disposable, we ensure all child disposables are disposed of at the end of each test.
 */
@Service(Service.Level.APP)
private class ApplicationCoroutineScopeDisposable : Disposable {
    override fun dispose() {}
}