
package ch.loewenfels.jira.plugin.synchronisation

import org.junit.Test
import org.scalatest.junit.AssertionsForJUnit
import org.specs2.matcher.ThrownExpectations
import org.specs2.mock.Mockito

import com.atlassian.jira.issue.fields.config.FieldConfig

class BackgroundTaskTest extends AssertionsForJUnit with Mockito with ThrownExpectations {

  @Test(expected = classOf[NullPointerException])
  def execute_nullArgument_exception() {
    //arrange
    val testee = new BackgroundTask
    //act + assert
    testee.execute(null)
  }

  @Test
  def execute_emptyMap_Noexception() {
    //arrange
    val testee = new BackgroundTask
    val jobDataMap = mock[java.util.Map[String, Object]]
    //act + assert
    testee.execute(jobDataMap)
  }

  @Test
  def execute_NoFieldConfig_NoSync() {
    //arrange
    val testee = new BackgroundTask
    val jobDataMap = mock[java.util.Map[String, Object]]
    val syncService = mock[SyncService]
    jobDataMap.get(BackgroundTaskConfig.SERVICE) returns syncService
    //act
    testee.execute(jobDataMap)
    //assert
    there was noCallsTo(syncService)
  }

  @Test
  def execute_NoSyncService_NoSync() {
    //arrange
    val testee = new BackgroundTask
    val jobDataMap = mock[java.util.Map[String, Object]]
    val fieldConfig = mock[FieldConfig]
    jobDataMap.get(BackgroundTaskConfig.FIELD_CONFIG) returns fieldConfig
    //act
    testee.execute(jobDataMap)
    //assert
    there was one(jobDataMap).get(BackgroundTaskConfig.SERVICE)
  }

  @Test
  def execute_SyncServiceAndTwoFieldConfig_TwoSync() {
    //arrange
    val testee = new BackgroundTask
    val jobDataMap = mock[java.util.Map[String, Object]]
    val syncService = mock[SyncService]
    val fieldConfig = mock[FieldConfig]
    jobDataMap.get(BackgroundTaskConfig.SERVICE) returns syncService
    jobDataMap.get(BackgroundTaskConfig.FIELD_CONFIG) returns Seq(fieldConfig, fieldConfig)
    //act
    testee.execute(jobDataMap)
    //assert
    there was 2.times(syncService).synchronize(fieldConfig)
  }

}