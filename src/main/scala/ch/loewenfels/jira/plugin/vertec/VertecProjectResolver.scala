package ch.loewenfels.jira.plugin.vertec

import ch.loewenfels.jira.plugin.config.JiraOptionMapper
import ch.loewenfels.jira.plugin.vertec.VertecProjectResolver.VertecIds
import com.atlassian.jira.issue.customfields.option.{Option => JiraOption}
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.worklog.Worklog
import org.slf4j.LoggerFactory

object VertecProjectResolver {

  case class VertecIds(projectId: String, phaseId: String)

}

class VertecProjectResolver(idMap: JiraOptionMapper, customField: CustomField) {
  val LOG = LoggerFactory.getLogger(classOf[VertecProjectResolver])

  def resolve(worklog: Worklog): Option[VertecIds] = {
    val issue = worklog.getIssue
    issue.getCustomFieldValue(customField) match {
      case m: java.util.Map[String, JiraOption] => evalVertecId(m)
      case _ => None
    }
  }

  def evalVertecId(m: java.util.Map[String, JiraOption]): Option[VertecIds] = {
    for {
      optProject <- Option(m.get(null))
      optPhase <- Option(m.get("1"))
      vertecProjectId <- idMap.get(optProject)
      vertecPhaseId <- idMap.get(optPhase)
    } yield {
      val vertecIds = VertecIds(vertecProjectId, vertecPhaseId)
      LOG.debug("found vertecIds '{}' from issue", vertecIds)
      vertecIds
    }
  }

}
