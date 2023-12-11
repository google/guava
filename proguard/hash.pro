# LittleEndianByteArray uses this
-dontwarn sun.misc.Unsafe

# Striped64 appears to make some assumptions about object layout that
# really might not be safe. This should be investigated.
-keepclassmembers class com.google.common.hash.Striped64 {
  *** base;
  *** busy;
}
-keepclassmembers class com.google.common.hash.Striped64$Cell {
  <fields>;
}
