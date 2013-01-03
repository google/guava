/*
 * Copyright (C) 2012 The Guava Authors
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

package com.google.common.base;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;

/**
 * Contains constant definitions for the standard system property keys.
 *
 * @author Kurt Alfred Kluever
 * @since 14.0
 */
@Beta
@GwtCompatible(serializable = true)
public final class StandardSystemProperties {
  private StandardSystemProperties() {}

  /** Java Runtime Environment version. */
  public static final String JAVA_VERSION = "java.version";

  /** Java Runtime Environment vendor. */
  public static final String JAVA_VENDOR = "java.vendor";

  /** Java vendor URL. */
  public static final String JAVA_VENDOR_URL = "java.vendor.url";

  /** Java installation directory. */
  public static final String JAVA_HOME = "java.home";

  /** Java Virtual Machine specification version. */
  public static final String JAVA_VM_SPECIFICATION_VERSION = "java.vm.specification.version";

  /** Java Virtual Machine specification vendor. */
  public static final String JAVA_VM_SPECIFICATION_VENDOR = "java.vm.specification.vendor";

  /** Java Virtual Machine specification name. */
  public static final String JAVA_VM_SPECIFICATION_NAME = "java.vm.specification.name";

  /** Java Virtual Machine implementation version. */
  public static final String JAVA_VM_VERSION = "java.vm.version";

  /** Java Virtual Machine implementation vendor. */
  public static final String JAVA_VM_VENDOR = "java.vm.vendor";

  /** Java Virtual Machine implementation name. */
  public static final String JAVA_VM_NAME = "java.vm.name";

  /** Java Runtime Environment specification version. */
  public static final String JAVA_SPECIFICATION_VERSION = "java.specification.version";

  /** Java Runtime Environment specification vendor. */
  public static final String JAVA_SPECIFICATION_VENDOR = "java.specification.vendor";

  /** Java Runtime Environment specification name. */
  public static final String JAVA_SPECIFICATION_NAME = "java.specification.name";

  /** Java class format version number. */
  public static final String JAVA_CLASS_VERSION = "java.class.version";

  /** Java class path. */
  public static final String JAVA_CLASS_PATH = "java.class.path";

  /** List of paths to search when loading libraries. */
  public static final String JAVA_LIBRARY_PATH = "java.library.path";

  /** Default temp file path. */
  public static final String JAVA_IO_TMPDIR = "java.io.tmpdir";

  /** Name of JIT compiler to use. */
  public static final String JAVA_COMPILER = "java.compiler";

  /** Path of extension directory or directories. */
  public static final String JAVA_EXT_DIRS = "java.ext.dirs";

  /** Operating system name. */
  public static final String OS_NAME = "os.name";

  /** Operating system architecture. */
  public static final String OS_ARCH = "os.arch";

  /** Operating system version. */
  public static final String OS_VERSION = "os.version";

  /** File separator ("/" on UNIX). */
  public static final String FILE_SEPARATOR = "file.separator";

  /** Path separator (":" on UNIX). */
  public static final String PATH_SEPARATOR = "path.separator";

  /** Line separator ("\n" on UNIX). */
  public static final String LINE_SEPARATOR = "line.separator";

  /** User's account name. */
  public static final String USER_NAME = "user.name";

  /** User's home directory. */
  public static final String USER_HOME = "user.home";

  /** User's current working directory. */
  public static final String USER_DIR = "user.dir";
}
