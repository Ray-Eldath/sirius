package ray.eldath.sirius.type

import ray.eldath.sirius.core.ValidationPredicate

abstract class RequireOption {
    var isRequired = false
        private set

    val required: Unit
        get() = run { isRequired = true }
}

typealias BaseValidationPredicate = ValidationPredicate<*>
typealias Predicate<T> = T.() -> Boolean