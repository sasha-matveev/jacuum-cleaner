package com.jacuum.algo

import org.springframework.context.ApplicationContext

class SpringAlgorithms(private val ctx: ApplicationContext) : Algorithms {

    private val beans: Map<String, Any> =
        ctx.getBeansWithAnnotation(RobotAlgorithm::class.java).toMap()

    override fun names(): List<String> = beans.values.map { displayName(it) }.sorted()

    @Throws(Exception::class)
    override fun instantiate(name: String): RobotAlgo {
        val entry = beans.entries.find { displayName(it.value) == name }
            ?: throw Exception("Unknown algorithm: $name")
        return ctx.getBean(entry.key) as RobotAlgo
    }

    private fun displayName(bean: Any): String {
        val ann = bean.javaClass.getAnnotation(RobotAlgorithm::class.java)
        return ann.value.ifBlank { bean.javaClass.simpleName }
    }
}
