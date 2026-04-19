package com.jacuum.web

import com.jacuum.algo.Algorithms
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class AlgosEndpoint(private val algorithms: Algorithms) : AlgosApi {
    @GetMapping("/algos")
    override fun algos(): List<String> = algorithms.names()

    @GetMapping("/avatars")
    override fun avatars(): List<String> =
        listOf("\uD83E\uDD16", "\uD83E\uDDBE", "\uD83D\uDC7E", "\uD83D\uDE80",
               "\uD83D\uDEF8", "\uD83E\uDD84", "\uD83D\uDC22", "\uD83E\uDD8A",
               "\uD83D\uDC31", "\uD83D\uDC38")
}
