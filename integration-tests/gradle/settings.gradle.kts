rootProject.name = "guava-integration-test"

val subprojects =
  listOf(
    "standardJvmCompileClasspathJava",
    "androidCompileClasspathJava",
    "standardJvmRuntimeClasspathJava",
    "androidRuntimeClasspathJava",
    "standardJvmCompileClasspathAndroid",
    "androidCompileClasspathAndroid",
    "standardJvmRuntimeClasspathAndroid",
    "androidRuntimeClasspathAndroid",
    "standardJvmAndroidConstraintCompileClasspathJava",
    "androidAndroidConstraintCompileClasspathJava",
    "standardJvmJreConstraintCompileClasspathAndroid",
    "androidJreConstraintCompileClasspathAndroid"
  )

for (p in subprojects) {
  include(p)
  file(p).mkdirs()
}
