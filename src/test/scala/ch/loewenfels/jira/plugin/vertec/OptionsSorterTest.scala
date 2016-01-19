package ch.loewenfels.jira.plugin.vertec

import org.junit.Test
import org.scalatest.junit.AssertionsForJUnit
import scala.collection.JavaConverters._
import org.specs2.mock.Mockito
import org.specs2.matcher.ThrownExpectations
import com.atlassian.jira.issue.customfields.option.Options
import com.atlassian.jira.issue.customfields.option.{Option => JiraOption}

class OptionsSorterTest extends AssertionsForJUnit with Mockito with ThrownExpectations  {

  @Test
  def apply_rootOptions_sortOptionsByValueCalled() {
    //arrange
    val options=mock[Options]
    options.getRootOptions() returns List[JiraOption]().asJava
    //act
    OptionsSorter.apply(options)
    //assert
    there was one(options).sortOptionsByValue(null)
  }

    @Test
  def apply_rootOptionsWithChild_sortRootOptionByValueCalled() {
    //arrange
    val options=mock[Options]
    val rootOption=mock[JiraOption]
    options.getRootOptions() returns List(rootOption).asJava
    //act
    OptionsSorter.apply(options)
    //assert
    there was one(options).sortOptionsByValue(rootOption)
  }

}
