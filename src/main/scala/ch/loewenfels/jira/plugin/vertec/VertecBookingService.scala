package ch.loewenfels.jira.plugin.vertec

import scala.collection.JavaConverters.asScalaBufferConverter
import scala.xml.Elem
import scala.xml.NodeBuffer
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.slf4j.LoggerFactory
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.worklog.Worklog
import ch.loewenfels.jira.plugin.config.JiraOptionMapper
import ch.loewenfels.jira.plugin.config.VertecConfig
import ch.loewenfels.jira.plugin.vertec.VertecClient.Fault
import ch.loewenfels.jira.plugin.vertec.VertecClient.InvalidObjidResponse
import ch.loewenfels.jira.plugin.vertec.VertecClient.ObjidResponse
import ch.loewenfels.jira.plugin.vertec.VertecClient.Success
import ch.loewenfels.jira.plugin.vertec.VertecProjectResolver.VertecIds
import ch.loewenfels.jira.plugin.vertec.VertecProjects.Projectphase
import ch.loewenfels.jira.plugin.vertec.VertecProjects.Taetigkeit
import ch.loewenfels.jira.plugin.worklog.TaetigkeitHash
import ch.loewenfels.jira.plugin.worklog.VertecHashes
import ch.loewenfels.jira.plugin.customfields.CustomFieldProvider
import ch.loewenfels.jira.plugin.vertec.VertecBookingService._
object VertecBookingService {
  val VertecDateFormat = DateTimeFormat.forPattern("yyyy-MM-dd")
  sealed trait VertecBookingResponse
  case class SuccessInsert(vertecObjId: String) extends VertecBookingResponse
  case class BookingFault(message: String) extends VertecBookingResponse
  case object SuccessUpdate extends VertecBookingResponse
}

trait VertecBookingLogger {
  private val BookingLogger = LoggerFactory.getLogger(classOf[VertecBookingLogger])
  def inserted(worklog: Worklog, vertecObjId: String) = log("insert", worklog, vertecObjId)
  def deleted(worklog: Worklog, vertecObjId: String) = log("delete", worklog, vertecObjId)
  def updated(worklog: Worklog, vertecObjId: String) = log("update", worklog, vertecObjId)
  private def log(action: String, worklog: Worklog, vertecObjId: String) =
    BookingLogger.info(s"$action $vertecObjId by ${worklog.getAuthorKey} for ${PrettyWorklogPrinter.print(worklog)}")

}

trait VertecBookingServiceLogger {
  private val BookingServiceLogger = LoggerFactory.getLogger(classOf[VertecBookingService])

  def createLoggedBookingFaultNotCreated(worklog: Worklog, reason: String) =
    createLoggedBookingFaultForWorklog(worklog, "created", reason)

  def createLoggedBookingFaultNotUpdated(worklog: Worklog, reason: String) =
    createLoggedBookingFaultForWorklog(worklog, "updated", reason)

  def createLoggedBookingFaultNotUpdated(issue: Issue, reason: String) =
    createLoggedBookingFault(s"${issue.getKey} ${issue.getSummary}", reason)

  def createLoggedBookingFaultNotDeleted(worklog: Worklog, reason: String) =
    createLoggedBookingFaultForWorklog(worklog, "deleted", reason)

  private def createLoggedBookingFaultForWorklog(worklog: Worklog, action: String, reason: String) =
    createLoggedBookingFault(s"${worklog.getAuthorKey()}: Worklog ${PrettyWorklogPrinter.print(worklog)} could not be $action", reason)

  private def createLoggedBookingFault(context: String, reason: String) = {
    val msg = s"$context: $reason"
    BookingServiceLogger.info(msg)
    BookingFault(msg)
  }
}

object PrettyWorklogPrinter {
  val PrettyFormat = DateTimeFormat.forPattern("dd.MM.yyyy")

  def print(worklog: Worklog): String = {
    val prettyDate = PrettyFormat.print(new DateTime(worklog.getStartDate))
    val timeSpentInMinutes = worklog.getTimeSpent / 60
    s"${worklog.getIssue.getKey} at $prettyDate spent $timeSpentInMinutes minutes (id=${worklog.getId})"
  }
}

class VertecBookingService(config: VertecConfig, customFieldProvider: CustomFieldProvider, client: Option[VertecClient], vertecProjectResolver: Option[VertecProjectResolver]) extends VertecBookingLogger with VertecBookingServiceLogger {

  case class WorklogWithXml(worklog: Worklog, vertecObjId: String, xml: Elem)

  private val LOG = LoggerFactory.getLogger(classOf[VertecBookingService])

  def this(config: VertecConfig, customFieldProvider: CustomFieldProvider) = this(config, customFieldProvider, Option.empty, Option.empty)

  private implicit def vertecClient = client.orElse(VertecClientBuilder.build(config)).orNull

  def insert(worklog: Worklog): VertecBookingResponse = {
    resolveVertecIds(worklog) match {
      case Some(vertecIds) if VertecProjects.isActive(vertecIds.phaseId) => insertVertec(worklog, vertecIds)
      case Some(vertecIds) => createLoggedBookingFaultNotCreated(worklog, s"Phase or Project is not active")
      case _ => createLoggedBookingFaultNotCreated(worklog, s"Could not resolve Vertec Project/Phase")
    }
  }

  def updateWorklogText(issue: Issue): VertecBookingResponse = {
    val newText = issue.getKey + " " + issue.getSummary
    val xmls = transformUpdateText(newText, issue)
    if (xmls.nonEmpty) {
      vertecClient.sendRequest(toUpdateXml(xmls)) match {
        case Success(r) =>
          xmls.foreach(i => updated(i.worklog, i.vertecObjId))
          SuccessUpdate
        case Fault(f) =>
          createLoggedBookingFaultNotUpdated(issue, s"Could not update worklogs with text $newText: $f")
        case _ =>
          createLoggedBookingFaultNotUpdated(issue, s"Could not update worklogs with text $newText: Unexpected vertec response.")
      }
    } else SuccessUpdate
  }

  def updateWorklogProjektPhase(parentIssue: Issue, issues: Seq[Issue]): VertecBookingResponse = {
    val xmls = transformUpdateProjektPhase(issues)
    if (xmls.nonEmpty) {
      vertecClient.sendRequest(toUpdateXml(xmls)) match {
        case Success(r) =>
          xmls.foreach(i => updated(i.worklog, i.vertecObjId))
          SuccessUpdate
        case Fault(f) =>
          createLoggedBookingFaultNotUpdated(parentIssue, "Could not update worklogs for issues: $f")
        case _ =>
          createLoggedBookingFaultNotUpdated(parentIssue, "Could not update worklogs for issues: Unexpected vertec response.")
      }
    } else SuccessUpdate
  }

  def delete(workLog: Worklog): VertecBookingResponse = {
    VertecHashes.vertecObjId(workLog.getComment) match {
      case Some(vertecObjid) =>
        vertecClient.sendRequest(transformDelete(vertecObjid)) match {
          case Success(r) =>
            deleted(workLog, vertecObjid)
            SuccessUpdate //o.k.
          case Fault(f) =>
            createLoggedBookingFaultNotDeleted(workLog, f)
          case _ =>
            createLoggedBookingFaultNotDeleted(workLog, "Unexpected vertec response.")
        }
      case _ =>
        createLoggedBookingFaultNotDeleted(workLog, "VertecObjid not found")
    }
  }

  private def insertVertec(worklog: Worklog, vertecIds: VertecIds): VertecBookingResponse = {
    UserResolver.mapEmail.apply(worklog.getAuthorObject.getEmailAddress) match {
      case Some(userObjId) => vertecClient.sendRequest(transformCreate(worklog, userObjId, vertecIds)) match {
        case InvalidObjidResponse(objid) =>
          LOG.error("Worklog could not be created because it's not valid.")
          vertecClient.sendRequest(transformDelete(objid))
          createLoggedBookingFaultNotCreated(worklog, "Booking is not valid")
        case ObjidResponse(objid) =>
          LOG.debug("inserted worklog with id '{}' in vertec", objid)
          inserted(worklog, objid)
          SuccessInsert(objid)
        case Fault(f) =>
          createLoggedBookingFaultNotCreated(worklog, f)
        case _ =>
          createLoggedBookingFaultNotCreated(worklog, "Unexpected vertec response.")
      }
      case _ =>
        createLoggedBookingFaultNotCreated(worklog, s"Could not find vertec user with email ${worklog.getAuthorObject.getEmailAddress}")
    }
  }

  private def transformCreate(w: Worklog, objid: String, vertecIds: VertecIds) = {
    val taetigkeit = sucheTaetigkeiten(TaetigkeitHash.vertecObjId(w.getComment))
    val startDate = new DateTime(w.getStartDate)
    <Create>
      <OffeneLeistung>
        <bearbeiter><objref>{ objid }</objref></bearbeiter>
        <datum>{ VertecDateFormat.print(startDate) }</datum>
        <projekt><objref>{ vertecIds.projectId }</objref></projekt>
        <phase><objref>{ vertecIds.phaseId }</objref></phase>
        <minutenintvon>{ startDate.getMinuteOfDay }</minutenintvon>
        <minutenintbis>{ (startDate.getMinuteOfDay + (w.getTimeSpent / 60)) }</minutenintbis>
        <text>{ w.getIssue.getKey + " " + w.getIssue.getSummary }</text>
        <typ><objref>{ taetigkeit.getOrElse(Taetigkeit("", "", "")).objid }</objref></typ>
      </OffeneLeistung>
    </Create>
  }

  private def sucheTaetigkeiten(taetigkeit: Option[String]) = taetigkeit match {
    case Some(tat) => VertecProjects.taetigkeiten.find(_.phase == tat)
    case _ => None
  }

  private def transformDelete(vertecObjId: String) = {
    <Delete>
      <OffeneLeistung>
        <objref>{ vertecObjId }</objref>
      </OffeneLeistung>
    </Delete>
  }

  private def transformUpdateText(newText: String, issue: Issue): Seq[WorklogWithXml] = {
    for {
      w <- getWorklogsFor(issue)
      id <- VertecHashes.vertecObjId(w.getComment)
    } yield {
      WorklogWithXml(w, id, <OffeneLeistung><objref>{ id }</objref><text>{ newText }</text></OffeneLeistung>)
    }
  }

  private def transformUpdateProjektPhase(issues: Seq[Issue]): Seq[WorklogWithXml] = {
    for {
      issue <- issues
      w <- getWorklogsFor(issue)
      id <- VertecHashes.vertecObjId(w.getComment)
      vertecIds <- resolveVertecIds(w)
    } yield {
      WorklogWithXml(w, id,
        <OffeneLeistung>
          <objref>{ id }</objref>
          <projekt><objref>{ vertecIds.projectId }</objref></projekt>
          <phase><objref>{ vertecIds.phaseId }</objref></phase>
        </OffeneLeistung>)
    }
  }

  private def toUpdateXml(nodes: Seq[WorklogWithXml]): Elem = <Update>{ nodes.map(_.xml) }</Update>

  private def getWorklogsFor(issue: Issue) = ComponentAccessor.getWorklogManager.getByIssue(issue).asScala.filterNot { x => x == null }

  private def resolveVertecIds(worklog: Worklog): Option[VertecIds] = {
    vertecProjectResolver.orElse(vertecProjectResolver(worklog.getIssue)).flatMap { r => r.resolve(worklog) }
  }

  private def vertecProjectResolver(issue: Issue): Option[VertecProjectResolver] = {
    for {
      customField <- customFieldProvider.find(CustomFieldProvider.VertecKey)
    } yield {
      val fieldConfig = customField.getRelevantConfig(issue)
      val idMap = config.vertecMap(fieldConfig.getFieldId)
      new VertecProjectResolver(new JiraOptionMapper(idMap), customField)
    }
  }

}
