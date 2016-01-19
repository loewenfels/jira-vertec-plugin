package ch.loewenfels.jira.plugin.config

import com.atlassian.sal.api.pluginsettings.PluginSettings
import com.atlassian.jira.issue.customfields.option.{ Option => JiraOption }

class JiraOptionMapper(idMap: PluginSettings) {

  def get(jiraOption: JiraOption): Option[String] = {
    idMap.get(jiraOptionIdAsString(jiraOption)) match {
      case vertecObjId: String => Some(vertecObjId)
      case _ => None
    }
  }

  def put(jiraOption: JiraOption, vertecObjId: String): Object = {
    idMap.put(jiraOptionIdAsString(jiraOption), vertecObjId)
  }

  private def jiraOptionIdAsString(jiraOption: JiraOption) = jiraOption.getOptionId.toString

}
