package me.milanvdm.mailslurp.playground.domain

case class Email(
                sender: EmailAddress,
                receivers: List[EmailAddress],
                subject: Email.Subject,
                content: Email.Content
                )
object Email {

  case class Subject(value: String) extends AnyVal

  case class Content(value: String) extends AnyVal

}
