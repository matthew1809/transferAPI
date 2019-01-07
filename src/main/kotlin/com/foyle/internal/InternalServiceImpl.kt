package com.foyle.internal

// Models
import com.foyle.models.Account

// Is what it is
import java.lang.Exception

// Concurrency specific
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

// Number specific
import java.util.concurrent.atomic.AtomicInteger
import java.math.BigDecimal
import java.math.RoundingMode

// Implements interface for API methods
class InternalServiceImpl : InternalService {

    // Initialise our accounts as a hashmap of Accounts
  private val accounts = hashMapOf(
    0 to Account(name = "Ivan", email = "ivan@revolut.ru", id = 0, closed = false, balance = BigDecimal(5000.00).setScale(2, RoundingMode.DOWN), country = "United Kingdom"),
    1 to Account(name = "Matthew", email = "matthew@revolut.ru", id = 1, closed = false, balance = BigDecimal(10.00).setScale(2, RoundingMode.DOWN), country = "United Kingdom"),
    2 to Account(name = "Carol", email = "carol@carol.kt", id = 2, closed = false, balance = BigDecimal(100.00).setScale(2, RoundingMode.DOWN), country = "United Kingdom"),
    3 to Account(name = "Dave", email = "dave@dave.kt", id = 3, closed = false, balance = BigDecimal(70.00).setScale(2, RoundingMode.DOWN), country = "United Kingdom"),
    4 to Account(name = "Marina", email = "marina@marina.kt", id = 4, closed = false, balance = BigDecimal(0.00).setScale(2, RoundingMode.DOWN), country = "United Kingdom"),
    5 to Account(name = "Darina", email = "darina@darina.kt", id = 5, closed = false, balance = BigDecimal(20.00).setScale(2, RoundingMode.DOWN), country = "United Kingdom"),
    6 to Account(name = "John", email = "john@john.kt", id = 6, closed = true, balance = BigDecimal(500.00).setScale(2, RoundingMode.DOWN), country = "United Kingdom")
  )

  // Initialise our variable for creating a new account ID
  private val lastId: AtomicInteger = AtomicInteger(accounts.size - 1)

  // Helper function to throw an exception
  fun fail(message: String?): Nothing {
    throw Exception(message)
  }

  // Return the account with the given ID
  override fun findSingle(id: Int): Account? {

    return try {
      accounts[id]
    } catch (e: Exception) {
      fail(e.message)
    }
  }

  // Transfer money from one account to another account
  override fun transfer(senderID: Int, receiverID: Int, amount: BigDecimal): Account? = try {

    val lock = ReentrantLock()

    lock.withLock {

      val senderAC = accounts.get(senderID) ?: fail("Sender not found!")

      val receiverAC = accounts.get(receiverID) ?: fail("Recipient not found!")

      if (senderID == receiverID) {
        fail("You cannot send money to your own account!")
      }

      if (senderAC.balance < amount) {
        fail("Sender does not have sufficient balance!")
      }

      if (senderAC.closed || receiverAC.closed) {
        fail("One of these accounts are no longer active!")
      }

      accounts.put(
        senderID,
        Account(
          name = senderAC.name,
          email = senderAC.email,
          id = senderID,
          closed = senderAC.closed,
          balance = senderAC.balance - amount,
          country = senderAC.country
        )
      )

      accounts.put(
        receiverID,
        Account(
          name = receiverAC.name,
          email = receiverAC.email,
          id = receiverID,
          closed = receiverAC.closed,
          balance = receiverAC.balance + amount,
          country = receiverAC.country
        )
      )

      return accounts[senderID]
    }
  } catch (e: Exception) {
    fail(e.message)
  }

  override fun save(name: String, email: String): Account? {

    try {
    val id = lastId.incrementAndGet()
    accounts.put(id, Account(name = name, email = email, id = id, closed = false, balance = BigDecimal(0.00).setScale(2), country = "United Kingdom"))

    return accounts[id]
    } catch (e: Exception) {
      fail(e.message)
    }
  }

  override fun findAll(): HashMap<Int, Account> {
    try {
    return accounts
  } catch (e: Exception) {
      fail(e.message)
    }
  }
}
