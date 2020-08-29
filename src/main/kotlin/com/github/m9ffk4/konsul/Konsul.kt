package com.github.m9ffk4.konsul

import com.ecwid.consul.v1.ConsulClient
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import com.github.ajalt.clikt.parameters.types.int
import com.github.m9ffk4.konsul.Default.Consul
import com.jayway.jsonpath.JsonPath
import mu.KotlinLogging
import org.yaml.snakeyaml.Yaml
import java.io.File

private val banner = String({}.javaClass.getResourceAsStream("/banner.txt").readAllBytes())
private val version = "0.4.0" //TODO Запилить, чтоб показывал текущую версию сам

private val log = KotlinLogging.logger { }
lateinit var consul: ConsulClient
lateinit var token: String
lateinit var prefix: String
lateinit var workDir: String
private var dry = false

private const val operation = "update" //TODO Будет задаваться во время выполнения операции, после добавления стратегий.
//create/delete/update

object Default {
    val workDir = "${System.getProperty("user.home")}/konsul/config"

    object Consul {
        const val host = "http://localhost"
        const val port = 8500
    }
}

class Konsul : CliktCommand(
    name = "Konsul", help = "Consul CLI for config synchronization Consul -> Git -> Consul",
    printHelpOnEmptyArgs = true
) {
    private val host by option(names = arrayOf("-h", "--host"), help = "Consul host")
        .default(Consul.host)
    private val port by option(names = arrayOf("-p", "--port"), help = "Consul port").int()
        .default(Consul.port)
    private val t by option(names = arrayOf("-t", "--token"), help = "Consul ACL token")
    private val pr by option(names = arrayOf("-pr", "--prefix"), help = "Consul KV prefix")
    private val wd by option(
        names = arrayOf("-w", "--workdir"),
        help = "The path to the directory where the configs will be saved"
    ).default(Default.workDir)
    private val d by option(
        names = arrayOf("-d", "--dry"),
        help = "Show the result of an operation without executing it\n"
    ).flag(default = false)

    init {
        log.info { banner.replace("{}", version) }
        versionOption(names = setOf("-v", "--version"), version = version)
    }

    override fun run() {
        consul = ConsulClient(host, port)
        token = if (t.isNullOrEmpty()) throw IllegalArgumentException("Where fucking Consul ACL token?!")
        else t.toString()
        prefix = if (pr.isNullOrEmpty()) throw IllegalArgumentException("Where fucking Consul KV prefix?!")
        else pr.toString()
        workDir = wd
        dry = d
        log.info {
            """Run $commandName with values:
            |   consul.host = $host
            |   consul.port = $port
            |   consul.token = [hidden]
            |   consul.prefix = $prefix
            |   workdir = $workDir
            |   dry = $dry
            |
        """.trimMargin()
        }
    }
}

class ConsulToGit : CliktCommand(name = "consulToGit", help = "Sync Consul -> Git") {
    override fun run() {
        consul.getKVKeysOnly(prefix, null, token).value
            .filter { !it.endsWith("/") }
            .forEach {
                // Получаем значение ключа из консула
                val value = consul.getKVValue(it, token).value.decodedValue
                // Формируем имя файла
                val fileName = "$it${getFileFormat(value)}".replace("$prefix/", "")
                log.info {
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
}

class GitToConsul : CliktCommand(name = "gitToConsul", help = "Sync Git -> Consul") {
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
                log.info { "$operation [${if (dry) "X" else "V"}] | $it -> $prefix$key" }
                if (!dry) {
                    consul.setKVValue("$prefix$key", value, token, null)
                }
            }
    }
}

class CopyToConsul : CliktCommand(name = "copyToConsul", help = "Sync Git -> Consul, with can change some values") {
    private val changes by option(
        names = arrayOf("-c", "--changes"),
        help = "Properties, that will be changed format \"key:value\""
    )

    override fun run() {
        changes ?: throw UnsupportedOperationException("Where fucking changes???")
        log.info {
            """  Run $commandName with values:
            |   changes = $changes
            |
        """.trimMargin()
        }

        // Разбираем изменения на пары
        val arrChanges = changes!!
            .split(",")
            .map {
                Pair(
                    it.replace(Regex(":.*"), ""),
                    it.replace(Regex(".*:"), "")
                )
            }

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

                log.info { "Есть изменения, меняем конфиги" }
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
                        disable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                    }).writeValueAsString(ObjectMapper().readTree(json.jsonString()))
                } catch (e: Exception) {
                    log.info { "В [$key] нечего менять, либо это не YAML" }
                }
                // Создаем запись в consul
                log.info { "[$operation ${if (dry) "X" else "V"}] | $it -> $prefix$key" }
                if (!dry) {
                    consul.setKVValue("$prefix$key", value, token, null)
                }
            }
    }
}

fun main(args: Array<String>) = Konsul().subcommands(ConsulToGit(), GitToConsul(), CopyToConsul()).main(args)

private fun getFileFormat(text: String) = when {
    text.startsWith("{") or text.startsWith("[") -> ".json"
    (!text.startsWith("{") or !text.startsWith("[")) and text.contains(": ") -> ".yml"
    else -> ""
}
