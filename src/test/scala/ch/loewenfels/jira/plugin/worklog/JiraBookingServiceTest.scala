package ch.loewenfels.jira.plugin.worklog

import org.junit.Ignore
import org.junit.Test
import org.ofbiz.core.entity.GenericEntityException
import org.ofbiz.core.entity.GenericValue
import org.scalatest.junit.AssertionsForJUnit
import org.specs2.matcher.ThrownExpectations
import org.specs2.mock.Mockito

import com.atlassian.crowd.embedded.api.User
import com.atlassian.jira.exception.DataAccessException
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.worklog.OfBizWorklogStore
import com.atlassian.jira.issue.worklog.Worklog
import com.atlassian.jira.issue.worklog.WorklogManager
import com.atlassian.jira.mock.component.MockComponentWorker
import com.atlassian.jira.ofbiz.OfBizDelegator
import com.atlassian.jira.security.JiraAuthenticationContext
import com.atlassian.jira.user.ApplicationUser

class JiraBookingServiceTest extends AssertionsForJUnit with Mockito with ThrownExpectations {

  @Test
  def updateComment_withUpdatedComment_stored {
    //arrange
    val expected = "expected comment"
    val worklogId = 123L
    val worklog = mock[Worklog]
    val worklogGV = mock[GenericValue]
    worklog.getId returns worklogId
    val delegator = mock[OfBizDelegator]
    delegator.findById(OfBizWorklogStore.WORKLOG_ENTITY, worklog.getId()) returns worklogGV
    new MockComponentWorker()
      .addMock(classOf[OfBizDelegator], delegator)
      .init()
    val testee = new JiraBookingService
    //act
    testee.updateComment(worklog, expected)
    //assert
    there was one(worklogGV).set("body", expected)
    there was one(worklogGV).store()
  }

  @Test(expected = classOf[DataAccessException])
  def updateComment_withUpdatedCommentStoreThrowsGenericEntityException_DataAccessException {
    //arrange
    val worklogId = 123L
    val worklog = mock[Worklog]
    val worklogGV = mock[GenericValue]
    worklogGV.store() throws new GenericEntityException()
    worklog.getId returns worklogId
    val delegator = mock[OfBizDelegator]
    delegator.findById(OfBizWorklogStore.WORKLOG_ENTITY, worklog.getId()) returns worklogGV
    new MockComponentWorker()
      .addMock(classOf[OfBizDelegator], delegator)
      .init()

    val testee = new JiraBookingService
    //act & assert
    testee.updateComment(worklog, "comment")
    //assert
  }

  @Test
  def delete_workLog_worklogStoreAndThendelted: Unit = {
    //arrange
    val user = mock[User]
    val jiraAuthContext = mockJiraAuthenticationContextWithUser(user)

    val worklogId = 123L
    val worklog = mockWorklog(worklogId, 600L, 60L)
    val worklogToDelete = mockWorklog(worklogId, 540L, 60L)

    val worklogManager = mock[WorklogManager]
    worklogManager.delete(any[User], any[Worklog], any[Long], any[Boolean]) returns true
    worklogManager.getById(any[Long]) returns worklogToDelete

    val worklogGV = mock[GenericValue]
    val delegator = mock[OfBizDelegator]
    delegator.findById(any[String], any[Long]) returns worklogGV

    new MockComponentWorker()
      .addMock(classOf[JiraAuthenticationContext], jiraAuthContext)
      .addMock(classOf[WorklogManager], worklogManager)
      .addMock(classOf[OfBizDelegator], delegator)
      .init()

    val testee = new JiraBookingService
    //act
    testee.delete(worklog)
    //assert
    there was one(delegator).findById(OfBizWorklogStore.WORKLOG_ENTITY, worklogId)
    there was one(worklogGV).store()
    there was one(worklogManager).getById(worklogId)
    there was one(worklogManager).delete(user, worklogToDelete, 600L, false)
  }

  @Test(expected = classOf[DataAccessException])
  def delete_deleteThrowsGenericEntityException_DataAccessException {
    //arrange
    val user = mock[User]
    val jiraAuthContext = mockJiraAuthenticationContextWithUser(user)
    val worklogManager = mock[WorklogManager]

    val worklogGV = mock[GenericValue]
    val delegator = mock[OfBizDelegator]
    delegator.findById(any[String], any[Long]) returns worklogGV
    worklogGV.store() throws new GenericEntityException()

    new MockComponentWorker()
      .addMock(classOf[JiraAuthenticationContext], jiraAuthContext)
      .addMock(classOf[WorklogManager], worklogManager)
      .addMock(classOf[OfBizDelegator], delegator)
      .init()

    val worklogId = 123L
    val worklog = mockWorklog(worklogId, 540L, 60L)
    val testee = new JiraBookingService
    //act & assert
    testee.delete(worklog)
    //assert
  }

  private def mockJiraAuthenticationContextWithUser(user: User): JiraAuthenticationContext = {
    val jiraAuthContext = mock[JiraAuthenticationContext]
    jiraAuthContext.getUser returns mock[ApplicationUser]
    jiraAuthContext.getUser.getDirectoryUser returns user
    jiraAuthContext
  }

  private def mockWorklog(worklogId: Long, estimate: Long, timeSpent: Long): Worklog = {
    val worklog = mock[Worklog]
    worklog.getIssue returns mock[Issue]
    worklog.getIssue.getEstimate returns estimate
    worklog.getTimeSpent returns timeSpent
    worklog.getId returns worklogId
    worklog
  }

}
