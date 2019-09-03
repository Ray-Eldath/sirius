package ray.eldath.sirius.test.jmh

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
@Warmup(iterations = 2, time = 8)
@Measurement(iterations = 4, time = 6)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
open class JmhTest {
    private val json: String = """
            {
                "abc": "1234567890",
                "cde": {
                    "456": true,
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

    @Benchmark
    @Setup
    fun build() {
        root = rootJsonObject(requiredByDefault = true) {
            "abc" string {
                maxLength = 9
                lengthRange = 1..10
                test { length in 1..12 }
            }

            "cde" jsonObject {
                any {
                    "123" string {}
                    "456" boolean { expected = true }
                }

                "789" jsonObject {
                    any {
                        "t" string { expected("something") }
                        "T" string { expected("everything", "something") }
                    }
                }

                "101" jsonObject {
                    "something" string { lengthRange = 2..4 }
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
    fun test(): Boolean = root.final(json).also { assert(it) }

    companion object {
        private const val dir = "build/reports/jmh"
        private const val debug = false

        private val gcFileRegex = Regex("gc(\\.(\\d{4})-(\\d{2})-(\\d{2})_(\\d{2})-(\\d{2})-(\\d{2}))*\\.log")

        @Suppress("ConstantConditionIf")
        @JvmStatic
        fun main(vararg args: String) {
            if (debug) {
                val instance = JmhTest()
                instance.build()
                println(instance.test())
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