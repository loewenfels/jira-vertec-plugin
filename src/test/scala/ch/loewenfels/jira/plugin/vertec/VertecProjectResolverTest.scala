package ch.loewenfels.jira.plugin.vertec

import scala.collection.JavaConverters._
import org.scalatest.junit.AssertionsForJUnit
import org.specs2.mock.Mockito
import org.specs2.matcher.ThrownExpectations
import org.junit.Test
import com.atlassian.sal.api.pluginsettings.PluginSettings
import com.atlassian.jira.issue.worklog.Worklog
import ch.loewenfels.jira.plugin.vertec.VertecProjectResolver.VertecIds
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.customfields.option.{ Option => JiraOption }
import com.atlassian.jira.issue.Issue
import java.util.HashMap
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.customfields.CustomFieldType
import ch.loewenfels.jira.plugin.config.JiraOptionMapper

class VertecProjectResolverTest extends AssertionsForJUnit with Mockito with ThrownExpectations {

  @Test def resolve__found {
    // arrange
    val worklog = mock[Worklog]
    val projectOptionField = mock[JiraOption]
    val phaseOptionField = mock[JiraOption]
    val idMap = mock[JiraOptionMapper]
    idMap.get(projectOptionField) returns Some("42")
    idMap.get(phaseOptionField ) returns Some("43")
    val customField = mock[CustomField]
    val issue = mock[Issue]
    val javaMap = new java.util.HashMap[String, JiraOption]()
    javaMap.put(null, projectOptionField)
    javaMap.put("1", phaseOptionField)
    issue.getCustomFieldValue(customField) returns javaMap
    worklog.getIssue() returns issue
    val testee = new VertecProjectResolver(idMap, customField)
    // act
    val result = testee.resolve(worklog)
    // assert
    assert(Some(VertecIds("42", "43")) === result)
  }

  @Test def resolve_noProject_none {
    // arrange
    val idMap = mock[JiraOptionMapper]
    val worklog = mock[Worklog]
    val customField = mock[CustomField]
    val issue = mock[Issue]
    val javaMap = mock[java.util.HashMap[String, JiraOption]]
    issue.getCustomFieldValue(customField) returns javaMap
    worklog.getIssue() returns issue
    val testee = new VertecProjectResolver(idMap, customField)
    // act
    val result = testee.resolve(worklog)
    // assert
    assert(None === result)
  }

  @Test def resolve_noPhase_none {
    // arrange
    val idMap = mock[JiraOptionMapper]
    val worklog = mock[Worklog]
    val issue = mock[Issue]
    val customField = mock[CustomField]
    val javaMap = new java.util.HashMap[String, JiraOption]()
    val projectOptionField = mock[JiraOption]
    javaMap.put(null, projectOptionField)
    idMap.get(projectOptionField) returns Some("42")
    issue.getCustomFieldValue(customField) returns javaMap
    worklog.getIssue() returns issue
    val testee = new VertecProjectResolver(idMap, customField)
    // act
    val result = testee.resolve(worklog)
    // assert
    assert(None === result)
  }

}
