package io.github.divios.dailyShop.commands;

import io.github.divios.core_lib.commands.abstractCommand;
import io.github.divios.core_lib.commands.cmdTypes;
import io.github.divios.core_lib.misc.FormatUtils;
import io.github.divios.dailyShop.guis.settings.shopsManagerGui;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class manager extends abstractCommand {

    public manager() {
        super(cmdTypes.PLAYERS);
    }

    @Override
    public String getName() {
        return "manager";
    }

    @Override
    public boolean validArgs(List<String> args) {
        if (args.size() == 0) return true;

        return Bukkit.getPlayer(args.get(0)) != null;
    }

    @Override
    public String getHelp() {
        return FormatUtils.color("&8- &6/rdshop manager [player]&8 " +
                "- &7Opens the shops manager gui");
    }

    @Override
    public List<String> getPerms() {
        return Collections.singletonList("DailyRandomShop.settings");
    }

    @Override
    public List<String> getTabCompletition(List<String> args) {
        if (args.size() == 1)
            return Bukkit.getOnlinePlayers()
                    .stream()
                    .map(Player::getName)
                    .collect(Collectors.toList());
        return null;
    }


    @Override
    public void run(CommandSender sender, List<String> args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("You cannot open a shop to yourself from console");
            return;
        }

        shopsManagerGui.open(args.size() > 0 ?
                Bukkit.getPlayer(args.get(0)) : (Player) sender);

    }
}