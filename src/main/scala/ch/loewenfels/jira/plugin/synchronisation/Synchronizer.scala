package ch.loewenfels.jira.plugin.synchronisation

import scala.collection.JavaConverters.asScalaBufferConverter

import com.atlassian.jira.issue.customfields.option.{ Option => JiraOption }
import com.atlassian.jira.issue.customfields.option.Options
import com.atlassian.sal.api.pluginsettings.PluginSettings

import ch.loewenfels.jira.plugin.config.JiraOptionMapper
import ch.loewenfels.jira.plugin.vertec.VertecProjects.Project
import ch.loewenfels.jira.plugin.vertec.VertecProjects.VertecObject

/**
 * Synchronizes Vertec projects and phases with jira options on given id map (jira option id -> vertec objid)
 *
 */
object Synchronizer {
  type VertecJiraMap = PluginSettings
  case class Builder(idMap: PluginSettings) {
    def withProjects(vertecProjects: Seq[Project]) = new {
      def toJiraOptions(options: Options) = {
        new Synchronizer(idMap, vertecProjects, options)
      }
    }
  }

  def on(idMap: VertecJiraMap) = Builder(idMap)
}

class Synchronizer(idMap: PluginSettings, vertecProjects: Seq[Project], options: Options) {
  val syncLogger = SyncProtocol.newProtocolWriter(idMap)
  val jiraOptionMapper = new JiraOptionMapper(idMap)
  case class UpdateResult[T](jiraOption: JiraOption, vertecObject: T)

  def doSync()={
    val updateResults = update(options.getRootOptions.asScala, vertecProjects)
    updateResults.foreach(updatePhases)
    val updatedProjects = vertecProjects.diff(updateResults.map(_.vertecObject))
    if (createOptionsForProjects(updatedProjects).isEmpty) {
      syncLogger.log("No new or updated vertec projects detected")
    }
    syncLogger.flush()
  }

  def updatePhases(s: UpdateResult[Project])={
    val phases = s.vertecObject.phases
    val updateResults = update(s.jiraOption.getChildOptions.asScala, phases)
    val updatedPhases = updateResults.map(_.vertecObject)
    phases.diff(updatedPhases).foreach { newPhase =>
      createOption(newPhase, Some(s.jiraOption))
    }
  }

  def createOptionsForProjects(projects: Seq[Project]): Seq[JiraOption] = {
    projects.map { project =>
      val o = createOption(project)
      project.phases.foreach(phase => createOption(phase, Some(o)))
      o
    }
  }

  def update[T <: VertecObject](jiraOptions: Seq[JiraOption], vertecObjects: Seq[T]): Seq[UpdateResult[T]] = {
    val found = jiraOptions.map(vertecMapper(vertecObjects))
    jiraOptions.zip(found).collect {
      case (o, Some(p)) =>
        updateJiraOption(o, p.code); Some(UpdateResult(o, p))
      case (o, None) => disableJiraOption(o); None;
    }.flatten
  }

  /**
   * Function for a list of vertec objects, translates given jira option to an optionally vertec object
   */
  def vertecMapper[T <: VertecObject](vertecObjects: Seq[T]): JiraOption => Option[T] = { o =>
    for {
      vertecObjId <- jiraOptionMapper.get(o)
      vertecObject <- vertecObjects.find(_.objid == vertecObjId)
    } yield vertecObject
  }

  def createOption(p: VertecObject, parentOption: Option[JiraOption] = None) = {
    val newOption = options.addOption(parentOption.orNull, p.code)
    jiraOptionMapper.put(newOption, p.objid)
    syncLogger.log(newOption, "added")
    newOption
  }

  def updateJiraOption(jiraOption: JiraOption, code: String) {
    if (code != jiraOption.getValue) {
      options.setValue(jiraOption, code)
      syncLogger.log(jiraOption, "updated")
    }
    if (jiraOption.getDisabled) {
      options.enableOption(jiraOption)
      syncLogger.log(jiraOption, "enabled")
    }
  }

  def disableJiraOption(jiraOption: JiraOption) {
    if (!jiraOption.getDisabled) {
      options.disableOption(jiraOption)
      syncLogger.log(jiraOption, "disabled")
    }
  }

}
