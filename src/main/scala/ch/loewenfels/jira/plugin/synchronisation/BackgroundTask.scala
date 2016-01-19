package ch.loewenfels.jira.plugin.synchronisation

import org.slf4j.LoggerFactory

import com.atlassian.jira.issue.fields.config.FieldConfig
import com.atlassian.sal.api.scheduling.PluginJob

class BackgroundTask extends PluginJob {
  val logger = LoggerFactory.getLogger(classOf[BackgroundTask])

  def execute(jobDataMap: java.util.Map[String, Object]): Unit = {
    logger.trace("Start execution of SyncService in background")
    for {
      syncService <- Option(jobDataMap.get(BackgroundTaskConfig.SERVICE)).map { o => o.asInstanceOf[SyncService] }
      fieldConfig <- Option(jobDataMap.get(BackgroundTaskConfig.FIELD_CONFIG)).map { o => o.asInstanceOf[Seq[FieldConfig]] }
    } yield {
      fieldConfig.foreach { fieldConfig => syncService.synchronize(fieldConfig) }
    }
    logger.trace("Finish execution of SyncService in background")
  }
}

object BackgroundTaskConfig {
  val SERVICE = classOf[BackgroundTask].getName.concat(":service")
  val FIELD_CONFIG = classOf[BackgroundTask].getName.concat(":fieldConfig")
}