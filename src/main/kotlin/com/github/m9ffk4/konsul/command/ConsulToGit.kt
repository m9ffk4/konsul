package com.github.m9ffk4.konsul.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.m9ffk4.konsul.consul
import com.github.m9ffk4.konsul.dry
import com.github.m9ffk4.konsul.operation
import com.github.m9ffk4.konsul.prefix
import com.github.m9ffk4.konsul.token
import com.github.m9ffk4.konsul.workDir
import java.io.File

class ConsulToGit : CliktCommand(
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
                val value = when {
                    token.isBlank() -> consul.getKVValue(it).value.decodedValue ?: ""
                    token.isNotBlank() -> consul.getKVValue(it, token).value.decodedValue ?: ""
                    else -> throw NoSuchElementException("Не очень понятно что произошло")
                }
                // Формируем имя файла
                val fileName = "$it${getFileFormat(value)}"
                    .replace("$prefix/", "")
                println(
                    "$operation [${if (dry) "X" else "V"}] | " +
                        "$prefix${it.replace(prefix, "")} -> ${File(workDir).absolutePath}/$fileName"
                )
                if (dry) {
                    return@forEach
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
