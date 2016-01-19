package ch.loewenfels.jira.plugin.config

import scala.collection.JavaConverters.asScalaBufferConverter
import scala.collection.JavaConverters.seqAsJavaListConverter
import org.junit.Test
import org.scalatest.junit.AssertionsForJUnit
import org.specs2.matcher.MustMatchers
import org.specs2.matcher.ThrownExpectations
import org.specs2.matcher.ValueCheck.typedValueCheck
import org.specs2.mock.Mockito
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.customfields.CustomFieldType
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.fields.config.FieldConfig
import com.atlassian.jira.issue.fields.config.FieldConfigScheme
import com.atlassian.jira.mock.component.MockComponentWorker
import com.atlassian.jira.security.JiraAuthenticationContext
import com.atlassian.jira.util.I18nHelper
import com.atlassian.jira.web.HttpServletVariables
import com.atlassian.jira.web.action.RedirectSanitiser
import com.atlassian.sal.api.pluginsettings.PluginSettings
import ch.loewenfels.jira.plugin.synchronisation.BackgroundTaskScheduler
import javax.servlet.http.HttpServletRequest
import scala.collection.mutable.Buffer
import ch.loewenfels.jira.plugin.customfields.CustomFieldProvider
import javax.servlet.http.HttpServletRequest

class VertecConfigActionTest extends AssertionsForJUnit with Mockito with ThrownExpectations with MustMatchers {

  @Test
  def doUpdate_propertiesWithValues_stored() {
    //arrange
    val expectedUrl = "aVertecUrl"
    val expectedUser = "aVertecUser"
    val expectedPassword = "aVertecPassword"
    val expectedIntervalInMs = "5000"
    val expectedIsBookingEnabled=true
    initComponentAccessorWithMock(Map("bookingEnabled"->"true"))
    val config = mock[VertecConfig]
    config.vertecUrl returns Some(expectedUrl)
    config.vertecUser returns Some(expectedUser)
    config.vertecPassword returns Some(expectedPassword)
    config.syncIntervalInMs returns Some(expectedIntervalInMs)
    config.isBookingEnabled returns expectedIsBookingEnabled
    val testee = new VertecConfigAction(config, mock[CustomFieldProvider], mock[BackgroundTaskScheduler])
    //act
    testee.doUpdate()
    //assert
    there was one(config).storeVertecUrl(expectedUrl)
    there was one(config).storeVertecUser(expectedUser)
    there was one(config).storeVertecPassword(expectedPassword)
    there was one(config).storeSyncIntervalInMs(expectedIntervalInMs)
    there was one(config).storeBookingEnabled(expectedIsBookingEnabled)
  }

  @Test
  def doUpdate_propertyBookingEnabledNotInRequest_storedAsFalse() {
    //arrange
    val expectedIsBookingEnabled=false
    initComponentAccessorWithMock(Map())
    val config = mockVertecConfigWithNoneCredentials
    config.isBookingEnabled returns true
    val testee = new VertecConfigAction(config, mock[CustomFieldProvider], mock[BackgroundTaskScheduler])
    //act
    testee.doUpdate()
    //assert
    there was one(config).storeBookingEnabled(expectedIsBookingEnabled)
  }

    @Test
  def doUpdate_propertyBookingEnabledIsInRequest_storedAsTrue() {
    //arrange
    val expectedIsBookingEnabled=true
    val httpParams=Map("bookingEnabled" -> "foo")
    initComponentAccessorWithMock(httpParams)
    val config = mockVertecConfigWithNoneCredentials
    config.isBookingEnabled returns true
    val testee = new VertecConfigAction(config, mock[CustomFieldProvider], mock[BackgroundTaskScheduler])
    //act
    testee.doUpdate()
    //assert
    there was one(config).storeBookingEnabled(expectedIsBookingEnabled)
  }

  @Test
  def getCustomfields_listOfCustomfields_vertecCustomFields {
    //arrange
    val customField = mock[CustomField]
    val customFieldManager = mockCustomFieldProvider(customField)
    val config = mockVertecConfigWithNoneCredentials
    val testee = new VertecConfigAction(config, customFieldManager, mock[BackgroundTaskScheduler])
    //act
    val actual = testee.getCustomfields()
    //assert
    actual.asScala must contain(customField)
  }

  @Test
  def getSchemeInfos_listOfCustomfields_listOfSchemeInfos {
    //arrange
    val customField = mock[CustomField]
    val customFieldManager = mockCustomFieldProvider(customField)
    val scheme = mock[FieldConfigScheme]
    val fieldConfig = mock[FieldConfig]
    val fieldConfigId = "1"
    fieldConfig.getFieldId returns fieldConfigId
    val config = mockVertecConfigWithNoneCredentials
    config.vertecMap(fieldConfigId) returns mock[PluginSettings]
    customField.getConfigurationSchemes returns List(scheme).asJava
    scheme.getOneAndOnlyConfig returns fieldConfig

    val testee = new VertecConfigAction(config, customFieldManager, mock[BackgroundTaskScheduler])
    //act
    val actual = testee.getSchemeInfos()
    //assert
    actual.size() must ===(1)
  }

  @Test
  def doSync_fieldIdAsParam_syncWithFieldInfo {
    //arrange
    val customFieldId = "1"
    val schemeId = "2"
    val customField = mock[CustomField]
    val customFieldManager = mockCustomFieldProvider(customField)
    val fieldConfig = mock[FieldConfig]
    val scheme = mock[FieldConfigScheme]
    scheme.getId() returns schemeId.toLong
    scheme.getOneAndOnlyConfig returns fieldConfig
    customField.getConfigurationSchemes returns List(scheme).asJava
    val httpParams=Map("fieldId" -> customFieldId,"schemeId" -> schemeId)
    initComponentAccessorWithMock(httpParams)
    val config = mockVertecConfigWithNoneCredentials
    config.syncIntervalInMs returns Option("60000")

    val scheduler = mock[BackgroundTaskScheduler]
    val testee = new VertecConfigAction(config, customFieldManager, scheduler)
    //act
    testee.doSync()
    //assert
    there was one(scheduler).reschedule(60000)
  }

  def initComponentAccessorWithMock(httpParams:Map[String,String]) {
    val i18nhelper = mock[I18nHelper]
    val jiraAuthContext = mock[JiraAuthenticationContext]
    jiraAuthContext.getI18nHelper returns i18nhelper
    val requestMock=mock[HttpServletRequest]
    httpParams.foreach{ case (k,v) => requestMock.getParameter(k) returns v }
    val httpVariables=mock[HttpServletVariables]
    httpVariables.getHttpRequest returns requestMock
    new MockComponentWorker()
      .addMock(classOf[RedirectSanitiser], mock[RedirectSanitiser])
      .addMock(classOf[JiraAuthenticationContext], jiraAuthContext)
      .addMock(classOf[HttpServletVariables], httpVariables)
      .init()
  }

  private def mockCustomFieldProvider(customField: CustomField = mock[CustomField]) = {
    val customFieldManager = mock[CustomFieldProvider]
    customFieldManager.filter(CustomFieldProvider.VertecKey) returns Buffer(customField)
    customFieldManager.find(CustomFieldProvider.VertecKey) returns Option(customField)
    customFieldManager
  }

  private def mockVertecConfigWithNoneCredentials = {
    val config = mock[VertecConfig]
    config.vertecUrl returns None
    config.vertecUser returns None
    config.vertecPassword returns None
    config.syncIntervalInMs returns None
    config
  }

}
