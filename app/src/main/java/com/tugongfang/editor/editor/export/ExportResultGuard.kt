package com.tugongfang.editor.editor.export

import java.util.concurrent.CancellationException

/**
 * Keep previous Result-based error handling for actual render failures, but never
 * swallow coroutine cancellation as an ordinary failure. This is important when
 * the user leaves the editor during an expensive preview/export operation.
 */
internal object ExportResultGuard {
    inline fun <T> run(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (cancel: CancellationException) {
        throw cancel
    } catch (error: Throwable) {
        Result.failure(error)
    }
}
