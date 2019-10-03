package ray.eldath.sirius.test

import ray.eldath.sirius.api.rootJsonObject
import ray.eldath.sirius.util.StringCase

object Test {
    @JvmStatic
    fun main(args: Array<String>) {
        val root = rootJsonObject(requiredByDefault = true) {
            "abc" string {
                lengthExact = 9
                requireCase(StringCase.PASCAL_CASE)
                acceptIf { it.length in 1..12 }
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
                "456" string { nonBlank }
            }
        }

        val json = """
            {
                "abc": "RayEldath",
                "cde": {
                    "123": "123",
                    "456": false
                },
                "fgh": {
                    "123": "cre_123",
                    "234": "pre_220.34",
                    "456": "\t   "
                }
            }
        """.trimIndent()
        println(root)
        println(root.final(json))
    }
}