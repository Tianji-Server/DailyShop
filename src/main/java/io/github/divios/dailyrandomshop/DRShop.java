package io.github.divios.dailyrandomshop;

import io.github.divios.core_lib.commands.CommandManager;
import io.github.divios.dailyrandomshop.commands.*;
import io.github.divios.dailyrandomshop.hooks.hooksManager;
import io.github.divios.dailyrandomshop.utils.conf_updater;
import io.github.divios.lib.managers.shopsManager;
import io.github.divios.lib.storage.dataManager;
import org.bukkit.plugin.java.JavaPlugin;

public class DRShop extends JavaPlugin {

    private static DRShop INSTANCE;

    @Override
    public void onEnable() {
        INSTANCE = this;
        INSTANCE.saveDefaultConfig();
                                /* Init hooks  */
        hooksManager.getInstance();
                                /* Init conf & msgs*/
        conf_msg.init();
                                /* Initiate database + getAllItems + timer */
        conf_updater.check();
        dataManager.getInstance();
        shopsManager.getInstance();

        CommandManager.register(INSTANCE.getCommand("dailyRandomShop"));
        CommandManager.addCommand(new add(), new helpCmd(),
                new open(), new Manager(), new reStock(), new importShops());

    }

    @Override
    public void onDisable() {
        shopsManager.getInstance().getShops()       // Updates all the guis before disable
                .forEach(shop -> dataManager.getInstance()
                        .syncUpdateGui(shop.getName(), shop.getGui()));
    }

    public void realoadPlugin() {
        conf_msg.reload();
                                /* Reload guis */
        /*buyGui.getInstance().reload();
        settingsGuiIH.reload();
        addDailyItemGuiIH.reload(); */
    }

    public static DRShop getInstance() {
        return INSTANCE;
    }

}
