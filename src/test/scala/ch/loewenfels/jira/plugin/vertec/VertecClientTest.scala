package ch.loewenfels.jira.plugin.vertec;

import scala.xml.Elem
import scala.xml.Node

import org.junit.Test
import org.scalatest.Matchers.convertToAnyShouldWrapper
import org.scalatest.Matchers.convertToStringShouldWrapper
import org.scalatest.junit.AssertionsForJUnit
import org.specs2.matcher.Matcher
import org.specs2.matcher.MustMatchers
import org.specs2.matcher.XmlMatchers
import org.specs2.mock.Mockito

import ch.loewenfels.jira.plugin.vertec.VertecClient.Connector
import ch.loewenfels.jira.plugin.vertec.VertecClient.Credential
import ch.loewenfels.jira.plugin.vertec.VertecClient.Fault
import ch.loewenfels.jira.plugin.vertec.VertecClient.HttpConnector
import ch.loewenfels.jira.plugin.vertec.VertecClient.InvalidObjidResponse
import ch.loewenfels.jira.plugin.vertec.VertecClient.ObjidResponse
import ch.loewenfels.jira.plugin.vertec.VertecClient.Success

class VertecClientTest extends AssertionsForJUnit with Mockito with XmlMatchers with MustMatchers {

  /**
   * Adapts given matcher for Seq[Node] to a mockito hamcrest Elem matcher
   * see also {@link http://etorreborre.github.io/specs2/guide/org.specs2.guide.Matchers.html#Custom}
   */
  def argAsElem(matcher: Matcher[Seq[Node]]) = argThat(matcher ^^ ((_: Elem).toSeq))

  @Test def oclQuery_oclExpression_queryRequestWithOclElement {
    //arrange
    val expectedExpression = "expectedOclExpression"
    val expectedElement = <Selection><ocl>{ expectedExpression }</ocl></Selection>
    val connector = mock[Connector]
    val testee = VertecClient.create(mock[Credential], connector)
    //act
    testee.oclQuery(expectedExpression, <Response/>)
    //assert
    there was one(connector).send(argAsElem(\\(expectedElement)))
  }

  @Test def query_selectionElement_queryRequestWithQueryElement {
    //arrange
    val expectedSelectionElement = <Selection>MyQuery</Selection>
    val responseElement = <Response/>
    val expectedElement = <Query>{ expectedSelectionElement } { responseElement } </Query>
    val connector = mock[Connector]
    val testee = VertecClient.create(mock[Credential], connector)
    //act
    testee.query(expectedSelectionElement, responseElement)
    //assert
    there was one(connector).send(argAsElem(\\(expectedElement)))
  }

  @Test def sendRequest_bodyElement_requestWithBodyElement {
    //arrange
    val bodyElement = <MyBodyPart/>
    val expectedElement = <Body>{ bodyElement } </Body>
    val connector = mock[Connector]
    val testee = VertecClient.create(mock[Credential], connector)
    //act
    testee.sendRequest(bodyElement)
    //assert
    there was one(connector).send(argAsElem(\\(expectedElement)))
  }

  @Test def sendRequest_always_requestWrappedWithEnvelopeElement {
    //arrange
    val connector = mock[Connector]
    val testee = VertecClient.create(mock[Credential], connector)
    //act
    testee.sendRequest(<MyBodyPart/>)
    //assert
    val captor = capture[Elem]
    there was one(connector).send(captor)
    assert(captor.value.label === "Envelope")
  }

  @Test def sendRequest_always_requestWithHeaderElementContainsCredentials {
    //arrange
    val expectedUser = "aUser"
    val expectedPassword = "aPassword"
    val expectedHeaderElement =
      <Header>
        <BasicAuth>
          <Name>{ expectedUser }</Name>
          <Password>{ expectedPassword }</Password>
        </BasicAuth>
      </Header>

    val connector = mock[Connector]
    val credential = mock[Credential]
    credential.user returns expectedUser
    credential.password returns expectedPassword
    val testee = VertecClient.create(credential, connector)
    //act
    testee.sendRequest(<MyBodyPart/>)
    //assert
    there was one(connector).send(argAsElem(\\(expectedHeaderElement)))
  }

  @Test(expected = classOf[NullPointerException])
  def objidResponse_null_Exception {
    val result = ObjidResponse.unapply(null)
  }

  @Test def objidResponse_xmlContainsObjIdElement_SomeObjid {
    //arrange
    val expectedObjid = "123"
    val elem = <Test><objid>{ expectedObjid }</objid><isValid>1</isValid></Test>
    //act
    val result = ObjidResponse.unapply(elem)
    //assert
    result shouldBe Some(expectedObjid)
  }

  @Test def objidResponse_xmlContainsInvalidObjIdElement_SomeObjid {
    //arrange
    val expectedObjid = "123"
    val elem = <Test><objid>{ expectedObjid }</objid><isValid>0</isValid></Test>
    //act
    val result = ObjidResponse.unapply(elem)
    //assert
    result shouldBe Some(expectedObjid)
  }

  @Test def invalidObjidResponse_xmlContainsObjIdElement_SomeObjid {
    //arrange
    val expectedObjid = "123"
    val elem = <Test><objid>{ expectedObjid }</objid><isValid>0</isValid></Test>
    //act
    val result = InvalidObjidResponse.unapply(elem)
    //assert
    result shouldBe Some(expectedObjid)
  }

  @Test def invalidObjidResponse_xmlContainsValidObjIdElement_None {
    //arrange
    val expectedObjid = "123"
    val elem = <Test><objid>{ expectedObjid }</objid><isValid>1</isValid></Test>
    //act
    val result = InvalidObjidResponse.unapply(elem)
    //assert
    result shouldBe None
  }

  @Test def objidResponse_xmlWithoutObjIdElemnt_None {
    //arrange
    val elem = <Test><otherxml>objid</otherxml></Test>
    //act + assert
    val result = ObjidResponse.unapply(elem)
    //assert
    result shouldBe None
  }

  @Test(expected = classOf[NullPointerException])
  def success_null_Exception {
    val result = Success.unapply(null)
  }

  @Test def successResponse_xmlContainsIsNeitherDeleteOrUpdateResponseElement_None {
    //arrange
    val elem = <Test><text>Deleted 1 Objects</text></Test>
    //act
    val result = Success.unapply(elem)
    //assert
    result shouldBe None
  }

  @Test def successResponse_xmlContainsDeleteResponseElement_SomeText {
    //arrange
    val elem = <Envelope><Body><DeleteResponse><text>Deleted 1 Objects</text></DeleteResponse></Body></Envelope>
    //act
    val result = Success.unapply(elem)
    //assert
    result shouldBe Some("Deleted 1 item")
  }

  @Test def successResponse_xmlContainsDeleteResponseWith0Element_None {
    //arrange
    val elem = <Envelop><Body><DeleteResponse><text>Deleted 0 Objects</text></DeleteResponse></Body></Envelop>
    //act
    val result = Success.unapply(elem)
    //assert
    result shouldBe None
  }
  @Test def successResponse_xmlContainsDeleteResponseElementWithWhitespaces_Some {

    val elem = <Envelope>
                 <Body>
                   <DeleteResponse>
                     <text>Deleted 1 Objects</text>
                   </DeleteResponse>
                 </Body>
               </Envelope>
    val result = Success.unapply(elem)
    //assert
    result shouldBe Some("Deleted 1 item")

  }

  @Test def successResponse_xmlContainsInvalidCount_None {
    //arrange
    val elem = <Envelop><Body><UpdateResponse><text>Updated x Objects</text></UpdateResponse></Body></Envelop>
    //act
    val result = Success.unapply(elem)
    //assert
    result shouldBe None
  }

  @Test def successResponse_xmlContainsUpdatedTenObjects_SomeText {
    //arrange
    val elem = <Envelope><Body><UpdateResponse><text>Updated 10 Objects</text></UpdateResponse></Body></Envelope>
    //act
    val result = Success.unapply(elem)
    //assert
    result shouldBe Some("Updated 10 items")
  }

  @Test(expected = classOf[NullPointerException])
  def faultResponse_null_Exception {
    val result = Fault.unapply(null)
  }

  @Test def faultResponse_xmlContainsDeleteResponseElement_SomeText {
    //arrange
    val elem =
      <Fault>
        <faultcode>Client</faultcode>
        <faultstring>Error(s) in XML input</faultstring>
        <details>
          <detailitem>Error: delete of object objref denied on line 11 col 40</detailitem>
        </details>
      </Fault>
    //act
    val result = Fault.unapply(elem)
    //assert
    result shouldBe Some(elem.text)
  }

  @Test def faultResponse_xmlWithoutDeleteResponseElemnt_None {
    //arrange
    val elem = <Test><text>Deleted 1 Objects</text></Test>
    //act + assert
    val result = Fault.unapply(elem)
    //assert
    result shouldBe None
  }

  @Test def mask_hidePassword_PasswordMasked {
    //arrange
    val password = "foobar"
    val elem = <Test><Password>{ password }</Password></Test>
    val connector = new HttpConnector("http://foo.bar")
    //act
    val result = connector.mask(elem.toString())
    //assert
    result shouldBe <Test><Password>***</Password></Test>.toString()
  }

}
