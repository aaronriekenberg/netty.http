package org.aaron.netty.http.templates

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.io.ClassPathTemplateLoader
import mu.KLogging

object HandlebarsContainer : KLogging() {

    val handlebars: Handlebars

    init {
        logger.info { "begin init" }

        val loader = ClassPathTemplateLoader()
        loader.prefix = "/templates"
        loader.suffix = ".hbs"

        handlebars = Handlebars(loader)

        logger.info { "end init" }
    }

}