// Copyright 2012 Google Inc. All Rights Reserved

package java.nio.charset;

/**
 * GWT emulation of {@link UnsupportedCharsetException}.
 * 
 * @author gak@google.com (Gregory Kick)
 */
public class UnsupportedCharsetException extends IllegalArgumentException {
  private final String charsetName;
  
  public UnsupportedCharsetException(String charsetName) {
    super(String.valueOf(charsetName));
    this.charsetName = charsetName;
  }
  
  public String getCharsetName() {
    return charsetName;
  }
}
