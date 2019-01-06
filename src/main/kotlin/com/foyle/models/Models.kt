package com.foyle.models

import java.math.BigDecimal

data class Transfer(val amount: Double, val recipient: Int)

data class Account(val name: String, val email: String, val id: Int, val closed: Boolean, val balance: BigDecimal, val country: String)

data class NewAccount(val name: String, val email: String)
