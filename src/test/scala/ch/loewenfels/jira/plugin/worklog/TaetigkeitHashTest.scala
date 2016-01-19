package ch.loewenfels.jira.plugin.worklog

import org.junit.Test
import org.scalatest.junit.AssertionsForJUnit
import org.specs2.matcher.ThrownExpectations
import org.specs2.mock.Mockito

class TaetigkeitHashTest extends AssertionsForJUnit with Mockito with ThrownExpectations {

  @Test
  def vertecObjId_commentWithVertecHash_vertecObjId {
    //arrange
    val expected="123"
    val comment=s"blabla #TAT=$expected blabla"
    //act
    val actual=TaetigkeitHash.vertecObjId(comment)
    //assert
    assert(Some(expected) == actual)
  }
  @Test
  def vertecObjId_commentWithoutVertecHash_none {
    //arrange
    val comment="blablablabla"
    //act
    val actual=TaetigkeitHash.vertecObjId(comment)
    //assert
    assert(None == actual)
  }

    @Test
  def add_commentWithoutVertecHash_vertecHashAdded {
    //arrange
    val expected=TaetigkeitHash.VertecObjectIdHash+"123"
    val comment="blablablabla"
    //act
    val actual=TaetigkeitHash.add(comment,"123")
    //assert
    assert(actual.endsWith(expected))
  }

    @Test
  def add_commentHasVertecHash_vertecHashReplaced {
    //arrange
    val expected=TaetigkeitHash.VertecObjectIdHash+"123"
    val comment="blablablabla "+TaetigkeitHash.VertecObjectIdHash+"333"
    //act
    val actual=TaetigkeitHash.add(comment,"123")
    //assert
    assert(actual.endsWith(expected))
  }
}
