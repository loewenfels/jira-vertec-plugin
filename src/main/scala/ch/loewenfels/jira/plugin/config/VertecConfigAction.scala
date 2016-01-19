package ch.loewenfels.jira.plugin.config

import scala.beans.BeanProperty
import scala.collection.JavaConverters.asScalaBufferConverter
import scala.collection.JavaConverters.bufferAsJavaListConverter
import scala.collection.JavaConverters.seqAsJavaListConverter
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.fields.config.FieldConfigScheme
import com.atlassian.jira.web.action.JiraWebActionSupport
import com.atlassian.jira.web.action.JiraWebActionSupport.MessageType
import ch.loewenfels.jira.plugin.synchronisation.BackgroundTaskScheduler
import ch.loewenfels.jira.plugin.synchronisation.SyncProtocol
import ch.loewenfels.jira.plugin.customfields.CustomFieldProvider

class VertecConfigAction(config: VertecConfig, customFieldProvider: CustomFieldProvider, scheduler: BackgroundTaskScheduler) extends JiraWebActionSupport {
  /**
   * Represents scheme and related sync protocol
   */
  case class SchemeSyncInfo(@BeanProperty customfieldId: String, @BeanProperty scheme: FieldConfigScheme, @BeanProperty timestamp: String, @BeanProperty protocol: java.util.List[String])

  @BeanProperty
  var vertecUrl = config.vertecUrl.getOrElse(null)
  @BeanProperty
  var vertecUser = config.vertecUser.getOrElse(null)
  @BeanProperty
  var vertecPassword = config.vertecPassword.getOrElse(null)
  @BeanProperty
  var interval: Long = config.syncIntervalInMs.getOrElse("3600000").toLong
  @BeanProperty
  var bookingEnabled:Boolean = config.isBookingEnabled

  @Override
  override def doExecute(): String = MessageType.SUCCESS.asWebParameter()

  def doUpdate(): String = {
    checkParamsForBookingEnabledCheckbox

    config.storeVertecUrl(vertecUrl)
    config.storeVertecUser(vertecUser)
    config.storeVertecPassword(vertecPassword)
    config.storeSyncIntervalInMs(interval.toString)
    config.storeBookingEnabled(bookingEnabled)
    doSync()
  }

  /**
   * Checks if there is a parameter value for bookingEnabled in the request
   * A unchecked html input checkbox does not submit a parameter at all and the bean property will never be notified.
   */
  private def checkParamsForBookingEnabledCheckbox= Option(getHttpRequest.getParameter("bookingEnabled")) match {
    case None => bookingEnabled=false
    case _ => //ok
  }

  def doSync(): String = {
    scheduler.reschedule(interval)
    getRedirect("VertecConfigAction.jspa")
  }

  def getCustomfields(): java.util.List[CustomField] =
    customFieldProvider.filter(CustomFieldProvider.VertecKey).asJava

  def getSchemeInfos(): java.util.List[SchemeSyncInfo] =
    collectSchemeSyncInfos.asJava

  private def collectSchemeSyncInfos: Seq[SchemeSyncInfo] = {
    for {
      customField <- customFieldProvider.filter(CustomFieldProvider.VertecKey)
      scheme <- customField.getConfigurationSchemes.asScala
    } yield {
      val fieldConfig = scheme.getOneAndOnlyConfig
      val relatedConfig = config.vertecMap(fieldConfig.getFieldId)
      val syncInfo = SyncProtocol.syncProtocol(relatedConfig)
      SchemeSyncInfo(customField.getId, scheme, syncInfo.timestamp, syncInfo.syncProtocol)
    }
  }
}
