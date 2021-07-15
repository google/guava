rootProject.name = "guava-integration-test"

include("standardJvmJava8CompileClasspathJava")
include("androidJava8CompileClasspathJava")
include("standardJvmJava8RuntimeClasspathJava")
include("androidJava8RuntimeClasspathJava")

include("standardJvmJava6CompileClasspathJava")
include("androidJava6CompileClasspathJava")
include("standardJvmJava6RuntimeClasspathJava")
include("androidJava6RuntimeClasspathJava")

include("standardJvmJava8CompileClasspathAndroid")
include("androidJava8CompileClasspathAndroid")
include("standardJvmJava8RuntimeClasspathAndroid")
include("androidJava8RuntimeClasspathAndroid")

include("standardJvmJava6CompileClasspathAndroid")
include("androidJava6CompileClasspathAndroid")
include("standardJvmJava6RuntimeClasspathAndroid")
include("androidJava6RuntimeClasspathAndroid")

// Enforce 'android' variant in Java projects via constraint
include("standardJvmAndroidConstraintCompileClasspathJava")
include("androidAndroidConstraintCompileClasspathJava")

// Enforce 'jre' variant in Android projects via constraint
include("standardJvmJreConstraintCompileClasspathAndroid")
include("androidJreConstraintCompileClasspathAndroid")
