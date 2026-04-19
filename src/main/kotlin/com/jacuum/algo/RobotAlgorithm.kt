package com.jacuum.algo

import org.springframework.core.annotation.AliasFor
import org.springframework.stereotype.Component

@Component
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RobotAlgorithm(
    @get:AliasFor(annotation = Component::class)
    val value: String = ""
)
