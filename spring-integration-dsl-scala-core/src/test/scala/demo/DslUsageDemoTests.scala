/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package demo

import org.junit.Test
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.SpelParserConfiguration
import org.springframework.integration.dsl.utils.DslUtils
import org.springframework.integration.dsl._
import org.springframework.integration.Message
import scala.collection.immutable.WrappedString

/**
 * @author Oleg Zhurakousky
 */
class DslUsageDemoTests {

  @Test
  def simpleServiceWithWrappedStringAsFunction = {

    def foo(m: Message[_]) = {
      println(m)
    }

    val messageFlow =
      handle{"hello"} -->
      handle{m:Message[_] => foo(m) }

    messageFlow.send(2)
    println("done")
  }

  @Test
  def demoSendWithFilter = {

    val messageFlow =
      filter { payload: String => payload == "hello" } -->
        transform { m: Message[String] => m.getPayload().toUpperCase() } -->
        handle { m: Message[_] => println(m) }

    messageFlow.send("hello")
    println("done")
  }

  @Test
  def demoSendWithExplicitDirectChannel = {
    val messageFlow =
      Channel("direct") -->
        transform { m: Message[String] => m.getPayload().toUpperCase() } -->
        handle { m: Message[_] => println(m) }

    messageFlow.send("hello")
    println("done")
  }

  @Test
  def demoSendWithExplicitPubSubChannelOneSubscriber = {
    val messageFlow =
      PubSubChannel("direct") -->
        transform { m: Message[String] => m.getPayload().toUpperCase() } -->
        handle { m: Message[_] => println(m) }

    messageFlow.send("hello")
    println("done")
  }

  @Test
  def demoSendWithExplicitPubSubChannelMultipleSubscriber = {
    val messageFlow =
      PubSubChannel("direct") --> (
        transform { m: Message[String] => m.getPayload().toUpperCase() } -->
        handle { m: Message[_] => println("Subscriber-1 - " + m) },
        transform { m: Message[String] => m.getPayload().toUpperCase() } -->
        handle { m: Message[_] => println("Subscriber-2 - " + m) })

    messageFlow.send("hello")
    println("done")
  }

  @Test
  def demoSendAndReceive = {
    val messageFlow =
      transform { m: Message[String] => m.getPayload().toUpperCase() } -->
        handle { m: Message[_] => println(m); m }

    val reply = messageFlow.sendAndReceive[String]("hello")
    println(reply)
  }

  @Test
  def demoSendWithPubSubChannel = {
    val messageFlow =
      handle { m: Message[String] => m.getPayload().toUpperCase() }.additionalAttributes(name = "myTransformer") -->
        PubSubChannel("pubSub") --> (
          transform { m: Message[_] => m.getPayload() + " - subscriber-1" } -->
          handle { m: Message[_] => println(m) },
          transform { m: Message[_] => m.getPayload() + " - subscriber-2" } -->
          handle { m: Message[_] => println(m) })

    println(messageFlow)
    println(DslUtils.toProductTraversble(messageFlow))
    messageFlow.send("hello")
    println("done")
  }

  @Test
  def demoSendWithBridge = {
    val messageFlow =
      Channel("A") -->
        Channel("B") -->
        handle { m: Message[_] => println("From Hello channel - " + m) }

    messageFlow.send("hello")

    println("done")
  }

  @Test
  def demoSendWithPolingBridge = {
    val messageFlow =
      Channel("A") -->
        Channel("B").withQueue --> poll.atFixedRate(1) -->
        handle { m: Message[_] => println("From Hello channel - " + m) }

    messageFlow.send("hello")
    Thread.sleep(1000)
    println("done")
  }

  @Test
  def headerEnricherWithTuple = {
    val enricherA = enrich.header("hello" -> "bye") --> handle { m: Message[_] => println(m) }
    enricherA.send("Hello")
    println("done")
  }

  @Test
  def headerEnricherWithFunctionAsValue = {
    val enricherB = enrich.header("hello" -> Some({ m: Message[String] => m.getPayload().toUpperCase() })) --> handle { m: Message[_] => println(m) }
    enricherB.send("Hello")
    println("done")
  }

  @Test
  def headerEnricherWithMessageFunctionAsProcessor = {
    val enricherB = enrich.header("hello" -> { m: Message[String] => m.getPayload().toUpperCase() }) --> handle { m: Message[_] => println(m) }
    enricherB.send("Hello")
    println("done")
  }

  @Test
  def headerEnricherWithExpression = {
    val expression = new SpelExpressionParser(new SpelParserConfiguration(true, true)).parseExpression("(2 * 6) + ' days of Christmas'");
    val enricherB = enrich.header("phrase" -> expression) --> handle { m: Message[_] => println(m) }
    enricherB.send("Hello")
    println("done")
  }

  @Test
  def headerEnricherWithMultiTuple = {
    val expression = new SpelExpressionParser(new SpelParserConfiguration(true, true)).parseExpression("(2 * 6) + ' days of Christmas'");
    val enricher =
      enrich.headers("foo" -> "foo",
        "bar" -> { m: Message[String] => m.getPayload().toUpperCase() },
        "phrase" -> expression) -->
        handle { m: Message[_] => println(m) }

    enricher.send("Hello")
    println("done")
  }

  @Test
  def contentEnricher = {
    val employee = new Employee("John", "Doe", 23)
    val enricher =
      enrich { p: Person => p.name = employee.firstName + " " + employee.lastName; p.age = employee.age; p } -->
        handle { m: Message[_] => println(m) }

    enricher.send(new Person)
    println("done")
  }

  @Test
  def contentEnricherWithSubFlow = {

    val employeeBuldingFlow =
      transform { attributes: List[String] => new Employee(attributes(0), attributes(1), Integer.parseInt(attributes(2))) }

    val enricher =
      enrich { p: Person =>
        val employee = employeeBuldingFlow.sendAndReceive[Employee](List[String]("John", "Doe", "25"))
        p.name = employee.firstName + " " + employee.lastName
        p.age = employee.age
        p
      } -->
        handle { m: Message[_] => println(m) }

    enricher.send(new Person)
    println("done")
  }

  case class Person(var name: String = null, var age: Int = 0)

  class Employee(val firstName: String, val lastName: String, val age: Int)

  @Test
  def headerValueRouter = {

    val messageFlow =
      route.onValueOfHeader("someHeaderName")(

        when(true) then
          handle { m: Message[_] => println("Header is 'foo': " + m) },
        when("bar") then
          handle { m: Message[_] => println("Header is 'bar': " + m) }
      ) -->
      handle { m: Message[_] => println("Header is not set: " + m) }

    messageFlow.send("FOO header", headers = Map("someHeaderName" -> true))

    messageFlow.send("BAR header", headers = Map("someHeaderName" -> false))
    
    messageFlow.send("BAR header", headers = Map("someHeaderName" -> "bar"))

    //messageFlow.send("Hello")

    println("done")
  }

  @Test
  def payloadTypeRouter = {

    val messageFlow =
      route.onPayloadType(

        when(classOf[String]) then
          handle { m: Message[_] => println("Payload is String: " + m) },
        when(classOf[Int]) then
          handle { m: Message[_] => println("Payload is Int: " + m) }) -->
        handle { m: Message[_] => println("Payload is: " + m.getPayload()) }

    messageFlow.send("Hello")

    messageFlow.send(25)

    messageFlow.send(new Person)

    println("done")
  }

  @Test
  def customRouter = {

    val messageFlow =
      route { m: Message[String] => m.getPayload }(

        when("Hello") then
          handle { m: Message[_] => println("Payload is Hello: " + m) },
        when("Bye") then
          handle { m: Message[_] => println("Payload is Bye: " + m) }) -->
        Channel("Hi") -->
        handle { m: Message[_] => println("Payload is: " + m.getPayload()) }

    messageFlow.send("Hello")

    messageFlow.send("Bye")

    messageFlow.send("Hi")

    println("done")
  }
}