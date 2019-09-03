package ray.eldath.sirius.type

import ray.eldath.sirius.core.ValidationScope

abstract class BasicOption(require: Boolean, nullable: Boolean) {
    var isRequired = require
        private set

    val required: Unit
        get() = run { isRequired = true }

    val optional: Unit
        get() = run { isRequired = false }

    //
    var isNullable = nullable
        private set

    open val nullable: Unit
        get() = run { isNullable = true }

    val nonnull: Unit
        get() = run { isNullable = false }
}

typealias AnyValidationScope = ValidationScope<*>

@DslMarker
annotation class TopClassValidationScopeMarker