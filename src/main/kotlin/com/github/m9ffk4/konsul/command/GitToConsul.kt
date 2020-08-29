package com.github.m9ffk4.konsul.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.m9ffk4.konsul.consul
import com.github.m9ffk4.konsul.prefix
import com.github.m9ffk4.konsul.token
import com.github.m9ffk4.konsul.workDir
import mu.KotlinLogging
import java.io.File

class GitToConsul(
    private val dry: Boolean,
    private val operation: String,
) : CliktCommand(
    name = "gitToConsul",
    help = "Sync Git -> Consul"
) {
    private val logger = KotlinLogging.logger {}

    override fun run() {
        // Берем все файлы из каталога
        File(workDir)
            .walkTopDown()
            // Оставляем только файлы
            .filter { it.isFile }
            .forEach {
                val key = it.path.substringAfter(workDir)
                    // Убираем формат
                    .replace(".yml", "")
                    .replace(".json", "")
                val value = String(it.readBytes())
                // Создаем запись в consul
                logger.info { "$operation [${if (dry) "X" else "V"}] | $it -> $prefix$key" }
                if (!dry) {
                    consul.setKVValue("$prefix$key", value, token, null)
                }
            }
    }
}
