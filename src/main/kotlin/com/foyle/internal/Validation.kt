package com.foyle.internal

import com.google.gson.JsonParser
import com.google.gson.Gson
import spark.Spark
import kotlin.math.sign
import java.lang.Double.parseDouble

object Validation {

    val gson = Gson()

    // Validates the body of a transfer request
    fun transferValidator(req: spark.Request) {
        val jsonObj = JsonParser().parse(req.body()).getAsJsonObject()

        if (!jsonObj.has("amount") || !jsonObj.has("recipient")) {
            Spark.halt(400, gson.toJson("Check you have supplied a recipient and an amount in the body"))
        }
        val amount = jsonObj.get("amount")

        try {
            parseDouble(amount.asString)
        } catch (e: NumberFormatException) {
            Spark.halt(400, gson.toJson("Amount must be a valid number"))
        }

        if (amount.asDouble.sign != 1.0) {
            Spark.halt(400, gson.toJson("You cannot transfer nothing or negative amounts"))
        }

        val decimalPlaceSplitter = amount.toString().split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        if (decimalPlaceSplitter.size == 2) {

            if(decimalPlaceSplitter[1].length > 2) {
                Spark.halt(400, "You must provide a number with 2 numbers after the decimal place i.e. 1.00")
            }

        }
    }

    // Validates the content of a POST request
    fun postValidator(req: spark.Request) {

        if (req.contentLength() == 0) {
            Spark.halt(400, "Expected a payload with content in it")
        }

        if ("application/json" !in req.contentType()) {
            Spark.halt(400, "Body must be of type application/json")
        }
    }

    // Validates the specific body of a a new account request
    fun newAccountValidator(req: spark.Request) {
        val jsonObj = JsonParser().parse(req.body()).getAsJsonObject()

        if (!jsonObj.has("name") || !jsonObj.has("email")) {
            Spark.halt(400, "You have not provided a name and email!")
        }
    }

    // Validates an ID of an account being requested
    fun iDValidator(str: String) {
        try {
            str.toInt()
        } catch(e: NumberFormatException) {
            Spark.halt(400, "You have not provided a valid ID")
        }

    }
}