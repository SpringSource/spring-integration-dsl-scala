/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.integration.dsl
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.integration.config.ServiceActivatorFactoryBean
import org.springframework.http.HttpMethod
import java.util.UUID
import org.springframework.integration.http.outbound.HttpRequestExecutingMessageHandler
import org.w3c.dom.Element
import org.w3c.dom.Document
import org.springframework.integration.channel.DirectChannel

/**
 * @author Oleg Zhurakousky
 * @author Soby Chacko
 */
object http {
  def GET[T](httpUrl: String)(implicit m: scala.reflect.Manifest[T]) =
    new SendingEndpointComposition(null, new HttpOutboundGateway(target = httpUrl,
      httpMethod = HttpMethod.GET,
      expectedResponseType = m.runtimeClass)) {
      def where(name: String, requestTimeout: Int) =
        new SendingEndpointComposition(null, new HttpOutboundGateway(name = name,
          target = httpUrl,
          requestTimeout = requestTimeout,
          httpMethod = HttpMethod.GET,
          expectedResponseType = m.runtimeClass))
    }

  def GET[T](function: Function[_, String])(implicit m: scala.reflect.Manifest[T]) =
    new SendingEndpointComposition(null, new HttpOutboundGateway(target = function,
      httpMethod = HttpMethod.GET,
      expectedResponseType = m.runtimeClass)) {
      def where(name: String, requestTimeout: Int) =
        new SendingEndpointComposition(null, new HttpOutboundGateway(name = name,
          target = function,
          requestTimeout = requestTimeout,
          httpMethod = HttpMethod.GET,
          expectedResponseType = m.runtimeClass))
    }

  def POST[T](httpUrl: String)(implicit m: scala.reflect.Manifest[T]) =
    new SendingEndpointComposition(null, new HttpOutboundGateway(target = httpUrl,
      httpMethod = HttpMethod.POST,
      expectedResponseType = m.runtimeClass)) {
      def where(name: String, requestTimeout: Int) =
        new SendingEndpointComposition(null, new HttpOutboundGateway(name = name,
          target = httpUrl,
          requestTimeout = requestTimeout,
          httpMethod = HttpMethod.POST,
          expectedResponseType = m.runtimeClass))
    }

  def POST[T](function: Function[_, String])(implicit m: scala.reflect.Manifest[T]) =
    new SendingEndpointComposition(null, new HttpOutboundGateway(target = function,
      httpMethod = HttpMethod.POST,
      expectedResponseType = m.runtimeClass)) {
      def where(name: String, requestTimeout: Int) =
        new SendingEndpointComposition(null, new HttpOutboundGateway(name = name,
          target = function,
          requestTimeout = requestTimeout,
          httpMethod = HttpMethod.POST,
          expectedResponseType = m.runtimeClass))
    }

  def PUT[T](httpUrl: String)(implicit m: scala.reflect.Manifest[T]) =
    new SendingEndpointComposition(null, new HttpOutboundGateway(target = httpUrl,
      httpMethod = HttpMethod.PUT,
      expectedResponseType = m.runtimeClass)) {
      def where(name: String, requestTimeout: Int) =
        new SendingEndpointComposition(null, new HttpOutboundGateway(name = name,
          target = httpUrl,
          requestTimeout = requestTimeout,
          httpMethod = HttpMethod.PUT,
          expectedResponseType = m.runtimeClass))
    }

  def PUT[T](function: Function[_, String])(implicit m: scala.reflect.Manifest[T]) =
    new SendingEndpointComposition(null, new HttpOutboundGateway(target = function,
      httpMethod = HttpMethod.PUT,
      expectedResponseType = m.runtimeClass)) {
      def where(name: String, requestTimeout: Int) =
        new SendingEndpointComposition(null, new HttpOutboundGateway(name = name,
          target = function,
          requestTimeout = requestTimeout,
          httpMethod = HttpMethod.PUT,
          expectedResponseType = m.runtimeClass))
    }

  def DELETE[T](httpUrl: String)(implicit m: scala.reflect.Manifest[T]) =
    new SendingEndpointComposition(null, new HttpOutboundGateway(target = httpUrl,
      httpMethod = HttpMethod.DELETE,
      expectedResponseType = m.runtimeClass)) {
      def where(name: String, requestTimeout: Int) =
        new SendingEndpointComposition(null, new HttpOutboundGateway(name = name,
          target = httpUrl,
          requestTimeout = requestTimeout,
          httpMethod = HttpMethod.DELETE,
          expectedResponseType = m.runtimeClass))
    }

  def DELETE[T](function: Function[_, String])(implicit m: scala.reflect.Manifest[T]) =
    new SendingEndpointComposition(null, new HttpOutboundGateway(target = function,
      httpMethod = HttpMethod.DELETE,
      expectedResponseType = m.runtimeClass)) {
      def where(name: String, requestTimeout: Int) =
        new SendingEndpointComposition(null, new HttpOutboundGateway(name = name,
          target = function,
          requestTimeout = requestTimeout,
          httpMethod = HttpMethod.DELETE,
          expectedResponseType = m.runtimeClass))
    }
}

private[dsl] class HttpOutboundGateway(name: String = "$http_out_" + UUID.randomUUID().toString.substring(0, 8),
  target: Any,
  val requestTimeout: Int = 0,
  val httpMethod: HttpMethod = HttpMethod.POST,
  val expectedResponseType: Class[_]) extends SimpleEndpoint(name, target) with OutboundAdapterEndpoint {

  override def build(document: Document = null,
    targetDefinitionFunction: Function1[Any, Tuple2[String, String]],
    compositionInitFunction: Function2[BaseIntegrationComposition, AbstractChannel, Unit] = null,
    inputChannel:AbstractChannel,
    outputChannel:AbstractChannel): Element = {

    require(inputChannel != null, "'inputChannel' must be provided")

    val beansElement = document.getElementsByTagName("beans").item(0).asInstanceOf[Element]

    if (!beansElement.hasAttribute("xmlns:int-http")){
       beansElement.setAttribute("xmlns:int-http", "http://www.springframework.org/schema/integration/http")
       val schemaLocation = beansElement.getAttribute("xsi:schemaLocation")
       beansElement.setAttribute("xsi:schemaLocation", schemaLocation +
    		   " http://www.springframework.org/schema/integration/http " +
    		   "https://www.springframework.org/schema/integration/http/spring-integration-http.xsd")
    }

    val element = document.createElement("int-http:outbound-gateway")
    element.setAttribute("id", this.name)

    element.setAttribute("http-method", this.httpMethod.toString)
    element.setAttribute("expected-response-type", this.expectedResponseType.getName)

    this.target match {
      case fn: Function[_, _] => {
        val urlExpressionTargetDefinition = targetDefinitionFunction.apply(this.target)
        val urlExpressionTargetParam =
          if (urlExpressionTargetDefinition._2.startsWith("sendMessage")) "#this"
          else if (urlExpressionTargetDefinition._2.startsWith("sendPayloadAndHeaders")) "payload, headers"
          else if (urlExpressionTargetDefinition._2.startsWith("sendPayload")) "payload"

        element.setAttribute("url-expression", "@" + urlExpressionTargetDefinition._1 + "." +
          urlExpressionTargetDefinition._2 + "(" + urlExpressionTargetParam + ")")
      }
      case url: String => {
        element.setAttribute("url", this.target.toString())
      }
    }
    element.setAttribute("request-channel", inputChannel.name)
    if (outputChannel != null){
       element.setAttribute("reply-channel", outputChannel.name)
    }
    element
  }
}