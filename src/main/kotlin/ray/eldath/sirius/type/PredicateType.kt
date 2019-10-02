package ray.eldath.sirius.type

import ray.eldath.sirius.core.ValidationPredicate

typealias AnyValidationPredicate = ValidationPredicate<*>

interface TopPredicate<T> {
    fun final(value: String): Boolean

    fun final(value: T): Boolean
}

data class LambdaTest<T>(val lambda: (T) -> Boolean, val purpose: String = "", val isBuiltIn: Boolean = false)