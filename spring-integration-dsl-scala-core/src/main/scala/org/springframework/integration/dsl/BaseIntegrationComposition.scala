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
package org.springframework.integration.dsl
import java.util.UUID
import org.apache.commons.logging.LogFactory
import org.springframework.integration.dsl.utils.DslUtils
import org.springframework.context.{ApplicationContextAware, ApplicationContext}
import org.w3c.dom.Element
import org.w3c.dom.Document

/**
 * @author Oleg Zhurakousky
 */
private[dsl] case class BaseIntegrationComposition(private[dsl] val parentComposition: BaseIntegrationComposition,
                                                   private[dsl] val target: IntegrationComponent) extends AnyRef with ApplicationContextAware {

  val logger = LogFactory.getLog(this.getClass());

  private val threadLocal: ThreadLocal[IntegrationContext] = new ThreadLocal[IntegrationContext]

  private[dsl] def compose[T <: BaseIntegrationComposition](parent: BaseIntegrationComposition, child: T): T = {

    /*
     * Will create a new composition of the appropriate type basing it from the 'merged'composition
     * which could be off different type
     */
    def normalize[T <: BaseIntegrationComposition](child: BaseIntegrationComposition, merged: BaseIntegrationComposition): T = {
      val normailizedComposition =
        child match {
          case pch: PollableChannelIntegrationComposition =>
            new PollableChannelIntegrationComposition(merged.parentComposition, merged.target)
          case ch: ChannelIntegrationComposition =>
            new ChannelIntegrationComposition(merged.parentComposition, merged.target)
          case sch: SendingChannelComposition =>
            new SendingChannelComposition(merged.parentComposition, merged.target)
          case _ =>
            new SendingEndpointComposition(merged.parentComposition, merged.target)
        }
      normailizedComposition.asInstanceOf[T]
    }

    val mergedComposition =
      if (child.parentComposition != null) {
        val copyComposition = child.copy()
        parent.merge(copyComposition)
        parent.generateComposition(copyComposition.parentComposition, copyComposition)
      } else
        parent.generateComposition(parent, child)

    normalize(child, mergedComposition)
  }

  /**
   * Will produce a copy of this composition
   */
  private[dsl] def copy(): BaseIntegrationComposition = {

    this.target match {
      case ic: IntegrationComponent => {
        val parentComposition = if (this.parentComposition != null) this.parentComposition.copy() else null
        this.generateComposition(parentComposition, this)
      }
      case _ => this
    }
  }

  /**
   * Will merge to compositions by assigning 'this' composition as a 'parentComposition' of 'toComposition'
   */
  private[dsl] def merge(toComposition: BaseIntegrationComposition) = {
    val startingComposition = DslUtils.getStartingComposition(toComposition)
    DslUtils.injectParentComposition(startingComposition, this)
  }

  /**
   * Will add an input-channel to this composition (as a DirectChannel) if it does not begin with one
   */
  private[dsl] def normalizeComposition(): BaseIntegrationComposition = {

    val newComposition = this.copy()
    val startingComposition = DslUtils.getStartingComposition(newComposition)
    if (!startingComposition.isInstanceOf[ChannelIntegrationComposition] &&
        !startingComposition.target.isInstanceOf[InboundMessageSource] &&
        !startingComposition.target.isInstanceOf[Poller]) {
      DslUtils.injectParentComposition(startingComposition, Channel("$ch_" + UUID.randomUUID().toString.substring(0, 8)))
    }
    new BaseIntegrationComposition(newComposition.parentComposition, newComposition.target)
  }

  /**
   *
   */
  private[dsl] def generateComposition[T <: BaseIntegrationComposition](parent: T, composition: T): BaseIntegrationComposition = {
    composition.target match {
      case ch: Channel =>
        new ChannelIntegrationComposition(parent, composition.target)
      case queue: PollableChannel =>
        new PollableChannelIntegrationComposition(parent, composition.target)
      case pubsub: PubSubChannel =>
        new ChannelIntegrationComposition(parent, composition.target)
      case _ =>
        new BaseIntegrationComposition(parent, composition.target)
    }
  }

  /**
   *
   */
  private[dsl] def getContext(parentContext:ApplicationContext): IntegrationContext = {

    threadLocal.get() match {
      case eipContext: IntegrationContext => {
        if (logger.isDebugEnabled) logger.debug("Retrieving existing IntegrationContext")
        eipContext
      }
      case _ => {
        if (logger.isDebugEnabled) logger.debug("Creating new IntegrationContext")
        val eipContext = new IntegrationContext(parentContext, this)
        threadLocal.set(eipContext)
        eipContext
      }
    }
  }

  /**
   * Lifecycle method
   */
  def setApplicationContext(applicationContext: ApplicationContext)  {

    if (this.logger.isDebugEnabled())
      this.logger.debug("Getting IntegrationContext for: " + this)

    this.getContext(applicationContext).start()
  }
}

/**
 *
 */
private[dsl] class SendingIntegrationComposition(parentComposition: BaseIntegrationComposition, target: IntegrationComponent)
  extends BaseIntegrationComposition(parentComposition, target) {
  /**
   *
   */
  def send(message: Any, timeout: Long = 0, headers: Map[String, Any] = null, parentContext:ApplicationContext = null): Boolean = {
    val context = this.getContext(parentContext)
    context.send(message, timeout, headers)
  }

  /**
   *
   */
  def sendAndReceive[T: Manifest](message: Any, timeout: Long = 0, headers: Map[String, Any] = null, errorFlow: SendingEndpointComposition = null, parentContext:ApplicationContext = null): T = {
    val context = this.getContext(parentContext)
    context.sendAndReceive(message, timeout, headers, errorFlow)
  }
}
/**
 *
 */
private[dsl] class SendingEndpointComposition(parentComposition: BaseIntegrationComposition, target: IntegrationComponent)
  extends SendingIntegrationComposition(parentComposition, target) {

  def -->[T <: BaseIntegrationComposition](a: T) = {
    if (this.logger.isDebugEnabled()) this.logger.debug("Adding " + a.target + " to " + this.target)

    val reply = this.compose(this, a)
    reply
  }
}

/**
 *
 */
private[dsl] class SendingChannelComposition(parentComposition: BaseIntegrationComposition, target: IntegrationComponent)
  extends SendingIntegrationComposition(parentComposition, target) {

  def -->[T <: BaseIntegrationComposition](compositions: T*): SendingIntegrationComposition = {
    if (this.logger.isDebugEnabled())
      for (composition <- compositions)
        this.logger.debug("Adding " + DslUtils.getStartingComposition(composition).target + " to " + this.target)

    new SendingIntegrationComposition(this, new ListOfCompositions((for (composition <- compositions) yield composition)))
  }

  def -->[T <: BaseIntegrationComposition](a: T) = {
    if (this.logger.isDebugEnabled())
      this.logger.debug("Adding " + a.target + " to " + this.target)

    this.compose(this, a)
  }
}

/**
 *
 */
private[dsl] class ListeningIntegrationComposition(parentComposition: BaseIntegrationComposition, target: IntegrationComponent)
  extends BaseIntegrationComposition(parentComposition, target) {
  /**
   * Lifecycle method that starts this IntegrationComposition
   */
  def start(parentContext:ApplicationContext = null) = this.getContext(parentContext).start
  
  def start = this.getContext(null)

  def stop = this.getContext(null).stop
 
  def -->[T <: BaseIntegrationComposition](a: T) = {
    if (this.logger.isDebugEnabled()) this.logger.debug("Adding " + a.target + " to " + this.target)
    val composed = this.compose(this, a)
    new ListeningIntegrationComposition(this, composed.target)
  }

}

/**
 *
 */
private[dsl] class PollerComposition(parentComposition: BaseIntegrationComposition, override val target: Poller)
  extends ListeningIntegrationComposition(parentComposition, target)

/**
 *
 */
private[dsl] class ChannelIntegrationComposition(parentComposition: BaseIntegrationComposition, target: IntegrationComponent)
  extends SendingChannelComposition(parentComposition, target)

/**
 *
 */
private[dsl] class PollableChannelIntegrationComposition(parentComposition: BaseIntegrationComposition, target: IntegrationComponent)
  extends SendingIntegrationComposition(parentComposition, target) {
  /**
   *
   */
  def -->(p: PollerComposition) =
    new SendingEndpointComposition(this, p.target)
}

/**
 *
 */
private[dsl] class ListOfCompositions[T](val compositions: Iterable[BaseIntegrationComposition])
             extends IntegrationComponent("ListOfCompositions")
/**
 *
 */
private[dsl] abstract class IntegrationComponent(val name: String = null) {
  override def toString = name
  def toMapOfProperties:Map[String, _] = Map("name" -> name)
}

private[dsl] abstract class SimpleEndpoint(name:String, val target:Any = null) extends IntegrationComponent(name) {

  override def toMapOfProperties:Map[String, _] = super.toMapOfProperties + ("target" -> target)

  def build(root: Document = null,
            targetDefinitionFunction: Function1[Any, Tuple2[String, String]],
            compositionInitFunction: Function2[BaseIntegrationComposition, AbstractChannel, Unit],
            inputChannel:AbstractChannel,
            outputChannel:AbstractChannel): Element
}

private[dsl] abstract trait OutboundAdapterEndpoint

private[dsl] abstract class InboundMessageSource(name:String, val target:Any = null) extends IntegrationComponent(name) {
  override def toString = name

  def build(document: Document = null,
            targetDefinitionFunction: Function1[Any, Tuple2[String, String]],
            pollerDefinitionFunction: Function3[IntegrationComponent, Poller, Element, Unit],
            requestChannelName: String): Element
}
