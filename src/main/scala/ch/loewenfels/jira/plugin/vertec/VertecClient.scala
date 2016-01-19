package ch.loewenfels.jira.plugin.vertec

import ch.loewenfels.jira.plugin.vertec.VertecClient.{ Connector, Credential }
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.methods.{ PostMethod, StringRequestEntity }
import org.slf4j.LoggerFactory
import scala.xml.{ Elem, XML, Node }

object VertecClient {

  case class Credential(user: String, password: String)

  trait Connector {
    def send(envelope: Elem): Elem
  }

  case class HttpConnector(vertecUrl: String) extends Connector with PasswordMasquerading {
    val LOG = LoggerFactory.getLogger(classOf[HttpConnector])
    def send(envelope: Elem): Elem = {
      val post = new PostMethod(vertecUrl)
      val requestString = envelope.toString()
      post.setRequestEntity(new StringRequestEntity(requestString))
      new HttpClient().executeMethod(post)
      val result = XML.load(post.getResponseBodyAsStream)
      LOG.debug("retrieved \n'{}'\n from vertec for request \n'{}'", result, mask(requestString))
      result
    }

  }

  trait PasswordMasquerading {
    def mask(request: String) = request.replaceAll("(?i)(<password>).+(</password>)", "$1***$2")
  }

  trait Response {
    def apply(value: Elem) = value
    def unapply(value: Elem): Option[String]
  }

  object Fault extends Response {
    def unapply(value: Elem): Option[String] = (value \\ "Fault").map(_.text).headOption
  }

  object Success extends Response {
    private val numberGreatherZero = """(Deleted|Updated) ([1-9]\d?) Objects""".r

    private def validResponse(node: Node): Option[String] = {
      node.text match {
        case numberGreatherZero(o, i) => if (i.toInt == 1) Some(s"$o $i item") else Some(s"$o $i items")
        case _ => None
      }
    }

    def unapply(value: Elem): Option[String] = {
      scala.xml.Utility.trim(value) match {
        case <Envelope><Body><DeleteResponse><text>{ msg }</text></DeleteResponse></Body></Envelope> => validResponse(msg)
        case <Envelope><Body><UpdateResponse><text>{ msg }</text></UpdateResponse></Body></Envelope> => validResponse(msg)
        case x => None
      }
    }

  }

  /**
   * Gets text of a <objid> element if available
   */
  object ObjidResponse extends Response {
    def unapply(value: Elem): Option[String] = (value \\ "objid").map(_.text).headOption
  }

  object InvalidObjidResponse extends Response {
    def unapply(value: Elem): Option[String] = {
      val objid = (value \\ "objid").map { _.text }.headOption
      (value \\ "isValid").map { _.text }.headOption match {
        case Some("0") => objid
        case _ => None
      }
    }
  }

  def create(credential: Credential, connector: Connector) = new VertecClient(credential, connector)
}

class VertecClient(credential: Credential, connector: Connector) {

  val header = {
    <Header>
      <BasicAuth>
        <Name>{ credential.user }</Name>
        <Password>{ credential.password }</Password>
      </BasicAuth>
    </Header>
  }

  def sendRequest(bodyContent: Elem): Elem = {
    val envelope = {
      <Envelope>
        { header }
        <Body>
          { bodyContent }
        </Body>
      </Envelope>
    }
    connector.send(envelope)
  }

  def query(selection: Elem, resultdef: Elem = null) =
    sendRequest(<Query>{ selection }{ resultdef }</Query>)

  def oclQuery(oclTerm: String, resultdef: Elem = null) =
    query(<Selection><ocl>{ oclTerm }</ocl></Selection>, resultdef)

}
