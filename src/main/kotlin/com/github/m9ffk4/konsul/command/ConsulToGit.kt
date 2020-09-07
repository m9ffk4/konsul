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
    override fun run() {
        val values = if (token.isNotBlank()) {
            consul.getKVKeysOnly(prefix, null, token).value
        } else {
            consul.getKVKeysOnly(prefix).value
        }
        values.filter { !it.endsWith("/") }
            .forEach {
                // Получаем значение ключа из консула
                val value = if (token.isNotBlank()) {
                    consul.getKVValue(it, token).value.decodedValue
                } else {
                    consul.getKVValue(it).value.decodedValue
                }
                // Формируем имя файла
                val fileName = "$it${getFileFormat(value)}"
                    .replace("$prefix/", "")
                println(
                    "$operation [${if (dry) "X" else "V"}] | " +
                        "$prefix${it.replace(prefix, "")} -> ${File(workDir).absolutePath}/$fileName"
                )
                if (dry) {
                    return
                }
                // Генерируем файл с содержимым
                File("$workDir/$fileName")
                    .also { file -> file.parentFile.mkdirs() }
                    .writeText(value, Charsets.UTF_8)
            }
    }

    /**
     * Определяет формат value по его содержимому (строка / json / yaml)
     */
    private fun getFileFormat(text: String) = when {
        text.startsWith("{") or text.startsWith("[") -> ".json"
        (!text.startsWith("{") or !text.startsWith("[")) and text.contains(": ") -> ".yml"
        else -> ""
    }
}
