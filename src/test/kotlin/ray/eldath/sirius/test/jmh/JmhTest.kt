package ray.eldath.sirius.test.jmh

import org.json.JSONObject
import org.json.JSONTokener
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.profile.GCProfiler
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
@Warmup(iterations = 2, time = 4)
@Measurement(iterations = 2, time = 8)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
/**
 * For JMH test only.
 *
 * Note that each type of test, except test for nesting performance, should
 * be tested only once.
 */
open class JmhTest {
    private val json: String = """
            {
                "abc": 123450,
                "abd": {
                    "456": "pre_  \t_END",
                    "789": 123.4567
                },
                "cde": {
                    "456": true,
                    "202": "pr_1234_ends",
                    "303": null,
                    "789": {
                        "t": "nothing",
                        "T": "everything"
                    },
                    "101": {
                        "something": "ss",
                        "anything": { "anything": { "anything": { "anything": "anything" } } }
                    }
                }
            }
        """.trimIndent()

    private lateinit var root: JsonObjectValidationPredicate
    private lateinit var jsonObject: JSONObject

    @Benchmark
    @Setup
    fun build() {
        root = rootJsonObject {
            "abc" integer {
                maxLength = 9
                lengthRange = 6..10
            }

            "abd" jsonObject {
                "456" string {
                    nonBlank
                    startsWithAny("Pre", "pre_")
                    endsWithAny("end", ignoreCase = true)
                }
                "789" decimal {
                    max = 124.0
                    precision = 4
                }
            }

            "cde" jsonObject {
                any {
                    "123" string {}
                    "456" boolean (true)
                }

                "202" string {
                    acceptIf { it.startsWith("pre_") || it.endsWith("_ends") }
                }

                "303" boolean { nullable }

                "789" jsonObject {
                    any {
                        "t" string { expected("something") }
                        "T" string { expected("everything", "something") }
                    }
                }

                "101" jsonObject {
                    "anything" jsonObject {
                        "anything" jsonObject {
                            "anything" jsonObject {
                                "anything" string { expected("anything", "nothing") }
                            }
                        }
                    }
                }
            }
        }
    }

    @Benchmark
    @Setup
    fun parse() {
        jsonObject = JSONTokener(json).nextValue() as JSONObject
    }

    @Benchmark
    fun pureTest(): Boolean = root.final(jsonObject).also { assert(it) }

    @Benchmark
    fun test(): Boolean = root.final(json).also { assert(it) }

    companion object {
        private const val dir = "build/reports/jmh"

        private val gcFileRegex = Regex("gc(\\.(\\d{4})-(\\d{2})-(\\d{2})_(\\d{2})-(\\d{2})-(\\d{2}))*\\.log")

        @JvmStatic
        fun main(vararg args: String) {
            if (args.size == 1 && args[0].toLowerCase() == "debug") {
                val instance = JmhTest()
                instance.build()
                instance.test().also {
                    println(it)
                    assert(it)
                }
                return
            }
            val dirPath = Paths.get(dir)
            Files.createDirectories(dirPath)
            Files.list(dirPath)
                .filter { it.fileName.toString().matches(gcFileRegex) }
                .forEach { Files.delete(it) }

            val options = OptionsBuilder()
                .include(JmhTest::class.java.simpleName)
                .output("$dir/benchmark.txt")
                .resultFormat(ResultFormatType.JSON)
                .result("$dir/results.json")
                .addProfiler(GCProfiler::class.java)
                .jvmArgsAppend("-Xlog:gc:file=$dir/gc.%t.log")
                .build()
            Runner(options).run()
        }
    }
}