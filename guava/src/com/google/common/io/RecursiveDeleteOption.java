/*
 * Copyright (C) 2014 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.io;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.j2objc.annotations.J2ObjCIncompatible;
import java.nio.file.SecureDirectoryStream;

/**
 * Options for use with recursive delete methods ({@link MoreFiles#deleteRecursively} and {@link
 * MoreFiles#deleteDirectoryContents}).
 *
 * @since 21.0
 * @author Colin Decker
 */
@J2ktIncompatible
@GwtIncompatible
@J2ObjCIncompatible // java.nio.file
@ElementTypesAreNonnullByDefault
public enum RecursiveDeleteOption {
  /**
   * Specifies that the recursive delete should not throw an exception when it can't be guaranteed
   * that it can be done securely, without vulnerability to race conditions (i.e. when the file
   * system does not support {@link SecureDirectoryStream}).
   *
   * <p><b>Warning:</b> On a file system that supports symbolic links, it is possible for an
   * insecure recursive delete to delete files and directories that are <i>outside</i> the directory
   * being deleted. This can happen if, after checking that a file is a directory (and not a
   * symbolic link), that directory is deleted and replaced by a symbolic link to an outside
   * directory before the call that opens the directory to read its entries. File systems that
   * support {@code SecureDirectoryStream} do not have this vulnerability.
   */
  ALLOW_INSECURE
}
