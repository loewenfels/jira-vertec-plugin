package ch.loewenfels.jira.plugin.vertec

import ch.loewenfels.jira.plugin.config.VertecConfig
import ch.loewenfels.jira.plugin.vertec.VertecClient.HttpConnector
import ch.loewenfels.jira.plugin.vertec.VertecClient.Credential

/**
 * Builds a optional VertecClient if config is valid
 */
object VertecClientBuilder {
  def build(implicit config: VertecConfig)= {
    for{
      connector <- buildConnector
      credential <- buildCredential
    } yield 
      VertecClient.create(credential,connector)
  }
  
  def buildConnector(implicit config: VertecConfig)=config.vertecUrl match {
    case Some(url) => Some(HttpConnector(url))
    case _ => None
  }
  
  
  def buildCredential(implicit config: VertecConfig)=(config.vertecUser,config.vertecPassword) match {
    case (Some(user),Some(password)) => Some(Credential(user,password))
    case _ => None
    
  }

}