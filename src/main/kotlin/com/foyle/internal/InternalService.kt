package com.foyle.internal

import com.foyle.models.*
import java.math.BigDecimal

interface InternalService {

  fun findSingle(id: Int): Account?

  fun findAll(): HashMap<Int, Account>

  fun transfer(senderID: Int, receiverID: Int, amount: BigDecimal): Account?

  fun save(name: String, email: String): Account?
}
