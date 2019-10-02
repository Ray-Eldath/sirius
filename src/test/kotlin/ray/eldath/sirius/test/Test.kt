package ray.eldath.sirius.test

import ray.eldath.sirius.api.rootJsonObject

object Test {
    @JvmStatic
    fun main(args: Array<String>) {
        val root = rootJsonObject(requiredByDefault = true) {
            "abc" string {
                lengthExact = 10
                test { it.length in 1..12 }
            }

            "cde" jsonObject {
                "123" string { expected("123", "456") }
                "456" boolean {
                    nonnull
                    optional
                    expected = false
                }
            }

            "fgh" jsonObject {
                "123" string { startsWithAny("pre", "cre") }
                "234" string { endsWithAny("4") }
            }
        }

        val json = """
            {
                "abc": "1234567890",
                "cde": {
                    "123": "123",
                    "456": false
                },
                "fgh": {
                    "123": "cre_123",
                    "234": "pre_220.34"
                }
            }
        """.trimIndent()
        println(root)
        println(root.final(json))
    }
}