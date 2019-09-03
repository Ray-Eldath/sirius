package ray.eldath.sirius.type

import ray.eldath.sirius.core.*

enum class Validatable(val displayName: String) {
    JSON_OBJECT("JsonObject"),
    JSON_ARRAY("JsonArray"),
    BOOLEAN("boolean"),
    STRING("string");

    companion object {
        fun fromPredicate(predicate: AnyValidationPredicate): Validatable =
            when (predicate) {
                is StringValidationPredicate -> STRING
                is JsonObjectValidationPredicate -> JSON_OBJECT
                is BooleanValidationPredicate -> BOOLEAN
            }

        fun fromScope(scope: AnyValidationScope): Validatable =
            when (scope) {
                is StringValidationScope -> STRING
                is JsonObjectValidationScope -> JSON_OBJECT
                is BooleanValidationScope -> BOOLEAN
            }
    }
}