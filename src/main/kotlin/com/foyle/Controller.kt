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
import com.foyle.models.Account
import com.foyle.models.Transfer
import com.foyle.models.NewAccount

// Gson specific
import com.google.gson.Gson
import com.google.gson.GsonBuilder
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

  init {
    initRoutes()
  }

  val gson = GsonBuilder().create()

  private fun postValidator(req: spark.Request) {

    if (req.contentLength() == 0) {
      halt(400, "Expected a payload with content in it")
    }

    if ("application/json" !in req.contentType()) {
      println("should halt")
      halt(400, "Body must be of type application/json")
    }
  }

  private fun initRoutes() {
    exception(Exception::class.java) { e, _, _ -> e.printStackTrace() }

    notFound { _, res ->
      res.type("application/json")
      "{\"message\":\"The route you are attempting to reach has not been found. Try GET /accounts, GET /accounts/:id, POST /accounts/:id/transfer, POST /accounts/new\"}"
    }

    path("accounts") {

      after("/*") { _, res -> res.type("application/json") }

      get("") { req, res ->

        try {
          val accounts = intService.findAll()

          res.status(200)
          Gson().toJsonTree(accounts)
        } catch (e: Exception) {
          halt(500, e.message)
        }
      }

      get("/:id") { req, res ->

        try {
          val ac = intService.findSingle(req.params("id").toInt())
          res.status(200)
          Gson().toJsonTree(ac)
        } catch (e: Exception) {
          halt(500, e.message)
        }
      }

      post("/new") { req, res ->

        postValidator(req)

        val jsonObj = JsonParser().parse(req.body()).getAsJsonObject()

        if (!jsonObj.has("name") || !jsonObj.has("email")) {
          halt(400, gson.toJson("You have not provided a name and email!"))
        }

        val payload: NewAccount = gson.fromJson(req.body(), NewAccount::class.java)

        try {
          val newAccountResult: Account? = intService.save(payload.name, payload.email)

          res.status(200)
          gson.toJson(newAccountResult)
        } catch (e: java.lang.Exception) {
          halt(500, gson.toJson(e.message))
        }
      }

      post("/:id/transfer") { req, res ->

        postValidator(req)

        val jsonObj = JsonParser().parse(req.body()).getAsJsonObject()

        if (!jsonObj.has("amount") || !jsonObj.has("recipient")) {
          halt(400, gson.toJson("Check you have supplied a recipient and an amount in the body"))
        }

        if (jsonObj.get("amount").asDouble.sign != 1.0) {
          halt(400, gson.toJson("You cannot transfer nothing or negative amounts"))
        }

        try {

          val payload: Transfer = gson.fromJson(req.body(), Transfer::class.java)

          val transferResult = intService.transfer(req.params("id").toInt(), payload.recipient, BigDecimal(payload.amount.toDouble()).setScale(2, RoundingMode.DOWN))

          val jsonTransferResult = Gson().toJsonTree(transferResult)

          res.status(200)

          gson.toJson(jsonTransferResult)
        } catch (e: Exception) {
          halt(403, gson.toJson(e.message))
        }
      }
    }
  }
}
