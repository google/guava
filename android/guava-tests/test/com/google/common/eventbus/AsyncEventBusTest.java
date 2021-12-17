/*
 * Copyright (C) 2007 The Guava Authors
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

package com.google.common.eventbus;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.concurrent.Executor;
import junit.framework.TestCase;

/**
 * Test case for {@link AsyncEventBus}.
 *
 * @author Cliff Biffle
 */
public class AsyncEventBusTest extends TestCase {
  private static final String EVENT = "Hello";

  /** The executor we use to fake asynchronicity. */
  private FakeExecutor executor;

  private AsyncEventBus bus;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    executor = new FakeExecutor();
    bus = new AsyncEventBus(executor);
  }

  public void testBasicDistribution() {
    StringCatcher catcher = new StringCatcher();
    bus.register(catcher);

    // We post the event, but our Executor will not deliver it until instructed.
    bus.post(EVENT);

    List<String> events = catcher.getEvents();
    assertTrue("No events should be delivered synchronously.", events.isEmpty());

    // Now we find the task in our Executor and explicitly activate it.
    List<Runnable> tasks = executor.getTasks();
    assertEquals("One event dispatch task should be queued.", 1, tasks.size());

    tasks.get(0).run();

    assertEquals("One event should be delivered.", 1, events.size());
    assertEquals("Correct string should be delivered.", EVENT, events.get(0));
  }

  /**
   * An {@link Executor} wanna-be that simply records the tasks it's given. Arguably the Worst
   * Executor Ever.
   *
   * @author cbiffle
   */
  public static class FakeExecutor implements Executor {
    List<Runnable> tasks = Lists.newArrayList();

    @Override
    public void execute(Runnable task) {
      tasks.add(task);
    }

    public List<Runnable> getTasks() {
      return tasks;
    }
  }
}
