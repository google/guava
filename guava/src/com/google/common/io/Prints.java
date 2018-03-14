/*
 * Copyright (C) 2006 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.io;

/**
 *
 * <p>Static methods of print and println, with which you can specify format, e.g.:</p>
 * <pre>
 *
 * println(1);                     //1
 * println("abc");                 //abc
 * println(new Date());            //Wed Mar 14 15:31:34 CST 2018
 * println(null);                  //null
 * println("%s=%d", "abc", 100);   //abc=100
 * </pre>
 * @author Leon Zeng
 * @since 24.0
 */
public class Prints {
    /**
     * print with format.
     *
     * @param objectOrFormat an object, or a formatting string
     * @param args arguments of formatting if the first argument is a formatting string
     */
    public static void print(Object objectOrFormat, Object... args) {
        if (objectOrFormat == null)
            System.out.print("null");
        else
            System.out.printf(objectOrFormat.toString(), args);
    }

    /**
     * println with format.
     *
     * @param objectOrFormat an object, or a formatting string
     * @param args arguments of formatting if the first argument is a formatting string
     */
    public static void println(Object objectOrFormat, Object... args) {
        print(objectOrFormat, args);
        System.out.println();
    }

    /**
     * The same as System.out.println().
     */
    public static void println() {
        System.out.println();
    }
}
