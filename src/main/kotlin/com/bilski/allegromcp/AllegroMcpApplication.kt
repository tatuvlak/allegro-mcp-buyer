package com.bilski.allegromcp

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AllegroMcpApplication

fun main(args: Array<String>) {
    runApplication<AllegroMcpApplication>(*args)
}
