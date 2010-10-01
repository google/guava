/*
 * Copyright (C) 2010 Google Inc.
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
package com.google.common.testing;

import junit.framework.TestCase;

/**
 * A test for the basic asserts.
 * 
 * @author cgruber@google.com (Christian Edward Gruber)
 *
 */
public class AssertTest extends TestCase {
  
  public void testStupidBoilerplateTestToMakeCoberturaShutUp() {
    new Assert();
  }

  public void testFail() {
    try {
      Assert.fail();
      throw new AssertionError("Failed to fail as expected.");
    } catch (AssertionFailedError e) {
      // success!
    }
  }

  public void testFailWithMessage() {
    try {
      Assert.fail("Message");
      throw new AssertionError("Failed to fail as expected.");
    } catch (AssertionFailedError e) {
      if (!e.getMessage().equals("Message")) {
        throw new AssertionError("Assert.fail threw exception but message was not the expected content.");
      }
    }
  }


  public void testAssertTrueSuccessfully() {
    Assert.assertTrue(true);
  }
  
  public void testAssertTrueSuccessfullyWithMessage() {
    Assert.assertTrue("This message should never appear.", true);
  }
  
  public void testAssertTrueButFail() {
    try {
      Assert.assertTrue(false);
      throw new AssertionError("Failed to throw AssertionFailed exception appropriately.");
    } catch (AssertionFailedError e) {
      // success!
    }
  }
  
  public void testAssertTrueButFailWithMessage() {
    try {
      Assert.assertTrue("Message", false);
      throw new AssertionError("Failed to throw AssertionFailed exception appropriately.");
    } catch (AssertionFailedError e) {
      if (!e.getMessage().equals("Message")) {
        throw new AssertionError("Assert.assertTrue threw exception but message was not the expected content.");
      }
    }
  }
  
  public void testAssertFalseSuccessfully() {
    Assert.assertFalse("This message should never appear.", false);
  }
  
  public void testAssertFalseSuccessfullyWithMessage() {
    Assert.assertFalse(false);
  }
  
  public void testAssertFalseSuccessfullyButFail() {
    try {
      Assert.assertFalse(true);
      throw new AssertionError("Failed to throw AssertionFailed exception appropriately.");
    } catch (AssertionFailedError e) {
      // success!
    }
  }
  
  public void testAssertFalseButFailWithMessage() {
    try {
      Assert.assertFalse("Message", true);
      throw new AssertionError("Failed to throw AssertionFailed exception appropriately.");
    } catch (AssertionFailedError e) {
      if (!e.getMessage().equals("Message")) {
        throw new AssertionError("Assert.assertTrue threw exception but message was not the expected content.");
      }
    }
  }

  public void testAssertNullAndSucceed() {
    Assert.assertNull(null);
  }
  
  public void testAssertNullAndFail() {
    try {
      Assert.assertNull(new Object());
    } catch (AssertionFailedError e) {
      // Success!
    }
  }

  public void testAssertNotNullAndSucceed() {
    Assert.assertNotNull(new Object());
  }
  
  public void testAssertNotNullAndFail() {
    try {
      Assert.assertNotNull(null);
    } catch (AssertionFailedError e) {
      // Success!
    }
  }
  
}
