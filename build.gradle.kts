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

neoForge {
    version = libs.versions.neoforge.asProvider().get()

    parchment {
        minecraftVersion = libs.versions.parchment.minecraft.get()
        mappingsVersion = libs.versions.parchment.asProvider().get()
    }

    mods.create("radiocraft").sourceSet(project.sourceSets.getByName("main"))

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
}

val jade_version = "15.10.0+neoforge"
val top_version="1.21_neo-12.0.4-6"

dependencies {
    compileOnly(libs.mixinextras.common)
    compileOnly(libs.voicechat.api)
    implementation(libs.voicechat)
    implementation("mcjty.theoneprobe:theoneprobe:${top_version}")
    implementation("maven.modrinth:jade:${jade_version}")
    // GeckoLib for block/entity geo models (used for desk charger testing)
    // Use the NeoForge-specific artifact which includes the Minecraft version in the artifact id
    implementation("software.bernie.geckolib:geckolib-neoforge-${mcVersion}:4.7.5.1")
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