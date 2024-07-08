group = "io.github.rysefoxx.inventory.plugin"
description = "RyseInventory"

repositories {
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")

    maven("https://oss.sonatype.org/content/repositories/snapshots")

    maven("https://repo.codemc.io/repository/maven-snapshots/")

    maven("https://mvn-repo.arim.space/lesser-gpl3/") // MorePaperLib
}

dependencies {
    implementation(project(":v1_19"))
    implementation(project(":v1_18"))
    implementation(project(":v1_17"))
    implementation(project(":v1_16"))
    implementation(project(":api"))
    implementation("net.wesjd:anvilgui:1.9.2-SNAPSHOT")
    implementation("space.arim.morepaperlib:morepaperlib:0.4.4")
    implementation("com.github.cryptomorin:XSeries:11.0.0") { isTransitive = false }

    compileOnly("net.kyori:adventure-platform-bukkit:4.3.1")
    compileOnly("org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT")
    compileOnly("org.apache.commons:commons-lang3:3.12.0")
    compileOnly("org.jetbrains:annotations:24.0.1")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")

    annotationProcessor("org.projectlombok:lombok:1.18.34")
    compileOnly("org.projectlombok:lombok:1.18.34")
}

publishing {
    publications {
        create<MavenPublication>("shadow") {
            groupId = "io.github.rysefoxx.inventory"
            artifactId = "RyseInventory-Plugin"
            version = "${project.version}"

            pom {
                name = "RyseInventory"
                packaging = "jar"
                description = "Inventory System"
                url = "https://github.com/Rysefoxx/RyseInventory"
            }
            project.extensions.configure<com.github.jengelman.gradle.plugins.shadow.ShadowExtension> {
                component(this@create)
            }
        }
    }

}

tasks {
    shadowJar {
        minimize()

        archiveClassifier.set("")
        relocate("net.wesjd.anvilgui", "io.github.rysefoxx.inventory.anvilgui")
        relocate("space.arim.morepaperlib", "io.github.rysefoxx.inventory.morepaperlib")
        relocate("com.cryptomorin.xseries", "io.github.rysefoxx.inventory.xseries")
        exclude("io/github/rysefoxx/inventory/plugin/ItemBuilder.class")
    }

    build {
        dependsOn(shadowJar)
    }
}