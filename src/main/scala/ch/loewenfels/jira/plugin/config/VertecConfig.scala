package ch.loewenfels.jira.plugin.config
import scala.collection.JavaConversions._
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory
import com.atlassian.sal.api.pluginsettings.PluginSettings
import scala.collection.mutable.ListBuffer
object VertecConfig {
  val VertecUrlKey = "ch.loewenfels.jira.plugin.config.vertecUrl"
  val VertecUserKey = "ch.loewenfels.jira.plugin.config.vertecUser"
  val VertecPasswordKey = "ch.loewenfels.jira.plugin.config.vertecPassword"
  val VertecFieldConfigsKey = "ch.loewenfels.jira.plugin.config.fieldconfigs"
  val SyncIntervalInMs = "ch.loewenfels.jira.plugin.config.sync.IntervalInMs"
  val BookingEnabledKey= "ch.loewenfels.jira.plugin.config.bookingEnabled"
}

class VertecConfig(pluginSettingsFactory: PluginSettingsFactory) {

  def storeVertecUrl(value: String) =
    pluginSettingsFactory.createGlobalSettings().put(VertecConfig.VertecUrlKey, value)

  def vertecUrl: Option[String] = optionalValueForKey(VertecConfig.VertecUrlKey)

  def storeVertecUser(value: String) =
    pluginSettingsFactory.createGlobalSettings().put(VertecConfig.VertecUserKey, value)

  def vertecUser: Option[String] = optionalValueForKey(VertecConfig.VertecUserKey)

  def storeVertecPassword(value: String) =
    pluginSettingsFactory.createGlobalSettings().put(VertecConfig.VertecPasswordKey, value)

  def vertecPassword: Option[String] = optionalValueForKey(VertecConfig.VertecPasswordKey)

  def storeSyncIntervalInMs(ms: String) = pluginSettingsFactory.createGlobalSettings().put(VertecConfig.SyncIntervalInMs, ms)

  def syncIntervalInMs: Option[String] = optionalValueForKey(VertecConfig.SyncIntervalInMs)

  def vertecMap(fieldConfigId: String): PluginSettings = pluginSettingsFactory.createSettingsForKey(fieldConfigId)

  private def optionalValueForKey(key: String): Option[String] = {
    pluginSettingsFactory.createGlobalSettings().get(key) match {
      case s: String => Some(s)
      case _ => None
    }
  }
  def storeBookingEnabled(enabled: Boolean) = pluginSettingsFactory.createGlobalSettings().put(VertecConfig.BookingEnabledKey, enabled.toString)

  def isBookingEnabled:Boolean = optionalValueForKey(VertecConfig.BookingEnabledKey).map(_.toBoolean).getOrElse(false)

}
