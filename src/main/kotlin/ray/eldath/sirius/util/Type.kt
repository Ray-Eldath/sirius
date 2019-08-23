package ray.eldath.sirius.util

import ray.eldath.sirius.core.*
import ray.eldath.sirius.type.BaseValidationPredicate
import ray.eldath.sirius.type.BaseValidationScope

enum class ConstrainType(val displayName: String) {
    JSON_OBJECT("JsonObject"),
    BOOLEAN("boolean"),
    STRING("string");

    companion object {
        fun fromPredicate(predicate: BaseValidationPredicate): ConstrainType =
            when (predicate) {
                is StringValidationPredicate -> STRING
                is JsonObjectValidationPredicate -> JSON_OBJECT
                is BooleanValidationPredicate -> BOOLEAN
            }

        fun fromScope(scope: BaseValidationScope): ConstrainType =
            when (scope) {
                is StringValidationScope -> STRING
                is JsonObjectValidationScope -> JSON_OBJECT
                is BooleanValidationScope -> BOOLEAN
            }
    }
}