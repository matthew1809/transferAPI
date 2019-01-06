package com.foyle

import spark.Spark.*
import com.foyle.models.Account
import com.foyle.models.Transfer

import com.google.gson.Gson
import com.google.gson.GsonBuilder

import com.foyle.internal.InternalServiceImpl
import com.foyle.models.NewAccount
import com.google.gson.JsonParser
import kotlin.math.sign
import java.math.BigDecimal
import java.math.RoundingMode
import com.beerboy.ss.SparkSwagger

class Controller {

  private val intService = InternalServiceImpl()

  init {
    init()
    initRoutes()
  }

  val gson = GsonBuilder().create()

  private fun initRoutes() {
    exception(Exception::class.java) { e, req, res -> e.printStackTrace() }

    // Initialise the spark service
    // val spark: spark.Service = spark.Service.ignite().port(4567)

    path("accounts") {

      get("/:id") { req, res ->
        res.type("application/json")

        try {
          val ac = intService.findSingle(req.params("id").toInt())
          res.status(200)
          Gson().toJsonTree(ac)
        } catch (e: Exception) {
          halt(500, e.message)
        }
      }

      post("/new") { req, res ->
        res.type("application/json")

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

        res.type("application/json")

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
