package ch.loewenfels.jira.plugin.synchronisation

import org.junit.Test
import org.scalatest.junit.AssertionsForJUnit
import org.specs2.matcher.MustMatchers
import org.specs2.matcher.ThrownExpectations
import org.specs2.mock.Mockito

import com.atlassian.jira.issue.customfields.option.{ Option => JiraOption }
import com.atlassian.sal.api.pluginsettings.PluginSettings

class SyncProtocolTest extends AssertionsForJUnit with Mockito with ThrownExpectations with MustMatchers {
  @Test
  def protocolWriter_logWithMessage_bufferAppended {
    //arrange
    val expected = "foo message"
    val settings = mock[PluginSettings]
    val testee = SyncProtocol.newProtocolWriter(settings)
    //act
    testee.log(expected)
    //assert
    testee.logBuffer must contain(expected)
  }

  @Test
  def protocolWriter_logWithJiraOptionAndMessage_bufferAppended {
    //arrange
    val expectedMessage = "foo message"
    val expectedOptionValue = "optionvalue"
    val jiraOption = mock[JiraOption].getValue returns expectedOptionValue
    val settings = mock[PluginSettings]
    val testee = SyncProtocol.newProtocolWriter(settings)
    //act
    testee.log(jiraOption, expectedMessage)
    //assert
    testee.logBuffer must contain(expectedMessage)
    testee.logBuffer must contain(expectedOptionValue)
  }

  @Test
  def protocolWriter_flush_putTimeStampToSetting {
    //arrange
    val settings = mock[PluginSettings]
    val testee = SyncProtocol.newProtocolWriter(settings)
    //act
    testee.flush
    //assert
    there was one(settings).put(===(SyncProtocol.TimeStampKey), anyLong)
  }

  @Test
  def protocolWriter_flush_putLogAsListToSetting {
    //arrange
    val settings = mock[PluginSettings]
    val testee = SyncProtocol.newProtocolWriter(settings)
    //act
    testee.flush
    //assert
    there was one(settings).put(===(SyncProtocol.EntriesKey), any[java.util.List[String]])
  }

  @Test
  def syncProtocol_noTimestamp_neverString {
    //arrange
    val settings = mock[PluginSettings]
    val testee = SyncProtocol.syncProtocol(settings)
    //act
    val actual = testee.timestamp
    //assert
    actual must ===("never")

  }

  @Test
  def syncProtocol_timestampExist_formattedTimestamp {
    //arrange
    val settings = mock[PluginSettings]
    settings.get(SyncProtocol.TimeStampKey) returns "123"
    val testee = SyncProtocol.syncProtocol(settings)
    //act
    val actual = testee.timestamp
    //assert
    actual must contain("1")
  }
}
