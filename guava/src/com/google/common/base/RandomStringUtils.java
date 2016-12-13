/*
 * Copyright (C) 2011 The Guava Authors
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
package com.google.common.base;

import java.util.Random;

/**
 * Provides API for random String generation
 * @author Petr Havlicek
 */

public class RandomStringUtils {
    
    private static final char ASCII_PRINT_START = 32;
    private static final char ASCII_PRINT_END = 126;
    private static final char ASCII_NUMERIC_START = 48;
    private static final char ASCII_NUMERIC_END = 57;
    
    /**
     * 
     * @param len length of the returned String
     * @param chars characters to be used - if chars contains non-alphabetic non-printable characters, they may appear in the returned String
     * @return String of length len from characters chars
     */
    public static String random(int len, char[] chars) {
        if (chars == null || chars.length == 0) {
            throw new IllegalArgumentException("The chars array must not be empty");
        }
        Random random = new Random();
        char[] res = new char[len];
        for (int i = 0; i < len; i++) {
            res[i] = chars[random.nextInt(chars.length)];
        }
        return new String(res);
    }

    /**
     * 
     * @param len length of the returned String
     * @param chars String od characters to be used - if chars contains non-alphabetic non-printable characters, they may appear in the returned String
     * @return String of length len from characters chars
     */
    public static String random(int len, String chars) {
        Random random = new Random();
        char[] res = new char[len];
        char[] c = new char[chars.length()];
        chars.getChars(0, c.length, c, 0);
        for (int i = 0; i < res.length; i++) {
            res[i] = c[random.nextInt(c.length)];
        }
        return new String(res);
    }
    
    /**
     *
     * @param len length of the returned String
     * @param start first usable character
     * @param end last usable character
     * @return String of length len from alphanumeric characters (Character.isAlphabetic == true) ranging from start to end
     */
    public static String randomAlphanumeric(int len, char start, char end) {
        if (start > end) {
            throw new IllegalArgumentException("Start char < end char!");
        }
        Random random = new Random();
        char[] res = new char[len];
        char gap = (char) (end - start);
        char ch;
        for (int i = 0; i < res.length; ) {
            ch = (char) (random.nextInt(gap) + start);
            if(Character.isAlphabetic(ch)){
                res[i] = ch;
                i++;
            }
        }
        return new String(res);
    }
    
    /**
     *
     * @param len length of the returned String
     * @param start first usable character
     * @param end last usable character
     * @return String of length len from numeric characters (Character.isDigit == true) ranging from start to end
     */
    public static String randomNumeric(int len, char start, char end) {
        if (start > end) {
            throw new IllegalArgumentException("Start char < end char!");
        }
        Random random = new Random();
        char[] res = new char[len];
        char gap = (char) (end - start);
        char ch;
        for (int i = 0; i < res.length; ) {
            ch = (char) (random.nextInt(gap) + start);
            if(Character.isDigit(ch)){
                res[i] = ch;
                i++;
            }
        }
        return new String(res);
    }
    
    /**
     *
     * @param len length of the returned String
     * @param start first usable character
     * @param end last usable character
     * @return String of length len from alphabetic characters (Character.isLetter == true) ranging from start to end
     */
    public static String randomAlphabetic(int len, char start, char end) {
        if (start > end) {
            throw new IllegalArgumentException("Start char < end char!");
        }
        Random random = new Random();
        char[] res = new char[len];
        char gap = (char) (end - start);
        char ch;
        for (int i = 0; i < res.length; ) {
            ch = (char) (random.nextInt(gap) + start);
            if(Character.isLetter(ch)){
                res[i] = ch;
                i++;
            }
        }
        return new String(res);
    }
    
    /**
     *
     * @param len length of the returned String
     * @param start first usable character
     * @param end last usable character
     * @return String of length len from characters ranging from start to end
     * NOTE: returned String may contain non-alphabetic non-printable characters
     */
    public static String random(int len, char start, char end) {
        if (start > end) {
            throw new IllegalArgumentException("Start char < end char!");
        }
        Random random = new Random();
        char[] res = new char[len];
        char gap = (char) (end - start);
        char ch;
        for (int i = 0; i < res.length; i++) {
            res[i] = (char) (random.nextInt(gap) + start);
        }
        return new String(res);
    }

    /**
     *
     * @param len length of the resturned String
     * @return String of length len from printable ASCII characters (32 - 127)
     */
    public static String randomAscii(int len) {
        return random(len, ASCII_PRINT_START, ASCII_PRINT_END);
    }
    
    /**
     * 
     * @param len length of the resturned String
     * @return String of length len from ASCII numerals
     */
    public static String randomAsciiNumeric(int len){
        return random(len, ASCII_NUMERIC_START, ASCII_NUMERIC_END);
    }
}
