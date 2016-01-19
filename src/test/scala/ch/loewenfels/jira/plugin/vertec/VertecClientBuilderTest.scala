package ch.loewenfels.jira.plugin.vertec



import org.junit.Test
import org.specs2.matcher.MustMatchers
import org.specs2.matcher.ThrownExpectations
import org.scalatest.junit.AssertionsForJUnit
import org.specs2.mock.Mockito
import ch.loewenfels.jira.plugin.config.VertecConfig

class VertecClientBuilderTest extends AssertionsForJUnit with Mockito with ThrownExpectations with MustMatchers {
  

  @Test
  def build_configNoUrl_None {
    //arrange
    implicit val config=mock[VertecConfig]
    config.vertecUrl returns None
    //act
    val actual=VertecClientBuilder.build
    //assert
    assert(actual === None)
  }
  
  

  @Test
  def build_configWithUrlNoUser_None {
    //arrange
    implicit val config=mock[VertecConfig]
    config.vertecUser returns None
    config.vertecUrl returns Some("url")
    //act
    val actual=VertecClientBuilder.build
    //assert
    assert(actual === None)
  }

  @Test
  def build_configWithUrlAndUserNoPassword_None {
    //arrange
    implicit val config=mock[VertecConfig]
    config.vertecUrl returns Some("url")
    config.vertecUser returns Some("user")
    config.vertecPassword returns None
    //act
    val actual=VertecClientBuilder.build
    //assert
    assert(actual === None)
  }

  @Test
  def build_configWithUrlAndUserAndPassword_Some {
    //arrange
    implicit val config=mock[VertecConfig]
    config.vertecUrl returns Some("url")
    config.vertecUser returns Some("user")
    config.vertecPassword returns Some("pw")
    //act
    val actual=VertecClientBuilder.build
    //assert
    assert(actual != None)
  }
    
}