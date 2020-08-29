package com.github.m9ffk4.konsul.command

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.MINIMIZE_QUOTES
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.m9ffk4.konsul.consul
import com.github.m9ffk4.konsul.prefix
import com.github.m9ffk4.konsul.token
import com.github.m9ffk4.konsul.workDir
import com.jayway.jsonpath.JsonPath
import mu.KotlinLogging
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.util.stream.Collectors

class CopyToConsul(
    private val dry: Boolean,
    private val operation: String,
) : CliktCommand(
    name = "copyToConsul",
    help = "Sync Git -> Consul, with can change some values"
) {
    private val logger = KotlinLogging.logger {}

    private val changes by option(
        names = arrayOf("-c", "--changes"),
        help = "Properties, that will be changed format \"key:value\""
    )

    override fun run() {
        changes ?: throw UnsupportedOperationException("Where fucking changes???")
        logger.info {
            """  Run $commandName with values:
            |   changes = $changes
            |
        """.trimMargin()
        }

        // Разбираем изменения на пары
        val arrChanges = changes!!.split(",").stream().map {
            Pair(
                it.replace(Regex(":.*"), ""),
                it.replace(Regex(".*:"), "")
            )
        }.collect(Collectors.toList())

        File(workDir)
            .walkTopDown()
            // Оставляем только файлы
            .filter { it.isFile }
            .forEach {
                val key = it.path.substringAfter(workDir)
                    // Убираем формат
                    .replace(".yml", "")
                    .replace(".json", "")
                var value = String(it.readBytes())

                logger.info { "Есть изменения, меняем конфиги" }
                // Разбираем Yaml в Json
                try {
                    val json = JsonPath.parse(
                        Yaml().load(value) as Map<String, Any>
                    )
                    // Изменяем поля
                    arrChanges.forEach { pair ->
                        json.set(pair.first, pair.second)
                    }
                    // Собираем YAML обратно
                    value = YAMLMapper(YAMLFactory().apply {
                        disable(MINIMIZE_QUOTES)
                    }).writeValueAsString(ObjectMapper().readTree(json.jsonString()))
                } catch (e: Exception) {
                    logger.info { "В [$key] нечего менять, либо это не YAML" }
                }
                // Создаем запись в consul
                logger.info { "[$operation ${if (dry) "X" else "V"}] | $it -> $prefix$key" }
                if (!dry) {
                    consul.setKVValue("$prefix$key", value, token, null)
                }
            }
    }
}
