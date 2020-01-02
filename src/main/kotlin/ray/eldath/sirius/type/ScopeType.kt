package ray.eldath.sirius.type

import ray.eldath.sirius.core.ValidationScope

open class BasicOption(require: Boolean, nullable: Boolean) {
    var isRequired = require
        private set

    val required: Unit
        get() = run { isRequired = true }

    val optional: Unit
        get() {
            isRequired = false
            nullable
        }

    //
    var isNullable = nullable
        private set

    val nullable: Unit
        get() = run { isNullable = true }

    val nonnull: Unit
        get() = run { isNullable = false }
}

typealias AnyValidationScope = ValidationScope<*, *>

@DslMarker
annotation class TopClassValidationScopeMarker