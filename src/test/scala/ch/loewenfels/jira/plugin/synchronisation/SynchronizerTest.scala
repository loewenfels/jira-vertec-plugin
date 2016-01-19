package ch.loewenfels.jira.plugin.vertec

import org.junit.Test

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

import org.scalatest.junit.AssertionsForJUnit
import org.specs2.mock.Mockito
import org.specs2.mock.mockito.MockitoMatchers
import org.specs2.matcher.MustMatchers
import org.specs2.matcher.ThrownExpectations
import org.specs2.matcher.Matcher
import org.specs2.matcher.Matchers
import com.atlassian.jira.issue.customfields.option.{ Option => JiraOption }

import com.atlassian.jira.issue.customfields.option.Options
import com.atlassian.sal.api.pluginsettings.PluginSettings

import ch.loewenfels.jira.plugin.vertec.VertecProjects.Project
import ch.loewenfels.jira.plugin.vertec.VertecProjects.Projectphase
import ch.loewenfels.jira.plugin.synchronisation.Synchronizer;

class SynchronizerTest extends AssertionsForJUnit with Mockito with ThrownExpectations with MustMatchers {

  @Test
  def doSync_newVertecProject_newOption {
    //arrange
    val expectedProjectCode = "aProjectCode"
    val expectedOptionId = 5L
    val expectedProjectObjId = "111"
    val a = arranger
      .withNewOption(expectedOptionId, expectedProjectCode)
      .withProject(Project(expectedProjectObjId, expectedProjectCode, Seq()))
    val testee = a.createTestee
    //act
    testee.doSync
    //assert
    there was one(a.options).addOption(null, expectedProjectCode)
    there was one(a.map).put(expectedOptionId.toString, expectedProjectObjId)
  }

  @Test
  def doSync_vertecProjectAsOptionWithSameCode_doNothing {
    //arrange
    val projectObjId = "111"
    val optionId = 5L
    val a = arranger
      .withExistingOption(optionId, "some")
      .withMap(projectObjId, optionId)
      .withProject(Project(projectObjId, "some", Seq()))

    val testee = a.createTestee
    //act
    testee.doSync
    //assert
    there was two(a.options).getRootOptions
    there was noMoreCallsTo(a.options)
    there was two(a.map).put(anyString, anyString) //log
  }

  @Test
  def doSync_vertecProjectAsOptionWithDifferentCode_valueUpdated {
    //arrange
    val projectObjId = "111"
    val optionId = 5L
    val a = arranger
      .withExistingOption(optionId, "some")
      .withMap(projectObjId, optionId)
      .withProject(Project(projectObjId, "updated", Seq()))

    val testee = a.createTestee
    //act
    testee.doSync
    //assert
    there was one(a.options).setValue(eqOptionWithId(optionId), ===("updated"))
  }

  @Test
  def doSync_notExistingVertecProjectAsOption_optionDisabled {
    //arrange
    val projectObjId = "111"
    val optionId = 5L
    val a = arranger
      .withExistingOption(optionId, "some")
      .withMap(projectObjId, optionId)

    val testee = a.createTestee
    //act
    testee.doSync
    //assert
    there was one(a.options).disableOption(eqOptionWithId(optionId))
  }

  @Test
  def doSync_notExistingVertecProjectAsDisabledOption_noInteraction {
    //arrange
    val projectObjId = "111"
    val optionId = 5L
    val a = arranger
      .withExistingOption(optionId, "some")
      .withMap(projectObjId, optionId)
    a.options.getRootOptions().head.getDisabled returns true

    val testee = a.createTestee
    //act
    testee.doSync
    //assert
    there was no(a.options).disableOption(eqOptionWithId(optionId))
  }

  @Test
  def doSync_notExistingVertecProjectphaseAsOption_optionDisabled {
    //arrange
    val projectObjId = "111"
    val projectPhaseObjId = "345"
    val optionId = 5L
    val phaseOptionId = 7L
    val a = arranger
      .withExistingOption(optionId, "some")
      .withExistingChildOption(phaseOptionId, "oldProjectPhase", optionId)
      .withMap(projectObjId, optionId)
      .withMap(projectPhaseObjId, phaseOptionId)
      .withProject(Project(projectObjId, "some", Seq()))

    val testee = a.createTestee
    //act
    testee.doSync
    //assert
    there was one(a.options).disableOption(eqOptionWithId(phaseOptionId))
  }

  @Test
  def doSync_vertecProjectphaseAsOptionDifferentCode_valueUpdated {
    //arrange
    val expectedProjectphaseCode = "updated"
    val projectObjId = "111"
    val projectPhaseObjId = "345"
    val parentOptionId = 5L
    val phaseOptionId = 7L
    val projectPhase = Projectphase(projectPhaseObjId, expectedProjectphaseCode, projectObjId)
    val a = arranger
      .withExistingOption(parentOptionId, "some")
      .withExistingChildOption(phaseOptionId, "oldProjectPhase", parentOptionId)
      .withMap(projectObjId, parentOptionId)
      .withMap(projectPhaseObjId, phaseOptionId)
      .withProject(Project(projectObjId, "some", Seq(projectPhase)))

    val testee = a.createTestee
    //act
    testee.doSync
    //assert
    there was one(a.options).setValue(eqOptionWithId(phaseOptionId), ===("updated"))
    there was no(a.options).addOption(eqOptionWithId(parentOptionId), ===(expectedProjectphaseCode))
  }
  @Test
  def doSync_vertecProjectAsDisabledOption_optionEnabled {
    //arrange
    val projectObjId = "111"
    val optionId = 5L
    val a = arranger
      .withExistingOption(optionId, "some", false)
      .withMap(projectObjId, optionId)
      .withProject(Project(projectObjId, "some", Seq()))

    val testee = a.createTestee
    //act
    testee.doSync
    //assert
    there was one(a.options).enableOption(eqOptionWithId(optionId))
  }

  @Test
  def doSync_vertecProjectWithNewPhase_newOption {
    //arrange
    val projectObjId = "111"
    val expectedProjectphaseCode = "codi"
    val expectedProjectphaseObjId = "123"
    val expectedOptionId = 4L
    val parentOptionId = 5L
    val projectPhase = Projectphase(expectedProjectphaseObjId, expectedProjectphaseCode, projectObjId)
    val a = arranger
      .withExistingOption(parentOptionId, "some")
      .withNewOption(expectedOptionId, expectedProjectphaseCode, Some(parentOptionId))
      .withMap(projectObjId, parentOptionId)
      .withProject(Project(projectObjId, "some", Seq(projectPhase)))
    val newOption = mock[JiraOption]
    val testee = a.createTestee
    //act
    testee.doSync
    //assert
    there was one(a.options).addOption(eqOptionWithId(parentOptionId), ===(expectedProjectphaseCode))
    there was one(a.map).put(expectedOptionId.toString, expectedProjectphaseObjId)
  }

  def arranger = {
    val options = mock[Options]
    val rootOptions = new ListBuffer[JiraOption]()
    options.getRootOptions() returns rootOptions
    Builder(options, Seq(), mock[PluginSettings])
  }

  case class Builder(options: Options, projects: Seq[Project], map: PluginSettings) {

    def mockExistingOption(id: Long, value: String, enabled: Boolean) = {
      val existingOption = mock[JiraOption]
      existingOption.getOptionId returns id
      existingOption.getValue() returns "some"
      existingOption.getDisabled returns !enabled
      existingOption
    }

    def withExistingOption(id: Long, value: String, enabled: Boolean = true) = {
      val existingOption = mockExistingOption(id, value, enabled)
      options.getRootOptions.add(existingOption)
      Builder(options, projects, map)
    }

    def withExistingChildOption(id: Long, value: String, parentId: Long, enabled: Boolean = true) = {
      val existingOption = mockExistingOption(id, value, enabled)
      val parentOption = options.getRootOptions.find(_.getOptionId == parentId)
      parentOption.get.getChildOptions returns List(existingOption)
      Builder(options, projects, map)
    }

    def withNewOption(id: Long, value: String, parentId: Option[Long] = None) = {
      val newOption = mock[JiraOption]
      newOption.getOptionId returns id
      parentId match {
        case Some(parent) => options.addOption(eqOptionWithId(parentId.get), ===(value)) returns newOption
        case None => options.addOption(null, value) returns newOption
      }
      Builder(options, projects, map)
    }

    def withProject(project: Project) = {
      Builder(options, projects :+ project, map)
    }
    def withMap(projObjId: String, optionId: Long) = {
      map.get(optionId.toString()) returns projObjId

      Builder(options, projects, map)
    }

    def createTestee = Synchronizer.on(map).withProjects(projects).toJiraOptions(options)
  }

  //matcher for match an Jira Option with given id
  def eqOptionWithId(id: Long): Matcher[JiraOption] = ((_: JiraOption).getOptionId == id, "option id " + id + " not same")

}
