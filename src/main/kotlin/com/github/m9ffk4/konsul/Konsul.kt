package com.github.m9ffk4.konsul

import com.ecwid.consul.v1.ConsulClient
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import com.github.ajalt.clikt.parameters.types.int
import com.github.m9ffk4.konsul.Default.Consul
import com.github.m9ffk4.konsul.command.ConsulToGit
import com.github.m9ffk4.konsul.command.CopyToConsul
import com.github.m9ffk4.konsul.command.GitToConsul
import mu.KotlinLogging

private val banner = String(
    {}.javaClass.getResourceAsStream("/banner.txt").readAllBytes()
)
private val version = {}.javaClass.`package`.implementationVersion ?: "0.0.0"

lateinit var consul: ConsulClient
lateinit var token: String
lateinit var prefix: String
lateinit var workDir: String
var dry = false

// TODO Будет задаваться во время выполнения операции, после добавления стратегий (create/delete/update).
private const val operation = "update"

object Default {
    val workDir = "${System.getProperty("user.home")}/konsul/config"
    const val dry = false

    object Consul {
        const val host = "http://localhost"
        const val port = 8500
        const val token = ""
        const val prefix = ""
    }
}

class Konsul : CliktCommand(
    name = "Konsul",
    help = "Consul CLI for synchronization Consul -> Git -> Consul",
    printHelpOnEmptyArgs = true
) {
    private val host by option(
        names = arrayOf("-h", "--host"),
        help = "Consul host"
    )
        .default(Consul.host)
    private val port by option(
        names = arrayOf("-p", "--port"),
        help = "Consul port"
    )
        .int()
        .default(Consul.port)
    private val t by option(
        names = arrayOf("-t", "--token"),
        help = "Consul ACL token"
    )
        .default(Consul.token)
    private val pr by option(
        names = arrayOf("-pr", "--prefix"),
        help = "Consul KV prefix"
    )
        .default(Consul.prefix)
    private val wd by option(
        names = arrayOf("-w", "--workdir"),
        help = "The path to the directory where the configs will be saved"
    )
        .default(Default.workDir)
    private val d by option(
        names = arrayOf("-d", "--dry"),
        help = "Show the result of an operation without executing it\n"
    )
        .flag(default = Default.dry)

    init {
        println(banner.replace("{}", version))
        versionOption(names = setOf("-v", "--version"), version = version)
    }

    override fun run() {
        consul = ConsulClient(host, port)
        token = t
        prefix = pr
        workDir = wd
        dry = d
        println(
            """Run $commandName with values:
            |   consul.host = $host
            |   consul.port = $port
            |   consul.token = [hidden]
            |   consul.prefix = $prefix
            |   workdir = $workDir
            |   dry = $dry
            |
        """.trimMargin()
        )
    }
}

fun main(args: Array<String>) = Konsul()
    .subcommands(
        ConsulToGit(dry = dry, operation = operation),
        GitToConsul(dry = dry, operation = operation),
        CopyToConsul(dry = dry, operation = operation)
    )
    .main(args)
