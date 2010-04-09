/*
 * Copyright (C) 2009 Google Inc.
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

package com.google.common.util.concurrent;

import com.google.common.base.Service;
import com.google.common.collect.ForwardingObject;

import java.util.concurrent.Future;

/**
 * A {@link Service} that forwards all method calls to another service.
 *
 * @author Chris Nokleberg
 * @since 2009.09.15 <b>tentative</b>
 */
public abstract class ForwardingService extends ForwardingObject
    implements Service {

  @Override protected abstract Service delegate();

  /*@Override*/ public Future<State> start() {
    return delegate().start();
  }

  /*@Override*/ public State state() {
    return delegate().state();
  }

  /*@Override*/ public Future<State> stop() {
    return delegate().stop();
  }

  /*@Override*/ public State startAndWait() {
    return delegate().startAndWait();
  }

  /*@Override*/ public State stopAndWait() {
    return delegate().stopAndWait();
  }

  /*@Override*/ public boolean isRunning() {
    return delegate().isRunning();
  }
}
