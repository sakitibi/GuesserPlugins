// Copyright SKNewRoles
package com.example.guesser

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.minecraft.scoreboard.Team
import net.minecraft.server.network.ServerPlayerEntity
import com.mojang.brigadier.builder.LiteralArgumentBuilder

// 必要なインポートを追加
import java.io.File

// GameManagerの定義をオブジェクト内に修正
object GameManager {
    var meeting: Boolean = false
}

class GuesserPlugins : ModInitializer {

    override fun onInitialize() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            val btCommand = literal("bt")
                .then(argument("username", StringArgumentType.word())
                    .then(argument("teamname", StringArgumentType.word())
                        .executes { ctx: CommandContext<ServerCommandSource> -> handleBtCommand(ctx) }
                    )
                )
            dispatcher.register(btCommand)
        }
    }

    private fun handleBtCommand(ctx: CommandContext<ServerCommandSource>): Int {
        val username = StringArgumentType.getString(ctx, "username")
        val teamName = StringArgumentType.getString(ctx, "teamname")
        val server = ctx.source.server
        val scoreboard = server.scoreboard

        val objective = scoreboard.getObjective("GameManager")

        val meetingObjective = scoreboard.getObjective("GameManager")
        val meetingScore = meetingObjective?.let { scoreboard.getPlayerScore("meeting", it)?.score } ?: 0
        GameManager.meeting = meetingScore != 0

        val executor = ctx.source.entity as? ServerPlayerEntity
        val allowedTeamNames = listOf("Niceguesser", "Evilguesser")

        val playerTeamName = scoreboard.getPlayerTeam(executor?.entityName)?.name
        if (executor == null || playerTeamName !in allowedTeamNames) {
            ctx.source.sendFeedback(Text.literal("このコマンドは ${allowedTeamNames.joinToString(", ")} チームのメンバーにしか実行できません。"), false)
            return 0
        }

        val player = server.playerManager.getPlayer(username)
        val team = scoreboard.getTeam(teamName)

        if (player != null && team != null && team.playerList.contains(player.entityName)) {
            server.commandManager.dispatcher.execute("kill $username", server.commandSource)
            ctx.source.sendFeedback(Text.literal("ターゲットプレイヤー $username をゲス成功しました!"), false)
            return 1
        } else {
            return if (executor != null) {
                executor.kill()
                ctx.source.sendFeedback(Text.literal("ターゲットプレイヤーの役職が違う為、\n自分をキルしました.."), false)
                1
            } else {
                ctx.source.sendFeedback(Text.literal("対象プレイヤーが見つからない、またはチームに所属していません\n引数はREADME.htmlをご覧下さい"), false)
                0
            }
        }
    }

    private fun logBtCommand(ctx: CommandContext<ServerCommandSource>, teamName: String, result: String) {
        val timestamp = System.currentTimeMillis()
        val playerName = ctx.source.entity?.name ?: "unknown"  // Safely handle null entity

        val logEntry = """
            {
                "time": $timestamp,
                "player": "$playerName",
                "command": "bt",
                "team": "$teamName",
                "meeting": ${GameManager.meeting},
                "result": "$result"
            }
        """.trimIndent()
    
        val logFile = File("analytics/bt_logs.json")
        logFile.parentFile.mkdirs()
        logFile.appendText(logEntry + ",\n")
    }
}
