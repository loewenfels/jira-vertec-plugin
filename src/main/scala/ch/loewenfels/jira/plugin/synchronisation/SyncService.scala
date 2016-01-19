package ch.loewenfels.jira.plugin.synchronisation

import java.util.Date
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import org.slf4j.LoggerFactory
import com.atlassian.jira.issue.customfields.manager.OptionsManager
import com.atlassian.jira.issue.fields.config.FieldConfig
import ch.loewenfels.jira.plugin.config.VertecConfig
import ch.loewenfels.jira.plugin.vertec.VertecProjects.Project
import ch.loewenfels.jira.plugin.vertec.OptionsSorter
import ch.loewenfels.jira.plugin.vertec.VertecClientBuilder
import ch.loewenfels.jira.plugin.vertec.VertecProjects

/**
 * Synchronizes options for a field info using vertec config
 */
class SyncService(config: VertecConfig, optionsManager: OptionsManager) {

  val logger = LoggerFactory.getLogger(classOf[SyncService])

  def synchronize(fieldConfig: FieldConfig) {
    synchronize(fieldConfig, SyncProtocol.newProtocolWriter(settings(fieldConfig)))
  }

  def synchronize(fieldConfig: FieldConfig, syncProtocol: SyncProtocolWriter) {
    VertecClientBuilder.build(config) match {
      case Some(vertecClient) => {
        Try(VertecProjects.allActiveProjects(vertecClient)) match {
          case (Success(projects)) if projects.nonEmpty =>
            sync(fieldConfig, projects)
          case Failure(e) =>
            syncProtocol.log("Not able to fetch vertec projects:").log(e.getMessage).flush()
          case _ =>
            syncProtocol.log("No vertec projects found. Check credentials.").flush()
        }
      }
      case _ => {
        syncProtocol.log("Not able to create a vertec connection. Check credentials.").flush()
        logger.warn("Vertec not configured")
      }

    }
  }

  private def settings(fieldConfig: FieldConfig) = config.vertecMap(fieldConfig.getFieldId)

  def sync(fieldConfig: FieldConfig, projects: Seq[Project]) {
    val options = optionsManager.getOptions(fieldConfig)
    val sync = Synchronizer
      .on(settings(fieldConfig))
      .withProjects(projects)
      .toJiraOptions(options)
      .doSync()
    val allOptions = optionsManager.getOptions(fieldConfig)
    OptionsSorter.apply(allOptions)
    logger.info("Options for {} with vertec synchronized", fieldConfig.getName)
  }

}
