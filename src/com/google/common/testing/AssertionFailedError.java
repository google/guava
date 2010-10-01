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

import com.google.common.annotations.Beta;


/**
 * A simple assertion failure error used in testing to isolate Guava 
 * testing from JUnit or TestNG or any other framework.  It is a 
 * direct descendant of AssertionError.
 *
 * @author cgruber@google.com (Christian Edward Gruber)
 * @since r08
 */
@Beta
public class AssertionFailedError extends AssertionError {

	private static final long serialVersionUID = -221922462818431635L;

	public AssertionFailedError() {
	}

	public AssertionFailedError(String message) {
		super(message);
	}
}