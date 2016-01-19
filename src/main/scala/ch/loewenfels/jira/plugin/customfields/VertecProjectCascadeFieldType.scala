package ch.loewenfels.jira.plugin.customfields

import java.util.Map
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.customfields.impl.CascadingSelectCFType
import com.atlassian.jira.issue.customfields.persistence.CustomFieldValuePersister
import com.atlassian.jira.issue.customfields.manager.GenericConfigManager
import com.atlassian.jira.issue.fields.rest.json.beans.JiraBaseUrls
import com.atlassian.jira.issue.customfields.manager.OptionsManager
import com.atlassian.jira.issue.customfields.option.{ Option => JiraOption }
import com.atlassian.jira.issue.CustomFieldManager

class VertecProjectCascadeFieldType(optionsManager: OptionsManager, customFieldValuePersister: CustomFieldValuePersister, genericConfigManager: GenericConfigManager, jiraBaseUrls: JiraBaseUrls, customFieldProvider: CustomFieldProvider)
  extends CascadingSelectCFType(optionsManager: OptionsManager, customFieldValuePersister: CustomFieldValuePersister, genericConfigManager: GenericConfigManager, jiraBaseUrls: JiraBaseUrls) {

  override def getValueFromIssue(field: CustomField, issue: Issue): Map[String, JiraOption] = {
    Option(super.getValueFromIssue(field, issue))
      .orElse(getValueFromParentIssue(field, issue))
      .orElse(getValueFromEpos(field, issue))
      .orNull
  }

  private lazy val epicLinkCustomField = customFieldProvider.find(CustomFieldProvider.AgileEpicLinkKey)

  private def getValueFromEpos(field: CustomField, issue: Issue): Option[Map[String, JiraOption]] = {
    for {
      c <- epicLinkCustomField
      eposIssue <- Option(issue.getCustomFieldValue(c)) if eposIssue.isInstanceOf[Issue]
      v <- Option(getValueFromIssue(field, eposIssue.asInstanceOf[Issue]))
    } yield v
  }

  private def getValueFromParentIssue(field: CustomField, issue: Issue): Option[Map[String, JiraOption]] = {
    Option(issue.getParentObject) match {
      case Some(i) => Option(getValueFromIssue(field, i))
      case _ => None
    }
  }

}
