package ch.loewenfels.jira.plugin.vertec;

import scala.collection.JavaConversions.bufferAsJavaList
import scala.collection.mutable.ArrayBuffer
import scala.xml.Elem
import scala.xml.Node
import org.joda.time.DateTime
import org.junit.Test
import org.scalatest.Inside
import org.scalatest.Matchers.convertToAnyShouldWrapper
import org.scalatest.Matchers.convertToStringShouldWrapper
import org.scalatest.Matchers.include
import org.scalatest.OptionValues.convertOptionToValuable
import org.scalatest.junit.AssertionsForJUnit
import org.specs2.matcher.Matcher
import org.specs2.matcher.Matchers
import org.specs2.matcher.ThrownExpectations
import org.specs2.matcher.XmlMatchers
import org.specs2.mock.Mockito
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.worklog.Worklog
import com.atlassian.jira.issue.worklog.WorklogManager
import com.atlassian.jira.mock.component.MockComponentWorker
import com.atlassian.jira.user.ApplicationUser
import ch.loewenfels.jira.plugin.config.VertecConfig
import ch.loewenfels.jira.plugin.vertec.VertecProjectResolver.VertecIds
import ch.loewenfels.jira.plugin.customfields.CustomFieldProvider
import ch.loewenfels.jira.plugin.vertec.VertecBookingService.{ BookingFault, SuccessUpdate, SuccessInsert }

class VertecBookingServiceTest extends AssertionsForJUnit with Mockito with ThrownExpectations with Inside with Matchers with XmlMatchers {

  /**
   * Adapts given matcher for Seq[Node] to a mockito hamcrest Elem matcher
   * see also {@link http://etorreborre.github.io/specs2/guide/org.specs2.guide.Matchers.html#Custom}
   */
  def argAsElem(matcher: Matcher[Seq[Node]]) = argThat(matcher ^^ ((_: Elem).toSeq))

  @Test
  def delete_worklogWithCommentContainsVertecObjId_vertecDelete {
    //arrange
    val expectedVertecObjId = "123"
    val expectedElement =
      <Delete>
        <OffeneLeistung>
          <objref>{ expectedVertecObjId }</objref>
        </OffeneLeistung>
      </Delete>

    val expected = "Deleted 1 Objects"
    val response = <Envelope><Body><DeleteResponse><text>{ expected }</text></DeleteResponse></Body></Envelope>

    val vertecClient = mock[VertecClient]
    vertecClient.sendRequest(any[Elem]) returns response
    val worklog = mock[Worklog]
    worklog.getIssue returns mock[Issue]
    worklog.getComment() returns s"a comment with #VERTEC=$expectedVertecObjId and other text"
    val testee = new VertecBookingService(mock[VertecConfig], mock[CustomFieldProvider], Option(vertecClient), Option.empty)
    //act
    val result = testee.delete(worklog)
    //arrange
    there was one(vertecClient).sendRequest(argAsElem(equalToIgnoringSpace(expectedElement)))
    result shouldBe SuccessUpdate
  }

  @Test
  def delete_worklogWithCommentContainsVertecObjId_vertecNotDeleted {
    //arrange
    val expectedVertecObjId = "123"
    val expectedElement =
      <Delete>
        <OffeneLeistung>
          <objref>{ expectedVertecObjId }</objref>
        </OffeneLeistung>
      </Delete>

    val response = <Fault>ErrorMessage</Fault>

    val vertecClient = mock[VertecClient]
    vertecClient.sendRequest(any[Elem]) returns response
    val worklog = mock[Worklog]
    worklog.getIssue returns mock[Issue]
    worklog.getComment() returns s"a comment with #VERTEC=$expectedVertecObjId and other text"
    val testee = new VertecBookingService(mock[VertecConfig], mock[CustomFieldProvider], Option(vertecClient), Option.empty)
    //act
    val result = testee.delete(worklog)
    //arrange
    there was one(vertecClient).sendRequest(argAsElem(equalToIgnoringSpace(expectedElement)))

    inside(result) { case BookingFault(msg) => msg should include(response.text) }
  }

  @Test
  def delete_worklogWithCommentContainsVertecObjIdFaultResponse_msgWithIssueKey {
    //arrange
    val expectedVertecObjId = "123"
    val expectedIssueKey = "DEV-XY"
    val expectedElement =
      <Delete>
        <OffeneLeistung>
          <objref>{ expectedVertecObjId }</objref>
        </OffeneLeistung>
      </Delete>

    val response = <Fault>ErrorMessage</Fault>

    val vertecClient = mock[VertecClient]
    vertecClient.sendRequest(any[Elem]) returns response
    val worklog = mock[Worklog]
    val issue = mock[Issue]
    issue.getKey returns expectedIssueKey
    worklog.getIssue returns issue
    worklog.getComment() returns s"a comment with #VERTEC=$expectedVertecObjId and other text"
    val testee = new VertecBookingService(mock[VertecConfig], mock[CustomFieldProvider], Option(vertecClient), Option.empty)
    //act
    val result = testee.delete(worklog)
    //arrange
    inside(result) { case BookingFault(msg) => msg should include(expectedIssueKey) }
  }

  @Test
  def delete_worklogWithCommentWithoutVertecObjId_noCall {
    //arrange
    val vertecClient = mock[VertecClient]
    val worklog = mock[Worklog]
    worklog.getComment() returns "a comment"
    worklog.getIssue returns mock[Issue]
    val testee = new VertecBookingService(mock[VertecConfig], mock[CustomFieldProvider], Option(vertecClient), Option.empty)
    //act
    val result = testee.delete(worklog)
    //arrange
    there was noCallsTo(vertecClient)
    inside(result) { case BookingFault(msg) => msg should include("VertecObjid not found") }
  }

  @Test
  def insert_worklogWithIssueWithVertecProject_someVertecObjId {
    //arrange
    val expectedElement = mockRequestElement
    val expectedVertecObjId = "123"
    val responseElement = mockResponseElement(expectedVertecObjId)
    val vertecClient = mock[VertecClient]
    vertecClient.oclQuery(contain("projektphase"), any[Elem]) returns aktivePhase
    vertecClient.oclQuery(contain("projektbearbeiter"), any[Elem]) returns <objid>252711</objid>
    vertecClient.sendRequest(argAsElem(equalToIgnoringSpace(expectedElement))) returns responseElement
    val worklog = mockWorklog
    val vertecProjectResolver = mock[VertecProjectResolver]
    vertecProjectResolver.resolve(worklog) returns Some(VertecIds("4242", "2424"))
    val testee = new VertecBookingService(mock[VertecConfig], mock[CustomFieldProvider], Option(vertecClient), Option(vertecProjectResolver))
    //act
    val actual = testee.insert(worklog)
    //assert
    actual shouldBe SuccessInsert(expectedVertecObjId)
  }

  @Test
  def insert_worklogWithIssueWithoutVertecProject_bookingFault {
    //arrange
    val vertecClient = mock[VertecClient]
    val worklog = mockWorklog
    val vertecProjectResolver = mock[VertecProjectResolver]
    vertecProjectResolver.resolve(worklog) returns None
    val testee = new VertecBookingService(mock[VertecConfig], mock[CustomFieldProvider], Option(vertecClient), Option(vertecProjectResolver))
    //act
    val actual = testee.insert(worklog)
    //assert
    actual shouldBe BookingFault("tester: Worklog DEV-XY at 16.05.2015 spent 200 minutes (id=0) could not be created: Could not resolve Vertec Project/Phase")
  }

  @Test
  def insert_worklogWithIssueWithVertecProjectNotValid_OffeneLeistungDeleted {
    //arrange
    val request = mockRequestElement
    val expectedVertecObjId = "123"
    val response = mockResponseElement(expectedVertecObjId, 0)
    val vertecClient = mock[VertecClient]
    vertecClient.oclQuery(contain("projektphase"), any[Elem]) returns aktivePhase
    vertecClient.oclQuery(contain("projektbearbeiter"), any[Elem]) returns <objid>252711</objid>
    vertecClient.sendRequest(argAsElem(equalToIgnoringSpace(request))) returns response
    val worklog = mockWorklog
    val vertecProjectResolver = mock[VertecProjectResolver]
    vertecProjectResolver.resolve(worklog) returns Some(VertecIds("4242", "2424"))
    val testee = new VertecBookingService(mock[VertecConfig], mock[CustomFieldProvider], Option(vertecClient), Option(vertecProjectResolver))
    //act
    val actual = testee.insert(worklog)
    //assert
    actual shouldBe BookingFault("tester: Worklog DEV-XY at 16.05.2015 spent 200 minutes (id=0) could not be created: Booking is not valid")

    val expectedElement = <Delete><OffeneLeistung><objref>{ expectedVertecObjId }</objref></OffeneLeistung></Delete>
    there was one(vertecClient).sendRequest(argAsElem(equalToIgnoringSpace(expectedElement)))
  }

  @Test
  def insert_worklogWithIssueWithVertecProjectResponseFault_bookingFault {
    //arrange
    val request = mockRequestElement
    val expectedVertecObjId = "123"
    val response = <Fault>ErrorMessage</Fault>
    val vertecClient = mock[VertecClient]
    vertecClient.oclQuery(contain("projektphase"), any[Elem]) returns aktivePhase
    vertecClient.oclQuery(contain("projektbearbeiter"), any[Elem]) returns <objid>252711</objid>
    vertecClient.sendRequest(argAsElem(equalToIgnoringSpace(request))) returns response
    val worklog = mockWorklog
    val vertecProjectResolver = mock[VertecProjectResolver]
    vertecProjectResolver.resolve(worklog) returns Some(VertecIds("4242", "2424"))
    val testee = new VertecBookingService(mock[VertecConfig], mock[CustomFieldProvider], Option(vertecClient), Option(vertecProjectResolver))
    //act
    val actual = testee.insert(worklog)
    //assert
    actual shouldBe BookingFault("tester: Worklog DEV-XY at 16.05.2015 spent 200 minutes (id=0) could not be created: ErrorMessage")
  }

  @Test
  def insert_worklogWithIssueWithVertecProjectPhaseNotActive_BookingFault {
    //arrange
    val vertecClient = mock[VertecClient]
    vertecClient.oclQuery(contain("projektphase"), any[Elem]) returns <Envelope/>
    val worklog = mockWorklog
    val vertecProjectResolver = mock[VertecProjectResolver]
    vertecProjectResolver.resolve(worklog) returns Some(VertecIds("4242", "2424"))
    val testee = new VertecBookingService(mock[VertecConfig], mock[CustomFieldProvider], Option(vertecClient), Option(vertecProjectResolver))
    //act
    val actual = testee.insert(worklog)
    //assert
    actual shouldBe BookingFault("tester: Worklog DEV-XY at 16.05.2015 spent 200 minutes (id=0) could not be created: Phase is not active")
  }

  @Test
  def updateWorklogText_IssueWithTwoWorklogs_BothWorklogsUpdated {
    //arrange
    val vertecClient = mock[VertecClient]
    val vertecProjectResolver = mock[VertecProjectResolver]
    val testee = new VertecBookingService(mock[VertecConfig], mock[CustomFieldProvider], Option(vertecClient), Option(vertecProjectResolver))
    val issue = mock[Issue]
    issue.getKey returns "FOO-1"
    issue.getSummary returns "Summary"
    val worklogManager = mock[WorklogManager]
    worklogManager.getByIssue(issue) returns ArrayBuffer(mockWorklog("#VERTEC=123"), mockWorklog("#VERTEC=234"))
    new MockComponentWorker().addMock(classOf[WorklogManager], worklogManager).init();
    vertecClient.sendRequest(any[Elem]) returns <Envelope><Body><UpdateResponse><text>Updated 2 Objects</text></UpdateResponse></Body></Envelope>
    //act
    val response = testee.updateWorklogText(issue)
    //assert
    response shouldBe SuccessUpdate
    val expectedRequest = <Update><OffeneLeistung><objref>123</objref><text>FOO-1 Summary</text></OffeneLeistung><OffeneLeistung><objref>234</objref><text>FOO-1 Summary</text></OffeneLeistung></Update>
    there was one(vertecClient).sendRequest(expectedRequest)
  }

  @Test
  def updateWorklogText_IssueWithWorklogWithoutVertecId_NoCall {
    //arrange
    val vertecClient = mock[VertecClient]
    val vertecProjectResolver = mock[VertecProjectResolver]
    val testee = new VertecBookingService(mock[VertecConfig], mock[CustomFieldProvider], Option(vertecClient), Option(vertecProjectResolver))
    val issue = mock[Issue]
    issue.getKey returns "FOO-1"
    issue.getSummary returns "Summary"
    val worklogManager = mock[WorklogManager]
    worklogManager.getByIssue(issue) returns ArrayBuffer(mockWorklog("#JIRA=123"))
    new MockComponentWorker().addMock(classOf[WorklogManager], worklogManager).init();
    //act
    val response = testee.updateWorklogText(issue)
    //assert
    response shouldBe SuccessUpdate
    there was noCallsTo(vertecClient)
  }

  @Test
  def updateWorklogText_IssueWithWorklogWithVertecIdVertecNotUpdated_ExceptionMessage {
    //arrange
    val vertecClient = mock[VertecClient]
    val vertecProjectResolver = mock[VertecProjectResolver]
    val testee = new VertecBookingService(mock[VertecConfig], mock[CustomFieldProvider], Option(vertecClient), Option(vertecProjectResolver))
    val issue = mock[Issue]
    issue.getKey returns "FOO-1"
    issue.getSummary returns "Summary"
    val worklogManager = mock[WorklogManager]
    worklogManager.getByIssue(issue) returns ArrayBuffer(mockWorklog("#VERTEC=123"))
    new MockComponentWorker().addMock(classOf[WorklogManager], worklogManager).init();
    vertecClient.sendRequest(any[Elem]) returns <UpdateResponse><text>Updated 0 Objects</text></UpdateResponse>
    //act
    val response = testee.updateWorklogText(issue)
    //assert
    val expected = BookingFault(s"${issue.getKey} ${issue.getSummary}: Could not update worklogs with text ${issue.getKey} ${issue.getSummary}: Unexpected vertec response.")
    response shouldBe expected
  }

  @Test
  def updateWorklogProjektPhase_TwoIssuesWithOneWorklogs_AllWorklogsUpdated {
    //arrange
    val project = "Project"
    val phase = "Phase"
    val vertecProjectResolver = mockVertecProjectResolver(project, phase)
    val worklogManager = mock[WorklogManager]
    val issue = mockIssue("FOO-1")
    val objrefOne = 123
    worklogManager.getByIssue(issue) returns ArrayBuffer(mockWorklog(s"#VERTEC=$objrefOne"))
    val issueTwo = mockIssue("FOO-2")
    val objrefTwo = 234
    worklogManager.getByIssue(issueTwo) returns ArrayBuffer(mockWorklog(s"#VERTEC=$objrefTwo"))
    new MockComponentWorker().addMock(classOf[WorklogManager], worklogManager).init();
    val vertecClient = mock[VertecClient]
    vertecClient.sendRequest(any[Elem]) returns <Envelope><Body><UpdateResponse><text>Updated 2 Objects</text></UpdateResponse></Body></Envelope>
    val testee = new VertecBookingService(mock[VertecConfig], mock[CustomFieldProvider], Option(vertecClient), Option(vertecProjectResolver))
    //act
    val response = testee.updateWorklogProjektPhase(issue, ArrayBuffer(issue, issueTwo))
    //assert
    response shouldBe SuccessUpdate
    val expectedRequest =
      <Update><OffeneLeistung>
                <objref>123</objref>
                <projekt><objref>Project</objref></projekt>
                <phase><objref>Phase</objref></phase>
              </OffeneLeistung><OffeneLeistung>
                                 <objref>234</objref>
                                 <projekt><objref>Project</objref></projekt>
                                 <phase><objref>Phase</objref></phase>
                               </OffeneLeistung></Update>
    there was one(vertecClient).sendRequest(argAsElem(equalToIgnoringSpace(expectedRequest)))
  }

  @Test
  def updateWorklogProjektPhase_IssuesWithWorklogsWithAndWithoutVertecId_OnlyWorklogsWithVertecIdUpdated {
    //arrange
    val project = "Project"
    val phase = "Phase"
    val vertecProjectResolver = mockVertecProjectResolver(project, phase)
    val worklogManager = mock[WorklogManager]
    val issue = mockIssue("FOO-1")
    worklogManager.getByIssue(issue) returns ArrayBuffer(mockWorklog("foo"))
    val issueTwo = mockIssue("FOO-2")
    val objref = 234
    worklogManager.getByIssue(issueTwo) returns ArrayBuffer(mockWorklog(s"#VERTEC=$objref"))
    new MockComponentWorker().addMock(classOf[WorklogManager], worklogManager).init();
    val vertecClient = mock[VertecClient]
    vertecClient.sendRequest(any[Elem]) returns <Envelope><Body><UpdateResponse><text>Updated 1 Objects</text></UpdateResponse></Body></Envelope>
    val testee = new VertecBookingService(mock[VertecConfig], mock[CustomFieldProvider], Option(vertecClient), Option(vertecProjectResolver))
    //act
    val response = testee.updateWorklogProjektPhase(issue, ArrayBuffer(issue, issueTwo))
    //assert
    response shouldBe SuccessUpdate
    val expectedRequest =
      <Update><OffeneLeistung>
                <objref>{ objref }</objref>
                <projekt><objref>{ project }</objref></projekt>
                <phase><objref>{ phase }</objref></phase>
              </OffeneLeistung></Update>
    there was one(vertecClient).sendRequest(argAsElem(equalToIgnoringSpace(expectedRequest)))
  }

  @Test
  def updateWorklogProjektPhase_IssuesWithoutWorklog_NoCall {
    //arrange
    val project = "Project"
    val phase = "Phase"
    val vertecProjectResolver = mockVertecProjectResolver(project, phase)
    val issue = mockIssue("FOO-1")
    val issueTwo = mockIssue("FOO-2")
    new MockComponentWorker().addMock(classOf[WorklogManager], mock[WorklogManager]).init();
    val vertecClient = mock[VertecClient]
    val testee = new VertecBookingService(mock[VertecConfig], mock[CustomFieldProvider], Option(vertecClient), Option(vertecProjectResolver))
    //act
    val response = testee.updateWorklogProjektPhase(issue, ArrayBuffer(issue, issueTwo))
    //assert
    response shouldBe SuccessUpdate
    there was noCallsTo(vertecClient)
  }

  @Test
  def updateWorklogProjektPhase_IssuesWithWorklogsVertecNotUpdated_ExceptionMessage {
    //arrange
    val project = "Project"
    val phase = "Phase"
    val vertecProjectResolver = mockVertecProjectResolver(project, phase)
    val worklogManager = mock[WorklogManager]
    val issue = mockIssue("FOO-1")
    val objref = 123
    worklogManager.getByIssue(issue) returns ArrayBuffer(mockWorklog(s"#VERTEC=$objref"))
    new MockComponentWorker().addMock(classOf[WorklogManager], worklogManager).init();
    val vertecClient = mock[VertecClient]
    vertecClient.sendRequest(any[Elem]) returns <UpdateResponse><text>Updated 0 Objects</text></UpdateResponse>
    val testee = new VertecBookingService(mock[VertecConfig], mock[CustomFieldProvider], Option(vertecClient), Option(vertecProjectResolver))
    //act
    val response = testee.updateWorklogProjektPhase(issue, ArrayBuffer(issue))
    //assert
    val expected = BookingFault("FOO-1 Summary: Could not update worklogs for issues: Unexpected vertec response.")
    response shouldBe expected
    val request = <Update><OffeneLeistung>
                            <objref>{ objref }</objref>
                            <projekt><objref>{ project }</objref></projekt>
                            <phase><objref>{ phase }</objref></phase>
                          </OffeneLeistung></Update>
    there was one(vertecClient).sendRequest(argAsElem(equalToIgnoringSpace(request)))
  }

  private def mockVertecProjectResolver(projectId: String = "Project", phaseId: String = "Phase"): VertecProjectResolver = {
    val vertecProjectResolver = mock[VertecProjectResolver]
    val vertecIds = mock[VertecIds]
    vertecIds.projectId returns projectId
    vertecIds.phaseId returns phaseId
    vertecProjectResolver.resolve(any[Worklog]) returns Some(vertecIds)

  }

  private def mockIssue(key: String, summary: String = "Summary"): Issue = {
    val issue = mock[Issue]
    issue.getKey returns key
    issue.getSummary returns summary
  }

  private def mockWorklog(comment: String): Worklog = {
    val worklog = mock[Worklog]
    worklog.getIssue returns mock[Issue]
    worklog.getComment returns comment
  }

  private def mockWorklog(): Worklog = {
    val worklog = mock[Worklog]
    val author = mock[ApplicationUser]
    author.getEmailAddress returns "somebody@loewenfels.ch"
    worklog.getAuthorObject returns author
    val start = new DateTime(2015, 5, 16, 11, 1) // 661
    worklog.getStartDate returns start.toDate()
    worklog.getTimeSpent returns 200 * 60L
    worklog.getAuthorKey returns "tester"
    val issue = mock[Issue]
    issue.getSummary returns "UnserTitel"
    issue.getKey returns "DEV-XY"
    worklog.getIssue returns issue
  }

  private def mockResponseElement(vertecObjid: String, isValid: Int = 1): Elem = {
    <Envelope>
      <Body>
        <CreateResponse>
          <OffeneLeistung>
            <objid>{ vertecObjid }</objid>
            <isValid>{ isValid }</isValid>
          </OffeneLeistung>
        </CreateResponse>
      </Body>
    </Envelope>
  }

  private def mockRequestElement: Elem = {
    <Create>
      <OffeneLeistung>
        <bearbeiter>
          <objref>252711</objref>
        </bearbeiter>
        <datum>2015-05-16</datum>
        <projekt>
          <objref>4242</objref>
        </projekt>
        <phase>
          <objref>2424</objref>
        </phase>
        <minutenintvon>661</minutenintvon>
        <minutenintbis>861</minutenintbis>
        <text>DEV-XY UnserTitel</text>
        <typ><objref></objref></typ>
      </OffeneLeistung>
    </Create>
  }

  private def aktivePhase = {
    <Envelope>
      <Body>
        <CreateResponse>
          <ProjektPhase>
            <objid>2424</objid>
            <code>1</code>
            <projekt>
              <objref>4242</objref>
            </projekt>
          </ProjektPhase>
        </CreateResponse>
      </Body>
    </Envelope>
  }
}
