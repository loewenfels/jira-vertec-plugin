package ch.loewenfels.jira.plugin.vertec

import org.scalatest.junit.AssertionsForJUnit
import org.specs2.mock.Mockito
import org.junit.Test
import com.atlassian.jira.issue.worklog.Worklog
import com.atlassian.jira.issue.Issue

class PrettyWorklogPrinterTest extends AssertionsForJUnit with Mockito  {

  @Test def print_worklog_stringWithIssueKeyDateAndTimeSpent {
    //arrange
    val worklog = mock[Worklog]

    val issue=mock[Issue]
    issue.getKey returns "DEV-XY"
    worklog.getStartDate returns new java.util.Date(1000000)
    worklog.getIssue returns issue
    worklog.getTimeSpent returns 60*60*2
    worklog.getId returns 5L
    //act
    val actual=PrettyWorklogPrinter.print(worklog)
    //assert
    assert(actual === "DEV-XY at 01.01.1970 spent 120 minutes (id=5)")
  }

}
