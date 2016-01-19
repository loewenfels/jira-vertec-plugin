package ch.loewenfels.jira.plugin.vertec

import org.slf4j.LoggerFactory

import ch.loewenfels.jira.plugin.vertec.VertecClient.ObjidResponse

object UserResolver {

  def mapEmail(implicit vertecClient: VertecClient):String => Option[String]={ email =>
    vertecClient.oclQuery(s"projektbearbeiter->select(aktiv)->select(briefEmail->sqlLikeCaseInsensitive('$email'))") match {
      case ObjidResponse(userobjid) => Some(userobjid)
      case _ => None
    }
  }

}
