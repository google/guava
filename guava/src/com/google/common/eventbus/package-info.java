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

/**
 * The EventBus allows publish-subscribe-style communication between components
 * without requiring the components to explicitly register with one another
 * (and thus be aware of each other).  It is designed exclusively to replace
 * traditional Java in-process event distribution using explicit registration.
 * It is <em>not</em> a general-purpose publish-subscribe system, nor is it
 * intended for interprocess communication.
 * 
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/EventBusExplained">
 * {@code EventBus}</a>.
 *
 * <h2>One-Minute Guide</h2>
 *
 * <p>Converting an existing EventListener-based system to use the EventBus is
 * easy.
 *
 * <h3>For Listeners</h3>
 * <p>To listen for a specific flavor of event (say, a CustomerChangeEvent)...
 * <ul>
 * <li><strong>...in traditional Java events:</strong> implement an interface
 *     defined with the event &mdash; such as CustomerChangeEventListener.</li>
 * <li><strong>...with EventBus:</strong> create a method that accepts
 *     CustomerChangeEvent as its sole argument, and mark it with the
 *     {@link com.google.common.eventbus.Subscribe} annotation.</li>
 * </ul>
 *
 * <p>To register your listener methods with the event producers...
 * <ul>
 * <li><strong>...in traditional Java events:</strong> pass your object to each
 *     producer's {@code registerCustomerChangeEventListener} method.  These
 *     methods are rarely defined in common interfaces, so in addition to
 *     knowing every possible producer, you must also know its type.</li>
 * <li><strong>...with EventBus:</strong> pass your object to the
 *     {@link com.google.common.eventbus.EventBus#register(Object)} method on an
 *     EventBus.  You'll need to
 *     make sure that your object shares an EventBus instance with the event
 *     producers.</li>
 * </ul>
 *
 * <p>To listen for a common event supertype (such as EventObject or Object)...
 * <ul>
 * <li><strong>...in traditional Java events:</strong> not easy.</li>
 * <li><strong>...with EventBus:</strong> events are automatically dispatched to
 *     listeners of any supertype, allowing listeners for interface types
 *     or "wildcard listeners" for Object.</li>
 * </ul>
 *
 * <p>To listen for and detect events that were dispatched without listeners...
 * <ul>
 * <li><strong>...in traditional Java events:</strong> add code to each
 *     event-dispatching method (perhaps using AOP).</li>
 * <li><strong>...with EventBus:</strong> subscribe to {@link
 *     com.google.common.eventbus.DeadEvent}.  The
 *     EventBus will notify you of any events that were posted but not
 *     delivered.  (Handy for debugging.)</li>
 * </ul>
 *
 * <h3>For Producers</h3>
 * <p>To keep track of listeners to your events...
 * <ul>
 * <li><strong>...in traditional Java events:</strong> write code to manage
 *     a list of listeners to your object, including synchronization, or use a
 *     utility class like EventListenerList.</li>
 * <li><strong>...with EventBus:</strong> EventBus does this for you.</li>
 * </ul>
 *
 * <p>To dispatch an event to listeners...
 * <ul>
 * <li><strong>...in traditional Java events:</strong> write a method to
 *     dispatch events to each event listener, including error isolation and
 *     (if desired) asynchronicity.</li>
 * <li><strong>...with EventBus:</strong> pass the event object to an EventBus's
 *     {@link com.google.common.eventbus.EventBus#post(Object)} method.</li>
 * </ul>
 *
 * <h2>Glossary</h2>
 *
 * <p>The EventBus system and code use the following terms to discuss event
 * distribution:
 * <dl>
 * <dt>Event</dt><dd>Any object that may be <em>posted</em> to a bus.</dd>
 * <dt>Subscribing</dt><dd>The act of registering a <em>listener</em> with an
 *     EventBus, so that its <em>subscriber methods</em> will receive events.</dd>
 * <dt>Listener</dt><dd>An object that wishes to receive events, by exposing
 *     <em>subscriber methods</em>.</dt>
 * <dt>Subscriber method</dt><dd>A public method that the EventBus should use to
 *     deliver <em>posted</em> events.  Subscriber methods are marked by the
 *     {@link com.google.common.eventbus.Subscribe} annotation.</dd>
 * <dt>Posting an event</dt><dd>Making the event available to any
 *     <em>listeners</em> through the EventBus.</dt>
 * </dl>
 *
 * <h2>FAQ</h2>
 * <h3>Why must I create my own Event Bus, rather than using a singleton?</h3>
 *
 * <p>The Event Bus doesn't specify how you use it; there's nothing stopping your
 * application from having separate EventBus instances for each component, or
 * using separate instances to separate events by context or topic.  This also
 * makes it trivial to set up and tear down EventBus objects in your tests.
 *
 * <p>Of course, if you'd like to have a process-wide EventBus singleton,
 * there's nothing stopping you from doing it that way.  Simply have your
 * container (such as Guice) create the EventBus as a singleton at global scope
 * (or stash it in a static field, if you're into that sort of thing).
 *
 * <p>In short, the EventBus is not a singleton because we'd rather not make
 * that decision for you.  Use it how you like.
 *
 * <h3>Why use an annotation to mark subscriber methods, rather than requiring the
 * listener to implement an interface?</h3>
 * <p>We feel that the Event Bus's {@code @Subscribe} annotation conveys your
 * intentions just as explicitly as implementing an interface (or perhaps more
 * so), while leaving you free to place event subscriber methods wherever you wish
 * and give them intention-revealing names.
 *
 * <p>Traditional Java Events use a listener interface which typically sports
 * only a handful of methods -- typically one.  This has a number of
 * disadvantages:
 * <ul>
 *   <li>Any one class can only implement a single response to a given event.
 *   <li>Listener interface methods may conflict.
 *   <li>The method must be named after the event (e.g. {@code
 *       handleChangeEvent}), rather than its purpose (e.g. {@code
 *       recordChangeInJournal}).
 *   <li>Each event usually has its own interface, without a common parent
 *       interface for a family of events (e.g. all UI events).
 * </ul>
 *
 * <p>The difficulties in implementing this cleanly has given rise to a pattern,
 * particularly common in Swing apps, of using tiny anonymous classes to
 * implement event listener interfaces.
 *
 * <p>Compare these two cases: <pre>
 *   class ChangeRecorder {
 *     void setCustomer(Customer cust) {
 *       cust.addChangeListener(new ChangeListener() {
 *         void customerChanged(ChangeEvent e) {
 *           recordChange(e.getChange());
 *         }
 *       };
 *     }
 *   }
 *
 *   // Class is typically registered by the container.
 *   class EventBusChangeRecorder {
 *     &#064;Subscribe void recordCustomerChange(ChangeEvent e) {
 *       recordChange(e.getChange());
 *     }
 *   }</pre>
 *
 * <p>The intent is actually clearer in the second case: there's less noise code,
 * and the event subscriber has a clear and meaningful name.
 *
 * <h3>What about a generic {@code Subscriber<T>} interface?</h3>
 * <p>Some have proposed a generic {@code Subscriber<T>} interface for EventBus
 * listeners.  This runs into issues with Java's use of type erasure, not to
 * mention problems in usability.
 *
 * <p>Let's say the interface looked something like the following: <pre>   {@code
 *   interface Subscriber<T> {
 *     void handleEvent(T event);
 *   }}</pre>
 *
 * <p>Due to erasure, no single class can implement a generic interface more than
 * once with different type parameters.  This is a giant step backwards from
 * traditional Java Events, where even if {@code actionPerformed} and {@code
 * keyPressed} aren't very meaningful names, at least you can implement both
 * methods!
 *
 * <h3>Doesn't EventBus destroy static typing and eliminate automated
 * refactoring support?</h3>
 * <p>Some have freaked out about EventBus's {@code register(Object)} and {@code
 * post(Object)} methods' use of the {@code Object} type.
 *
 * <p>{@code Object} is used here for a good reason: the Event Bus library
 * places no restrictions on the types of either your event listeners (as in
 * {@code register(Object)}) or the events themselves (in {@code post(Object)}).
 *
 * <p>Event subscriber methods, on the other hand, must explicitly declare their
 * argument type -- the type of event desired (or one of its supertypes).  Thus,
 * searching for references to an event class will instantly find all subscriber
 * methods for that event, and renaming the type will affect all subscriber methods
 * within view of your IDE (and any code that creates the event).
 *
 * <p>It's true that you can rename your {@code @Subscribed} event subscriber
 * methods at will; Event Bus will not stop this or do anything to propagate the
 * rename because, to Event Bus, the names of your subscriber methods are
 * irrelevant.  Test code that calls the methods directly, of course, will be
 * affected by your renaming -- but that's what your refactoring tools are for.
 *
 * <h3>What happens if I {@code register} a listener without any subscriber
 * methods?</h3>
 * <p>Nothing at all.
 *
 * <p>The Event Bus was designed to integrate with containers and module
 * systems, with Guice as the prototypical example.  In these cases, it's
 * convenient to have the container/factory/environment pass <i>every</i>
 * created object to an EventBus's {@code register(Object)} method.
 *
 * <p>This way, any object created by the container/factory/environment can
 * hook into the system's event model simply by exposing subscriber methods.
 *
 * <h3>What Event Bus problems can be detected at compile time?</h3>
 * <p>Any problem that can be unambiguously detected by Java's type system.  For
 * example, defining a subscriber method for a nonexistent event type.
 *
 * <h3>What Event Bus problems can be detected immediately at registration?</h3>
 * <p>Immediately upon invoking {@code register(Object)} , the listener being
 * registered is checked for the <i>well-formedness</i> of its subscriber methods.
 * Specifically, any methods marked with {@code @Subscribe} must take only a
 * single argument.
 *
 * <p>Any violations of this rule will cause an {@code IllegalArgumentException}
 * to be thrown.
 *
 * <p>(This check could be moved to compile-time using APT, a solution we're
 * researching.)
 *
 * <h3>What Event Bus problems may only be detected later, at runtime?</h3>
 * <p>If a component posts events with no registered listeners, it <i>may</i>
 * indicate an error (typically an indication that you missed a
 * {@code @Subscribe} annotation, or that the listening component is not loaded).
 *
 * <p>(Note that this is <i>not necessarily</i> indicative of a problem.  There
 * are many cases where an application will deliberately ignore a posted event,
 * particularly if the event is coming from code you don't control.)
 *
 * <p>To handle such events, register a subscriber method for the {@code DeadEvent}
 * class.  Whenever EventBus receives an event with no registered subscribers, it
 * will turn it into a {@code DeadEvent} and pass it your way -- allowing you to
 * log it or otherwise recover.
 *
 * <h3>How do I test event listeners and their subscriber methods?</h3>
 * <p>Because subscriber methods on your listener classes are normal methods, you can
 * simply call them from your test code to simulate the EventBus.
 */
package com.google.common.eventbus;
