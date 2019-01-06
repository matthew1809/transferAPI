package com.foyle.models

import java.math.BigDecimal

// Payload for a transfer between accounts
data class Transfer(val amount: Double, val recipient: Int)

// Account model
data class Account(val name: String, val email: String, val id: Int, val closed: Boolean, val balance: BigDecimal, val country: String)

// Payload for creating a new account
data class NewAccount(val name: String, val email: String)
