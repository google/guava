package com.google.common.testing;

import com.google.common.annotations.GwtCompatible;
import com.google.errorprone.annotations.ThreadSafe;
import java.io.Serializable;

/**
 * One of our per-package copies of {@link com.google.common.base.XplatMonitor}.
 */
@GwtCompatible
@ThreadSafe
@ElementTypesAreNonnullByDefault
final class XplatMonitor implements Serializable {}
