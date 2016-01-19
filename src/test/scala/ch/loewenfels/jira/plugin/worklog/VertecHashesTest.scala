package ch.loewenfels.jira.plugin.worklog

import org.scalatest.junit.AssertionsForJUnit
import org.specs2.mock.Mockito
import org.specs2.matcher.ThrownExpectations
import com.atlassian.event.api.EventPublisher
import ch.loewenfels.jira.plugin.config.VertecConfig
import org.junit.Test
import com.atlassian.jira.event.issue.IssueEvent
import com.atlassian.jira.event.`type`.EventType
import com.atlassian.jira.issue.worklog.Worklog
import com.atlassian.jira.issue.Issue
import java.util.Collections
import com.atlassian.crowd.embedded.api.User
import com.atlassian.jira.issue.CustomFieldManager

class VertecHashesTest extends AssertionsForJUnit with Mockito with ThrownExpectations {

  @Test
  def vertecObjId_commentWithVertecHash_vertecObjId {
    //arrange
    val expected="123"
    val comment=s"blabla #VERTEC=$expected blabla"
    //act
    val actual=VertecHashes.vertecObjId(comment)
    //assert
    assert(Some(expected) === actual)
  }
  @Test
  def vertecObjId_commentWithoutVertecHash_none {
    //arrange
    val comment="blablablabla"
    //act
    val actual=VertecHashes.vertecObjId(comment)
    //assert
    assert(None === actual)
  }

    @Test
  def add_commentWithoutVertecHash_vertecHashAdded {
    //arrange
    val expected=VertecHashes.VertecObjectIdHash+"123"
    val comment="blablablabla"
    //act
    val actual=VertecHashes.add(comment,"123")
    //assert
    assert(actual.endsWith(expected))
  }

    @Test
  def add_commentHasVertecHash_vertecHashReplaced {
    //arrange
    val expected=VertecHashes.VertecObjectIdHash+"123"
    val comment="blablablabla "+VertecHashes.VertecObjectIdHash+"333"
    //act
    val actual=VertecHashes.add(comment,"123")
    //assert
    assert(actual.endsWith(expected))
  }
}
