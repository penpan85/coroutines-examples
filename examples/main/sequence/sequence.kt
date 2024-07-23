package sequence

import kotlin.coroutines.*
import kotlin.experimental.*

@RequiresOptIn
annotation class ExperimentalApi


// 标记一个实验性的函数
@ExperimentalApi
fun experimentalFunction() {
    println("This is an experimental function.")
}

@RestrictsSuspension
interface SequenceScope<in T> {
    suspend fun yield(value: T)
}

// 使用实验性的函数，需要显式声明 @OptIn
@OptIn(ExperimentalApi::class, ExperimentalTypeInference::class)
fun <T> sequence(@BuilderInference block: suspend SequenceScope<T>.() -> Unit): Sequence<T> = Sequence {
    SequenceCoroutine<T>().apply {
        nextStep = block.createCoroutine(receiver = this, completion = this)
    }
}

private class SequenceCoroutine<T>: AbstractIterator<T>(), SequenceScope<T>, Continuation<Unit> {
    lateinit var nextStep: Continuation<Unit>

    // AbstractIterator implementation
    override fun computeNext() { nextStep.resume(Unit) }

    // Completion continuation implementation
    override val context: CoroutineContext get() = EmptyCoroutineContext

    override fun resumeWith(result: Result<Unit>) {
        result.getOrThrow() // bail out on error
        done()
    }

    // Generator implementation
    override suspend fun yield(value: T) {
        setNext(value)
        return suspendCoroutine { cont -> nextStep = cont }
    }
}
