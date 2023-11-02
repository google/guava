rootProject.name = "guava-integration-test"

include("standardJvmCompileClasspathJava")

include("androidCompileClasspathJava")

include("standardJvmRuntimeClasspathJava")

include("androidRuntimeClasspathJava")

include("standardJvmCompileClasspathAndroid")

include("androidCompileClasspathAndroid")

include("standardJvmRuntimeClasspathAndroid")

include("androidRuntimeClasspathAndroid")

// Enforce 'android' variant in Java projects via constraint

include("standardJvmAndroidConstraintCompileClasspathJava")

include("androidAndroidConstraintCompileClasspathJava")

// Enforce 'jre' variant in Android projects via constraint

include("standardJvmJreConstraintCompileClasspathAndroid")

include("androidJreConstraintCompileClasspathAndroid")
