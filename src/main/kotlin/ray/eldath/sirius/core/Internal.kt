package ray.eldath.sirius.core

import ray.eldath.sirius.config.SiriusValidationConfig
import ray.eldath.sirius.trace.ExceptionLocator
import ray.eldath.sirius.trace.Tracer.LocationBasedTracer.invalidAssert
import ray.eldath.sirius.type.LambdaTest

object PredicateBuildInterceptor {
    internal inline fun <E, reified T : ValidationScope<*, out E>> jsonObjectIntercept(
        initializer: T.() -> Unit,
        key: String,
        depth: Int,
        config: SiriusValidationConfig
    ): E {
        val d = depth + 1
        val inst = T::class.java
            .getDeclaredConstructor(Int::class.java, SiriusValidationConfig::class.java)
            .newInstance(d, config)

        val applied = inst.apply(initializer)

        inst.isAssertsValid().filterKeys { !it }.map { it.value }.firstOrNull()?.let {
            throw invalidAssert(it, ExceptionLocator.jsonObjectLocator(inst, depth, key))
        }
        return applied.build()
    }
}

internal fun <T> assertsOf(tests: List<LambdaTest<T>> = emptyList(), vararg asserts: AnyAssert) =
    AssertWrapper(asserts.toList(), tests)