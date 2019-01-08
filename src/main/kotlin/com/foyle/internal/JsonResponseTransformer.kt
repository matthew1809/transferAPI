package com.foyle.internal

import spark.ResponseTransformer
import com.google.gson.Gson

class JsonResponseTransformer : ResponseTransformer {

    private val gson = Gson()

    override fun render(model: Any?) = gson.toJson(model)
}

