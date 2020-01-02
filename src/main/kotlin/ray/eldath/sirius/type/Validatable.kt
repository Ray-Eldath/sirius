package ray.eldath.sirius.type

import org.json.JSONArray
import org.json.JSONObject
import ray.eldath.sirius.core.*
import kotlin.reflect.KClass

enum class Validatable(
    val displayName: String,
    val actualType: KClass<*>,
    vararg val actualTypeName: String = arrayOf(actualType.javaObjectType.simpleName)
) {
    JSON_OBJECT("JsonObject", JSONObject::class),
    JSON_ARRAY("JsonArray", JSONArray::class),
    STRING("string", String::class),
    INTEGER("integer", Long::class, "Integer", "Long"),
    BOOLEAN("boolean", Boolean::class);

    companion object {
        fun fromPredicate(predicate: AnyValidationPredicate): Validatable =
            when (predicate) {
                is StringValidationPredicate -> STRING
                is JsonObjectValidationPredicate -> JSON_OBJECT
                is IntegerValidationPredicate -> INTEGER
                is BooleanValidationPredicate -> BOOLEAN
            }

        fun fromScope(scope: AnyValidationScope): Validatable =
            when (scope) {
                is StringValidationScope -> STRING
                is JsonObjectValidationScope -> JSON_OBJECT
                is IntegerValidationScope -> INTEGER
                is BooleanValidationScope -> BOOLEAN
            }
    }
}