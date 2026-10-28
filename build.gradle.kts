plugins {
    `java`
    `maven-publish`
    `idea`
    `eclipse`

    alias(libs.plugins.moddevgradle)
}

/**
 * Determines the version from Git tags following semantic versioning.
 * If the current commit is tagged and clean, uses the tag as version.
 * If dirty (uncommitted changes), appends "-dirty" to the version.
 * If not on a tagged commit, uses the latest tag with commit distance and hash.
 */
fun getVersionFromGit(): String {
    return try {
        val gitDescribe = executeCommand("git", "describe", "--tags", "--long", "--dirty", "--always")

        when {
            gitDescribe.contains("-dirty") -> {
                // Clean up the describe output and add dirty suffix
                val cleanVersion = gitDescribe.replace("-dirty", "")
                parseGitDescribe(cleanVersion) + "-dirty"
            }
            gitDescribe.matches(Regex("^v?\\d+\\.\\d+\\.\\d+$")) -> {
                // We're exactly on a tagged commit, clean state
                gitDescribe.removePrefix("v")
            }
            else -> {
                // We're not on a tagged commit or it's a complex describe output
                parseGitDescribe(gitDescribe)
            }
        }
    } catch (e: Exception) {
        logger.warn("Failed to get version from git: ${e.message}")
        "unknown"
    }
}

fun parseGitDescribe(describe: String): String {
    // Parse git describe output like "v1.0.0-5-g1234567" or "1.0.0-5-g1234567"
    val parts = describe.split("-")
    return when {
        parts.size >= 3 -> {
            val tag = parts[0].removePrefix("v")
            val distance = parts[1].toIntOrNull() ?: 0
            val hash = parts[2]
            if (distance > 0) {
                "$tag-dev.$distance+$hash"
            } else {
                tag
            }
        }
        else -> describe.removePrefix("v")
    }
}

fun executeCommand(vararg command: String): String {
    val process = ProcessBuilder(*command)
        .directory(rootDir)
        .start()

    val output = process.inputStream.bufferedReader().readText().trim()
    val exitCode = process.waitFor()

    if (exitCode != 0) {
        val error = process.errorStream.bufferedReader().readText()
        throw RuntimeException("Command failed with exit code $exitCode: $error")
    }

    return output
}

// Set version from Git
version = getVersionFromGit()

val mcVersion = libs.versions.minecraft.asProvider().get()
val mcVersionRange = libs.versions.minecraft.range.get()
val neoVersion = libs.versions.neoforge.asProvider().get()
val neoLoaderRange = libs.versions.neoforge.loader.range.get()
val neoVersionRange = libs.versions.neoforge.range.get()

base {
    archivesName = "radiocraft-neoforge-${mcVersion}"
}

// In-world GameTests live in their own source set so they never ship in the mod jar.
// They are attached to the mod for dev runs only (see doc/Testing.md).
val gametest: SourceSet = sourceSets.create("gametest") {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}
configurations[gametest.compileClasspathConfigurationName].extendsFrom(configurations.compileClasspath.get())
configurations[gametest.runtimeClasspathConfigurationName].extendsFrom(configurations.runtimeClasspath.get())

neoForge {
    version = libs.versions.neoforge.asProvider().get()

    parchment {
        minecraftVersion = libs.versions.parchment.minecraft.get()
        mappingsVersion = libs.versions.parchment.asProvider().get()
    }

    mods.create("radiocraft") {
        sourceSet(project.sourceSets.getByName("main"))
        sourceSet(gametest)
    }

    // Puts Minecraft/NeoForge on the unit test classpath (plain JUnit, no game bootstrap).
    addModdingDependenciesTo(sourceSets.test.get())

    runs {
        configureEach {
            logLevel = org.slf4j.event.Level.DEBUG
        }

        create("client") {
            environment("RADIOCRAFT_DEV_ENV", "true")
            client()
            gameDirectory = file("runs/client")
        }

        create("client2") {
            environment("RADIOCRAFT_DEV_ENV", "true")
            client()
            gameDirectory = file("runs/client")
            programArguments.addAll(
                "--username","Dev2"
            )
        }

        create("server") {
            environment("RADIOCRAFT_DEV_ENV", "true")
            server()
            gameDirectory = file("runs/server")
            programArgument("--nogui")
        }

        create("data") {
            data()
            gameDirectory = file("runs/data")
            programArguments.addAll(
                "--mod", "radiocraft", "--all",
                "--output", file("src/generated/resources/").absolutePath,
                "--existing", file("src/main/resources/").absolutePath
            )
        }

        // Headless server that runs every @GameTest in the "radiocraft" namespace, then exits
        // with a non-zero code if a required test failed. Use: ./gradlew runGameTestServer
        create("gameTestServer") {
            environment("RADIOCRAFT_DEV_ENV", "true")
            type = "gameTestServer"
            gameDirectory = file("runs/gametest")
            systemProperty("neoforge.enabledGameTestNamespaces", "radiocraft")
            logLevel = org.slf4j.event.Level.INFO
        }

    }
}

sourceSets.main.get().resources.srcDir(file("src/generated/resources"))

repositories {
    maven("https://maven.maxhenkel.de/repository/public") { name = "VoiceChat Maven" }
    maven("https://cursemaven.com") { name = "CurseMaven" }
    maven ("https://api.modrinth.com/maven") { name = "Modrinth" } // Used by Jade
    maven ("https://maven.blamejared.com") { name = "JEI Maven" } // Dependency of TOP
    maven ("https://maven.k-4u.nl") { name = "TOP Maven" }
    // GeckoLib Cloudsmith repository (contains geckolib-neoforge artifacts)
    maven("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/") { name = "GeckoLib Cloudsmith" }
    maven("https://maven.theillusivec4.top/") { name = "Curios Maven" }
    maven("https://maven.createmod.net") { name = "Create Maven" } // Create, Ponder, Flywheel
    maven("https://mvn.devos.one/snapshots") { name = "Registrate Maven" } // Registrate, which Create needs at runtime
    // Sable and Sable Companion (moving block structures: airships, boats). Only these two groups come from here.
    exclusiveContent {
        forRepository { maven("https://maven.ryanhcode.dev/releases") { name = "Sable Maven" } }
        filter {
            includeGroup("dev.ryanhcode.sable")
            includeGroup("dev.ryanhcode.sable-companion")
        }
    }
}

val jade_version = "15.10.0+neoforge"
val top_version="1.21_neo-12.0.4-6"
val jei_version="19.9.1.125" // Same JEI as TOP brings in at runtime
val geckolib_version = "4.7.5.1"
val patchouli_version = "1.21.1-92-NEOFORGE"
val curios_version = "9.5.1+1.21.1"
val create_version = "6.0.10-280"
val ponder_version = "1.0.82"
val flywheel_version = "1.0.6"
val registrate_version = "MC1.21-1.3.0+67"
val sable_companion_version = "1.6.0"
val sable_version = "2.0.6"
val veil_version = "4.3.2"

dependencies {
    compileOnly(libs.mixinextras.common)
    compileOnly(libs.voicechat.api)
    implementation(libs.voicechat)
    implementation("mcjty.theoneprobe:theoneprobe:${top_version}")
    compileOnly("mezz.jei:jei-1.21.1-neoforge-api:${jei_version}") // JEI API from the JEI Maven (maven.blamejared.com), for client/compat
    implementation("maven.modrinth:jade:${jade_version}")
    // GeckoLib for block/entity geo models
    implementation("software.bernie.geckolib:geckolib-neoforge-${mcVersion}:${geckolib_version}")
    // Patchouli, for the guide book (assets/radiocraft/patchouli_books). Optional for players: the book only exists
    // when they install it. Runtime only, so dev runs can open the book. From maven.blamejared.com (declared above).
    runtimeOnly("vazkii.patchouli:Patchouli:${patchouli_version}")
    // Curios, for wearing the handheld (and a spare battery) on the belt. Optional: RadioCraft only talks to it when it
    // is installed (common/compat/CuriosCompat). From maven.theillusivec4.top (declared above).
    compileOnly("top.theillusivec4.curios:curios-neoforge:${curios_version}:api")
    runtimeOnly("top.theillusivec4.curios:curios-neoforge:${curios_version}")
    // Another Weather Mod (MIT, https://github.com/ITCMD/AnotherWeatherMod), localized storms for WX reports and WEFAX
    // charts. Optional for players: compat/anotherweathermod only runs when it is installed. The jar is kept in libs/
    // until the mod is on a Maven (Modrinth); it is on the runtime classpath too, so dev runs have localized weather.
    compileOnly(files("libs/anotherweathermod-neoforge-1.21.1-0.1.0.jar"))
    runtimeOnly(files("libs/anotherweathermod-neoforge-1.21.1-0.1.0.jar"))

    // JOrbis (LGPL, Maven Central), to decode the server's tape .ogg files into audio on a dedicated server, where
    // Minecraft's own Vorbis decoder (LWJGL) is not present. Bundled in the mod jar.
    implementation("org.jcraft:jorbis:0.0.17")
    jarJar("org.jcraft:jorbis:[0.0.17,0.1)")
    additionalRuntimeClasspath("org.jcraft:jorbis:0.0.17")

    // Create (https://github.com/Creators-of-Create/Create), for the alternator: compat/create only loads when Create is
    // installed and Create Crafts & Additions is not (it has its own). From maven.createmod.net (declared above).
    // Dev runs leave Create out unless asked: ./gradlew runClient -PwithCreate (or runGameTestServer -PwithCreate).
    compileOnly("com.simibubi.create:create-${mcVersion}:${create_version}:slim") { isTransitive = false }
    compileOnly("net.createmod.ponder:ponder-neoforge:${ponder_version}+mc${mcVersion}") { isTransitive = false }
    compileOnly("dev.engine-room.flywheel:flywheel-neoforge-api-${mcVersion}:${flywheel_version}")
    if(project.hasProperty("withCreate")) {
        runtimeOnly("com.simibubi.create:create-${mcVersion}:${create_version}:slim") { isTransitive = false }
        runtimeOnly("net.createmod.ponder:ponder-neoforge:${ponder_version}+mc${mcVersion}") { isTransitive = false }
        runtimeOnly("dev.engine-room.flywheel:flywheel-neoforge-${mcVersion}:${flywheel_version}")
        runtimeOnly("com.tterrag.registrate:Registrate:${registrate_version}")
    }

    // Sable Companion (MIT, https://github.com/ryanhcode/sable-companion), from maven.ryanhcode.dev (declared above).
    // Bundled in the mod jar: without Sable it returns every position unchanged; with Sable installed it maps blocks on a
    // moving sub-level (airship, boat) to where they really are, so radios on ships keep working (compat/sable/Sublevels).
    implementation("dev.ryanhcode.sable-companion:sable-companion-common-${mcVersion}:${sable_companion_version}")
    jarJar("dev.ryanhcode.sable-companion:sable-companion-common-${mcVersion}:[${sable_companion_version},)") {
        version { prefer(sable_companion_version) }
    }
    // Sable's API (PolyForm Shield), compile only and never bundled: mixin/MixinSableAssembly carries antenna wires onto
    // a new ship, and is skipped (@Pseudo) when Sable is not installed.
    compileOnly("dev.ryanhcode.sable:sable-common-${mcVersion}:${sable_version}") { isTransitive = false }
    // Sable itself only in dev runs that ask for it: ./gradlew runClient -PwithSable. It needs Veil
    // (maven.blamejared.com) and its Rapier physics natives.
    if(project.hasProperty("withSable")) {
        runtimeOnly("dev.ryanhcode.sable:sable-neoforge-${mcVersion}:${sable_version}") { isTransitive = false }
        runtimeOnly("dev.ryanhcode.sable:sable-sable_rapier-${mcVersion}:${sable_version}") { isTransitive = false }
        runtimeOnly("foundry.veil:veil-neoforge-${mcVersion}:${veil_version}") { isTransitive = false }
    }

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    // Loading Minecraft classes initializes log4j, which writes logs/ into the working directory.
    val testRunDir = layout.buildDirectory.dir("test-run").get().asFile
    workingDir = testRunDir
    doFirst { testRunDir.mkdirs() }
    testLogging {
        events("failed", "skipped")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.SHORT
    }
}

tasks.withType<ProcessResources>().configureEach {
    val expandProps = mapOf(
        "version" to version,
        "minecraft_version" to mcVersion,
        "neo_version_range" to neoVersionRange,
        "loader_version_range" to neoLoaderRange,
        "minecraft_version_range" to mcVersionRange
    )

    filesMatching(listOf("pack.mcmeta", "META-INF/neoforge.mods.toml", "*.mixins.json")) {
        expand(expandProps)
    }

    inputs.properties(expandProps)
}


tasks.withType<Jar>().configureEach {
    from(rootProject.file("LICENSE")) {
        rename { "${it}_RadioCraft" }
    }

    manifest {
        attributes(mapOf(
            "Specification-Title"     to "RadioCraft",
            "Specification-Vendor"    to "The American Radio Relay League",
            "Specification-Version"   to version,
            "Implementation-Title"    to "RadioCraft",
            "Implementation-Version"  to version,
            "Implementation-Vendor"   to "The RadioCraft Developers",
            "Built-On-Minecraft"      to mcVersion
        ))
    }
}

tasks.withType<JavaCompile>().configureEach {
    this.options.encoding = "UTF-8"
    this.options.getRelease().set(21)
    this.options.compilerArgs.add("-Xlint:all")
}

tasks.withType<Javadoc>().configureEach {
    this.include("**/api/**/*.java")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)

    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("radiocraft") {
            from(components["java"])
            artifactId = base.archivesName.get()
        }
    }
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}

// Optional: Add a task to print the current version
tasks.register("printVersion") {
    doLast {
        println("Current version: ${version}")
    }
}