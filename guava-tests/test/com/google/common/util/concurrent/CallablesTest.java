/*
 * Copyright (C) 2009 The Guava Authors
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

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

import junit.framework.TestCase;

import java.security.Permission;
import java.util.concurrent.Callable;

/**
 * Unit tests for {@link Callables}.
 *
 * @author Isaac Shum
 */
public class CallablesTest extends TestCase {

  public void testReturning() throws Exception {
    assertNull(Callables.returning(null).call());

    Object value = new Object();
    Callable<Object> callable = Callables.returning(value);
    assertSame(value, callable.call());
    // Expect the same value on subsequent calls
    assertSame(value, callable.call());
  }

  public void testRenaming() throws Exception {
    String oldName = Thread.currentThread().getName();
    final Supplier<String> newName = Suppliers.ofInstance("MyCrazyThreadName");
    Callable<Void> callable = new Callable<Void>() {
      @Override public Void call() throws Exception {
        assertEquals(Thread.currentThread().getName(), newName.get());
        return null;
      }
    };
    Callables.threadRenaming(callable, newName).call();
    assertEquals(oldName, Thread.currentThread().getName());
  }

  public void testRenaming_exceptionalReturn() throws Exception {
    String oldName = Thread.currentThread().getName();
    final Supplier<String> newName = Suppliers.ofInstance("MyCrazyThreadName");
    class MyException extends Exception {}
    Callable<Void> callable = new Callable<Void>() {
      @Override public Void call() throws Exception {
        assertEquals(Thread.currentThread().getName(), newName.get());
        throw new MyException();
      }
    };
    try {
      Callables.threadRenaming(callable, newName).call();
      fail();
    } catch (MyException expected) {}
    assertEquals(oldName, Thread.currentThread().getName());
  }

  public void testRenaming_noPermissions() throws Exception {
    System.setSecurityManager(new SecurityManager() {
      @Override public void checkAccess(Thread t) {
        throw new SecurityException();
      }
      @Override public void checkPermission(Permission perm) {
        // Do nothing so we can clear the security manager at the end
      }
    });
    try {
      final String oldName = Thread.currentThread().getName();
      Supplier<String> newName = Suppliers.ofInstance("MyCrazyThreadName");
      Callable<Void> callable = new Callable<Void>() {
        @Override public Void call() throws Exception {
          assertEquals(Thread.currentThread().getName(), oldName);
          return null;
        }
      };
      Callables.threadRenaming(callable, newName).call();
      assertEquals(oldName, Thread.currentThread().getName());
    } finally {
      System.setSecurityManager(null);
    }
  }
}
