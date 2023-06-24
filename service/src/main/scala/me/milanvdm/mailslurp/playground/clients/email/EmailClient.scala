package me.milanvdm.mailslurp.playground.clients.email

import java.time.Instant

import me.milanvdm.mailslurp.playground.clients.email.mailslurp.MailSlurpEmailClient
import me.milanvdm.mailslurp.playground.domain.Email
import me.milanvdm.mailslurp.playground.domain.EmailAddress
import me.milanvdm.mailslurp.playground.domain.MaskedInbox
import me.milanvdm.mailslurp.playground.domain.Name
import me.milanvdm.mailslurp.playground.domain.Offset
import me.milanvdm.mailslurp.playground.domain.Size
import me.milanvdm.mailslurp.playground.domain.Tag

import com.mailslurp.clients.ApiClient

object EmailClient {

  def mailSlurp(mailSlurpApiClient: ApiClient): EmailClient = new MailSlurpEmailClient(mailSlurpApiClient)

}

trait EmailClient {

  def createMaskedInbox(
                         sender: EmailAddress,
                        senderName: Option[Name],
                        receiver: EmailAddress,
                        receiverName: Option[Name],
                        expirationTime: Instant,
                        tags: List[Tag]
                                ): MaskedInbox

  def sendEmail(
                 sender: EmailAddress,
                 receivers: List[EmailAddress],
                 subject: Email.Subject,
                 content: Email.Content,
                 bcc: List[EmailAddress],
               ): Unit

  def sendEmailFromInbox(
                          senderInboxId: MaskedInbox.Id,
                          receivers: List[EmailAddress],
                          subject: Email.Subject,
                          content: Email.Content,
                          bcc: List[EmailAddress],
                        ): Unit

  def getEmailsByTag(
                      tag: Tag,
                      offset: Offset,
                      size: Size
                    ): List[Email]

}
