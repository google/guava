/*
 * Copyright 2016 KUBA.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.common.base;

import java.util.ArrayList;
import junit.framework.TestCase;

/**
 *
 * Unit test for {@link RandomStringUtils}.
 *
 * @author Petr Havlicek
 */
public class RandomStringUtilsTest extends TestCase {

    public void testRandomFromChars() {
        int len = 100;
        char[] cha = {'a', '5', 'č', '磨', '१'};
        ArrayList<Character> al = new ArrayList<>();
        for (int i = 0; i < cha.length; i++) {
            al.add(cha[i]);
        }
        String s = RandomStringUtils.random(len, cha);
        int slen = s.length();
        assertEquals(len, slen);
        for (int i = 0; i < slen; i++) {
            assertTrue(al.contains(s.charAt(i)));
        }
    }
    
    public void testRandomFromString(){
        int len = 100;
        String cha = "a5č磨१";
        String s = RandomStringUtils.random(len, cha);
        int slen = s.length();
        assertEquals(len, slen);
        for (int i = 0; i < slen; i++) {
            assertTrue(cha.indexOf(s.charAt(i)) >= 0);
        }
    }
    
    public void testRandomAlphanumeric(){
        int len = 100;
        char start = 30000;
        char end = 60000;
        String s = RandomStringUtils.randomAlphanumeric(len, start, end);
        int slen = s.length();
        assertEquals(len, slen);
        for (int i = 0; i < slen; i++) {
            char c = s.charAt(i);
            assertTrue(c >= start && c <= end && Character.isAlphabetic(c));
        }
    }
    
    public void testRandomAlphabetic(){
        int len = 100;
        char start = 30000;
        char end = 60000;
        String s = RandomStringUtils.randomAlphabetic(len, start, end);
        int slen = s.length();
        assertEquals(len, slen);
        for (int i = 0; i < slen; i++) {
            char c = s.charAt(i);
            assertTrue(c >= start && c <= end && Character.isLetter(c));
        }
    }
    
    public void testRandomNumeric(){
        int len = 100;
        char start = 30000;
        char end = 60000;
        String s = RandomStringUtils.randomNumeric(len, start, end);
        int slen = s.length();
        assertEquals(len, slen);
        for (int i = 0; i < slen; i++) {
            char c = s.charAt(i);
            assertTrue(c >= start && c <= end && Character.isDigit(c));
        }
    }
    
    public void testRandomAscii(){
        int len = 100;
        String s = RandomStringUtils.randomAscii(len);
        int slen = s.length();
        assertEquals(len, slen);
        for (int i = 0; i < slen; i++) {
            char c = s.charAt(i);
            assertTrue(c >= 32 && c <=126);
        }
    }
    
    public void testRandomAsciiNumeric(){
        int len = 100;
        String s = RandomStringUtils.randomAsciiNumeric(len);
        int slen = s.length();
        assertEquals(len, slen);
        for (int i = 0; i < slen; i++) {
            char c = s.charAt(i);
            assertTrue(c >= 48 && c <=57);
        }
    }
}
