package ch.loewenfels.jira.plugin.synchronisation

import java.util.Date

import com.atlassian.jira.issue.fields.config.FieldConfig
trait BackgroundTaskScheduler {

  def reschedule(interval: Long)

}