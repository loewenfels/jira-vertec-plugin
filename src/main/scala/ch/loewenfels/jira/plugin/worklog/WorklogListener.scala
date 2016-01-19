package ch.loewenfels.jira.plugin.worklog

import scala.collection.JavaConverters.asScalaBufferConverter
import scala.collection.JavaConverters.collectionAsScalaIterableConverter
import scala.collection.immutable.Iterable
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Buffer
import org.ofbiz.core.entity.GenericValue
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.InitializingBean
import com.atlassian.event.api.EventListener
import com.atlassian.event.api.EventPublisher
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.`type`.EventType
import com.atlassian.jira.event.issue.IssueEvent
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.worklog.Worklog
import com.atlassian.jira.jql.builder.JqlClauseBuilder
import com.atlassian.jira.jql.builder.JqlQueryBuilder
import com.atlassian.jira.web.bean.PagerFilter
import com.atlassian.mail.Email
import com.atlassian.mail.queue.SingleMailQueueItem
import ch.loewenfels.jira.plugin.vertec.VertecBookingService
import ch.loewenfels.jira.plugin.customfields.CustomFieldProvider
import ch.loewenfels.jira.plugin.vertec.VertecBookingService.SuccessInsert
import ch.loewenfels.jira.plugin.vertec.VertecBookingService.BookingFault
import ch.loewenfels.jira.plugin.config.VertecConfig

/**
 * Listener for worklogs, invoked after event
 */
class WorklogListener(eventPublisher: EventPublisher, vertec: VertecBookingService, jira: JiraBookingService, customFieldProvider: CustomFieldProvider, config: VertecConfig) extends InitializingBean with DisposableBean {
  val LOG = LoggerFactory.getLogger(classOf[WorklogListener])

  def afterPropertiesSet(): Unit = eventPublisher.register(this)

  def destroy(): Unit = eventPublisher.unregister(this)

  @EventListener
  def onIssueEvent(issueEvent: IssueEvent): Unit = {
    if (config.isBookingEnabled && hasVertecCustomField(issueEvent.getIssue)) {
      issueEvent.getEventTypeId match {
        case EventType.ISSUE_WORKLOGGED_ID => insert(issueEvent.getWorklog)
        case EventType.ISSUE_WORKLOG_UPDATED_ID => update(issueEvent.getWorklog)
        case EventType.ISSUE_WORKLOG_DELETED_ID => delete(issueEvent.getWorklog)
        case EventType.ISSUE_UPDATED_ID | EventType.ISSUE_MOVED_ID => updateIssue(issueEvent.getIssue, issueEvent.getChangeLog)
        case _ => //do nothing
      }
    }
  }

  def insert(workLog: Worklog): Unit = {
    vertec.insert(workLog) match {
      case SuccessInsert(vertecObjId) => jira.updateComment(workLog, VertecHashes.add(workLog.getComment, vertecObjId))
      case BookingFault(msg) => {
        sendMail("VERTEC: WorkLog not created", msg)
        jira.delete(workLog)
      }
      case _ => LOG.error("No valid result.")
    }
  }

  def delete(workLog: Worklog): Unit = {
    vertec.delete(workLog) match {
      case BookingFault(fault) => sendMail("VERTEC: WorkLog not deleted", fault)
      case _ => //o.k.
    }
  }

  def update(workLog: Worklog): Unit = {
    vertec.delete(workLog) match {
      case BookingFault(fault) => sendMail("VERTEC: WorkLog not updated", fault)
      case _ => insert(workLog)
    }
  }

  def updateIssue(issue: Issue, changelog: GenericValue): Unit = {
    changelog match {
      case SummaryChanged(c) => updateSummary(issue, c)
      case VertecChanged(c) => updateProject(issue, c)
      case _ => //do nothing
    }
  }

  private def updateSummary(issue: Issue, changelog: GenericValue): Unit = {
    LOG.debug("Summary of issue {} has changed: {}", issue.getKey, changelog)
    vertec.updateWorklogText(issue) match {
      case BookingFault(fault) => sendMail("VERTEC: Worklogs could not be renamed", fault)
      case _ => //o.k.
    }
  }

  private def updateProject(issue: Issue, changelog: GenericValue): Unit = {
    LOG.debug("Vertec Field of issue {} has changed: {}", issue.getKey, changelog)
    val issues = collectIssues(issue)
    LOG.debug("Updating worklogs for these issues: {}", issues)
    vertec.updateWorklogProjektPhase(issue, issues) match {
      case BookingFault(fault) => sendMail("VERTEC: Worklogs could not be updated", fault)
      case _ => //o.k.
    }
  }

  private def collectIssues(root: Issue): Seq[Issue] = {
    customFieldProvider.find(CustomFieldProvider.VertecKey) match {
      case Some(cf) =>
        val vertec = root.getCustomFieldValue(cf)
        val sameVertec: Issue => Boolean = issue => Option(issue.getCustomFieldValue(cf)).getOrElse(vertec) == vertec
        collectAllIssues(root).filter(sameVertec)
      case _ => Nil
    }
  }
  private def collectAllIssues(root: Issue): Seq[Issue] = {
    if (isEpos(root)) {
      collectSelfAndSubtasks(root) ++ getAllLinkedByEpic(root).flatMap(collectSelfAndSubtasks)
    } else {
      collectSelfAndSubtasks(root)
    }
  }
  private def collectSelfAndSubtasks(issue: Issue) = issue +: issue.getSubTaskObjects.asScala.toSeq

  private def getAllLinkedByEpic(issue: Issue): Seq[Issue] = {
    customFieldProvider.find(CustomFieldProvider.AgileEpicLinkKey) match {
      case Some(field) =>
        val user = ComponentAccessor.getJiraAuthenticationContext.getUser.getDirectoryUser
        val query = JqlQueryBuilder.newClauseBuilder().customField(field.getIdAsLong).eq(issue.getKey).buildQuery()
        LOG.debug("Search for issues with this JQL-Query {}", query)
        ComponentAccessor.getComponent(classOf[SearchService]).search(user, query, PagerFilter.getUnlimitedFilter).getIssues.asScala
      case _ => Nil
    }
  }

  private def isEpos(issue: Issue): Boolean = {
    customFieldProvider.find(CustomFieldProvider.AgileEpicLabelKey).exists(issue.getCustomFieldValue(_) != null)
  }

  private def sendMail(subject: String, message: String) = {
    val address = ComponentAccessor.getJiraAuthenticationContext.getUser.getEmailAddress
    val mail = new Email(address).setSubject(subject).setBody(message)
    val mailItem = new SingleMailQueueItem(mail)
    ComponentAccessor.getMailQueue.addItem(mailItem)
  }

  private def hasVertecCustomField(issue: Issue) = customFieldProvider.findForIssue(CustomFieldProvider.VertecKey, issue).isDefined
}
