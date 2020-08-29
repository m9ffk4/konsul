package com.github.m9ffk4.konsul.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.m9ffk4.konsul.consul
import com.github.m9ffk4.konsul.prefix
import com.github.m9ffk4.konsul.token
import com.github.m9ffk4.konsul.workDir
import mu.KotlinLogging
import java.io.File

class ConsulToGit(
    private val dry: Boolean,
    private val operation: String,
) : CliktCommand(
    name = "consulToGit",
    help = "Sync Consul -> Git"
) {
    private val logger = KotlinLogging.logger {}

    override fun run() {
        consul.getKVKeysOnly(prefix, null, token).value
            .filter { !it.endsWith("/") }
            .forEach {
                // Получаем значение ключа из консула
                val value = consul.getKVValue(it, token).value.decodedValue
                // Формируем имя файла
                val fileName = "$it${getFileFormat(value)}"
                    .replace("$prefix/", "")
                logger.info {
                    "$operation [${if (dry) "X" else "V"}] | " +
                        "$prefix${it.replace(prefix, "")} -> ${File(workDir).absolutePath}/$fileName"
                }
                if (!dry) {
                    // Генерируем файл с содержимым
                    File("$workDir/$fileName")
                        .also { file -> file.parentFile.mkdirs() }
                        .writeText(value, Charsets.UTF_8)
                }
            }
    }

    private fun getFileFormat(text: String) = when {
        text.startsWith("{") or text.startsWith("[") -> ".json"
        (!text.startsWith("{") or !text.startsWith("[")) and text.contains(": ") -> ".yml"
        else -> ""
    }
}
