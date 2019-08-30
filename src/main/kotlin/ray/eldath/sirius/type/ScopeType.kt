package ray.eldath.sirius.type

import ray.eldath.sirius.core.ValidationScope

abstract class RequireOption(defaultValue: Boolean) {
    var isRequired = defaultValue
        private set

    open val required: Unit
        get() = run { isRequired = true }

    val optional: Unit
        get() = run { isRequired = false }
}

typealias AnyValidationScope = ValidationScope<*>

@DslMarker
annotation class TopClassValidationScopeMarker