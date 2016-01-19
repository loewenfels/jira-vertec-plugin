package ch.loewenfels.jira.plugin.synchronisation

import java.util.Date
import scala.collection.JavaConverters.asScalaBufferConverter
import scala.collection.mutable.Buffer
import org.slf4j.LoggerFactory
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.fields.config.FieldConfig
import com.atlassian.sal.api.lifecycle.LifecycleAware
import com.atlassian.sal.api.scheduling.PluginScheduler
import ch.loewenfels.jira.plugin.config.VertecConfig
import ch.loewenfels.jira.plugin.customfields.CustomFieldProvider

class BackgroundTaskSchedulerImpl(pluginScheduler: PluginScheduler, config: VertecConfig, customFieldProvider: CustomFieldProvider, syncService: SyncService) extends LifecycleAware with BackgroundTaskScheduler {
  val logger = LoggerFactory.getLogger(classOf[BackgroundTaskSchedulerImpl])
  val name = classOf[BackgroundTaskSchedulerImpl].getName.concat(":job")

  def onStart(): Unit = {
    logger.trace("Starting SyncServiceMonitor")
    reschedule(config.syncIntervalInMs.getOrElse("3600000").toLong)
  }

  def reschedule(interval: Long): Unit = {
    var map = new java.util.HashMap[String, Object]()
    map.put(BackgroundTaskConfig.SERVICE, syncService)
    map.put(BackgroundTaskConfig.FIELD_CONFIG, collectFieldConfigs)
    pluginScheduler.scheduleJob(name, classOf[BackgroundTask], map, new Date(), interval)
    logger.info("Synchronisation task scheduled to run every {}ms", interval)
  }

  private def collectFieldConfigs: Seq[FieldConfig] = {
    for {
      customField <- customFieldProvider.filter(CustomFieldProvider.VertecKey)
      scheme <- customField.getConfigurationSchemes.asScala
    } yield {
      scheme.getOneAndOnlyConfig
    }
  }

}
