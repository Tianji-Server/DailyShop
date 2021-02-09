package io.github.divios.dailyrandomshop.utils;

import io.github.divios.dailyrandomshop.builders.factory.dailyItem;
import io.github.divios.dailyrandomshop.conf_msg;
import io.github.divios.dailyrandomshop.database.dataManager;
import io.github.divios.dailyrandomshop.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class utils {

    private static final io.github.divios.dailyrandomshop.main main = io.github.divios.dailyrandomshop.main.getInstance();
    private static final dataManager dbManager = dataManager.getInstance();

    public static void translateAllItemData(ItemStack recipient, ItemStack receiver) {
        try {
            receiver.setData(recipient.getData());
            receiver.setType(recipient.getType());
            receiver.setItemMeta(recipient.getItemMeta());
            receiver.setAmount(recipient.getAmount());
            receiver.setDurability(recipient.getDurability());
        } catch (IllegalArgumentException ignored) {}
    }

    public static void translateAllItemData(ItemStack recipient,
                                            ItemStack receiver, boolean dailyMetadata) {
        try {
            receiver.setData(recipient.getData());
            receiver.setType(recipient.getType());
            receiver.setItemMeta(recipient.getItemMeta());
            receiver.setAmount(recipient.getAmount());
            receiver.setDurability(recipient.getDurability());
            if(dailyMetadata) dailyItem.transferDailyMetadata(recipient, receiver);
        } catch (IllegalArgumentException ignored) {}
    }

    public static void setDisplayName(ItemStack item, String name) {
        if (name == null) return;
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(formatString(name));
        item.setItemMeta(meta);
    }

    public static void setLore(ItemStack item, List<String> lore) {
        if (lore == null) return;
        ItemMeta meta = item.getItemMeta();
        List<String> coloredLore = meta.getLore();
        if (coloredLore == null) coloredLore = new ArrayList<>();
        for (String s : lore) {
            coloredLore.add(formatString(s));
        }
        meta.setLore(coloredLore);
        item.setItemMeta(meta);
    }

    public static void removeLore(ItemStack item, int n) {
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.getLore();
        if (lore.isEmpty() || lore == null) return;
        if (n == -1) lore.clear();
        else
            for (int i = 0; i < n; i++) {
                lore.remove(lore.size() - 1);
            }
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    public static String formatString(String str) {
        return ChatColor.translateAlternateColorCodes('&', str);
    }

    public static String trimString(String str) {
        return ChatColor.stripColor(str);
    }

    public static boolean isEmpty(ItemStack item) {
        return item == null || item.getType() == Material.AIR;
    }

    public static boolean isEmpty(String s) { return s == null || s.isEmpty(); }

    /**
     * Queue a task to be run asynchronously. <br>
     *
     * @param runnable task to run
     */
    public static BukkitTask async(Runnable runnable) {
        return Bukkit.getScheduler().runTaskAsynchronously(main, runnable);
    }

    /**
     * Queue a task to be run synchronously.
     *
     * @param runnable task to run on the next server tick
     */
    public static BukkitTask sync(Runnable runnable) {
        return Bukkit.getScheduler().runTask(main, runnable);
    }

    public static void runTaskLater(Runnable r, Long ticks) {
        Bukkit.getScheduler().runTaskLater(main, r, ticks);
    }

    public static void sendSound(Player p, Sound s) {
        try {
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5F, 1);
        } catch (NoSuchFieldError Ignored) {
        }
    }

    public static int randomValue(int minValue, int maxValue) {

        return minValue + (int) (Math.random() * ((maxValue - minValue) + 1));
    }

    public static ItemStack getEntry(Map<ItemStack, Double> list, int index) {
        int i = 0;
        for (ItemStack item : list.keySet()) {
            if (index == i) return item;
            i++;
        }
        return null;
    }

    public static ItemStack getRedPane() {
        ItemStack redPane = XMaterial.RED_STAINED_GLASS_PANE.parseItem();
        utils.setDisplayName(redPane, "&cOut of stock");
        return redPane;
    }

    public static void addFlag(ItemStack i, ItemFlag f) {
        ItemMeta meta = i.getItemMeta();
        meta.addItemFlags(f);
        i.setItemMeta(meta);
    }

    public static void removeFlag(ItemStack i, ItemFlag f) {
        ItemMeta meta = i.getItemMeta();
        meta.removeItemFlags(f);
        i.setItemMeta(meta);
    }

    public static boolean hasFlag(ItemStack item, ItemFlag f) {
        return item.getItemMeta().hasItemFlag(f);
    }

    public static List<String> replaceOnLore(List<String> lore, String pattern, String replace) {
        List<String> loreX = new ArrayList<>();
        loreX.addAll(lore);
        loreX.replaceAll(s -> s.replaceAll(pattern, replace));
        return loreX;
    }

    public static boolean isPotion(ItemStack item) {
        return item.getType().equals(XMaterial.POTION.parseMaterial()) ||
                item.getType().equals(XMaterial.SPLASH_POTION.parseMaterial());
    }

    public static int inventoryFull (Inventory inv) {

        int freeSlots = 0;
        for (int i = 0; i < 36; i++) {

            if (utils.isEmpty(inv.getItem(i))) {
                freeSlots++;
            }
        }
        return freeSlots;
    }

    public static void noPerms(Player p) {
        p.sendMessage(conf_msg.PREFIX + conf_msg.MSG_NOT_PERMS);
    }

    public static void changeItemPrice(ItemStack toSearch, Double price) {
        for(Map.Entry<ItemStack, Double> e : dbManager.listSellItems.entrySet()) {
            if(e.getKey().getType().equals(toSearch.getType())) {
                e.setValue(price);
                return;
            }
        }
    }

    public static void removeItem(ItemStack toSearch) {
        dbManager.listSellItems.entrySet().removeIf(e -> e.getKey().getType().
                equals(toSearch.getType()));
    }

    public static boolean hasItem(ItemStack toSearch) {
        for(Map.Entry<ItemStack, Double> e : dbManager.listSellItems.entrySet()) {
            if(e.getKey().getType().equals(toSearch.getType())) {
                return true;
            }
        }
        return false;
    }

    public static Double getPrice(ItemStack toSearch) {
        for(Map.Entry<ItemStack, Double> e : dbManager.listSellItems.entrySet()) {
            if(e.getKey().getType().equals(toSearch.getType())) {
                return e.getValue();
            }
        }
        return -1D;
    }

    public static Double getPriceModifier(Player p) {
        AtomicReference<Double> modifier = new AtomicReference<>(1.0);

        p.getEffectivePermissions().forEach(perms -> {
            String perm = perms.getPermission();
            if (perm.startsWith("dailyrandomshop.sellpricemodifier.")) {
                String[] splitStr = perm.split("dailyrandomshop.sellpricemodifier.");
                if(splitStr.length == 1) return;
                double newValue;
                try{
                    newValue = Math.abs(Double.parseDouble(splitStr[1]));
                } catch (NumberFormatException e) { return; }
                if (newValue > modifier.get())
                    modifier.set(newValue);
            }
        });
        return modifier.get();
    }

}