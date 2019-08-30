package ray.eldath.sirius.type

import ray.eldath.sirius.core.ValidationPredicate

typealias AnyValidationPredicate = ValidationPredicate<*>
typealias Predicate<T> = T.() -> Boolean

internal interface TopPredicate<T> {
    fun final(value: T): Boolean
}