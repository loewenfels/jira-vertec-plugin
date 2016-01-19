package ch.loewenfels.jira.plugin.worklog

import scala.collection.JavaConverters.asScalaBufferConverter
import org.ofbiz.core.entity.GenericEntityException
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.exception.DataAccessException
import com.atlassian.jira.issue.worklog.OfBizWorklogStore
import com.atlassian.jira.issue.worklog.Worklog
import com.atlassian.jira.issue.worklog.WorklogManager
import com.atlassian.jira.ofbiz.OfBizDelegator
import org.springframework.transaction.compensating.support.CompensatingTransactionHolderSupport

class JiraBookingService {

  def delete(implicit worklog: Worklog) {
    val worklogGV = ComponentAccessor.getOfBizDelegator.findById(OfBizWorklogStore.WORKLOG_ENTITY, worklog.getId)
    try {
      worklogGV.store()
      val user = ComponentAccessor.getJiraAuthenticationContext.getUser.getDirectoryUser
      val worklogManager = ComponentAccessor.getWorklogManager
      worklogManager.delete(user, worklogManager.getById(worklog.getId), originalEstimate.orNull, false)
    } catch {
      case e: GenericEntityException => throw new DataAccessException(e);
    }
  }

  def updateComment(worklog: Worklog, updatedComment: String) {
    val worklogGV = ComponentAccessor.getOfBizDelegator.findById(OfBizWorklogStore.WORKLOG_ENTITY, worklog.getId)
    worklogGV.set("body", updatedComment)
    try {
      worklogGV.store()
    } catch {
      case e: GenericEntityException => throw new DataAccessException(e);
    }
  }

  private def originalEstimate(implicit worklog: Worklog): Option[java.lang.Long] = {
    Option(worklog).map { w => Option(w.getIssue).map { i => i.getEstimate }.orNull }
  }

}