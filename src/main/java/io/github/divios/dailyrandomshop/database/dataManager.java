package io.github.divios.dailyrandomshop.database;

import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTContainer;
import de.tr7zw.changeme.nbtapi.NBTItem;
import io.github.divios.dailyrandomshop.builders.factory.dailyItem;
import io.github.divios.dailyrandomshop.conf_msg;
import io.github.divios.dailyrandomshop.guis.buyGui;
import io.github.divios.dailyrandomshop.builders.itemBuildersHooks.itemsBuilderManager;
import io.github.divios.dailyrandomshop.utils.utils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.sql.*;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class dataManager {

    private static final io.github.divios.dailyrandomshop.main main = io.github.divios.dailyrandomshop.main.getInstance();
    private static dataManager instance = null;
    public Map<ItemStack, Double> listDailyItems, listSellItems;
    public Map<String, Integer> currentItems;

    private dataManager() {
    }

    public static dataManager getInstance() {
        if (instance == null) init(new BukkitRunnable() {
            @Override
            public void run() {
                buyGui.getInstance();
            }
        });
        return instance;
    }

    private static void init(BukkitRunnable c) {
        instance = new dataManager();
        sqlite.getInstance();
        try {
            files.createdb();
        } catch (IOException e) {
            main.getLogger().severe("Couldn't load databases");
            e.printStackTrace();
            main.getServer().getPluginManager().disablePlugin(main);
        }
        utils.async(() -> {
            instance.createTables();
            instance.getSyncBuyItem();
            instance.getSyncSellItems();
            instance.getSyncCurrentItems();
            c.run();
        });
    }

    public void createTables() {
        try {
            Connection con = sqlite.getConnection();
            Statement statement = con.createStatement();
            statement.execute("CREATE TABLE IF NOT EXISTS timer"
                    + "(time int);");

            statement.execute("CREATE TABLE IF NOT EXISTS sell_items"
                    + "(serial varchar [255], price int);");

            statement.execute("CREATE TABLE IF NOT EXISTS daily_items"
                    + "(serial varchar [255], price int);");

            statement.execute("CREATE TABLE IF NOT EXISTS current_items"
                    + "(uuid varchar [255], amount int);");
        } catch (SQLException e) {
            e.printStackTrace();
            main.getLogger().severe("Couldn't load tables from database");
            main.getServer().getPluginManager().disablePlugin(main);
        }
    }

    public int getTimer() {
        int time = -1;
        try {
            Connection con = sqlite.getConnection();
            String selectTimer = "SELECT * FROM timer";
            PreparedStatement statement = con.prepareStatement(selectTimer);
            ResultSet result = statement.executeQuery();

            result.next();
            time = result.getInt("time");
        } catch (SQLException e) {
            main.getLogger().warning("Couldn't read timer value from database, setting it to value on config");
            time = conf_msg.TIMER;
        }
        return time;
    }

    private void updateAbstractTimer(int time) {
        Connection con = sqlite.getConnection();
        try {
            deleteElements("timer");

            PreparedStatement statement;
            String updateTimer = "INSERT INTO timer (time) VALUES (?)";
            statement = con.prepareStatement(updateTimer);
            statement.setInt(1, time);
            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateAsyncTimer(int time) {
        utils.async(() -> updateAbstractTimer(time));
    }

    public void updateSyncTimer(int time) {
        updateAbstractTimer(time);
    }

    public void updateAsyncSellItems() {
        utils.async(() -> AbstractUpdateList(listSellItems, "sell_items"));
    }

    public void updateSyncSellItems() {
        AbstractUpdateList(listSellItems, "sell_items");
    }

    public void getSyncSellItems() {
        listSellItems = AbstractGetList("sell_items", false);
    }

    public void getASyncSellItems() {
        utils.async(() -> listSellItems = AbstractGetList("sell_items", false));
    }

    public void updateAsyncBuyItems() {
        utils.async(() -> AbstractUpdateList(listDailyItems, "daily_items"));
    }

    public void updateSyncBuyItems() {
        AbstractUpdateList(listDailyItems, "daily_items");
    }

    public void getSyncBuyItem() {
        listDailyItems = AbstractGetList("daily_items", true);
    }

    public void getASyncBuyItem() {
        utils.async(() -> listDailyItems = AbstractGetList("daily_items", true));
    }

    public void updateAsyncCurrentItems() { utils.async(this::updateCurrentItems); }

    public void updateSyncCurrentItems() { updateCurrentItems(); }

    public void getAsyncCurrentItems() { utils.async(() -> currentItems = getCurrentItems()); }

    public void getSyncCurrentItems() { currentItems = getCurrentItems(); }

    private Map<ItemStack, Double> AbstractGetList(String table, boolean isDailyItems) {
        Map<ItemStack, Double> items = Collections.synchronizedMap(new LinkedHashMap<>());

        try {
            Connection con = sqlite.getConnection();
            String SQL_Create = "SELECT * FROM " + table;
            PreparedStatement statement = con.prepareStatement(SQL_Create);
            ResultSet result = statement.executeQuery();
            String string;
            NBTCompound itemData;
            ItemStack item;
            byte[] itemserial;

            while (result.next()) {
                itemserial = Base64.getDecoder().decode(result.getString("serial"));
                try {
                    string = new String(itemserial);
                    itemData = new NBTContainer(string);
                    item = NBTItem.convertNBTtoItem(itemData);
                    if (utils.isEmpty(item)) continue;
                    if (isDailyItems) {
                        if (utils.isEmpty(dailyItem.getUuid(item))) continue;
                    }

                    try {
                        Material.valueOf(item.getType().toString());
                    } catch (Exception e) {
                        continue;
                    }

                    //itemsBuilderManager.updateItem(item);

                } catch (Exception e) {
                    main.getLogger().warning("A previous sell item registered " +
                            "on the db is now unsupported, skipping...");
                    continue;
                }

                if (utils.isEmpty(dailyItem.getUuid(item))) {
                    new dailyItem(item).craft();
                }

                items.put(item, result.getDouble(2));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            main.getLogger().warning("Couldn't get sell items on database");
        }

        return items;
    }

    private void AbstractUpdateList(Map<ItemStack, Double> list, String table) {
        Connection con = sqlite.getConnection();
        PreparedStatement statement;
        try {
            //Quitamos los elementos previos
            deleteElements(table);

            for (Map.Entry<ItemStack, Double> entry : list.entrySet()) {

                String updateItem = "INSERT INTO " + table + " (serial, price) VALUES (?, ?)";
                statement = con.prepareStatement(updateItem);

                NBTCompound itemData = NBTItem.convertItemtoNBT(entry.getKey());
                String base64 = Base64.getEncoder().encodeToString(itemData.toString().getBytes());

                statement.setString(1, base64);
                statement.setDouble(2, entry.getValue());

                statement.executeUpdate();
            }

        } catch (SQLException Ignored) {
        }
    }

    public Map<String, Integer> getCurrentItems() {
        Map<String, Integer> items = new LinkedHashMap<>();
        try {
            Connection con = sqlite.getConnection();
            String SQL_Create = "SELECT * FROM current_items";
            PreparedStatement statement = con.prepareStatement(SQL_Create);
            ResultSet result = statement.executeQuery();

            while (result.next()) {
                items.put(result.getString(1),
                        result.getInt(2));
            }

        } catch (SQLException e) {
            main.getLogger().warning("Couldn't get current items from database");
        }
        return items;
    }

    public void updateCurrentItems() {
            currentItems = buyGui.getInstance().getCurrentItems();
            try {
                Connection con = sqlite.getConnection();
                PreparedStatement statement;

                deleteElements("current_items");

                for (Map.Entry<String, Integer> s : currentItems.entrySet()) {

                    String insertItem = "INSERT INTO " + "current_items (uuid, amount) VALUES (?, ?)";
                    statement = con.prepareStatement(insertItem);

                    statement.setString(1, s.getKey());
                    statement.setInt(2, s.getValue());
                    statement.executeUpdate();
                }

            } catch (SQLException e) {
                main.getLogger().warning("Couldn't update current items on database");
            }
    }

    public void deleteElements(String table) throws SQLException {
        Connection con = sqlite.getConnection();
        PreparedStatement statement;
        String deleteTable = "DELETE FROM " + table + ";";
        statement = con.prepareStatement(deleteTable);
        statement.executeUpdate();
    }


}