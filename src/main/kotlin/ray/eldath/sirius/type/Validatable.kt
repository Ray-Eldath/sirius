package ray.eldath.sirius.type

import org.json.JSONArray
import org.json.JSONObject
import ray.eldath.sirius.core.*
import kotlin.reflect.KClass

enum class Validatable(val displayName: String, val actualType: KClass<*>) {
    JSON_OBJECT("JsonObject", JSONObject::class),
    JSON_ARRAY("JsonArray", JSONArray::class),
    BOOLEAN("boolean", Boolean::class),
    STRING("string", String::class);

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