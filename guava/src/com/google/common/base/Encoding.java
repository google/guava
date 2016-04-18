package com.google.common.base;

import static java.lang.Character.MAX_SURROGATE;
import static java.lang.Character.MIN_SURROGATE;

public class Encoding {

	public static String unpairedSurrogateMsg(int i) {
	    return "Unpaired surrogate at index " + i;
	  }

	public static int encodedLengthGeneral(CharSequence sequence, int start) {
	    int utf16Length = sequence.length();
	    int utf8Length = 0;
	    for (int i = start; i < utf16Length; i++) {
	      char c = sequence.charAt(i);
	      if (c < 0x800) {
	        utf8Length += (0x7f - c) >>> 31; // branch free!
	      } else {
	        utf8Length += 2;
	        // jdk7+: if (Character.isSurrogate(c)) {
	        if (MIN_SURROGATE <= c && c <= MAX_SURROGATE) {
	          // Check that we have a well-formed surrogate pair.
	          if (Character.codePointAt(sequence, i) == c) {
	            throw new IllegalArgumentException(unpairedSurrogateMsg(i));
	          }
	          i++;
	        }
	      }
	    }
	    return utf8Length;
	  }

	public Encoding() {
		super();
	}

}