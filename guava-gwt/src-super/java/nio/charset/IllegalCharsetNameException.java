// Copyright 2012 Google Inc. All Rights Reserved

package java.nio.charset;

/**
 * GWT emulation of {@link IllegalCharsetNameException}.
 *
 * @author gak@google.com (Gregory Kick)
 */
public class IllegalCharsetNameException extends IllegalArgumentException {
  private final String charsetName;

  public IllegalCharsetNameException(String charsetName) {
    super(String.valueOf(charsetName));
    this.charsetName = charsetName;
  }

  public String getCharsetName() {
    return charsetName;
  }
}
