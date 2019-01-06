package com.foyle.testing

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import com.google.gson.Gson
import spark.Spark
import org.junit.AfterClass
import org.junit.BeforeClass
import com.foyle.Main
import com.google.gson.JsonParser
import com.kennycason.struktural.Struktural

val PORT = "4567"
private var client: Client? = null
private var gson: Gson? = null

class MainTest {

    companion object {
        @BeforeClass @JvmStatic
        fun startServer() {
            val args = arrayOf(PORT)
            Main.main(args)
        }

        @BeforeClass @JvmStatic
        @Throws(Exception::class)
        fun setUp() {
            client = Client("http://localhost:$PORT")
            gson = Gson()
        }

        @AfterClass @JvmStatic
        fun stopServer() {
            Spark.stop()
        }
    }

    fun getBalance(id: Int): Double {
        val getAccountRes: Response? = client?.request("GET", "/accounts/$id")

        // Senders account to JSON
        val jsonAccountRes = JsonParser().parse(getAccountRes?.body).getAsJsonObject()

        // Senders balance as double
        val balance = jsonAccountRes.get("balance").getAsDouble()

        return balance
    }

    @Test
    @Throws(Exception::class)
    fun accountCorrectTest() {

        Spark.awaitInitialization()

        // Get the first account
        val getAccountRes: Response? = client?.request("GET", "/accounts/0")

        // Assert 200 response
        assertEquals(200, getAccountRes?.status)

        // Assert ID of account
        Struktural.assertValues(getAccountRes!!.body, listOf(
            Pair("id", 0)
        ))
    }

    @Test
    @Throws(Exception::class)
    fun transferCorrectTest() {

        Spark.awaitInitialization()

        // Set transfer values
        val sender: Int = 1
        val recipient: Int = 2
        val amountToTransfer: Double = 1.00

        // Sender Check
        val initialSenderBalance = getBalance(sender)

        val initialRecipientBalance = getBalance(recipient)

        // Transfer
        // JSON string payload for the transfer
        val testTransferPayload: String = "{\"amount\": $amountToTransfer, \"recipient\": $recipient }"

        // Make the transfer request
        val transferRes: Response? = client?.request("POST", "/accounts/$sender/transfer", testTransferPayload)

        // Assert response code
        assertEquals(200, transferRes?.status)

        // Pull out the transfer response body (contains sender account)
        val transferResBody = transferRes!!.body

        // Expect sender balance to now be initial balance minus the transferred amount
        val expectedPostSenderBalance: Double = initialSenderBalance - amountToTransfer

        // Assert sender account ID and balance in transfer response
        Struktural.assertValues(transferResBody, listOf(
            Pair("balance", expectedPostSenderBalance),
            Pair("id", 1)
        ))

        // Fetch transfer recipients account
        val transferredRes: Response? = client?.request("GET", "/accounts/$recipient")

        // Pull out recipient account from response
        val recipientResBody = transferredRes!!.body

        // Expect recipient balance to be their initial balance plus the amount being transferred
        val expectedRecipientBalance: Double = initialRecipientBalance + amountToTransfer

        // Assert recipient id and expected post transfer balance
        Struktural.assertValues(recipientResBody, listOf(
            Pair("balance", expectedRecipientBalance),
            Pair("id", recipient)
        ))
    }

    @Test
    @Throws(Exception::class)
    // Tests for negative numbers being transferred, should always fail with 400 bad request
    fun negativeTransferValidationTest() {

        Spark.awaitInitialization()

        // Set values
        val recipient: Int = 4
        val sender: Int = 3
        val negativeAmount: Int = -1

        // Attempt transfer using negative value
        val testTransferWithNegativeNumberBody: String = "{\"amount\": $negativeAmount, \"recipient\": $recipient }"
        val testTransferWithNegativeNumberRes: Response? = client?.request("POST", "/accounts/$sender/transfer", testTransferWithNegativeNumberBody)

        // Assert 403 forbidden response code
        assertEquals("Transfer amount should not be negative", 400, testTransferWithNegativeNumberRes?.status)
    }

    @Test
    @Throws(Exception::class)
    // Tests for insufficient balance being transferred, should always fail with 403 forbidden
    fun insufficientBalanceTransferTest() {

        Spark.awaitInitialization()

        // Set values
        val recipient: Int = 4
        val sender: Int = 3

        // Fetch senders current balance
        val initialSenderBalance = getBalance(3)

        // Set amount to transfer as more than account balance
        val amountToTransfer: Double = initialSenderBalance + 100

        // Make sure senders balance is insufficient
        assertTrue("Sender balance should be sufficient at time of transfer request", initialSenderBalance < amountToTransfer)

        // Attempt transfer from account with insufficient balance
        val testTransferWithInsufficientBalanceBody: String = "{\"amount\": $amountToTransfer, \"recipient\": $recipient }"
        val testTransferWithInsufficientBalanceRes: Response? = client?.request("POST", "/accounts/$sender/transfer", testTransferWithInsufficientBalanceBody)

        assertEquals("Balance of sender should be more than transfer amount", 403, testTransferWithInsufficientBalanceRes?.status)
    }

    @Test
    @Throws(Exception::class)
    // Tests for invalid recipient of transfer, should always fail with 403 forbidden
    fun invalidRecipientTransferTest() {

        Spark.awaitInitialization()

        val recipient: String = "x"
        val sender: Int = 3
        val amount: Double = 10000.00

        // Negative Values
        val testTransferWithInvalidRecipientBody: String = "{\"amount\": $amount, \"recipient\": $recipient }"
        val testTransferWithInvalidRecipientRes: Response? = client?.request("POST", "/accounts/$sender/transfer", testTransferWithInvalidRecipientBody)

        assertEquals("Recipient ID should refer to a valid account", 403, testTransferWithInvalidRecipientRes?.status)
    }

    @Test
    @Throws(Exception::class)
    // Tests for invalid recipient of transfer, should always fail with 403 forbidden
    fun selfRecipientTransferTest() {

        Spark.awaitInitialization()

        val recipient: Int = 3
        val sender: Int = 3
        val amount: Double = 10000.00

        // Negative Values
        val testTransferWithSelfRecipientBody: String = "{\"amount\": $amount, \"recipient\": $recipient }"
        val testTransferWithSelfRecipientRes: Response? = client?.request("POST", "/accounts/$sender/transfer", testTransferWithSelfRecipientBody)

        assertEquals("Recipient account should not be your own", 403, testTransferWithSelfRecipientRes?.status)
    }

    @Test
    @Throws(Exception::class)
    // Tests for transfer where recipients account is closed, should always fail with 403 forbidden
    fun accountClosedTransferTest() {

        Spark.awaitInitialization()

        val recipient: Int = 6
        val sender: Int = 3
        val amount: Double = 1.00

        val getClosedAccountRes: Response? = client?.request("GET", "/accounts/$recipient")

        // Assert status of recipient account
        Struktural.assertValues(getClosedAccountRes!!.body, listOf(
            Pair("closed", true)
        ))

        val testTransferAccountClosedBody: String = "{\"amount\": $amount, \"recipient\": $recipient }"
        val testTransferAccountClosedRes: Response? = client?.request("POST", "/accounts/$sender/transfer", testTransferAccountClosedBody)

        assertEquals("You should not transfer to an account which is closed", 403, testTransferAccountClosedRes?.status)
    }

    @Test
    @Throws(Exception::class)
    // Tests for malformed or invalid payloads being passed to transfer, should always fail
    fun payloadTransferValidationTest() {

        Spark.awaitInitialization()

        val sender: Int = 3
        val recipient: Int = 4
        val amount: Double = 1.0

        // No recipient
        val testTransferWithNoRecipientBody: String = "{\"amount\": $amount }"
        val testTransferWithNoRecipientRes: Response? = client?.request("POST", "/accounts/$sender/transfer", testTransferWithNoRecipientBody)

        assertEquals(400, testTransferWithNoRecipientRes?.status)

        // No amount
        val testTransferWithNoAmountBody: String = "{\"recipient\": $recipient }"
        val testTransferWithNoAmountRes: Response? = client?.request("POST", "/accounts/$sender/transfer", testTransferWithNoAmountBody)

        assertEquals(400, testTransferWithNoAmountRes?.status)

        // No payload data
        val testTransferWithNoPayloadDataBody: String = "{}"
        val testTransferWithNoPayloadDataRes: Response? = client?.request("POST", "/accounts/$sender/transfer", testTransferWithNoPayloadDataBody)

        assertEquals(400, testTransferWithNoPayloadDataRes?.status)
    }

    @Test
    @Throws(java.lang.Exception::class)
    // Tests fetching all accounts at once
    fun getAllAccounts() {
        Spark.awaitInitialization()

        val allAccountsRes: Response? = client?.request("GET", "/accounts")

        assertEquals(200, allAccountsRes?.status)
    }

    @Test
    @Throws(java.lang.Exception::class)
    // Tests fetching all accounts at once
    fun createAccount() {

        val name = "test"

        Spark.awaitInitialization()
        val createAccountBody = """{"name": $name, "email": "test@test.com"}"""
        val createAccountRes: Response? = client?.request("POST", "/accounts/new", createAccountBody)

        assertEquals(200, createAccountRes?.status)
        // Assert status of recipient account
        Struktural.assertValues(createAccountRes!!.body, listOf(
            Pair("name", name),
            Pair("balance", 0.00)
        ))
        // Senders account to JSON
        val jsonCreateAccountRes = JsonParser().parse(createAccountRes?.body).getAsJsonObject()

        // New account id as int
        val id = jsonCreateAccountRes.get("id").getAsInt()

        // Fetch the created account
        val createdAccountRes: Response? = client?.request("GET", "/accounts/$id")

        // Assert the new account ID is same as creation response
        Struktural.assertValues(createdAccountRes!!.body, listOf(
            Pair("name", name),
            Pair("id", id)
        ))
    }
}
