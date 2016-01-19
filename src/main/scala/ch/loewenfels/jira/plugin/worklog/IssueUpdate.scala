package ch.loewenfels.jira.plugin.worklog

import com.atlassian.jira.component.ComponentAccessor
import org.ofbiz.core.entity.GenericValue
import com.atlassian.jira.model.ChangeItem
import scala.collection.JavaConverters.asScalaBufferConverter
import ch.loewenfels.jira.plugin.customfields.CustomFieldProvider

object SummaryChanged extends IssueUpdate {

  def unapply(changelog: GenericValue): Option[GenericValue] = {
    changelog.getRelated("ChildChangeItem").asScala.find { i => hasFieldChanged(Set("Project", "summary"), i) }
  }

}

object VertecChanged extends IssueUpdate {
  def unapply(changelog: GenericValue): Option[GenericValue] = {
    Option(ComponentAccessor.getCustomFieldManager).fold(Option.empty[GenericValue])(manager => new CustomFieldProvider(manager).find(CustomFieldProvider.VertecKey) match {
      case Some(cf) => changelog.getRelated("ChildChangeItem").asScala.find { i => hasFieldChanged(cf.getFieldName, i) }
      case _ => None
    })
  }
}

trait IssueUpdate {
  def apply(changelog: GenericValue) = changelog
  def unapply(changelog: GenericValue): Option[GenericValue]

  def hasFieldChanged(fieldname: String, item: GenericValue): Boolean = {
    hasFieldChanged(Set(fieldname), item)
  }

  def hasFieldChanged(fieldnames: Set[String], item: GenericValue): Boolean = {
    if (fieldnames.contains(item.get(ChangeItem.FIELD).toString)) {
      val oldString = item.get(ChangeItem.OLDSTRING).toString
      val newString = item.get(ChangeItem.NEWSTRING).toString
      oldString != newString
    } else {
      false
    }
  }
}
