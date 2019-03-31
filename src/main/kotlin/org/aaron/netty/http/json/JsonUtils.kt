package org.aaron.netty.http.json

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

object ObjectMapperContainer {

    val objectMapper = ObjectMapper().registerKotlinModule()

}