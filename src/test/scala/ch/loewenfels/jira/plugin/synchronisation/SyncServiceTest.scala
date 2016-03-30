package ch.loewenfels.jira.plugin.synchronisation

import java.util.ArrayList

import org.junit.Test
import org.scalatest.junit.AssertionsForJUnit
import org.specs2.matcher.MustMatchers
import org.specs2.matcher.ThrownExpectations
import org.specs2.mock.Mockito

import com.atlassian.jira.issue.customfields.manager.OptionsManager
import com.atlassian.jira.issue.customfields.option.{ Option => JiraOption }
import com.atlassian.jira.issue.customfields.option.Options
import com.atlassian.jira.issue.fields.config.FieldConfig
import com.atlassian.sal.api.pluginsettings.PluginSettings

import ch.loewenfels.jira.plugin.config.VertecConfig

class SyncServiceTest extends AssertionsForJUnit with Mockito with ThrownExpectations with MustMatchers {

  @Test def synchronize_vertecClientNone_noInteractionsOfOptionsManager {
    //arrange
    val fieldConfigId = "foo"
    val config = mock[VertecConfig]
    val optionsManager = mock[OptionsManager]
    config.vertecUrl returns None
    val fieldConfig = createFieldConfig(fieldConfigId)
    val pluginSettings = createSettings(config, fieldConfigId)
    val testee = new SyncService(config, mock[OptionsManager])
    //act
    testee.synchronize(fieldConfig, createSyncProtocolWriter)
    //assert
    there was noCallsTo(optionsManager)
  }

  @Test def synchronize_vertecClientNone_logFailedSync {
    //arrange
    val fieldConfigId = "foo"
    val syncProtocolWriter = createSyncProtocolWriter
    val config = mock[VertecConfig]
    config.vertecUrl returns None
    val fieldConfig = createFieldConfig(fieldConfigId)
    val pluginSettings = createSettings(config, fieldConfigId)
    val testee = new SyncService(config, mock[OptionsManager])
    //act
    testee.synchronize(fieldConfig, syncProtocolWriter)
    //assert
    there was one(syncProtocolWriter).log("Not able to create a vertec connection. Check credentials.")
    there was one(syncProtocolWriter).flush
  }

  @Test
  def synchronize_fieldInfoAndThrowsExceptionWhileFetchProjects_logFailedSync() {
    //arrange
    val fieldConfigId = "foo"
    val syncProtocolWriter = createSyncProtocolWriter
    val config = mock[VertecConfig]
    config.vertecUrl returns Some("http://localhost")
    config.vertecUser returns Some("dummy")
    config.vertecPassword returns Some("dummy")

    val fieldConfig = createFieldConfig(fieldConfigId)
    val pluginSettings = createSettings(config, fieldConfigId)
    val testee = new SyncService(config, mock[OptionsManager])
    //act
    testee.synchronize(fieldConfig, syncProtocolWriter)
    //assert
    there was one(syncProtocolWriter).log("No vertec projects found. Check connection and state of Vertec.")
    there was one(syncProtocolWriter).flush
  }

  @Test
  def sync_fieldInfoAndProjects_optionsResort() {
    //arrange
    val fieldConfigId = "a"
    val config = mock[VertecConfig]
    config.vertecMap(fieldConfigId) returns mock[PluginSettings]
    val fieldConfig = mock[FieldConfig]
    fieldConfig.getFieldId returns fieldConfigId
    val optionsManager = mock[OptionsManager]
    val options = mock[Options]
    val rootOptions = new ArrayList[JiraOption]()
    options.getRootOptions() returns rootOptions
    optionsManager.getOptions(fieldConfig) returns options
    val testee = new SyncService(config, optionsManager)
    //act
    testee.sync(fieldConfig, Seq())
    //assert
    there was one(options).sortOptionsByValue(null)
  }

  def createFieldConfig(fieldConfigId: String) =
    mock[FieldConfig].getFieldId returns fieldConfigId

  def createSettings(config: VertecConfig, fieldConfigId: String) = {
    val pluginSettings = mock[PluginSettings]
    config.vertecMap(fieldConfigId) returns pluginSettings
    pluginSettings
  }

  def createSyncProtocolWriter = {
    val syncProtocolWriter = mock[SyncProtocolToSettingsWriter]
    syncProtocolWriter.log(anyString) returns syncProtocolWriter
    syncProtocolWriter
  }

}
