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

class GuesserPlugins : ModInitializer {
    // GameManager仮定のシングルトンインスタンス（架空のもの）
    object GameManager {
        var meeting: Boolean = false // meetingの状態
    }

    override fun onInitialize() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            val btCommand: LiteralArgumentBuilder<ServerCommandSource> = literal("bt")
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
    
        // 実行者が許可されたチームのいずれかに所属しているか確認
        val executor = ctx.source.player
        val allowedTeamNames = listOf("Niceguesser", "Evilguesser")  // 許可されたチーム名のリスト
    
        if (executor == null || !allowedTeamNames.contains(scoreboard.getPlayerTeam(executor.entityName)?.name)) {
            // 実行者が許可されたチームに所属していない場合、エラーメッセージを返す
            ctx.source.sendFeedback(Text.literal("このコマンドは ${allowedTeamNames.joinToString(", ")} チームのメンバーにしか実行できません。"), false)
            return 0
        }
    
        // 指定されたユーザーとチーム名の確認
        val player: ServerPlayerEntity? = server.playerManager.getPlayer(username)
        val team: Team? = scoreboard.getTeam(teamName)
    
        if (player != null && team != null && team.playerList.contains(player.entityName)) {
            // 正常条件：対象プレイヤーがそのチームに所属している → kill する
            server.commandManager.dispatcher.execute("kill $username", server.commandSource)
            ctx.source.sendFeedback(Text.literal("プレイヤー $username をキルしました"), false)
            return 1
        } else {
            // 条件外：実行者自身をkill（またはコンソールならメッセージ）
            return if (executor != null) {
                executor.kill()
                ctx.source.sendFeedback(Text.literal("条件不一致のため自分をキルしました"), false)
                1
            } else {
                ctx.source.sendFeedback(Text.literal("対象プレイヤーが見つからない、またはチームに所属していません"), false)
                0
            }
        }
    }
}