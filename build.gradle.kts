import java.net.URL

plugins {
    kotlin("jvm") version "1.4.21-2"
    kotlin("kapt") version "1.4.0"
    id("org.jetbrains.dokka") version "1.4.20"
    id("io.gitlab.arturbosch.detekt") version "1.7.0"
    id("io.morethan.jmhreport") version "0.9.0"
}

group = "ray.eldath"
version = "0.0.1"

val jmhVersion = "1.25.2"

dependencies {
    // https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter
    testImplementation("org.junit.jupiter:junit-jupiter:5.6.1")
    // https://mvnrepository.com/artifact/org.openjdk.jmh/jmh-core
    testImplementation("org.openjdk.jmh:jmh-core:$jmhVersion")
    // https://mvnrepository.com/artifact/org.openjdk.jmh/jmh-generator-annprocess
    kaptTest("org.openjdk.jmh:jmh-generator-annprocess:$jmhVersion")
    // https://mvnrepository.com/artifact/org.json/json
    implementation("org.json:json:20200518")
    // https://mvnrepository.com/artifact/org.apache.commons/commons-lang3
    implementation("org.apache.commons:commons-lang3:3.9")

    implementation(kotlin("stdlib-jdk8"))
}

detekt {
    config = files("detekt.yml")
    buildUponDefaultConfig = true

    reports {
        xml { enabled = false }
        html { enabled = false }
    }
}

tasks.dokkaHtml.configure {
    outputDirectory.set(buildDir.resolve("dokka"))

    dokkaSourceSets {
        configureEach {
            reportUndocumented.set(false)

            // Allows linking to documentation of the project"s dependencies (generated with Javadoc or Dokka)
            // Repeat for multiple links
            externalDocumentationLink {
                // Root URL of the generated documentation to link with. The trailing slash is required!
                url.set(URL("https://commons.apache.org/proper/commons-lang/javadocs/api-release/"))

                packageListUrl.set(URL("https://commons.apache.org/proper/commons-lang/javadocs/api-release/package-list"))
            }

            // Specifies the location of the project source code on the Web.
            // If provided, Dokka generates "source" links for each declaration.
            // Repeat for multiple mappings
            sourceLink {
                // Unix based directory relative path to the root of the project (where you execute gradle respectively).
                localDirectory.set(file("src/main/kotlin")) // or simply "./"

                // URL showing where the source code can be accessed through the web browser
                remoteUrl.set(URL("https://github.com/Ray-Eldath/sirius/blob/master/src/main/kotlin"))
                // remove src/main/kotlin if you use "./" above

                // Suffix which is used to append the line number to the URL. Use #L for GitHub
                remoteLineSuffix.set("#L")
            }
        }
    }
}

task("jmh", JavaExec::class) {
    main = "ray.eldath.sirius.test.jmh.JmhTest"
    classpath = sourceSets["test"].runtimeClasspath
    defaultCharacterEncoding = "UTF-8"

    finalizedBy(tasks.named("jmhReport"))
}

tasks.test.configure {
    useJUnitPlatform()
    exclude("**/experiment/**", "**/jmh/**")
}

repositories {
    jcenter()
    mavenCentral()
}

listOf(tasks.compileKotlin, tasks.compileTestKotlin).forEach { it.get().kotlinOptions.jvmTarget = "11" }
listOf(tasks.compileJava, tasks.compileTestJava).forEach { it.get().options.encoding = "UTF-8" }