/**
 * jvfault Framework — common build configuration for all framework modules.
 *
 * Policy decisions (see docs/architecture-decisions.md):
 *  - Pure Java: no Kotlin, no fat-jar/shadow, no external quality plugins.
 *  - Baseline target: Java 8 (--release 8) for the classic-era modules;
 *    AI-era modules (ai/rag/mcp) opt up to --release 17 in their own scripts.
 *  - Only build-in plugins: java-library + maven-publish (+jacoco).
 */

allprojects {
    group = "com.jvfault"
    version = project.property("jvfaultVersion") as String
}

// The BOM project is configured separately (java-platform cannot take java-library).
subprojects {
    // distribution 是 java-platform BOM（与 java-library 互斥），
    // 它有自己的 build 单独发布；根发布跳过
    if (name == "distribution" || path.startsWith(":examples:")) return@subprojects

    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "jacoco")

    configure<BasePluginExtension> {
        archivesName = "jvfault-${project.name}"
    }

    repositories {
        mavenCentral()
    }

    configure<JavaPluginExtension> {
        withSourcesJar()
        withJavadocJar()
    }

    // 构建期写入版本，运行时可读取（避免源码里硬编码版本号随迭代漂移）
    val generateBuildInfo = tasks.register("generateBuildInfo") {
        val outDir = layout.buildDirectory.dir("generated/build-info")
        val moduleVersion = project.version.toString()
        inputs.property("version", moduleVersion)
        outputs.dir(outDir)
        doLast {
            val dir = outDir.get().asFile
            dir.mkdirs()
            java.io.File(dir, "META-INF/jvfault-build.properties").apply {
                parentFile.mkdirs()
            }.writeText("version=$moduleVersion\n")
        }
    }

    tasks.withType<ProcessResources>().configureEach {
        from(generateBuildInfo)
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release = 8
        options.compilerArgs.addAll(listOf("-parameters", "-Xlint:all,-processing,-serial"))
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging {
            events("failed", "skipped")
            showStackTraces = true
            showExceptions = true
            showCauses = true
        }
        maxParallelForks = 2
    }

    tasks.withType<Javadoc>().configureEach {
        options.encoding = "UTF-8"
        (options as? StandardJavadocDocletOptions)?.apply {
            charSet = "UTF-8"
            docEncoding = "UTF-8"
            addStringOption("Xdoclint:none", "-quiet")
        }
    }

    configure<PublishingExtension> {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
                pom {
                    name.set("jvfault-${project.name}")
                    description.set("jvfault Framework module: ${project.name}")
                    url.set("https://github.com/jvfault/jvfault")
                    licenses {
                        license {
                            name.set("Apache License 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                }
            }
        }
    }
}

// distribution 模块自管（BOM 配置在 distribution/build.gradle.kts）
// 这里不需要额外的全局配置，避免 java-library/java-platform 冲突
