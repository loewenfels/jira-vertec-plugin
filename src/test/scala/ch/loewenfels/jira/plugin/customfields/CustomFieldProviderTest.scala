package ch.loewenfels.jira.plugin.customfields

import scala.collection.JavaConverters.asScalaBufferConverter
import scala.collection.JavaConverters.seqAsJavaListConverter
import org.junit.Test
import org.specs2.mock.Mockito
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory
import com.atlassian.sal.api.pluginsettings.PluginSettings
import org.scalatest.junit.AssertionsForJUnit
import org.specs2.matcher.ThrownExpectations
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.fields.CustomField
import scala.collection.mutable.Buffer
import scala.collection.JavaConverters.bufferAsJavaListConverter
import com.atlassian.jira.issue.customfields.CustomFieldType
import org.specs2.matcher.OptionMatchers
import org.scalatest.Matchers._
import com.atlassian.jira.issue.Issue
class CustomFieldProviderTest extends AssertionsForJUnit with Mockito with ThrownExpectations {

  @Test
  def find_noCustomFieldObjects_None: Unit = {
    //arrange
    val testee = new CustomFieldProvider(mock[CustomFieldManager])
    //act
    val result = testee.find("gg")
    //assert
    result shouldBe None
  }

  @Test
  def find_emptyCustomFieldObjects_None: Unit = {
    //arrange
    val manager = mock[CustomFieldManager]
    manager.getCustomFieldObjects returns List().asJava
    val testee = new CustomFieldProvider(manager)
    //act
    val result = testee.find("gg")
    //assert
    result shouldBe None
  }

  @Test
  def find_NotOurCustomFieldObject_None: Unit = {
    //arrange
    val manager = mock[CustomFieldManager]
    manager.getCustomFieldObjects returns List(mock[CustomField]).asJava
    val testee = new CustomFieldProvider(manager)
    //act
    val result = testee.find("gg")
    //assert
    result shouldBe None
  }

  @Test
  def find_OurCustomFieldObject_Some: Unit = {
    //arrange
    val manager = mock[CustomFieldManager]
    val field = mockVertecCustomField("gg", manager)
    val testee = new CustomFieldProvider(manager)
    //act
    val result = testee.find("gg")
    //assert
    result shouldBe Some(field)
  }

    @Test
  def findForIssue_OurCustomFieldObject_Some: Unit = {
    //arrange
    val manager = mock[CustomFieldManager]
    val issue= mock[Issue]
    val field = mockVertecCustomField("gg", manager)
    manager.getCustomFieldObjects(issue) returns List(field).asJava
    val testee = new CustomFieldProvider(manager)
    //act
    val result = testee.findForIssue("gg",issue)
    //assert
    result shouldBe Some(field)
  }

  @Test
  def filter_OurCustomFieldObject_ListContainsCustomField: Unit = {
    //arrange
    val manager = mock[CustomFieldManager]
    val field = mockVertecCustomField("gg", manager)
    val testee = new CustomFieldProvider(manager)
    //act
    val result = testee.filter("gg")
    //assert
    result shouldBe List(field)
  }

  def mockVertecCustomField(expectedKey: String, customFieldManager: CustomFieldManager) = {
    val customField = mock[CustomField]
    val customFieldType = mock[CustomFieldType[_, _]]
    customFieldType.getKey() returns expectedKey
    org.mockito.Mockito.doReturn(customFieldType).when(customField).getCustomFieldType
    customFieldManager.getCustomFieldObjects returns List(customField).asJava
    customField
  }

}
