package ray.eldath.sirius.test

import org.json.JSONObject
import org.json.JSONTokener
import ray.eldath.sirius.api.rootJsonObject

object Test {
    @JvmStatic
    fun main(args: Array<String>) {
        val root = rootJsonObject {
            "abc" string {
                required
                maxLength = 9
                lengthRange = 1..10
                test { length in 1..12 }
            }

            "cde" jsonObject {
                required

                "123" string {
                    required
                    expected("123", "456")
                }

                "456" boolean {
                    expected = true
                }
            }
        }

        val json = """
            {
                "abc": "1234567890",
                "cde": {
                    "123": "123",
                    "456": true
                }
            }
        """.trimIndent()
        val obj = (JSONTokener(json)).nextValue() as JSONObject

        println(root)
        println(root.final(obj))
    }
}