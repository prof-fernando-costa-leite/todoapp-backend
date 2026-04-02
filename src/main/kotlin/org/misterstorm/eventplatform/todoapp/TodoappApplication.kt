package org.misterstorm.eventplatform.todoapp

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class TodoappApplication

fun main(args: Array<String>) {
    runApplication<TodoappApplication>(*args)
}
