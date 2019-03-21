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

import org.junit.{Assert, Test}
import org.springframework.scheduling.support.PeriodicTrigger
import org.springframework.core.task.{SimpleAsyncTaskExecutor, SyncTaskExecutor}

/**
 * @author Oleg Zhurakousky
 */

class PollerTests {

  @Test
  def validatePollerConfigSyntax() {
    poll.usingTrigger(new PeriodicTrigger(4))

    poll.usingTrigger(new PeriodicTrigger(4)).withExecutor(new SyncTaskExecutor)

    poll.usingTrigger(new PeriodicTrigger(4)) withExecutor(new SyncTaskExecutor)

    // fixed delay
    poll.withFixedDelay(5).withExecutor(new SyncTaskExecutor)

    poll.withFixedDelay(5) withExecutor(new SyncTaskExecutor)

    poll.withFixedDelay(5).withMaxMessagesPerPoll(3).withExecutor(new SyncTaskExecutor)

    poll.withFixedDelay(5) withMaxMessagesPerPoll(3) withExecutor(new SyncTaskExecutor)

    // fixed rate
    poll.atFixedRate(5).withExecutor(new SyncTaskExecutor)

    poll.atFixedRate(5) withExecutor(new SyncTaskExecutor)

    poll.atFixedRate(5).withMaxMessagesPerPoll(3).withExecutor(new SyncTaskExecutor)

    poll.atFixedRate(5) withMaxMessagesPerPoll(3) withExecutor(new SyncTaskExecutor)
  }

  @Test
  def validatePollerValues(){
    val executor = new SimpleAsyncTaskExecutor

    val pollerWithFixedRate = poll.atFixedRate(4).withMaxMessagesPerPoll(90).withExecutor(executor)
    Assert.assertEquals(4, pollerWithFixedRate.target.asInstanceOf[Poller].fixedRate)
    Assert.assertEquals(-1, pollerWithFixedRate.target.asInstanceOf[Poller].fixedDelay)
    Assert.assertEquals(90, pollerWithFixedRate.target.asInstanceOf[Poller].maxMessagesPerPoll)
    Assert.assertEquals(executor, pollerWithFixedRate.target.asInstanceOf[Poller].taskExecutor)

    val pollerWithFixedDelay = poll.withFixedDelay(5)
    Assert.assertEquals(5, pollerWithFixedDelay.target.asInstanceOf[Poller].fixedDelay)

  }
}