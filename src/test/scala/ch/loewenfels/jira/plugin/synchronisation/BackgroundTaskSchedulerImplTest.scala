
package ch.loewenfels.jira.plugin.synchronisation

import org.junit.Test
import org.scalatest.MustMatchers
import org.scalatest.junit.AssertionsForJUnit
import org.specs2.matcher.ThrownExpectations
import org.specs2.mock.Mockito
import com.atlassian.sal.api.scheduling.PluginScheduler
import ch.loewenfels.jira.plugin.config.VertecConfig
import java.util.Date
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.fields.CustomField
import scala.collection.mutable.Buffer
import ch.loewenfels.jira.plugin.customfields.CustomFieldProvider

class BackgroundTaskSchedulerImplTest extends AssertionsForJUnit with Mockito with ThrownExpectations with MustMatchers {

  @Test def reschedule_validInterval_JobScheduled() {
    //arrange
    val scheduler = mock[PluginScheduler]
    val config = mock[VertecConfig]
    config.syncIntervalInMs returns Option("1000")
    val customFieldProvider = mock[CustomFieldProvider]
    customFieldProvider.filter(CustomFieldProvider.VertecKey) returns Buffer(mock[CustomField])
    val testee = new BackgroundTaskSchedulerImpl(scheduler, config, customFieldProvider, mock[SyncService])
    val interval = 1000L
    //act
    testee.reschedule(interval)
    //assert
    there was one(scheduler).scheduleJob(anyString, any, any[java.util.Map[String, Object]], any[Date], anyLong)
  }

  @Test def onStart_IntervalOnConfig_JobScheduled() {
    //arrange
    val scheduler = mock[PluginScheduler]
    val config = mock[VertecConfig]

    config.syncIntervalInMs returns Option("1000")
    val customFieldProvider = mock[CustomFieldProvider]
    customFieldProvider.filter(CustomFieldProvider.VertecKey) returns Buffer(mock[CustomField])
    val testee = new BackgroundTaskSchedulerImpl(scheduler, config, customFieldProvider, mock[SyncService])
    val interval: Long = 1000
    //act
    testee.onStart()
    //assert
    there was one(scheduler).scheduleJob(anyString, any, any[java.util.Map[String, Object]], any[Date], anyLong)
  }

}
