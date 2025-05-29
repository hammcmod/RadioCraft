plugins {
    `java`
    `maven-publish`
    `idea`
    `eclipse`

    alias(libs.plugins.moddevgradle)
}


version = libs.versions.radiocraft.get()
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
}

dependencies {
    compileOnly(libs.mixinextras.common)
    compileOnly(libs.voicechat.api)
    implementation(libs.voicechat)
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
