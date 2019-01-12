package com.foyle.internal

import com.foyle.models.Account
import java.math.BigDecimal
import java.util.concurrent.locks.Lock

interface InternalService {

  fun findSingle(id: Int, lock: Lock): Account?

  fun findAll(lock: Lock): HashMap<Int, Account>

  fun transfer(senderID: Int, receiverID: Int, amount: BigDecimal, lock: Lock): Account?

  fun newAccount(name: String, email: String): Account?
}
