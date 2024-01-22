val runningGradle5 = gradle.gradleVersion.startsWith("5.")

val guavaVersionJre =
  "<version>(.*)</version>".toRegex().find(file("../../pom.xml").readText())?.groups?.get(1)?.value
    ?: error("version not found in pom")

val expectedReducedRuntimeClasspathAndroidVersion =
  setOf(
    "guava-${guavaVersionJre.replace("jre", "android")}.jar",
    "failureaccess-1.0.2.jar",
    "jsr305-3.0.2.jar",
    "checker-qual-3.42.0.jar",
    "error_prone_annotations-2.24.1.jar",
    "listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar"
  )
val expectedReducedRuntimeClasspathJreVersion =
  setOf(
    "guava-$guavaVersionJre.jar",
    "failureaccess-1.0.2.jar",
    "jsr305-3.0.2.jar",
    "checker-qual-3.42.0.jar",
    "error_prone_annotations-2.24.1.jar",
    "listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar"
  )
val expectedCompileClasspathAndroidVersion =
  expectedReducedRuntimeClasspathAndroidVersion + setOf("j2objc-annotations-2.8.jar")
val expectedCompileClasspathJreVersion =
  expectedReducedRuntimeClasspathJreVersion + setOf("j2objc-annotations-2.8.jar")

val extraLegacyDependencies = setOf("google-collections-1.0.jar")

buildscript {
  val agpVersion = if (gradle.gradleVersion.startsWith("5.")) "3.6.4" else "7.0.4"
  repositories {
    google()
    mavenCentral()
  }
  dependencies {
    classpath("com.android.tools.build:gradle:$agpVersion") {
      exclude(
        group = "org.jetbrains.trove4j"
      ) // Might not be available on Maven Central and not needed for this test
    }
  }
}

subprojects {
  if (name.endsWith("Java")) {
    apply(plugin = "java-library")
  } else {
    apply(plugin = "com.android.application")
    the<com.android.build.gradle.AppExtension>().compileSdkVersion(30)
  }

  var expectedClasspath =
    if (runningGradle5) {
      // without Gradle Module Metadata (only the POM is used)
      // - variant decision is made based on version suffix (android/jre) and not on the actual
      // environment
      // - runtime classpath equals the compile classpath
      // - dependency conflict with Google Collections is not detected
      if (name.startsWith("android")) {
        expectedCompileClasspathAndroidVersion + extraLegacyDependencies
      } else {
        expectedCompileClasspathJreVersion + extraLegacyDependencies
      }
    } else {
      // with Gradle Module Metadata
      // - variant is chosen based on the actual environment, independent of version suffix
      // - reduced runtime classpath is used (w/o annotation libraries)
      // - capability conflicts are detected with Google Collections
      if (name.contains("Android") && !name.contains("JreConstraint")) {
        when {
          name.contains("RuntimeClasspath") -> {
            expectedReducedRuntimeClasspathAndroidVersion
          }
          name.contains("CompileClasspath") -> {
            expectedCompileClasspathAndroidVersion
          }
          else -> {
            error("unexpected classpath type: $name")
          }
        }
      } else {
        when {
          name.contains("RuntimeClasspath") -> {
            expectedReducedRuntimeClasspathJreVersion
          }
          name.contains("CompileClasspath") -> {
            expectedCompileClasspathJreVersion
          }
          else -> {
            error("unexpected classpath type: $name")
          }
        }
      }
    }
  val guavaVersion =
    if (name.startsWith("android")) {
      guavaVersionJre.replace("jre", "android")
    } else {
      guavaVersionJre
    }
  val javaVersion = JavaVersion.VERSION_1_8

  repositories {
    mavenCentral()
    mavenLocal()
  }
  val java = the<JavaPluginExtension>()
  java.targetCompatibility = javaVersion
  java.sourceCompatibility = javaVersion

  if (!runningGradle5) {
    configurations.all {
      resolutionStrategy.capabilitiesResolution {
        withCapability("com.google.collections:google-collections") {
          candidates
            .find {
              val idField =
                it.javaClass.getDeclaredMethod(
                  "getId"
                ) // reflective access to make this compile with Gradle 5
              (idField.invoke(it) as ModuleComponentIdentifier).module == "guava"
            }
            ?.apply { select(this) }
        }
      }
    }

    if (name.contains("AndroidConstraint")) {
      dependencies {
        constraints {
          "api"("com.google.guava:guava") {
            attributes {
              // if the Gradle version is 7+, you can use
              // TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE
              attribute(Attribute.of("org.gradle.jvm.environment", String::class.java), "android")
            }
          }
        }
      }
      configurations.all {
        resolutionStrategy.capabilitiesResolution {
          withCapability("com.google.guava:guava") {
            candidates
              .find {
                val variantName = it.javaClass.getDeclaredMethod("getVariantName")
                (variantName.invoke(it) as String).contains("android")
              }
              ?.apply { select(this) }
          }
        }
      }
    }

    if (name.contains("JreConstraint")) {
      dependencies {
        constraints {
          "api"("com.google.guava:guava") {
            attributes {
              // if the Gradle version is 7+, you can use
              // TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE
              attribute(
                Attribute.of("org.gradle.jvm.environment", String::class.java),
                "standard-jvm"
              )
            }
          }
        }
      }
      configurations.all {
        resolutionStrategy.capabilitiesResolution {
          withCapability("com.google.guava:guava") {
            candidates
              .find {
                val variantName = it.javaClass.getDeclaredMethod("getVariantName")
                (variantName.invoke(it) as String).contains("jre")
              }
              ?.apply { select(this) }
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
      val classpathConfiguration =
        if (project.name.contains("RuntimeClasspath")) {
          if (project.name.endsWith("Java")) configurations["runtimeClasspath"]
          else configurations["debugRuntimeClasspath"]
        } else if (project.name.contains("CompileClasspath")) {
          if (project.name.endsWith("Java")) configurations["compileClasspath"]
          else configurations["debugCompileClasspath"]
        } else {
          error("unexpected classpath type: " + project.name)
        }

      val actualClasspath = classpathConfiguration.files.map { it.name }.toSet()
      if (actualClasspath != expectedClasspath) {
        throw RuntimeException(
          """
                    Expected: ${expectedClasspath.sorted()}
                    Actual:   ${actualClasspath.sorted()}
          """
            .trimIndent()
        )
      }
    }
  }
}
