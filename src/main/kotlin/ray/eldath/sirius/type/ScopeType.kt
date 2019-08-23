package ray.eldath.sirius.type

import ray.eldath.sirius.core.ValidationScope

abstract class RequireOption {
    var isRequired = false
        private set

    val required: Unit
        get() = run { isRequired = true }

    val optional: Unit
        get() = run { isRequired = false }
}

typealias BaseValidationScope = ValidationScope<*>

@DslMarker
annotation class TopClassValidationScopeMarker