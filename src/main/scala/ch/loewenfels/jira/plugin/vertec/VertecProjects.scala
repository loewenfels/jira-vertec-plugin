package ch.loewenfels.jira.plugin.vertec

import java.util

import org.slf4j.LoggerFactory

import scala.xml.XML
import scala.xml.NodeSeq
import scala.xml.Node

object VertecProjects {
  val ObjidElem2Text = (n: NodeSeq) => (n \\ "objid").text
  val CodeElem2Text = (n: Node) => (n \ "code").text
  val ObjrefElem2Text = (n: Node) => (n \ "projekt" \ "objref").text
  val TaetigkeitenElem2Text = (n: Node) => (n \ "taetigkeiten").text.trim
  val PhasenElem2Text = (n: Node) => (n \ "phasen").text.trim

  abstract class VertecObject {
    def objid: String
    def code: String
  }
  case class Project(objid: String, code: String,phases:Seq[Projectphase]) extends VertecObject {
    lazy val phaseMap=phases.map( phase => phase.objid -> phase).toMap
  }
  case class Projectphase(objid: String, code: String, objref: String) extends VertecObject

  case class Taetigkeit(objid: String, code: String, phase: String) extends VertecObject

  def allActiveProjects(implicit vertecClient: VertecClient) = {
    val result = vertecClient.oclQuery("projekt->select(aktiv)",
      <Resultdef>
        <member>code</member>
        <member>beschrieb</member>
      </Resultdef>)

    val phasesGroupByProject=allAktivePhases.groupBy(_.objref)
    for (n <- result \\ "Projekt") yield {
      val objid=ObjidElem2Text(n)
      Project(objid, CodeElem2Text(n),phasesGroupByProject.getOrElse(objid, List()))
    }
  }

  def allAktivePhases(implicit vertecClient: VertecClient) = {
    val result = vertecClient.oclQuery("projektphase->select(aktiv)",
      <Resultdef>
        <member>code</member>
        <member>projekt</member>
      </Resultdef>)
    for (n <- result \\ "ProjektPhase")
      yield Projectphase(ObjidElem2Text(n), CodeElem2Text(n), ObjrefElem2Text(n))
  }

  def isPhaseAktiv(phaseObjId:String)(implicit vertecClient: VertecClient) = {
    val result = vertecClient.oclQuery(s"projektphase->select(boldid=$phaseObjId)->select(aktiv)")
    (result \\ "objid").nonEmpty
  }

  def taetigkeiten(implicit vertecClient: VertecClient) = {
    val taetigkeit2Phase = taetigkeitPhaseLink
     val result = vertecClient.oclQuery("taetigkeit",
      <Resultdef>
        <member>code</member>
      </Resultdef>)
    for (n <- result \\ "Taetigkeit")
      yield {
        val objid = ObjidElem2Text(n)
        val phase = taetigkeit2Phase.get(objid)
        Taetigkeit(objid, phase.getOrElse("noPhase"), CodeElem2Text(n))
      }
  }

  private def taetigkeitPhaseLink(implicit vertecClient: VertecClient): Map[String, String] = {
     val result = vertecClient.oclQuery("taetigkeitPhaseLink",
      <Resultdef>
        <member>taetigkeiten</member>
        <member>phasen</member>
      </Resultdef>)
    (for (n <- result \\ "TaetigkeitPhaseLink")
      yield TaetigkeitenElem2Text(n) -> PhasenElem2Text(n)) toMap
  }

}
