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

package com.google.common.io;

import junit.framework.TestCase;

import java.io.IOException;
import java.util.Date;

import static com.google.common.io.Prints.println;

/**
 * Tests for the default implementations of {@code Prints} methods. It asserts nothing but shows the usages.
 *
 * @author Leon Zeng
 */
public class PrintsTest  extends TestCase{
    public void testPrintln() throws IOException {
        println();
        println("");
        println(1);
        println("abc");
        println(new Date());
        println(null);
        println("%s=%d", "abc", 100);
        System.out.println(100);
    }
}
