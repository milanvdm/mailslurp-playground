package me.milanvdm.mailslurp.playground.api

import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import scala.jdk.CollectionConverters._
import me.milanvdm.mailslurp.playground.api.utils.MailSlurpTestClient
import me.milanvdm.mailslurp.playground.clients.email.EmailClient
import me.milanvdm.mailslurp.playground.domain._
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeAll

class SendBookingEmailToLodgingSpec extends Specification with BeforeAll {

  var mailSlurpTestClient: MailSlurpTestClient = _
  var reservationEmail: String = _
  var directHotelBookingsEmail: String = _
  var emailClient: EmailClient = _

  override def beforeAll(): Unit = {
    mailSlurpTestClient = new MailSlurpTestClient()

    reservationEmail = mailSlurpTestClient.createTestingInbox()
    directHotelBookingsEmail = mailSlurpTestClient.createTestingInbox()
    emailClient = EmailClient.mailSlurp(mailSlurpTestClient.mailSlurpApiClient)
  }

  "Our lodging and customer" should {

    "be able to have a back-and-forth conversation without knowing each others emails" in {

      val lodgingEmailAddress = mailSlurpTestClient.createTestingInbox()
      val lodgingId = LodgingId(UUID.randomUUID().toString)
      val lodgingName = Name("lodgingName")

      val userEmailAddress = mailSlurpTestClient.createTestingInbox()
      val userId = UserId(UUID.randomUUID().toString)
      val userName = Name("userName")

      val subject = Email.Subject("subject")
      val template = Template("template")
      val checkOut = LocalDate.now()

      val maskedInbox = emailClient.createMaskedInbox(
        senderName = Some(lodgingName),
        sender = EmailAddress(lodgingEmailAddress),
        receiverName = Some(userName),
        receiver = EmailAddress(userEmailAddress),
        expirationTime = checkOut.atStartOfDay().toInstant(ZoneOffset.UTC),
        tags = List(Tag(lodgingId.value), Tag(userId.value))
      )

      emailClient.sendEmail(
        sender = EmailAddress(reservationEmail),
        receivers = List(EmailAddress(lodgingEmailAddress)),
        subject = subject,
        content = Email.Content(s"${template.value}000${maskedInbox.maskedReceiver.value}"),
        bcc = List.empty
      )

      val lodgingEmail1 = mailSlurpTestClient.getNthEmailFromTestingInbox(lodgingEmailAddress, 0)

      lodgingEmail1.getSubject must_=== subject.value
      lodgingEmail1.getFrom must_=== reservationEmail
      lodgingEmail1.getBody.split("000").head must_=== template.value

      val maskedUserEmail = lodgingEmail1.getBody.split("000").last

      println(s"lodgingEmailAddress: $lodgingEmailAddress")
      println(s"maskedUserEmail: $maskedUserEmail")
      println(s"userEmailAddress: $userEmailAddress")

      emailClient.sendEmail(
        sender = EmailAddress(lodgingEmailAddress),
        receivers = List(EmailAddress(maskedUserEmail)),
        subject = Email.Subject("hello1"),
        content = Email.Content("hello1"),
        bcc = List.empty
      )

      val userEmail1 = mailSlurpTestClient.getNthEmailFromTestingInbox(userEmailAddress, 0)

      val maskedLodgingEmail = userEmail1.getFrom

      userEmail1.getSubject must_=== "hello1"
      userEmail1.getBcc must_=== List.empty.asJava
      maskedLodgingEmail must_!== lodgingEmailAddress
      userEmail1.getBody must_=== "hello1"

      emailClient.sendEmail(
        sender = EmailAddress(userEmailAddress),
        receivers = List(EmailAddress(userEmail1.getFrom)),
        subject = Email.Subject("hello2"),
        content = Email.Content("hello2"),
        bcc = List.empty
      )

      val lodgingEmail2 = mailSlurpTestClient.getNthEmailFromTestingInbox(lodgingEmailAddress, 1)

      lodgingEmail2.getSubject must_=== "hello2"
      lodgingEmail2.getBcc must_=== List.empty.asJava
      lodgingEmail2.getFrom must_=== maskedUserEmail
      lodgingEmail2.getBody must_=== "hello2"

      emailClient.sendEmail(
        sender = EmailAddress(lodgingEmailAddress),
        receivers = List(EmailAddress(lodgingEmail2.getFrom)),
        subject = Email.Subject("hello3"),
        content = Email.Content("hello3"),
        bcc = List.empty
      )

      val userEmail2 = mailSlurpTestClient.getNthEmailFromTestingInbox(userEmailAddress, 1)

      userEmail2.getSubject must_=== "hello3"
      userEmail2.getBcc must_=== List.empty.asJava
      userEmail2.getFrom must_=== maskedLodgingEmail
      userEmail2.getBody must_=== "hello3"

      val taggedLodgingEmails = emailClient.getEmailsByTag(
        Tag(lodgingId.value),
        Offset(0),
        Size(100)
      )

      taggedLodgingEmails.size must_=== 6

      val taggedUserEmails = emailClient.getEmailsByTag(
        Tag(userId.value),
        Offset(0),
        Size(100)
      )

      taggedUserEmails.size must_=== 6

      taggedUserEmails must_=== taggedLodgingEmails

    }
  }

}
