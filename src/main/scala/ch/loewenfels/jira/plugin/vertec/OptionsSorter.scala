package ch.loewenfels.jira.plugin.vertec
import scala.collection.JavaConverters._
import com.atlassian.jira.issue.customfields.option.Options

object OptionsSorter {
  def apply(options:Options):Unit={
    options.sortOptionsByValue(null /*parentOption*/)
    options.getRootOptions.asScala.foreach( o=> options.sortOptionsByValue(o))
  }
}
