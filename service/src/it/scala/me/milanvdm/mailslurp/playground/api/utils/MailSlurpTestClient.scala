package me.milanvdm.mailslurp.playground.api.utils

import com.mailslurp.apis.InboxControllerApi
import com.mailslurp.apis.WaitForControllerApi
import com.mailslurp.clients.ApiClient
import com.mailslurp.models.Email

class MailSlurpTestClient() {

  val apiKey = ""

  val mailSlurpApiClient = {
    val apiClient = new ApiClient()
    apiClient.setApiKey(apiKey)
    apiClient.setConnectTimeout(100000)
    apiClient.setReadTimeout(100000)
    apiClient.setWriteTimeout(100000)
    apiClient
  }

  private val inboxControllerApi = new InboxControllerApi(mailSlurpApiClient)
  private val waitControllerApi = new WaitForControllerApi(mailSlurpApiClient)

  def createTestingInbox(): String = {
    val inbox = inboxControllerApi.createInboxWithDefaults

    inbox.getEmailAddress
  }

  def getNthEmailFromTestingInbox(emailAddress: String, n: Int): Email = {
    val inbox = inboxControllerApi.getInboxByEmailAddress(emailAddress)

    waitControllerApi.waitForNthEmail(inbox.getInboxId, n, null, null, null, null, null, null)

  }

}
