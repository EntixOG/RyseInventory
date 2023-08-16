group = "io.github.rysefoxx.inventory.plugin"
description = "RyseInventory"

repositories {
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")

    maven("https://oss.sonatype.org/content/repositories/snapshots")

    maven("https://repo.codemc.io/repository/maven-snapshots/")
}

dependencies {
    implementation(project(":v1_19"))
    implementation(project(":v1_18"))
    implementation(project(":v1_17"))
    implementation(project(":v1_16"))
    implementation(project(":api"))
    implementation("net.wesjd:anvilgui:1.6.4-SNAPSHOT")

    compileOnly("net.kyori:adventure-platform-bukkit:4.2.0")
    compileOnly("org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT")
    compileOnly("org.apache.commons:commons-lang3:3.12.0")
    compileOnly("org.jetbrains:annotations:23.1.0")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")

    annotationProcessor("org.projectlombok:lombok:1.18.24")
    compileOnly("org.projectlombok:lombok:1.18.24")
}

publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            groupId = "io.github.rysefoxx.inventory"
            artifactId = "RyseInventory-Plugin"
            version = "${project.version}"

            pom {
                name = "RyseInventory"
                packaging = "jar"
                description = "Inventory System"
                url = "https://github.com/Rysefoxx/RyseInventory"
            }
            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "nexus"
            url = uri("https://repo.networkmanager.xyz/repository/maven-networkmanager/")
            credentials {
                username = project.property("NEXUS_USERNAME").toString()
                password = project.property("NEXUS_PASSWORD").toString()
            }
        }
    }
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        relocate("net.wesjd.anvilgui", "io.github.rysefoxx.inventory.anvilgui")
        exclude("io/github/rysefoxx/inventory/plugin/ItemBuilder.class")
    }

    build {
        dependsOn(shadowJar)
    }
}