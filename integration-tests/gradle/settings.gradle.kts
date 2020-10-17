rootProject.name = "guava-integration-test"

include("jreJava8CompileClasspathJava")
include("androidJava8CompileClasspathJava")
include("jreJava8RuntimeClasspathJava")
include("androidJava8RuntimeClasspathJava")

include("jreJava6CompileClasspathJava")
include("androidJava6CompileClasspathJava")
include("jreJava6RuntimeClasspathJava")
include("androidJava6RuntimeClasspathJava")

include("jreJava8CompileClasspathAndroid")
include("androidJava8CompileClasspathAndroid")
include("jreJava8RuntimeClasspathAndroid")
include("androidJava8RuntimeClasspathAndroid")

include("jreJava6CompileClasspathAndroid")
include("androidJava6CompileClasspathAndroid")
include("jreJava6RuntimeClasspathAndroid")
include("androidJava6RuntimeClasspathAndroid")

// Enforce 'android' variant in Java projects via constraint
include("jreJava6ConstraintCompileClasspathJava")
include("androidJava6ConstraintCompileClasspathJava")

// Enforce 'jre' variant in Android projects via constraint
include("jreJava8ConstraintCompileClasspathAndroid")
include("androidJava8ConstraintCompileClasspathAndroid")
