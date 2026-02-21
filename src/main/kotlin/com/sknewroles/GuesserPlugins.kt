// Copyright 2025 SKNewRoles
// src/main/kotlin/com/sknewroles/GuesserPlugins.kt
package com.sknewroles

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import java.io.File

object GameManager {
    var meeting: Boolean = false
}

class GuesserPlugins : ModInitializer {

    override fun onInitialize() {
        // コマンド登録
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            val btCommand = literal("bt")
                .then(argument("username", StringArgumentType.word())
                    .then(argument("teamname", StringArgumentType.word())
                        .executes { ctx -> handleBtCommand(ctx) }
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

        // ミーティング状態確認
        val meetingObjective = scoreboard.getObjective("GameManager")
        val meetingScore = meetingObjective?.let { scoreboard.getPlayerScore("meeting", it)?.score } ?: 0
        GameManager.meeting = meetingScore != 0

        val executor = ctx.source.entity as? ServerPlayerEntity
        val allowedTeams = listOf("Niceguesser", "Evilguesser")
        val executorTeam = executor?.let { scoreboard.getPlayerTeam(it.entityName)?.name }

        if (executor == null || executorTeam == null || executorTeam !in allowedTeams) {
            ctx.source.sendFeedback(
                Text.literal("このコマンドは ${allowedTeams.joinToString(", ")} チームのメンバーにしか実行できません。"),
                false
            )
            return 0
        }

        val targetPlayer = server.playerManager.getPlayer(username)
        val targetTeam = scoreboard.getTeam(teamName)

        return if (targetPlayer != null && targetTeam != null && targetTeam.playerList.contains(targetPlayer.entityName)) {
            // ターゲットキル成功
            targetPlayer.kill() // 安全に死亡処理
            ctx.source.sendFeedback(Text.literal("ターゲットプレイヤー $username を推測成功しました!"), false)
            logBtCommand(ctx, teamName, "success")
            1
        } else {
            // 自殺処理
            executor.kill() // 安全に死亡処理
            ctx.source.sendFeedback(Text.literal("ターゲットプレイヤーの役職が違う為、自分をキルしました.."), false)
            logBtCommand(ctx, teamName, "fail")
            1
        }
    }

    private fun logBtCommand(ctx: CommandContext<ServerCommandSource>, teamName: String, result: String) {
        val timestamp = System.currentTimeMillis()
        val playerName = ctx.source.entity?.name ?: "unknown"
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

        val logFile = File(ctx.source.server.runDirectory, "analytics/bt_logs.json")
        logFile.parentFile.mkdirs()
        logFile.appendText(logEntry + ",\n")
    }
}