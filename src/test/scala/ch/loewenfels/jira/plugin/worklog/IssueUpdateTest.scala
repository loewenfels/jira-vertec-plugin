package ch.loewenfels.jira.plugin.worklog

import scala.collection.JavaConversions.bufferAsJavaList
import scala.collection.mutable.ArrayBuffer
import org.junit.Test
import org.ofbiz.core.entity.GenericValue
import org.scalatest.Matchers.convertToAnyShouldWrapper
import org.scalatest.junit.AssertionsForJUnit
import org.specs2.matcher.Matchers
import org.specs2.matcher.ThrownExpectations
import org.specs2.mock.Mockito
import com.atlassian.jira.action.issue.customfields.MockCustomFieldType
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.fields.MockCustomField
import com.atlassian.jira.mock.component.MockComponentWorker
import com.atlassian.jira.model.ChangeItem

class IssueUpdateTest extends AssertionsForJUnit with Mockito with ThrownExpectations with Matchers {

  @Test
  def summaryChanged_NoChangeItem_None() {
    //arrange
    val changelog = mock[GenericValue]
    //act
    val result = SummaryChanged.unapply(changelog)
    //assert
    result shouldBe None
  }

  @Test
  def summaryChanged_NoValueChange_None() {
    //arrange
    val changeitem = mockChangeitem("summary", "foo", "foo")
    val changelog = mockChangelog(changeitem)
    //act
    val result = SummaryChanged.unapply(changelog)
    //assert
    result shouldBe None
  }

  @Test
  def summaryChanged_ValueChanged_Some() {
    //arrange
    val changeitem = mockChangeitem("summary", "foo", "bar")
    val changelog = mockChangelog(changeitem)
    //act
    val result = SummaryChanged.unapply(changelog)
    //assert
    result shouldBe Some(changeitem)
  }

  @Test
  def summaryChanged_ProjectChanged_Some() {
    //arrange
    val changeitem = mockChangeitem("Project", "SUP", "DEV")
    val changelog = mockChangelog(changeitem)
    //act
    val result = SummaryChanged.unapply(changelog)
    //assert
    result shouldBe Some(changeitem)
  }

  @Test
  def vertecChanged_ValueChanged_Some() {
    //arrange
    trainCustomFieldManagerWithVertecCustomField("vertec")
    val changeitem = mockChangeitem("vertec", "foo", "bar")
    val changelog = mockChangelog(changeitem)
    //act
    val result = VertecChanged.unapply(changelog)
    //assert
    result shouldBe Some(changeitem)
  }

  @Test
  def vertecChanged_SummaryChanged_None() {
    //arrange
    trainCustomFieldManagerWithVertecCustomField("vertec")
    val changeitem = mockChangeitem("summary", "foo", "bar")
    val changelog = mockChangelog(changeitem)
    //act
    val result = VertecChanged.unapply(changelog)
    //assert
    result shouldBe None
  }

  private def trainCustomFieldManagerWithVertecCustomField(fieldname: String) {
    val fieldtype = new MockCustomFieldType("ch.loewenfels.jira.plugin.jira-vertec-plugin:vertec.project.cascading", "vertectype")
    val customField: CustomField = new MockCustomField("id", fieldname, fieldtype)
    val customFieldManager = mock[CustomFieldManager]
    customFieldManager.getCustomFieldObjects returns ArrayBuffer(customField)
    new MockComponentWorker().addMock(classOf[CustomFieldManager], customFieldManager).init();
  }

  private def mockChangelog(changeitem: GenericValue): GenericValue = {
    val changelog = mock[GenericValue]
    changelog.getRelated("ChildChangeItem") returns ArrayBuffer(changeitem)
  }

  private def mockChangeitem(fieldname: String, oldstring: String, newstring: String): GenericValue = {
    val changeitem = mock[GenericValue]
    changeitem.get(ChangeItem.FIELD) returns fieldname
    changeitem.get(ChangeItem.OLDSTRING) returns oldstring
    changeitem.get(ChangeItem.NEWSTRING) returns newstring
  }

}
