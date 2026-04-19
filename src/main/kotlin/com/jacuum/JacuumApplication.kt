package com.jacuum

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class JacuumApplication

fun main(args: Array<String>) {
    runApplication<JacuumApplication>(*args)
}
