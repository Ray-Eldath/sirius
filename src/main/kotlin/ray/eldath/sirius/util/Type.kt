package ray.eldath.sirius.util

import ray.eldath.sirius.core.JsonObjectValidationPredicate
import ray.eldath.sirius.core.StringValidationPredicate
import ray.eldath.sirius.type.BaseValidationPredicate

enum class PredicateType(val displayName: String) {
    JSON_OBJECT("JsonObject"),
    STRING("string");

    companion object {
        fun fromPredicate(predicate: BaseValidationPredicate): PredicateType =
            when (predicate) {
                is StringValidationPredicate -> STRING
                is JsonObjectValidationPredicate -> JSON_OBJECT
            }
    }
}