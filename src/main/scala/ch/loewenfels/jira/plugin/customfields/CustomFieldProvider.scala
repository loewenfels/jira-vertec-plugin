package ch.loewenfels.jira.plugin.customfields

import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.Issue

object CustomFieldProvider {
  val VertecKey="ch.loewenfels.jira.plugin.jira-vertec-plugin:vertec.project.cascading"
  val AgileEpicLinkKey="com.pyxis.greenhopper.jira:gh-epic-link"
  val AgileEpicLabelKey="com.pyxis.greenhopper.jira:gh-epic-label"
}

class CustomFieldProvider(customFieldManager: CustomFieldManager) {

  def find(key:String): Option[CustomField] = customFieldObjects.find(filterFun(key))

  def filter(key:String): Seq[CustomField] = customFieldObjects.filter(filterFun(key))

  def findForIssue(key:String,issue:Issue):Option[CustomField] = {
    import scala.collection.JavaConverters._
    customFieldManager.getCustomFieldObjects(issue).asScala.find(filterFun(key))
  }

  private def filterFun(key:String)= (input:CustomField)=> Option(input.getCustomFieldType).exists(_.getKey == key )

  private def customFieldObjects={
    import scala.collection.JavaConverters._
    customFieldManager.getCustomFieldObjects.asScala
  }

}
