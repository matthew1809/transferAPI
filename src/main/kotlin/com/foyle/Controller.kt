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
import com.google.gson.JsonParser

// Internal Service
import com.foyle.internal.InternalServiceImpl

// Number specific
import kotlin.math.sign
import java.math.BigDecimal
import java.math.RoundingMode

// Holds all API routes
class Controller {

  private val intService = InternalServiceImpl()
  private val gson = Gson()

  init {
    initRoutes()
  }

  // Validates the body of a transfer request
  private fun transferValidator(req: spark.Request) {
    val jsonObj = JsonParser().parse(req.body()).getAsJsonObject()

    if (!jsonObj.has("amount") || !jsonObj.has("recipient")) {
      halt(400, gson.toJson("Check you have supplied a recipient and an amount in the body"))
    }

    if (jsonObj.get("amount").asDouble.sign != 1.0) {
      halt(400, gson.toJson("You cannot transfer nothing or negative amounts"))
    }
  }

  // Validates the content of a POST request
  private fun postValidator(req: spark.Request) {

    if (req.contentLength() == 0) {
      halt(400, "Expected a payload with content in it")
    }

    if ("application/json" !in req.contentType()) {
      halt(400, "Body must be of type application/json")
    }
  }

  // Validates the specific body of a a new account request
  private fun newAccountValidator(req: spark.Request) {
    val jsonObj = JsonParser().parse(req.body()).getAsJsonObject()

    if (!jsonObj.has("name") || !jsonObj.has("email")) {
      halt(400, "You have not provided a name and email!")
    }
  }

  // Validates an ID of an account being requested
  private fun iDValidator(str: String) {
    try {
      str.toInt()
    } catch(e: NumberFormatException) {
      halt(400, "You have not provided a valid ID")
    }

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
        iDValidator(id)

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

        postValidator(req)
        newAccountValidator(req)

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

        postValidator(req)
        transferValidator(req)

        try {

          val payload: Transfer = gson.fromJson(req.body(), Transfer::class.java)
          val id = req.params("id")

          iDValidator(id)

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
