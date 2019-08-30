package ray.eldath.sirius.test.jmh

import org.json.JSONObject
import org.json.JSONTokener
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.results.format.ResultFormatType
import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.options.OptionsBuilder
import ray.eldath.sirius.api.rootJsonObject
import ray.eldath.sirius.core.JsonObjectValidationPredicate
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

@BenchmarkMode(Mode.Throughput)
@Fork(2)
@Threads(1)
@Warmup(iterations = 2, time = 8)
@Measurement(iterations = 4, time = 6)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
open class JmhTest {
    private val obj: JSONObject

    init {
        val json = """
            {
                "abc": "1234567890",
                "cde": {
                    "456": true
                }
            }
        """.trimIndent()
        obj = (JSONTokener(json)).nextValue() as JSONObject
    }

    private lateinit var root: JsonObjectValidationPredicate

    @Benchmark
    @Setup
    fun build() {
        root = rootJsonObject {
            "abc" string {
                required
                maxLength = 9
                lengthRange = 1..10
                test { length in 1..12 }
            }

            "cde" jsonObject {
                required

                any {
                    "123" string {
                        required
                    }

                    "456" boolean {
                        expected = true
                    }
                }
            }
        }
    }

    @Benchmark
    fun test() {
        root.test(obj)
    }

    companion object {
        private const val dic = "build/reports/jmh"

        @JvmStatic
        fun main(vararg args: String) {
            Files.createDirectories(Paths.get(dic))
            val options = OptionsBuilder()
                .include(JmhTest::class.java.simpleName)
                .output("$dic/benchmark.txt")
                .resultFormat(ResultFormatType.JSON)
                .result("$dic/results.json")
                .build()
            Runner(options).run()
        }
    }
}