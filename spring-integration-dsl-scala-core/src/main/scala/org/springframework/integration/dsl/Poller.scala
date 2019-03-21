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

import org.springframework.scheduling.Trigger
import java.util.concurrent.Executor
import org.springframework.core.task.SyncTaskExecutor
import java.util.UUID

/**
 * @author Oleg Zhurakousky
 */

object poll {
  /**
   * Will define poller which takes a reference to an instance of
   * org.springframework.scheduling.Trigger
   */
  def usingTrigger(trigger: Trigger) = new Poller() {
    def withExecutor(taskExecutor: Executor = new SyncTaskExecutor) = new PollerComposition(null, new Poller(taskExecutor = taskExecutor))
  }

  /**
   *
   */
  def atFixedRate(fixedRate: Int) = new PollerComposition(null, new Poller(fixedRate = fixedRate)) {

    def withExecutor(taskExecutor: Executor) = new Poller(fixedRate = fixedRate) {
      def withMaxMessagesPerPoll(maxMessagesPerPoll: Int) =
        new PollerComposition(null, new Poller(fixedRate = fixedRate, maxMessagesPerPoll = maxMessagesPerPoll))
    }

    def withMaxMessagesPerPoll(maxMessagesPerPoll: Int) =
      new PollerComposition(null, new Poller(fixedRate = fixedRate, maxMessagesPerPoll = maxMessagesPerPoll)) {

        def withExecutor(taskExecutor: Executor = new SyncTaskExecutor) =
          new PollerComposition(null, new Poller(fixedRate = fixedRate, maxMessagesPerPoll = maxMessagesPerPoll, taskExecutor = taskExecutor))
      }
  }

  /**
   *
   */
  def withFixedDelay(fixedDelay: Int) = new PollerComposition(null, new Poller(fixedDelay = fixedDelay)) {

    def withExecutor(taskExecutor: Executor) = new PollerComposition(null, new Poller(fixedDelay = fixedDelay, taskExecutor = taskExecutor) {
      def withMaxMessagesPerPoll(maxMessagesPerPoll: Int) =
        new PollerComposition(null, new Poller(fixedDelay = fixedDelay, maxMessagesPerPoll = maxMessagesPerPoll))
    })

    def withMaxMessagesPerPoll(maxMessagesPerPoll: Int) =
      new PollerComposition(null, new Poller(fixedDelay = fixedDelay, maxMessagesPerPoll = maxMessagesPerPoll)) {

        def withExecutor(taskExecutor: Executor = new SyncTaskExecutor) =
          new PollerComposition(null, new Poller(fixedDelay = fixedDelay, maxMessagesPerPoll = maxMessagesPerPoll, taskExecutor = taskExecutor))
      }
  }
}
/**
 *
 */
private[dsl] case class Poller(override val name: String = "$poll_" + UUID.randomUUID().toString.substring(0, 8),
  val fixedRate: Int = 1000,
  val fixedDelay: Int = -1,
  val maxMessagesPerPoll: Int = 0,
  val taskExecutor: Executor = null,
  val trigger: Trigger = null) extends IntegrationComponent(name)