package ray.eldath.sirius.test

import ray.eldath.sirius.api.rootJsonObject
import ray.eldath.sirius.util.StringCase
import ray.eldath.sirius.util.StringContentPattern.*

object Test {
    @JvmStatic
    fun main(args: Array<String>) {
        val root = rootJsonObject {
            "abc" string {
                lengthExact = 9
                requireCase(StringCase.PASCAL_CASE)
                acceptIf { it.length in 1..12 }
            }

            "cde" jsonObject {
                "123" string { requireContent(NUMBER, ALPHA, NON_ASCII) }
                "456" boolean {
                    optional
                    expected = false
                }
            }

            "fgh" jsonObject {
                "123" string { startsWithAny("pre", "cre") }
                "234" string {
                    endsWithAny("4")
                    matches("\\S+")
                }
                "456" string { nonBlank }
                +"45\\d" string { nonBlank }
                Regex("45\\d") string { nonBlank }

                "479" integer {
                    lengthRange = 3..6
                    max = 123456
                }
            }
        }

        val json = """
            {
                "abc": "RayEldath",
                "cde": {
                    "123": "Phosphorous你好",
                    "456": false
                },
                "fgh": {
                    "123": "cre_123",
                    "234": "pre_220.34",
                    "456": "\t   123", 
                    "457": "\t   123",
                    "479": 123456
                }
            }
        """.trimIndent()
        println(root)
        println(root.final(json))
    }
}