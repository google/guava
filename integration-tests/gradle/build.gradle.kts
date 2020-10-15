val guavaVersionJre = "<version>(.*)</version>".toRegex().find(file("../../pom.xml").readText())
  ?.groups?.get(1)?.value ?: error("version not found in pom")

val expectedReducedRuntimeClasspathJava6 = setOf(
  "guava-${guavaVersionJre.replace("jre", "android")}.jar",
  "failureaccess-1.0.1.jar"
)
val expectedReducedRuntimeClasspathJava8 = setOf(
  "guava-${guavaVersionJre}.jar",
  "failureaccess-1.0.1.jar"
)
val expectedCompileClasspathJava6 = expectedReducedRuntimeClasspathJava6 + setOf(
  "jsr305-3.0.2.jar",
  "checker-compat-qual-2.5.5.jar",
  "error_prone_annotations-2.3.4.jar",
  "j2objc-annotations-1.3.jar"
)
val expectedCompileClasspathJava8 = expectedReducedRuntimeClasspathJava8 + setOf(
  "jsr305-3.0.2.jar",
  "checker-qual-3.5.0.jar",
  "error_prone_annotations-2.3.4.jar",
  "j2objc-annotations-1.3.jar"
)

val extraLegacyDependencies = setOf(
  "listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar",
  "google-collections-1.0.jar"
)

subprojects {
  apply(plugin = "java-library")

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
      // - reduced runtime classpath is used (w/o annotation libraries)
      // - capability conflicts are detected between Google Collections and Listenablefuture
      if (name.contains("Java6")) {
        if (name.contains("RuntimeClasspath")) {
          expectedReducedRuntimeClasspathJava6
        } else if (name.contains("CompileClasspath")) {
          expectedCompileClasspathJava6
        } else {
          error("unexpected classpath type: " + name)
        }
      } else {
        if (name.contains("RuntimeClasspath")) {
          expectedReducedRuntimeClasspathJava8
        } else if (name.contains("CompileClasspath")) {
          expectedCompileClasspathJava8
        } else {
          error("unexpected classpath type: " + name)
        }
      }
    }
  val guavaVersion = if (name.startsWith("jre")) {
    guavaVersionJre
  } else {
    guavaVersionJre.replace("jre", "android")
  }
  val javaVersion = if (name.contains("Java6")) {
    JavaVersion.VERSION_1_6
  } else {
    JavaVersion.VERSION_1_8
  }
  val classpathConfiguration = if (name.contains("RuntimeClasspath")) {
    configurations["runtimeClasspath"]
  } else if (name.contains("CompileClasspath")) {
    configurations["compileClasspath"]
  } else {
    error("unexpected classpath type: " + name)
  }

  repositories {
    mavenCentral()
    mavenLocal()
  }
  val java = extensions.getByType<JavaPluginExtension>()
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
  }

  dependencies {
    "api"("com.google.collections:google-collections:1.0")
    "api"("com.google.guava:listenablefuture:1.0")
    "api"("com.google.guava:guava:$guavaVersion")
  }

  tasks.register("testClasspath") {
    doLast {
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
