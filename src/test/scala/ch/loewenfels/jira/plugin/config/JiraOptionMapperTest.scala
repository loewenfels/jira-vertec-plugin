package ch.loewenfels.jira.plugin.config

import org.scalatest.junit.AssertionsForJUnit
import org.specs2.matcher.ThrownExpectations
import org.specs2.mock.Mockito
import org.junit.Test
import com.atlassian.sal.api.pluginsettings.PluginSettings
import com.atlassian.jira.issue.customfields.option.{ Option => JiraOption }

class JiraOptionMapperTest extends AssertionsForJUnit with Mockito with ThrownExpectations {
  @Test
  def get_jiraOptionNotExist_None {
    //arrange
    val idMap = mock[PluginSettings]
    val testee = new JiraOptionMapper(idMap)
    //act
    val actual = testee.get(mock[JiraOption])
    //assert
    assert(actual === None)
  }

  @Test
  def get_jiraOptionExistInMapping_SomeVertecId {
    //arrange
    val expected = "aVertecId"
    val jiraOptionId = 1L
    val idMap = mock[PluginSettings]
    val jiraOption = mock[JiraOption]
    jiraOption.getOptionId returns jiraOptionId
    idMap.get(jiraOptionId.toString()) returns expected
    val testee = new JiraOptionMapper(idMap)
    //act
    val actual = testee.get(jiraOption)
    //assert
    assert(actual === Some(expected))
  }

  @Test
  def get_jiraOptionExistInMappingButNoString_None {
    //arrange
    val jiraOptionId = 1L
    val idMap = mock[PluginSettings]
    val jiraOption = mock[JiraOption]
    jiraOption.getOptionId returns jiraOptionId
    idMap.get(jiraOptionId.toString()) returns new Object()
    val testee = new JiraOptionMapper(idMap)
    //act
    val actual = testee.get(jiraOption)
    //assert
    assert(actual === None)
  }

  @Test
  def put_jiraOptionWithVertecId_putWithJiraOptionId {
    //arrange
    val jiraOptionId = 1L
    val vertecId = "aVertecId"
    val idMap = mock[PluginSettings]
    val jiraOption = mock[JiraOption]
    jiraOption.getOptionId returns jiraOptionId
    val testee = new JiraOptionMapper(idMap)
    //act
    val actual = testee.put(jiraOption, vertecId)
    //assert
    there was one(idMap).put(jiraOptionId.toString(), vertecId)
  }

}
