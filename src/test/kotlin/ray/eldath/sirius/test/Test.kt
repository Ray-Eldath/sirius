package ray.eldath.sirius.test

import ray.eldath.sirius.api.rootJsonObject

object Test {
    @JvmStatic
    fun main(args: Array<String>) {
        val root = rootJsonObject(requiredByDefault = true) {
            "abc" string {
                lengthExact = 10
                test { length in 1..12 }
            }

            "cde" jsonObject {
                "123" string { expected("123", "456") }
                "456" boolean {
                    nonnull
                    optional
                    expected = false
                }
            }
        }

        val json = """
            {
                "abc": "1234567890",
                "cde": {
                    "123": "123",
                    "456": null
                }
            }
        """.trimIndent()
        println(root)
        println(root.final(json))
    }
}