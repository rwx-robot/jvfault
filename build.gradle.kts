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

    // JPMS：构件写入 Automatic-Module-Name（Java 8 侧）；
    // 若模块提供 src/main/java9/module-info.java，则再打**多版本 JAR**（Java 9+ 侧），
    // 描述符置于 META-INF/versions/9/、manifest 置 Multi-Release: true。
    // 主代码仍以 --release 8 编译，Java 8 基线与 JPMS 兼容性兼得。
    val moduleName = "com.jvfault." + project.name.replace('-', '.')
    val jarTask = tasks.named<Jar>("jar")
    val moduleInfo9 = file("src/main/java9/module-info.java")
    jarTask.configure {
        manifest {
            attributes["Automatic-Module-Name"] = moduleName
            if (moduleInfo9.exists()) {
                attributes["Multi-Release"] = "true"
            }
        }
    }
    if (moduleInfo9.exists()) {
        val mainClasses = extensions.getByType<org.gradle.api.tasks.SourceSetContainer>()
            .getByName("main").output.classesDirs
        // 依赖须以「JAR」形态上 module path（项目依赖默认解析为 classes 目录，无模块描述符）
        val deps = configurations.getByName("compileClasspath")
        val modulePath = deps.incoming.artifactView {
            attributes {
                attribute(
                    org.gradle.api.attributes.LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                    objects.named(
                        org.gradle.api.attributes.LibraryElements::class.java,
                        org.gradle.api.attributes.LibraryElements.JAR
                    )
                )
            }
        }.files
        val compileModuleInfo9 = tasks.register<JavaCompile>("compileJava9ModuleInfo") {
            source(moduleInfo9)
            destinationDirectory.set(layout.buildDirectory.dir("classes/java9"))
            classpath = files()
            options.release.set(9)
            options.encoding = "UTF-8"
            options.compilerArgs.addAll(listOf(
                "--patch-module", moduleName + "=" + mainClasses.asPath,
                "--module-path", modulePath.asPath
            ))
            dependsOn(tasks.named("classes"))
        }
        jarTask.configure {
            from(compileModuleInfo9.flatMap { it.destinationDirectory }) {
                into("META-INF/versions/9")
            }
        }
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
        // module-info 描述符需 --release 9（在多版本 JAR 任务里单独设定）；其余模块保持 Java 8 基线
        if (name != "compileJava9ModuleInfo") {
            options.release = 8
        }
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
