# Warning: common.reflect (like reflection in general) is typically slow and
# unreliable under Android. We do not recommend using it. This Proguard config
# exists only to avoid breaking the builds of users who already have
# common.reflect in their transitive dependencies.
#
-dontwarn com.google.common.reflect.Invokable
-dontwarn com.google.common.reflect.Invokable$ConstructorInvokable
-dontwarn com.google.common.reflect.Invokable$MethodInvokable
-dontwarn com.google.common.reflect.Parameter
