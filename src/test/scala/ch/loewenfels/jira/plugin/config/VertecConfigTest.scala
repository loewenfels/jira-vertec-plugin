package ch.loewenfels.jira.plugin.config

import org.junit.Test
import org.specs2.mock.Mockito
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory
import com.atlassian.sal.api.pluginsettings.PluginSettings
import org.scalatest.junit.AssertionsForJUnit
import org.specs2.matcher.ThrownExpectations
import org.scalatest.Matchers
class VertecConfigTest extends AssertionsForJUnit with Mockito with ThrownExpectations with Matchers {

  @Test
  def storeVertecUrl_aVertecUrl_storedAsGlobalSetting {
    //arrange
    val expected = "aUrl"
    val settingsFactory = mock[PluginSettingsFactory]
    val globalSettings = mock[PluginSettings]
    settingsFactory.createGlobalSettings() returns globalSettings
    val testee = new VertecConfig(settingsFactory)
    //act
    testee.storeVertecUrl(expected)
    //assert
    there was one(globalSettings).put(VertecConfig.VertecUrlKey, expected)
  }

  @Test
  def storeVertecUser_aVertecUser_storedAsGlobalSetting {
    //arrange
    val expected = "aVertecUser"
    val settingsFactory = mock[PluginSettingsFactory]
    val globalSettings = mock[PluginSettings]
    settingsFactory.createGlobalSettings() returns globalSettings
    val testee = new VertecConfig(settingsFactory)
    //act
    testee.storeVertecUser(expected)
    //assert
    there was one(globalSettings).put(VertecConfig.VertecUserKey, expected)
  }

  @Test
  def storeVertecPassword_aVertecPassword_storedAsGlobalSetting {
    //arrange
    val expected = "aVertecPassword"
    val settingsFactory = mock[PluginSettingsFactory]
    val globalSettings = mock[PluginSettings]
    settingsFactory.createGlobalSettings() returns globalSettings
    val testee = new VertecConfig(settingsFactory)
    //act
    testee.storeVertecPassword(expected)
    //assert
    there was one(globalSettings).put(VertecConfig.VertecPasswordKey, expected)
  }

    @Test
  def storeBookingEnabled_true_storedAsStringInGlobalSetting {
    //arrange
    val expected = "true"
    val settingsFactory = mock[PluginSettingsFactory]
    val globalSettings = mock[PluginSettings]
    settingsFactory.createGlobalSettings() returns globalSettings
    val testee = new VertecConfig(settingsFactory)
    //act
    testee.storeBookingEnabled(expected.toBoolean)
    //assert
    there was one(globalSettings).put(VertecConfig.BookingEnabledKey, expected)
  }

  @Test
  def getVertecUrl_null_None {
    //arrange
    val settingsFactory = mock[PluginSettingsFactory]
    val globalSettings = mock[PluginSettings]
    settingsFactory.createGlobalSettings() returns globalSettings
    val testee = new VertecConfig(settingsFactory)
    //act
    val actual=testee.vertecUrl
    //assert
    assert(None === actual)
  }

  @Test
  def getVertecUrl_storedAsString_SomeString {
    //arrange
    val expected = "aVertecUrl"
    val settingsFactory = mock[PluginSettingsFactory]
    val globalSettings = mock[PluginSettings]
    globalSettings.get(VertecConfig.VertecUrlKey) returns expected
    settingsFactory.createGlobalSettings() returns globalSettings
    val testee = new VertecConfig(settingsFactory)
    //act
    val actual=testee.vertecUrl
    //assert
    assert(Some(expected) === actual)
  }

  @Test
  def getVertecUser_null_None {
    //arrange
    val settingsFactory = mock[PluginSettingsFactory]
    val globalSettings = mock[PluginSettings]
    settingsFactory.createGlobalSettings() returns globalSettings
    val testee = new VertecConfig(settingsFactory)
    //act
    val actual=testee.vertecUser
    //assert
    assert(None === actual)
  }

  @Test
  def getVertecUser_storedAsString_SomeString {
    //arrange
    val expected = "aVertecUser"
    val settingsFactory = mock[PluginSettingsFactory]
    val globalSettings = mock[PluginSettings]
    globalSettings.get(VertecConfig.VertecUserKey) returns expected
    settingsFactory.createGlobalSettings() returns globalSettings
    val testee = new VertecConfig(settingsFactory)
    //act
    val actual=testee.vertecUser
    //assert
    assert(Some(expected) === actual)
  }

  @Test
  def getVertecPassword_null_None {
    //arrange
    val settingsFactory = mock[PluginSettingsFactory]
    val globalSettings = mock[PluginSettings]
    settingsFactory.createGlobalSettings() returns globalSettings
    val testee = new VertecConfig(settingsFactory)
    //act
    val actual=testee.vertecPassword
    //assert
    assert(None === actual)
  }

  @Test
  def getVertecPassword_storedAsString_SomeString {
    //arrange
    val expected = "aVertecPassword"
    val settingsFactory = mock[PluginSettingsFactory]
    val globalSettings = mock[PluginSettings]
    globalSettings.get(VertecConfig.VertecPasswordKey) returns expected
    settingsFactory.createGlobalSettings() returns globalSettings
    val testee = new VertecConfig(settingsFactory)
    //act
    val actual=testee.vertecPassword
    //assert
    assert(Some(expected) === actual)
  }

  @Test
  def isBookingEnabled_null_false {
    //arrange
    val settingsFactory = mock[PluginSettingsFactory]
    settingsFactory.createGlobalSettings() returns mock[PluginSettings]
    val testee = new VertecConfig(settingsFactory)
    //act
    val actual=testee.isBookingEnabled
    //assert
    actual shouldBe false
  }

  @Test
  def isBookingEnabled_storedAsTrue_true {
    //arrange
    val expected=true
    val settingsFactory = mock[PluginSettingsFactory]
    val globalSettings = mock[PluginSettings]
    globalSettings.get(VertecConfig.BookingEnabledKey) returns expected.toString
    settingsFactory.createGlobalSettings() returns globalSettings
    val testee = new VertecConfig(settingsFactory)
    //act
    val actual=testee.isBookingEnabled
    //assert
    actual shouldBe expected
  }

}
