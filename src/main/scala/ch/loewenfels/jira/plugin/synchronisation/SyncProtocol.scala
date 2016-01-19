package ch.loewenfels.jira.plugin.synchronisation

import java.text.SimpleDateFormat

import scala.collection.JavaConverters.bufferAsJavaListConverter
import scala.collection.mutable.ListBuffer

import com.atlassian.jira.issue.customfields.option.{ Option => JiraOption }
import com.atlassian.sal.api.pluginsettings.PluginSettings

/**
 * Single point for write and read synchronization protocol
 */
object SyncProtocol {
  val EntriesKey = "syncprotocol"
  val TimeStampKey = "synctimestamp"
  def syncProtocol(settings: PluginSettings): SyncProtocol = new SyncProtocol(settings)
  def newProtocolWriter(settings: PluginSettings): SyncProtocolWriter = new SyncProtocolToSettingsWriter(settings)
}
class SyncProtocol(settings: PluginSettings) {
  lazy val syncProtocol = settings.get(SyncProtocol.EntriesKey) match {
    case logs: java.util.List[String] => logs
    case _ => java.util.Collections.emptyList[String]
  }

  lazy val timestamp = settings.get(SyncProtocol.TimeStampKey) match {
    case t: String => new SimpleDateFormat().format(new java.util.Date(t.toLong))
    case _ => "never"
  }
}

class SyncProtocolToSettingsWriter(settings: PluginSettings) extends SyncProtocolWriter {
  def put(key: String, value: Object): Unit = settings.put(key, value)
}

trait SyncProtocolWriter {
  def put(key: String, value: Object): Unit

  val logBuffer = ListBuffer[String]()

  def log(message: String): SyncProtocolWriter = {
    logBuffer += message
    this
  }

  def log(option: JiraOption, message: String): SyncProtocolWriter = {
    val sb = new StringBuilder()
    if (option.getParentOption != null) {
      sb.append(option.getParentOption.getValue)
      sb.append(" ")
    }
    sb.append(option.getValue)
    sb.append(" ")
    sb.append(message)
    log(sb.mkString)
  }

  def flush()= {
    put(SyncProtocol.EntriesKey, logBuffer.asJava)
    put(SyncProtocol.TimeStampKey, System.currentTimeMillis.toString)
  }
}
