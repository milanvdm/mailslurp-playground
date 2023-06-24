package me.milanvdm.mailslurp.playground.domain

import java.time.Instant

case class MaskedInbox(
                        senderInboxId: MaskedInbox.Id,
                        receiverInboxId: MaskedInbox.Id,
                        sender: EmailAddress,
                        receiver: EmailAddress,
                        maskedSender: EmailAddress,
                        maskedReceiver: EmailAddress,
                        expirationTime: Instant
                      )
object MaskedInbox {
  case class Id(value: String) extends AnyVal
}
