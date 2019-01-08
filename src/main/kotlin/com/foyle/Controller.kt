package com.foyle

// Spark specific
import spark.Spark.exception
import spark.Spark.notFound
import spark.Spark.path
import spark.Spark.after
import spark.Spark.get
import spark.Spark.post
import spark.Spark.halt

// Models
import com.foyle.models.Transfer
import com.foyle.models.NewAccount

// Gson specific
import com.google.gson.Gson

// Internal Service
import com.foyle.internal.InternalServiceImpl

import com.foyle.internal.Validation

// Number specific
import java.math.BigDecimal
import java.math.RoundingMode

// Holds all API routes
class Controller {

  private val intService = InternalServiceImpl()
  private val gson = Gson()

  init {
    initRoutes()
  }

  private fun initRoutes() {

    exception(Exception::class.java) { e, _, _ -> e.printStackTrace() }

    notFound { _, res ->
      "{\"message\":\"The route you are attempting to reach has not been found. Try GET /accounts, GET /accounts/:id, POST /accounts/:id/transfer, POST /accounts/new\"}"
    }

    // Always set JSON as the response type
    after("/*") { _, res -> res.type("application/json") }

    path("accounts") {

      // Get all accounts
      get("", { req, res ->
        try {
          res.status(200)
          intService.findAll()

        } catch (e: Exception) {
          halt(500, gson.toJson(e.message))
        }
      }, gson::toJson)


      // Find a single account by ID
      get("/:id", { req, res ->

        val id = req.params("id")
        Validation.iDValidator(id)

        try {
          res.status(200)

          intService.findSingle(id.toInt())
        } catch (e: Exception) {
          res.status(403)
          gson.toJson(e.message)
        }
      }, gson::toJson)


      // Create a new account
      post("/new", { req, res ->

        Validation.postValidator(req)
        Validation.newAccountValidator(req)

        val payload: NewAccount = gson.fromJson(req.body(), NewAccount::class.java)

        try {
          res.status(200)
          intService.newAccount(payload.name, payload.email)
        } catch (e: Exception) {
          halt(500, gson.toJson(e.message))
        }
      }, gson::toJson)


      // Transfer money from one account to another
      post("/:id/transfer", { req, res ->

        Validation.postValidator(req)
        Validation.transferValidator(req)

        try {

          val payload: Transfer = gson.fromJson(req.body(), Transfer::class.java)
          val id = req.params("id")

          Validation.iDValidator(id)

          res.status(200)
          intService.transfer(id.toInt(), payload.recipient, BigDecimal(payload.amount).setScale(2, RoundingMode.DOWN))

        } catch (e: Exception) {
          res.status(403)
          gson.toJson(e.message)
        }
      }, gson::toJson)

    }
  }
}
