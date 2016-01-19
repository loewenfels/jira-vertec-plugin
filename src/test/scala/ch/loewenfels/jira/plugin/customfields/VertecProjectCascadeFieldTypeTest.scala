package ch.loewenfels.jira.plugin.customfields

import org.junit.Test
import scala.collection.JavaConversions._
import org.scalatest.junit.AssertionsForJUnit
import org.specs2.mock.Mockito
import org.specs2.matcher.ThrownExpectations
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.fields.config.FieldConfig
import com.atlassian.jira.issue.customfields.impl.CascadingSelectCFType
import com.atlassian.jira.issue.customfields.persistence.CustomFieldValuePersister
import com.atlassian.jira.issue.context.JiraContextNode
import com.atlassian.jira.issue.customfields.manager.GenericConfigManager
import com.atlassian.jira.issue.fields.rest.json.beans.JiraBaseUrls
import com.atlassian.jira.issue.customfields.manager.OptionsManager
import com.atlassian.jira.issue.customfields.option.Options
import com.atlassian.jira.issue.customfields.option.{Option => JiraOption}
import ch.loewenfels.jira.plugin.synchronisation.SyncService
import com.atlassian.jira.issue.CustomFieldManager
import scala.collection.mutable.ListBuffer
import com.atlassian.jira.issue.customfields.CustomFieldType
import ch.loewenfels.jira.plugin.config.VertecConfig
class VertecProjectCascadeFieldTypeTest extends AssertionsForJUnit with Mockito with ThrownExpectations {

  @Test
  def getValueFromIssue_issueWithParentIssueHasOption_map() {
    //arrange
    val customFieldValuePersister=mock[CustomFieldValuePersister]
    val optionsManager=mock[OptionsManager]
    val customField=mock[CustomField]
    val issueId=1
    val issue=mockIssue(issueId)
    val expectedJiraOption=mockOption(123L,optionsManager)
    val values=List("123")
    customFieldValuePersister.getValues(customField,issueId,CascadingSelectCFType.CASCADE_VALUE_TYPE,null) returns values
    val testee=new VertecProjectCascadeFieldType(optionsManager, customFieldValuePersister ,  mock[GenericConfigManager], mock[JiraBaseUrls],mock[CustomFieldProvider])
    //act
    val actual=testee.getValueFromIssue(customField, issue)
    //assert
    assert( expectedMap(expectedJiraOption) === actual )
  }

    @Test
  def getValueFromIssue_noOption_null() {
    //arrange
    val issue=mockIssue(1)
    val parentIssue=mockIssue(2)
    val customFieldProvider=mock[CustomFieldProvider]
    customFieldProvider.find(CustomFieldProvider.AgileEpicLinkKey) returns None
    issue.getParentObject returns parentIssue
    val testee=new VertecProjectCascadeFieldType(mock[OptionsManager], mock[CustomFieldValuePersister] ,  mock[GenericConfigManager], mock[JiraBaseUrls],customFieldProvider)
    //act
    val actual=testee.getValueFromIssue(mock[CustomField], issue)
    //assert
    assert( actual === null )
  }

  @Test
  def getValueFromIssue_parentIssueHasOption_map() {
    //arrange
    val customFieldValuePersister=mock[CustomFieldValuePersister]
    val optionsManager=mock[OptionsManager]
    val customField=mock[CustomField]
    val issue=mockIssue(1)
    val parentIssue=mockIssue(2)
    issue.getParentObject returns parentIssue
    val expectedJiraOption=mockOption(123L,optionsManager)
    val values=List("123")
    customFieldValuePersister.getValues(customField,2,CascadingSelectCFType.CASCADE_VALUE_TYPE,null) returns values
    val testee=new VertecProjectCascadeFieldType(optionsManager, customFieldValuePersister ,  mock[GenericConfigManager], mock[JiraBaseUrls],mock[CustomFieldProvider])
    //act
    val actual=testee.getValueFromIssue(customField, issue)
    //assert
    assert( expectedMap(expectedJiraOption) === actual )
  }

  @Test
  def getValueFromIssue_optionOnEpicIssue_map() {
    //arrange
    val customFieldValuePersister=mock[CustomFieldValuePersister]
    val optionsManager=mock[OptionsManager]
    val customFieldProvider=mock[CustomFieldProvider]
    val customField=mock[CustomField]
    val epicLinkCustomField=mock[CustomField]
    customFieldProvider.find(CustomFieldProvider.AgileEpicLinkKey) returns Some(epicLinkCustomField)
    val issue=mockIssue(1)
    val epicIssue=mockIssue(2)
    issue.getCustomFieldValue(epicLinkCustomField) returns epicIssue
    val expectedJiraOption=mockOption(123L,optionsManager)
    val values=List("123")
    customFieldValuePersister.getValues(customField,2,CascadingSelectCFType.CASCADE_VALUE_TYPE,null) returns values
    val testee=new VertecProjectCascadeFieldType(optionsManager, customFieldValuePersister ,  mock[GenericConfigManager], mock[JiraBaseUrls],customFieldProvider)
    //act
    val actual=testee.getValueFromIssue(customField, issue)
    //assert
    assert( expectedMap(expectedJiraOption) === actual )
  }

  def mockIssue(id:Long)={
    val issue=mock[Issue]
    issue.getId returns id
    issue
  }

  def mockOption(id:Long,m:OptionsManager)={
    val o=mock[JiraOption]
    m.findByOptionId(id) returns o
    o
  }

  def expectedMap(o:JiraOption)= {
    val expectedMap=new java.util.HashMap[Long,JiraOption]()
    expectedMap.put(null.asInstanceOf[Long],o)
    expectedMap
  }
}
