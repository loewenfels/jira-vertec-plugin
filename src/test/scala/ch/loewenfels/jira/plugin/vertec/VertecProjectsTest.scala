package ch.loewenfels.jira.plugin.vertec

;

import ch.loewenfels.jira.plugin.vertec.VertecProjects.{Project, Projectphase}
import org.junit.Test
import org.scalatest.junit.AssertionsForJUnit
import org.specs2.matcher.{XmlMatchers, ThrownExpectations, MustMatchers}
import org.specs2.mock.Mockito

import scala.xml.Elem

class VertecProjectsTest extends AssertionsForJUnit with Mockito with ThrownExpectations with MustMatchers {
  val ProjectResponseXml =
    <Envelope>
      <Body>
        <QueryResponse>
          <Projekt>
            <objid>123</objid>
            <beschrieb>Mitarbeit eAHV/IV Arbeitsgruppen</beschrieb>
            <code>PROJECTCODE</code>
          </Projekt>
        </QueryResponse>
      </Body>
    </Envelope>
  val PhaseResponseXml =
    <Envelope>
      <Body>
        <QueryResponse>
          <ProjektPhase>
            <objid>12</objid>
            <code>PHASECODE</code>
            <projekt>
              <objref>123</objref>
            </projekt>
          </ProjektPhase>
        </QueryResponse>
      </Body>
    </Envelope>

  val OtherPhaseResponseXml =
    <Envelope>
      <Body>
        <QueryResponse>
          <ProjektPhase>
            <objid>12</objid>
            <code>OTHERPHASECODE</code>
            <projekt>
              <objref>otherId</objref>
            </projekt>
          </ProjektPhase>
        </QueryResponse>
      </Body>
    </Envelope>

  val EmptyResponseXml =
    <Envelope>
      <Body>
        <QueryResponse/>
      </Body>
    </Envelope>

  val TaetigkeitPhaseLinkResponseXml =
    <Envelope>
      <Body>
        <QueryResponse>
          <TaetigkeitPhaseLink>
            <objid>31</objid>
            <taetigkeiten>41</taetigkeiten>
            <phasen>3232</phasen>
          </TaetigkeitPhaseLink>
        </QueryResponse>
      </Body>
    </Envelope>
  val TaetigkeitResponseXml =
    <Envelope>
      <Body>
        <QueryResponse>
          <Taetigkeit>
            <objid>41</objid>
            <code>FOO</code>
          </Taetigkeit>
        </QueryResponse>
      </Body>
    </Envelope>

  @Test def activeProjects_projectHasPhases_projectWithPhases {
    //arrange
    implicit val vertecClientMock = mock[VertecClient]
    vertecClientMock.oclQuery(startWith("projekt->"), any[Elem]) returns ProjectResponseXml
    vertecClientMock.oclQuery(startWith("projektphase"), any[Elem]) returns PhaseResponseXml
    //act
    val result = VertecProjects.allActiveProjects
    //assert
    assert(result.size === 1)
    assert(Project("123", "PROJECTCODE", List(Projectphase("12", "PHASECODE", "123"))) == result.head)
  }

  @Test def activeProjects_projectWithoutPhases_projectWithEmptyPhaseSeq {
    //arrange
    implicit val vertecClientMock = mock[VertecClient]

    vertecClientMock.oclQuery(startWith("projekt->"), any[Elem]) returns ProjectResponseXml
    vertecClientMock.oclQuery(startWith("projektphase"), any[Elem]) returns OtherPhaseResponseXml
    //act
    val result = VertecProjects.allActiveProjects
    //assert
    assert(result.size === 1)
    assert(Project("123", "PROJECTCODE", List()) == result.head)
  }

  @Test def projectPhaseMap_somePhases_map {
    //arrange
    val expected = Projectphase("12", "PHASECODE", "123")
    val testee = Project("123", "PROJECTCODE", List(expected))
    //act
    val actual = testee.phaseMap
    //assert
    assert(Map("12" -> expected) == actual)
  }

  @Test def allAktivePhases_resonse_listWithPhase {
    //arrange
    implicit val vertecClientMock = mock[VertecClient]
    vertecClientMock.oclQuery(startWith("projektphase"), any) returns PhaseResponseXml
    val expected = Projectphase("12", "PHASECODE", "123")
    //act
    val result = VertecProjects.allAktivePhases
    //assert
    assert(List(expected) === result)
  }

  @Test def allAktivePhases_callToVertecClient_oclContainsAktive {
    //arrange
    implicit val vertecClientMock = mock[VertecClient]
    vertecClientMock.oclQuery(startWith("projektphase"), any) returns PhaseResponseXml
    //act
    VertecProjects.allAktivePhases
    //assert
    there was one(vertecClientMock).oclQuery(containing("aktiv"), any)
  }

  @Test def isPhaseAktiv_responseMitObjId_true {
    //arrange
    implicit val vertecClientMock = mock[VertecClient]
    vertecClientMock.oclQuery(startWith("projektphase"), any) returns PhaseResponseXml
    //act
    val result = VertecProjects.isPhaseAktiv("123")
    //assert
    assert(true === result)
  }

  @Test def isPhaseAktiv_emptyResponse_false {
    //arrange
    implicit val vertecClientMock = mock[VertecClient]
    vertecClientMock.oclQuery(startWith("projektphase"), any) returns EmptyResponseXml
    //act
    val result = VertecProjects.isPhaseAktiv("123")
    //assert
    assert(false === result)
  }

  @Test def taetigkeiten_callToVertecClient_ocl {
    //arrange
    implicit val vertecClientMock = mock[VertecClient]
    vertecClientMock.oclQuery(startWith("taetigkeitPhaseLink"), any) returns TaetigkeitPhaseLinkResponseXml
    vertecClientMock.oclQuery(startWith("taetigkeit"), any) returns TaetigkeitResponseXml
    //act
    VertecProjects.taetigkeiten
    //assert
    there was one(vertecClientMock).oclQuery(containing("taetigkeitPhaseLink"), any)
    there was two(vertecClientMock).oclQuery(containing("taetigkeit"), any)

  }

}
