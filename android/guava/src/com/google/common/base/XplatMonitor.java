package com.google.common.base;

import com.google.common.annotations.GwtCompatible;
import com.google.errorprone.annotations.ThreadSafe;
import java.io.Serializable;

/**
 * Using this class instead of Object will allow J2kt to substitute monitors in a way that works for
 * Kotlin native. This class is marked as Serializable so it can be "just" included in serializable
 * classes despite this not making much sense.
 */
@GwtCompatible
@ThreadSafe
@ElementTypesAreNonnullByDefault
final class XplatMonitor implements Serializable {}
