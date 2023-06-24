package me.milanvdm.mailslurp.playground.clients.email.mailslurp

import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

import scala.jdk.CollectionConverters._

import me.milanvdm.mailslurp.playground.clients.email.EmailClient
import me.milanvdm.mailslurp.playground.domain._

import com.mailslurp.apis.EmailControllerApi
import com.mailslurp.apis.InboxControllerApi
import com.mailslurp.apis.InboxForwarderControllerApi
import com.mailslurp.clients.ApiClient
import com.mailslurp.models.CreateInboxDto
import com.mailslurp.models.CreateInboxForwarderOptions
import com.mailslurp.models.SendEmailOptions

class MailSlurpEmailClient(mailSlurpApiClient: ApiClient) extends EmailClient {

  private val inboxControllerApi: InboxControllerApi = new InboxControllerApi(mailSlurpApiClient)
  private val inboxForwarderControllerApi: InboxForwarderControllerApi = new InboxForwarderControllerApi(mailSlurpApiClient)
  private val emailControllerApi: EmailControllerApi = new EmailControllerApi(mailSlurpApiClient)

  /**
   * ┌──────┐┌───────────────────┐┌─────────────────┐ ┌────────┐
   * │sender││maskedReceiverInbox││maskedSenderInbox│ │receiver│
   * └──┬───┘└─────────┬─────────┘└────────┬────────┘ └───┬────┘
   *    │              │                   │              │
   *    │  Send email  │                   │              │
   *    │─────────────>│                   │              │
   *    │              │                   │              │
   *    │              │   Forward email   │              │
   *    │              │──────────────────>│              │
   *    │              │                   │              │
   *    │              │                   │Forward email │
   *    │              │                   │─────────────>│
   *    │              │                   │              │
   *    │              │                   │Reply to email│
   *    │              │                   │<─────────────│
   *    │              │                   │              │
   *    │              │   Forward email   │              │
   *    │              │<──────────────────│              │
   *    │              │                   │              │
   *    │Forward email │                   │              │
   *    │<─────────────│                   │              │
   * ┌──┴───┐┌─────────┴─────────┐┌────────┴────────┐ ┌───┴────┐
   * │sender││maskedReceiverInbox││maskedSenderInbox│ │receiver│
   * └──────┘└───────────────────┘└─────────────────┘ └────────┘
   */
  override def createMaskedInbox(
                                  sender: EmailAddress,
                                  senderName: Option[Name],
                                  receiver: EmailAddress,
                                  receiverName: Option[Name],
                                  expirationTime: Instant,
                                  tags: List[Tag]
                 ): MaskedInbox = {

    val commonCreateInboxOptions = new CreateInboxDto()
      .expiresAt(expirationTime.atOffset(ZoneOffset.UTC))
      .inboxType(CreateInboxDto.InboxTypeEnum.HTTP_INBOX)
      .tags(tags.map(_.value).asJava)

    val maskedSenderInboxOptions = senderName match {
      case Some(name) => commonCreateInboxOptions.name(name.value)
      case None => commonCreateInboxOptions
    }
    val maskedReceiverInboxOptions = receiverName match {
      case Some(name) => commonCreateInboxOptions.name(name.value)
      case None => commonCreateInboxOptions
    }

    val maskedSenderInbox = inboxControllerApi.createInboxWithOptions(maskedSenderInboxOptions)
    val maskedReceiverInbox = inboxControllerApi.createInboxWithOptions(maskedReceiverInboxOptions)

    val maskedReceiverInboxToMaskedSenderInboxOptions = new CreateInboxForwarderOptions()
      .field(CreateInboxForwarderOptions.FieldEnum.SENDER)
      .`match`(sender.value)
      .addForwardToRecipientsItem(maskedSenderInbox.getEmailAddress)

    val _ = inboxForwarderControllerApi.createNewInboxForwarder(
      maskedReceiverInbox.getId,
      maskedReceiverInboxToMaskedSenderInboxOptions
    )


    val maskedSenderInboxToReceiverOptions = new CreateInboxForwarderOptions()
      .field(CreateInboxForwarderOptions.FieldEnum.SENDER)
      .`match`(maskedReceiverInbox.getEmailAddress)
      .addForwardToRecipientsItem(receiver.value)

    val _ = inboxForwarderControllerApi.createNewInboxForwarder(
      maskedSenderInbox.getId,
      maskedSenderInboxToReceiverOptions
    )

    val maskedSenderInboxToMaskedReceiverInboxOptions = new CreateInboxForwarderOptions()
      .field(CreateInboxForwarderOptions.FieldEnum.SENDER)
      .`match`(receiver.value)
      .addForwardToRecipientsItem(maskedReceiverInbox.getEmailAddress)

    val _ = inboxForwarderControllerApi.createNewInboxForwarder(
      maskedSenderInbox.getId,
      maskedSenderInboxToMaskedReceiverInboxOptions
    )

    val maskedReceiverInboxToSenderOptions = new CreateInboxForwarderOptions()
      .field(CreateInboxForwarderOptions.FieldEnum.SENDER)
      .`match`(maskedSenderInbox.getEmailAddress)
      .addForwardToRecipientsItem(sender.value)

    val _ = inboxForwarderControllerApi.createNewInboxForwarder(
      maskedReceiverInbox.getId,
      maskedReceiverInboxToSenderOptions
    )

    MaskedInbox(
      senderInboxId = MaskedInbox.Id(maskedSenderInbox.getId.toString),
      receiverInboxId = MaskedInbox.Id(maskedReceiverInbox.getId.toString),
      sender = sender,
      receiver = receiver,
      maskedSender = EmailAddress(maskedSenderInbox.getEmailAddress),
      maskedReceiver = EmailAddress(maskedReceiverInbox.getEmailAddress),
      expirationTime = expirationTime
    )

  }

  override def sendEmail(
                          sender: EmailAddress,
                          receivers: List[EmailAddress],
                          subject: Email.Subject,
                          content: Email.Content,
                          bcc: List[EmailAddress],
                        ): Unit = {
    val inboxByEmail = inboxControllerApi.getInboxByEmailAddress(
      sender.value
    )

    val inboxId = inboxByEmail.getInboxId


      sendEmailFromInbox(
        senderInboxId = MaskedInbox.Id(inboxId.toString),
        receivers = receivers,
        subject = subject,
        content = content,
        bcc = bcc,
      )
  }

  override def sendEmailFromInbox(
                          senderInboxId: MaskedInbox.Id,
                          receivers: List[EmailAddress],
                          subject: Email.Subject,
                          content: Email.Content,
                          bcc: List[EmailAddress],
                        ): Unit = {
    val sendEmailOptions = new SendEmailOptions()
      .subject(subject.value)
      .to(receivers.map(_.value).asJava)
      .bcc(bcc.map(_.value).asJava)
      .useInboxName(true)
      .isHTML(true)
      .body(content.value)


      inboxControllerApi.sendEmail(
        UUID.fromString(senderInboxId.value),
        sendEmailOptions
      )
  }

  override def getEmailsByTag(
                               tag: Tag,
                               offset: Offset,
                               size: Size,
                             ): List[Email] = {
    val inboxesByTag = inboxControllerApi.getAllInboxes(
      offset.value,
      size.value,
      null,
      null,
      null,
      tag.value,
      null,
      null,
      null,
      null,
      null
    )

    val inboxIds = inboxesByTag.getContent.asScala.map(_.getId)

    val emails = emailControllerApi
      .getEmailsPaginated(
        inboxIds.asJava,
        0,
        100,
        null,
        null,
        null,
        null,
        null
      )

      emails
      .getContent
      .asScala
      .map { email =>
        Email(
            sender = EmailAddress(email.getFrom),
            receivers = email.getTo.asScala.map(EmailAddress).toList,
            subject = Email.Subject(email.getSubject),
            content = Email.Content(email.getBodyExcerpt)
          )
      }
      .toList
  }

}
