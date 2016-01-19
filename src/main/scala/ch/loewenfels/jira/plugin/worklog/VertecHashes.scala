package ch.loewenfels.jira.plugin.worklog

object VertecHashes {
  val VertecObjectIdHash = "#VERTEC="
  val VertecObjectIdPattern = (VertecObjectIdHash + "(\\d+)").r.unanchored

  def vertecObjId(comment: String): Option[String] = comment match {
    case VertecObjectIdPattern(vertecObjId) => Some(vertecObjId)
    case _ => None
  }

  def add(comment: String, vertecId: String) = {
    if (comment.contains(VertecObjectIdHash))
      VertecObjectIdPattern.replaceAllIn(comment, VertecObjectIdHash + vertecId)
    else
      comment + "\n" + VertecObjectIdHash + vertecId
  }
}
