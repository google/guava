val guavaVersionJre = "<version>(.*)</version>".toRegex().find(file("../../pom.xml").readText())
  ?.groups?.get(1)?.value ?: error("version not found in pom")

val expectedReducedRuntimeClasspathJava6 = setOf(
  "guava-${guavaVersionJre.replace("jre", "android")}.jar",
  "failureaccess-1.0.1.jar",
  "jsr305-3.0.2.jar",
  "checker-compat-qual-2.5.5.jar",
  "error_prone_annotations-2.3.4.jar"
)
val expectedReducedRuntimeClasspathJava8 = setOf(
  "guava-$guavaVersionJre.jar",
  "failureaccess-1.0.1.jar",
  "jsr305-3.0.2.jar",
  "checker-qual-3.5.0.jar",
  "error_prone_annotations-2.3.4.jar"
)
val expectedCompileClasspathJava6 = expectedReducedRuntimeClasspathJava6 + setOf(
  "j2objc-annotations-1.3.jar"
)
val expectedCompileClasspathJava8 = expectedReducedRuntimeClasspathJava8 + setOf(
  "j2objc-annotations-1.3.jar"
)

val extraLegacyDependencies = setOf(
  "listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar",
  "google-collections-1.0.jar"
)

buildscript {
  val agpVersion = if (gradle.gradleVersion.startsWith("5.")) "3.6.4" else "7.0.0-alpha08"
  repositories {
    google()
    mavenCentral()
  }
  dependencies {
    classpath("com.android.tools.build:gradle:$agpVersion") {
      exclude(group = "org.jetbrains.trove4j") // Might not be available on Maven Central and not needed for this test
    }
  }
}

subprojects {
  if (name.endsWith("Java")) {
    apply(plugin = "java-library")
  } else {
    apply(plugin = "com.android.application")
    the<com.android.build.gradle.AppExtension>().compileSdkVersion(30)
    // === TODO Remove this when https://issuetracker.google.com/issues/179488433 is fixed in AGP 7.0.0
    configurations.whenObjectAdded {
      if (name in listOf("releaseRuntimeClasspath", "debugRuntimeClasspath", "releaseCompileClasspath", "debugCompileClasspath")) {
        attributes.attribute(Attribute.of("org.gradle.jvm.environment", String::class.java), "android")
      }
    }
    // ===
  }

  val expectedClasspath =
    if (gradle.gradleVersion.startsWith("5.")) {
      // without Gradle Module Metadata (only the POM is used)
      // - variant decision is made based on version suffix (android/jre) and not on actual Java version
      // - runtime classpath equals the compile classpath
      // - dependency conflict with Google Collections is not detected and '9999.0' hack is present
      if (name.startsWith("android")) {
        expectedCompileClasspathJava6 + extraLegacyDependencies
      } else {
        expectedCompileClasspathJava8 + extraLegacyDependencies
      }
    } else {
      // with Gradle Module Metadata
      // - variant is chosen based on Java version used independent of version suffix
      //   (for Android projects, the 'android' variant is always chosen)
      // - reduced runtime classpath is used (w/o annotation libraries)
      // - capability conflicts are detected between Google Collections and Listenablefuture
      if (name.contains("Java6") || (name.endsWith("Android") && !name.contains("Java8Constraint")) ) {
        when {
            name.contains("RuntimeClasspath") -> {
              expectedReducedRuntimeClasspathJava6
            }
            name.contains("CompileClasspath") -> {
              expectedCompileClasspathJava6
            }
            else -> {
              error("unexpected classpath type: $name")
            }
        }
      } else {
        when {
            name.contains("RuntimeClasspath") -> {
              expectedReducedRuntimeClasspathJava8
            }
            name.contains("CompileClasspath") -> {
              expectedCompileClasspathJava8
            }
            else -> {
              error("unexpected classpath type: $name")
            }
        }
      }
    }
  val guavaVersion = if (name.startsWith("jre")) {
    guavaVersionJre
  } else {
    guavaVersionJre.replace("jre", "android")
  }
  val javaVersion = when {
      name.contains("Java8Constraint") -> {
        JavaVersion.VERSION_1_6
      }
      name.contains("Java6Constraint") -> {
        JavaVersion.VERSION_1_8
      }
      name.contains("Java6") -> {
        JavaVersion.VERSION_1_6
      }
      else -> {
        JavaVersion.VERSION_1_8
      }
  }

  repositories {
    mavenCentral()
    mavenLocal()
  }
  val java = the<JavaPluginExtension>()
  java.targetCompatibility = javaVersion
  java.sourceCompatibility = javaVersion

  if (!gradle.gradleVersion.startsWith("5.")) {
    configurations.all {
      resolutionStrategy.capabilitiesResolution {
        withCapability("com.google.collections:google-collections") {
          candidates.find {
            val idField = it.javaClass.getDeclaredMethod("getId") // reflective access to make this compile with Gradle 5
            (idField.invoke(it) as ModuleComponentIdentifier).module == "guava"
          }?.apply {
            select(this)
          }
        }
        withCapability("com.google.guava:listenablefuture") {
          candidates.find {
            val idField = it.javaClass.getDeclaredMethod("getId") // reflective access to make this compile with Gradle 5
            (idField.invoke(it) as ModuleComponentIdentifier).module == "guava"
          }?.apply {
            select(this)
          }
        }
      }
    }

    if (name.contains("Java6Constraint")) {
      dependencies {
        constraints {
          "api"("com.google.guava:guava") {
            attributes {
              // if the Gradle version is 7+, you can use TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE
              attribute(Attribute.of("org.gradle.jvm.environment", String::class.java), "android")
            }
          }
        }
      }
      configurations.all {
        resolutionStrategy.capabilitiesResolution {
          withCapability("com.google.guava:guava") {
            candidates.find {
              val variantName = it.javaClass.getDeclaredMethod("getVariantName")
              (variantName.invoke(it) as String).contains("6")
            }?.apply {
              select(this)
            }
          }
        }
      }
    }

    if (name.contains("Java8Constraint")) {
      dependencies {
        constraints {
          "api"("com.google.guava:guava") {
            attributes {
              // if the Gradle version is 7+, you can use TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE
              attribute(Attribute.of("org.gradle.jvm.environment", String::class.java), "standard-jvm")
            }
          }
        }
      }
      configurations.all {
        resolutionStrategy.capabilitiesResolution {
          withCapability("com.google.guava:guava") {
            candidates.find {
              val variantName = it.javaClass.getDeclaredMethod("getVariantName")
              (variantName.invoke(it) as String).contains("8")
            }?.apply {
              select(this)
            }
          }
        }
      }
    }
  }

  dependencies {
    "api"("com.google.collections:google-collections:1.0")
    "api"("com.google.guava:listenablefuture:1.0")
    "api"("com.google.guava:guava:$guavaVersion")
  }

  tasks.register("testClasspath") {
    doLast {
      val classpathConfiguration = if (project.name.contains("RuntimeClasspath")) {
        if (project.name.endsWith("Java")) configurations["runtimeClasspath"] else configurations["debugRuntimeClasspath"]
      } else if (project.name.contains("CompileClasspath")) {
        if (project.name.endsWith("Java")) configurations["compileClasspath"] else configurations["debugCompileClasspath"]
      } else {
        error("unexpected classpath type: " + project.name)
      }

      val actualClasspath = classpathConfiguration.files.map { it.name }.toSet()
      if (actualClasspath != expectedClasspath) {
        throw RuntimeException(
          """
                    Expected: ${expectedClasspath.sorted()}
                    Actual:   ${actualClasspath.sorted()}
          """.trimIndent()
        )
      }
    }
  }
}
