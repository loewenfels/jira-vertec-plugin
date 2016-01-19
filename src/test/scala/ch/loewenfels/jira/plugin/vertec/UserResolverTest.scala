package ch.loewenfels.jira.plugin.vertec;

import scala.xml.Elem

import org.junit.Test
import org.scalatest.junit.AssertionsForJUnit
import org.specs2.matcher.MustMatchers
import org.specs2.matcher.ThrownExpectations
import org.specs2.mock.Mockito

class UserResolverTest extends AssertionsForJUnit with Mockito with ThrownExpectations with MustMatchers {

  @Test def mapEmail_activeUserFound_someVertecUserId {
    //arrange
    val vertecClient = mock[VertecClient]
    val email = "foo.bar@loewenfels.ch"
    vertecClient.oclQuery(contain(email).and(contain("select(aktiv)")), any[Elem]) returns ProjektbearbeiterResonseXml
    val testee = UserResolver.mapEmail(vertecClient)
    //act
    val actual = testee.apply(email)
    //assert
    assert(actual === Some("252711"))
  }

  @Test def mapEmail_userNotFound_None {
    //arrange
    val vertecClient = mock[VertecClient]
    val email = "foo.bar@loewenfels.ch"
    vertecClient.oclQuery(contain(email), any[Elem]) returns <Envelope/>
    val testee = UserResolver.mapEmail(vertecClient)
    //act
    val actual = testee.apply(email)
    //assert
    assert(actual === None)
  }

  val ProjektbearbeiterResonseXml = <Envelope>
                                      <Body>
                                        <QueryResponse>
                                          <Projektbearbeiter>
                                            <objid>252711</objid>
                                          </Projektbearbeiter>
                                        </QueryResponse>
                                      </Body>
                                    </Envelope>
}
