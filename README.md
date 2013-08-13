REMOTE CHAT SERVER 
==================

Introduction
------------

* `Tutorial source code. <https://github.com/tbrown1979/akka-chat-server>

Writing correct concurrent, fault-tolerant and scalable applications is too hard. Most of the time it’s because we are using the wrong tools and the wrong level of abstraction.

Akka is an attempt to change that.

Akka uses the Actor Model together with Software Transactional Memory to raise the abstraction level and provide a better platform to build correct concurrent and scalable applications.

For fault-tolerance Akka adopts the “Let it crash”, also called “Embrace failure”, model which has been used with great success in the telecom industry to build applications that self-heal, systems that never stop.

Actors also provides the abstraction for transparent distribution and the basis for truly scalable and fault-tolerant applications.

Akka is Open Source and available under the Apache 2 License.

In this article we will introduce you to Akka and see how we can utilize it to build a highly concurrent, scalable and fault-tolerant network server.

But first let’s take a step back and discuss what Actors really are and what they are useful for.

Actors
------

The [Actor Model](http://en.wikipedia.org/wiki/Actor_model) provides a higher level of abstraction for writing concurrent and distributed systems. It alleviates the developer from having to deal with explicit locking and thread management. It makes it easier to write correct concurrent and parallel systems. Actors are really nothing new, they were defined in the 1963 paper by Carl Hewitt and have been popularized by the Erlang language which emerged in the mid 80s. It has been used by for example at Ericsson with great success to build highly concurrent and extremely reliable (99.9999999 % availability - 31 ms/year downtime) telecom systems.

Actors encapsulate state and behavior into a lightweight process/thread. In a sense they are like OO objects but with a major semantic difference; they do not share state with any other Actor. Each Actor has its own view of the world and can only have impact on other Actors by sending messages to them. Messages are sent asynchronously and non-blocking in a so-called “fire-and-forget” manner where the Actor sends off a message to some other Actor and then do not wait for a reply but goes off doing other things or are suspended by the runtime. Each Actor has a mailbox (ordered message queue) in which incoming messages are processed one by one. Since all processing is done asynchronously and Actors do not block and consume any resources while waiting for messages, Actors tend to give very good concurrency and scalability characteristics and are excellent for building event-based systems.

Creating Actors
---------------

Akka has both a Scala API (Actors (Scala)) and a Java API (Actors (Java)). In this article we will only look at the Scala API since that is the most expressive one. The article assumes some basic Scala knowledge, but even if you don’t know Scala I don’t think it will not be too hard to follow along anyway.

Akka has adopted the same style of writing Actors as Erlang in which each Actor has an explicit message handler which does pattern matching to match on the incoming messages.

Actors can be created either by: * Extending the ‘Actor’ class and implementing the ‘receive’ method. * Create an anonymous Actor using one of the ‘actor’ methods.

Here is a little example before we dive into a more interesting one.