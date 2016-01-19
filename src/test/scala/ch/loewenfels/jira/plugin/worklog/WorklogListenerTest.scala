package ch.loewenfels.jira.plugin.worklog

import java.util.Collections
import scala.collection.JavaConversions.bufferAsJavaList
import scala.collection.mutable.ArrayBuffer
import org.junit.Before
import org.junit.Test
import org.ofbiz.core.entity.GenericValue
import org.scalatest.junit.AssertionsForJUnit
import org.specs2.matcher.Matchers
import org.specs2.matcher.ThrownExpectations
import org.specs2.mock.Mockito
import com.atlassian.crowd.embedded.api.User
import com.atlassian.event.api.EventListener
import com.atlassian.event.api.EventPublisher
import com.atlassian.jira.action.issue.customfields.MockCustomFieldType
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.event.`type`.EventType
import com.atlassian.jira.event.issue.IssueEvent
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.fields.MockCustomField
import com.atlassian.jira.issue.worklog.Worklog
import com.atlassian.jira.issue.worklog.WorklogManager
import com.atlassian.jira.mock.component.MockComponentWorker
import com.atlassian.jira.model.ChangeItem
import com.atlassian.jira.security.JiraAuthenticationContext
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.mail.queue.MailQueue
import com.atlassian.mail.queue.MailQueueItem
import ch.loewenfels.jira.plugin.vertec.VertecBookingService
import com.atlassian.query.Query
import com.atlassian.jira.web.bean.PagerFilter
import com.atlassian.jira.issue.search.SearchResults
import ch.loewenfels.jira.plugin.customfields.CustomFieldProvider
import ch.loewenfels.jira.plugin.vertec.VertecBookingService.{SuccessInsert,SuccessUpdate,BookingFault}
import ch.loewenfels.jira.plugin.config.VertecConfig
import com.atlassian.jira.issue.comments.Comment

class WorklogListenerTest extends AssertionsForJUnit with Mockito with Matchers with ThrownExpectations {
  private val vertec = mock[VertecBookingService]
  private val jira = mock[JiraBookingService]
  private val issue = mock[Issue]
  private val user = mock[ApplicationUser]
  private val mailQueue = mock[MailQueue]
  private val eventPublisher = mock[EventPublisher]
  private val customFieldProvider = mock[CustomFieldProvider]
  private val vertecConfig = mock[VertecConfig]

  @Before
  def setUp() {
    val jiraAuthContext = mockJiraAuthenticationContextWithUser
    new MockComponentWorker()
      .addMock(classOf[JiraAuthenticationContext], jiraAuthContext)
      .addMock(classOf[MailQueue], mailQueue)
      .init();
  }

  private def mockJiraAuthenticationContextWithUser: JiraAuthenticationContext = {
    val jiraAuthContext = mock[JiraAuthenticationContext]
    jiraAuthContext.getUser returns user
  }

  @Test
  def afterPropertiesSet_always_registered {
    //arrange
    val testee = new WorklogListener(eventPublisher, vertec, jira,customFieldProvider,vertecConfig)
    //act
    testee.afterPropertiesSet()
    //assert
    there was one(eventPublisher).register(testee)
  }

  @Test
  def destroy_always_unregistered {
    //arrange
    val testee = new WorklogListener(eventPublisher, vertec, jira,customFieldProvider,vertecConfig)
    //act
    testee.destroy()
    //assert
    there was one(eventPublisher).unregister(testee)
  }

  @Test
  def onIssueEvent_eventWithTypeWorkloggedBookingDisabled_noVertecInteraction {
    //arrange
    val worklog = mockWorklog
    val issueEvent = new IssueEvent(issue, Collections.emptyMap(), mock[User], EventType.ISSUE_WORKLOGGED_ID)
    vertecConfig.isBookingEnabled returns false
    val testee = new WorklogListener(eventPublisher, vertec, jira,customFieldProvider,vertecConfig)
    //act
    testee.onIssueEvent(issueEvent)
    //assert
    there was noCallsTo(customFieldProvider)
    there was noCallsTo(vertec)
  }

  @Test
  def onIssueEvent_eventWithTypeWorklogged_vertecInteraction {
    //arrange
    val worklog = mockWorklog
    val issueEvent = new IssueEvent(issue, Collections.emptyMap(), mock[User], EventType.ISSUE_WORKLOGGED_ID)
    customFieldProvider.findForIssue(CustomFieldProvider.VertecKey, issue) returns Some(mock[CustomField])
    issueEvent.setWorklog(worklog)
    vertec.insert(any) returns SuccessInsert("1")
    vertecConfig.isBookingEnabled returns true
    val testee = new WorklogListener(eventPublisher, vertec, jira,customFieldProvider,vertecConfig)
    //act
    testee.onIssueEvent(issueEvent)
    //assert
    there was one(vertec).insert(worklog)
    there was one(jira).updateComment(be(worklog), endingWith(VertecHashes.VertecObjectIdHash + "1"))

  }

  @Test
  def onIssueEvent_eventWithTypeWorkloggedIssueHasNoVertecCustomField_noInteractionOnVertecConfig {
    //arrange
    val issueEvent = new IssueEvent(issue, Collections.emptyMap(), mock[User], EventType.ISSUE_WORKLOGGED_ID)
    customFieldProvider.findForIssue(CustomFieldProvider.VertecKey, issue) returns None
    val worklog = mock[Worklog]
    issueEvent.setWorklog(worklog)
    val testee = new WorklogListener(eventPublisher, vertec, jira,customFieldProvider,vertecConfig)
    //act
    testee.onIssueEvent(issueEvent)
    //assert
    there was noCallsTo(vertec)
    there was noCallsTo(jira)
  }

  @Test
  def onIssueEvent_eventWithTypeWorklogDeleted_vertecInteraction {
    //arrange
    val issueEvent = new IssueEvent(issue, Collections.emptyMap(), mock[User], EventType.ISSUE_WORKLOG_DELETED_ID)
    customFieldProvider.findForIssue(CustomFieldProvider.VertecKey, issue) returns Some(mock[CustomField])
    val worklog = mockWorklog
    issueEvent.setWorklog(worklog)
    vertec.delete(worklog) returns SuccessUpdate
    vertecConfig.isBookingEnabled returns true
    val testee = new WorklogListener(eventPublisher, vertec, jira,customFieldProvider,vertecConfig)
    //act
    testee.onIssueEvent(issueEvent)
    //assert
    there was one(vertec).delete(worklog)
  }

  @Test
  def onIssueEvent_eventWithTypeWorklogUpdated_vertecInteraction {
    //arrange
    val issueEvent = new IssueEvent(issue, Collections.emptyMap(), mock[User], EventType.ISSUE_WORKLOG_UPDATED_ID)
    customFieldProvider.findForIssue(CustomFieldProvider.VertecKey, issue) returns Some(mock[CustomField])
    val worklog = mockWorklog
    worklog.getComment returns VertecHashes.VertecObjectIdHash + "1"
    issueEvent.setWorklog(worklog)
    vertecConfig.isBookingEnabled returns true
    val testee = new WorklogListener(eventPublisher, vertec, jira,customFieldProvider,vertecConfig)
    vertec.delete(worklog) returns SuccessUpdate
    vertec.insert(any) returns SuccessInsert("2")
    //act
    testee.onIssueEvent(issueEvent)
    //assert
    there was one(vertec).delete(worklog)
    there was one(vertec).insert(worklog)
    there was one(jira).updateComment(be(worklog), matching(VertecHashes.VertecObjectIdHash + "2"))
  }
  

  @Test
  def onIssueEvent_eventWithTypeWorklogUpdatedDeletionNotPossible_VertecNotDeletedEmailToUser {
    //arrange
    val issueEvent = new IssueEvent(issue, Collections.emptyMap(), mock[User], EventType.ISSUE_WORKLOG_UPDATED_ID)
    customFieldProvider.findForIssue(CustomFieldProvider.VertecKey, issue) returns Some(mock[CustomField])
    val worklog = mockWorklog
    worklog.getComment returns VertecHashes.VertecObjectIdHash + "1"
    issueEvent.setWorklog(worklog)
    vertecConfig.isBookingEnabled returns true
    val testee = new WorklogListener(eventPublisher, vertec, jira,customFieldProvider,vertecConfig)
    vertec.delete(worklog) returns BookingFault("Fault")
    vertec.insert(any) returns SuccessInsert("2")
    user.getEmailAddress returns "somebody@loewenfels.ch"
    //act
    testee.onIssueEvent(issueEvent)
    //assert
    there was one(vertec).delete(worklog)
    there was no(vertec).insert(worklog)
    there was no(jira).updateComment(be(worklog), anyString)
    there was one(mailQueue).addItem(any[MailQueueItem])
  }

  @Test
  def onIssueEvent_eventWithOtherType_noInteractionOnVertecConfig {
    //arrange
    val issueEvent = new IssueEvent(issue, Collections.emptyMap(), mock[User], EventType.ISSUE_DELETED_ID)
    customFieldProvider.findForIssue(CustomFieldProvider.VertecKey, issue) returns Some(mock[CustomField])
    val worklog = mock[Worklog]
    issueEvent.setWorklog(worklog)
    val testee = new WorklogListener(eventPublisher, vertec, jira,customFieldProvider,vertecConfig)
    //act
    testee.onIssueEvent(issueEvent)
    //assert
    there was noCallsTo(vertec)
    there was noCallsTo(jira)
  }
  
  @Test
  def onIssueEvent_eventWithTypeIssueUpdated_issueUpdated {
    //arrange
    val changeitem = mockChangeitem("summary", "Foo", "Bar")
    val changelog = mockChangelog(changeitem)
    val issueEvent = new IssueEvent(issue, mock[User], mock[Comment], mock[Worklog], changelog, Collections.emptyMap(), EventType.ISSUE_UPDATED_ID)
    val testee = new WorklogListener(eventPublisher, vertec, jira,customFieldProvider,vertecConfig)
    vertecConfig.isBookingEnabled returns true
    customFieldProvider.findForIssue(CustomFieldProvider.VertecKey, issue) returns Some(mock[CustomField])
    //act
    testee.onIssueEvent(issueEvent)
    //assert
    there was one(vertec).updateWorklogText(issue)
  }  
  
  @Test
  def onIssueEvent_eventWithTypeIssueMoved_issueUpdated {
    //arrange
    val changeitem = mockChangeitem("Project", "SUP", "DEV")
    val changelog = mockChangelog(changeitem)
    val issueEvent = new IssueEvent(issue, mock[User], mock[Comment], mock[Worklog], changelog, Collections.emptyMap(), EventType.ISSUE_MOVED_ID)
    val testee = new WorklogListener(eventPublisher, vertec, jira,customFieldProvider,vertecConfig)
    vertecConfig.isBookingEnabled returns true
    customFieldProvider.findForIssue(CustomFieldProvider.VertecKey, issue) returns Some(mock[CustomField])
    //act
    testee.onIssueEvent(issueEvent)
    //assert
    there was one(vertec).updateWorklogText(issue)
  }

  @Test
  def updateIssue_EmptyChangelog_NoUpdate {
    //arrange
    val changelog = mock[GenericValue]
    val testee = new WorklogListener(eventPublisher, vertec, jira,customFieldProvider,vertecConfig)
    //act
    testee.updateIssue(issue, changelog)
    //assert
    there was noCallsTo(vertec)
  }

  @Test
  def updateIssue_ChangelogForDescription_NoUpdate {
    //arrange
    val changeitem = mockChangeitem("description", "old", "new")
    val changelog = mockChangelog(changeitem)
    val testee = new WorklogListener(eventPublisher, vertec, jira,customFieldProvider,vertecConfig)
    //act
    testee.updateIssue(issue, changelog)
    //assert
    there was noCallsTo(vertec)
  }

  @Test
  def updateIssue_SummaryChanged_Update {
    //arrange
    val changeitem = mockChangeitem("summary", "old", "new")
    val changelog = mockChangelog(changeitem)
    val testee = new WorklogListener(eventPublisher, vertec, jira,customFieldProvider,vertecConfig)
    //act
    testee.updateIssue(issue, changelog)
    //assert
    there was one(vertec).updateWorklogText(issue)
  }

  @Test
  def updateIssue_SummaryChangedVertecError_Email {
    //arrange
    val changeitem = mockChangeitem("summary", "old", "new")
    val changelog = mockChangelog(changeitem)
    val testee = new WorklogListener(eventPublisher, vertec, jira,customFieldProvider,vertecConfig)
    vertec.updateWorklogText(issue) returns BookingFault("Fault")
    user.getEmailAddress returns "somebody@loewenfels.ch"
    //act
    testee.updateIssue(issue, changelog)
    //assert
    there was one(vertec).updateWorklogText(issue)
    there was one(mailQueue).addItem(any[MailQueueItem])
  }

  @Test
  def updateIssue_VertecChangedOnIssue_AllIssueWithSameVertecValueUpdated {
    val customfield = createCustomField("ch.loewenfels.jira.plugin.jira-vertec-plugin:vertec.project.cascading", "vertec")
    val customFieldManager = mockCustomFieldManager(customfield)
    customFieldProvider.find(CustomFieldProvider.VertecKey) returns Some(customfield)
    customFieldProvider.find(CustomFieldProvider.AgileEpicLinkKey) returns None
    customFieldProvider.find(CustomFieldProvider.AgileEpicLabelKey) returns None
    val worklogManager = mock[WorklogManager]
    new MockComponentWorker()
        .addMock(classOf[CustomFieldManager], customFieldManager)
        .addMock(classOf[WorklogManager], worklogManager)
        .init();
    trainCustomFieldValue(issue, customfield, "value")
    val subtaskOne = mock[Issue]
    trainCustomFieldValue(subtaskOne, customfield, "value")
    val subtaskTwo = mock[Issue]
    trainCustomFieldValue(subtaskTwo, customfield, "anothervalue")
    val subtaskThree = mock[Issue]
    trainCustomFieldValue(subtaskThree, customfield, null)
    issue.getSubTaskObjects returns ArrayBuffer(subtaskOne, subtaskTwo, subtaskThree)
    val changeitem = mockChangeitem("vertec", "old", "new")
    val changelog = mockChangelog(changeitem)
    val testee = new WorklogListener(eventPublisher, vertec, jira,customFieldProvider,vertecConfig)
    //act
    testee.updateIssue(issue, changelog)
    //assert
    there was one(vertec).updateWorklogProjektPhase(issue, ArrayBuffer(issue, subtaskOne, subtaskThree))
  }

   @Test
  def updateIssue_VertecChangedOnEpos_AllLinkedIssuesWithSameVertecValueUpdated {
    val customfield = createCustomField("ch.loewenfels.jira.plugin.jira-vertec-plugin:vertec.project.cascading", "vertec")
    val epiclinkField = createCustomField("com.pyxis.greenhopper.jira:gh-epic-link", "epic-link")
    val epiclabelField = createCustomField("com.pyxis.greenhopper.jira:gh-epic-label", "epic-label")
    customFieldProvider.find(CustomFieldProvider.VertecKey) returns Some(customfield)
    customFieldProvider.find(CustomFieldProvider.AgileEpicLinkKey) returns Some(epiclinkField)
    customFieldProvider.find(CustomFieldProvider.AgileEpicLabelKey) returns Some(epiclabelField)
    val customFieldManager = mockCustomFieldManager(customfield, epiclinkField, epiclabelField)
    val worklogManager = mock[WorklogManager]
    val jiraAuthContext = mock[JiraAuthenticationContext]
    jiraAuthContext.getUser returns mock[ApplicationUser]
    jiraAuthContext.getUser.getDirectoryUser returns mock[User]
    val searchService = mock[SearchService]
    val searchResults = mock[SearchResults]
    searchService.search(any[User], any[Query], any[PagerFilter[_]]) returns searchResults
    new MockComponentWorker()
        .addMock(classOf[CustomFieldManager], customFieldManager)
        .addMock(classOf[WorklogManager], worklogManager)
        .addMock(classOf[JiraAuthenticationContext], jiraAuthContext)
        .addMock(classOf[SearchService], searchService)
        .init();
    val epos = mock[Issue]
    trainCustomFieldValue(epos, customfield, "value")
    val issueOne = mock[Issue]
    trainCustomFieldValue(issueOne, customfield, "value")
    val issueTwo = mock[Issue]
    trainCustomFieldValue(issueTwo, customfield, "anothervalue")
    val issueThree = mock[Issue]
    trainCustomFieldValue(issueThree, customfield, null)

    val subtaskOne = mock[Issue]
    trainCustomFieldValue(subtaskOne, customfield, "value")
    val subtaskThree = mock[Issue]
    trainCustomFieldValue(subtaskThree, customfield, "value")

    epos.getKey returns "FOO-1"
    epos.getCustomFieldValue(epiclabelField) returns "Epos"
    issueOne.getCustomFieldValue(epiclinkField) returns "FOO-1"
    issueOne.getSubTaskObjects returns ArrayBuffer(subtaskOne)
    issueTwo.getCustomFieldValue(epiclinkField) returns "FOO-1"
    issueThree.getCustomFieldValue(epiclinkField) returns "FOO-1"
    issueThree.getSubTaskObjects returns ArrayBuffer(subtaskThree)
    searchResults.getIssues returns  ArrayBuffer(issueOne, issueTwo, issueThree)
    val changeitem = mockChangeitem("vertec", "old", "new")
    val changelog = mockChangelog(changeitem)
    val testee = new WorklogListener(eventPublisher, vertec, jira,customFieldProvider,vertecConfig)
    //act
    testee.updateIssue(epos, changelog)
    //assert
    there was one(vertec).updateWorklogProjektPhase(epos, ArrayBuffer(epos, issueOne, subtaskOne, issueThree, subtaskThree))
  }

  private def trainCustomFieldValue(issue: Issue, customfield: CustomField, customfieldValue: String) {
    issue.getCustomFieldValue(customfield) returns customfieldValue
  }

  private def createCustomField(key: String, fieldname: String): CustomField = {
      val fieldtype = new MockCustomFieldType(key, s"${fieldname}type")
      new MockCustomField("1", fieldname, fieldtype)
  }

  private def mockCustomFieldManager(customFields: CustomField*): CustomFieldManager = {
    val customFieldManager = mock[CustomFieldManager]
    customFieldManager.getCustomFieldObjects returns bufferAsJavaList(customFields.toBuffer)
    customFieldManager
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

  private def mockWorklog: Worklog = {
    val worklog = mock[Worklog]
    worklog.getComment returns "comment"
  }
}
