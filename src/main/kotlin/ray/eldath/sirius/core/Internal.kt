package ray.eldath.sirius.core

import ray.eldath.sirius.util.ExceptionAssembler

object PredicateBuildInterceptor {
    internal inline fun <E, reified T : ValidationScope<out E>> jsonObjectIntercept(
        initializer: T.() -> Unit,
        key: String,
        depth: Int
    ): E {
        val d = depth + 1
        val inst = T::class.java.getDeclaredConstructor(Int::class.java).newInstance(d)

        if (!inst.validateConstrains())
            throw ExceptionAssembler.assembleJsonObjectICE(scope = inst, key = key, depth = d)
        return inst.apply(initializer).build()
    }
}