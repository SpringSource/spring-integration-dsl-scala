/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.integration.dsl
import org.apache.commons.logging.LogFactory
import org.w3c.dom.Document
import org.w3c.dom.Element
import utils.DslUtils
import java.util.UUID
import org.springframework.integration.file.remote.session.SessionFactory
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory
/**
 * @author Oleg Zhurakousky
 */
private[dsl] class SftpInboundAdapterConfig(name: String = "$sftp_in_" + UUID.randomUUID().toString.substring(0, 8),
  override val target: String,
  val localDirectory:String,
  val poller: Poller,
  val sessionFactory:DefaultSftpSessionFactory,
  additionalAttributes:Map[String,_] = null) extends InboundMessageSource(name, target) {

  private val logger = LogFactory.getLog(this.getClass());

  def build(document: Document = null,
            targetDefinitionFunction: Function1[Any, Tuple2[String, String]],
            pollerDefinitionFunction: Function3[IntegrationComponent, Poller, Element, Unit],
            requestChannelName: String): Element = {

    require(requestChannelName != null, "SFTP Inbound Channel Adapter requires continuation but message flow ends here")

    val beansElement = document.getElementsByTagName("beans").item(0).asInstanceOf[Element]
    if (!beansElement.hasAttribute("xmlns:int-sftp")){
       beansElement.setAttribute("xmlns:int-sftp", "http://www.springframework.org/schema/integration/sftp")
       val schemaLocation = beansElement.getAttribute("xsi:schemaLocation")
       beansElement.setAttribute("xsi:schemaLocation", schemaLocation + SftpDsl.sftpSchema);
    }

    val element = document.createElement("int-sftp:inbound-channel-adapter")
    element.setAttribute("id", this.name)

    val sessionFactoryName = targetDefinitionFunction(Some(sessionFactory))._1

    element.setAttribute("session-factory", sessionFactoryName)

    element.setAttribute("channel", requestChannelName)

    element.setAttribute("remote-directory", target)

    element.setAttribute("local-directory", localDirectory)

    element.setAttribute("auto-startup", "false")
    pollerDefinitionFunction(this, poller, element)
    if (additionalAttributes != null){
      DslUtils.setAdditionalAttributes(element, additionalAttributes, null)
    }
    element
  }

}
