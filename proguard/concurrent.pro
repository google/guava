# Futures.getChecked, in both of its variants, is incompatible with proguard.

# Used by AtomicReferenceFieldUpdater, sun.misc.Unsafe, and VarHandle.
# We could be more precise about which classes these are defined in, but that feels error-prone.
-keepclassmembers class com.google.common.util.concurrent.AbstractFuture** {
  *** waitersField;
  *** valueField;
  *** listenersField;
  *** thread;
  *** next;
}
-keepclassmembers class com.google.common.util.concurrent.AbstractFutureState** {
  *** waitersField;
  *** valueField;
  *** listenersField;
  *** thread;
  *** next;
}
-keepclassmembers class com.google.common.util.concurrent.AtomicDouble {
  *** value;
}
-keepclassmembers class com.google.common.util.concurrent.AggregateFutureState {
  *** remainingField;
  *** seenExceptionsField;
}

# Since Unsafe is using the field offsets of these inner classes, we don't want
# to have class merging or similar tricks applied to these classes and their
# fields. It's safe to allow obfuscation, since the by-name references are
# already preserved in the -keep statement above.
-keep,allowshrinking,allowobfuscation class com.google.common.util.concurrent.AbstractFuture** {
  <fields>;
}
-keep,allowshrinking,allowobfuscation class com.google.common.util.concurrent.AbstractFutureState** {
  <fields>;
}

# AbstractFuture uses this
-dontwarn sun.misc.Unsafe

# MoreExecutors references AppEngine
-dontnote com.google.appengine.api.ThreadManager
-keep class com.google.appengine.api.ThreadManager {
  static *** currentRequestThreadFactory(...);
}
-dontnote com.google.apphosting.api.ApiProxy
-keep class com.google.apphosting.api.ApiProxy {
  static *** getCurrentEnvironment (...);
}
