package ray.eldath.sirius.api

import ray.eldath.sirius.core.JsonObjectValidationPredicate
import ray.eldath.sirius.core.JsonObjectValidationScope

// checkpoint
fun rootJsonObject(block: RootJsonObjectValidationScope.() -> Unit): RootJsonObjectValidationPredicate =
    JsonObjectValidationScope(0).apply {
        block()
        required
    }.build()

typealias RootJsonObjectValidationScope = JsonObjectValidationScope
typealias RootJsonObjectValidationPredicate = JsonObjectValidationPredicate
// these will not be typealias soon!