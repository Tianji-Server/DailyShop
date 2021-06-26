package io.github.divios.dailyShop.guis.settings;

import com.cryptomorin.xseries.XMaterial;
import io.github.divios.core_lib.inventory.InventoryGUI;
import io.github.divios.core_lib.inventory.ItemButton;
import io.github.divios.core_lib.inventory.dynamicGui;
import io.github.divios.core_lib.itemutils.ItemBuilder;
import io.github.divios.core_lib.itemutils.ItemUtils;
import io.github.divios.core_lib.misc.FormatUtils;
import io.github.divios.core_lib.misc.Task;
import io.github.divios.dailyShop.DRShop;
import io.github.divios.dailyShop.conf_msg;
import io.github.divios.dailyShop.guis.confirmIH;
import io.github.divios.dailyShop.guis.customizerguis.customizeGui;
import io.github.divios.dailyShop.guis.customizerguis.customizerMainGuiIH;
import io.github.divios.dailyShop.lorestategy.loreStrategy;
import io.github.divios.dailyShop.lorestategy.shopsManagerLore;
import io.github.divios.dailyShop.utils.utils;
import io.github.divios.lib.itemHolder.dItem;
import io.github.divios.lib.itemHolder.dShop;
import io.github.divios.lib.managers.shopsManager;
import io.github.divios.lib.storage.dataManager;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class shopsManagerGui {

    private static final DRShop plugin = DRShop.getInstance();
    private static final shopsManager sManager = shopsManager.getInstance();
    private static final dataManager dManager = dataManager.getInstance();

    private static final loreStrategy strategy = new shopsManagerLore();

    private final List<InventoryGUI> invs = new ArrayList<>();
    private final List<ItemStack> items;

    private final Player p;

    private shopsManagerGui(Player p) {
        this.p = p;

        items = shopsManager.getInstance().getShops().stream()
                .map(dShop -> new ItemBuilder(XMaterial.PLAYER_HEAD)
                                .setName("&f&l" + dShop.getName())
                                .applyTexture("7e3deb57eaa2f4d403ad57283ce8b41805ee5b6de912ee2b4ea736a9d1f465a7"))
                .peek(strategy::setLore)
                .collect(Collectors.toList());

        createInvs();
        invs.get(0).open(p);
    }

    public static void open(Player p) {
        new shopsManagerGui(p);
    }

    private void createInvs() {
        IntStream.range(0, items.isEmpty() ? 0 : (int) Math.ceil(items.size() / 32D))
                .forEach(value -> invs.add(new InventoryGUI(plugin, 54, conf_msg.SHOPS_MANAGER_TITLE)));

        final int[] sum = {0};

        invs.forEach(inventoryGUI -> {
            inventoryGUI.setDestroyOnClose(false);

            IntStream.of(0, 1, 9, 7, 8, 17, 45, 46, 36, 52, 53, 44).forEach(value ->
                    inventoryGUI.addButton(new ItemButton(
                            new ItemBuilder(XMaterial.BLUE_STAINED_GLASS_PANE)
                                    .setName("&c"), e -> {
                    }), value));

            IntStream.of(2, 6, 47, 51).forEach(value ->
                    inventoryGUI.addButton(new ItemButton(
                            new ItemBuilder(XMaterial.LIGHT_BLUE_STAINED_GLASS_PANE)
                                    .setName("&c"), e -> {
                    }), value));

            IntStream.of(3, 4, 5, 48, 49, 50).forEach(value ->
                    inventoryGUI.addButton(new ItemButton(
                            new ItemBuilder(XMaterial.WHITE_STAINED_GLASS_PANE)
                                    .setName("&c"), e -> {
                    }), value));

            int index = invs.indexOf(inventoryGUI);
            if (index != invs.size() - 1) {      // next buttom
                inventoryGUI.addButton(new ItemButton(
                        new ItemBuilder(XMaterial.PLAYER_HEAD)
                                .setName("&1&lNext").applyTexture("19bf3292e126a105b54eba713aa1b152d541a1d8938829c56364d178ed22bf"),
                        e -> invs.get(index + 1).open(p)), 51);
            }

            if (index != 0) {                   // previous buttom
                inventoryGUI.addButton(new ItemButton(
                        new ItemBuilder(XMaterial.PLAYER_HEAD)
                                .setName("&1&lPrevious").applyTexture("bd69e06e5dadfd84e5f3d1c21063f2553b2fa945ee1d4d7152fdc5425bc12a9"),
                        e -> invs.get(index - 1).open(p)), 47);
            }

            inventoryGUI.addButton(new ItemButton(new ItemBuilder(XMaterial.OAK_DOOR)
                    .setName("&cReturn").setLore("&7Click to return"), e -> {
                Task.syncDelayed(plugin, this::destroyAll, 3L);
                p.closeInventory();
            }), 8);

            inventoryGUI.addButton(new ItemButton(new ItemBuilder(XMaterial.ANVIL)
                    .setName(conf_msg.SHOPS_MANAGER_CREATE), e -> nonContentAction()), 53);

            for (int i = 0; i < 54; i++) {
                if (sum[0] >= items.size()) break;
                if (!ItemUtils.isEmpty(inventoryGUI.getInventory().getItem(i))) continue;

                inventoryGUI.addButton(new ItemButton(new ItemBuilder(items.get(sum[0])),
                        this::contentAction), i);

                sum[0]++;
            }
        });
    }

    private void destroyAll() {
        invs.forEach(InventoryGUI::destroy);
    }

    private void contentAction(InventoryClickEvent e) {
        ItemStack selected = e.getCurrentItem();
        dShop shop = sManager.getShop(FormatUtils.stripColor(utils.getDisplayName(selected))).get();
        Player p = (Player) e.getWhoClicked();

        if (e.isShiftClick() && e.isLeftClick())
            customizeGui.open(p, shop);

        else if (e.getClick().equals(ClickType.MIDDLE)) {
            new AnvilGUI.Builder()
                    .onClose(player -> Task.syncDelayed(plugin, () -> open(p), 1L))
                    .onComplete((player, s) -> {
                        if (s.isEmpty())
                            return AnvilGUI.Response.text("Cat be empty");

                        dManager.renameShop(shop.getName(), s);
                        shop.setName(s);
                        return AnvilGUI.Response.close();
                    })
                    .title(conf_msg.SHOPS_MANAGER_RENAME)
                    .text(FormatUtils.stripColor(conf_msg.SHOPS_MANAGER_RENAME))
                    .plugin(plugin)
                    .open(p);
        }

        else if (e.getClick().equals(ClickType.DROP)) {
            new AnvilGUI.Builder()
                    .onClose(player -> Task.syncDelayed(plugin, () -> open(p), 1L))
                    .onComplete((player, s) -> {
                        int time;
                        try {
                            time = Integer.parseInt(s);
                        } catch (Exception err) {return  AnvilGUI.Response.text(conf_msg.MSG_NOT_INTEGER);}

                        if (time < 50) return AnvilGUI.Response.text("Time cannot be less than 50");
                        shop.setTimer(time);
                        return AnvilGUI.Response.close();
                    })
                    .itemLeft(XMaterial.CLOCK.parseItem())
                    .plugin(plugin)
                    .open(p);
        }

        else if (e.isRightClick()) {
            new confirmIH(p, (player, aBoolean) -> {
                if (aBoolean)
                    shopsManager.getInstance().deleteShop(shop.getName());
                open(player);
            }, selected,
                    conf_msg.CONFIRM_GUI_ACTION_NAME,
                    conf_msg.CONFIRM_MENU_YES, conf_msg.CONFIRM_MENU_NO);
            return;
        }

        else
            shopGui.open(p, shop.getName());
        return;
    }

    private void nonContentAction() {

        new AnvilGUI.Builder()
                .onComplete((player, s) -> {

                    if (s.isEmpty())
                        return AnvilGUI.Response.text("Cat be empty");

                    if (sManager.getShop(s).isPresent())
                        return AnvilGUI.Response.text("Already exits");

                    shopsManager.getInstance().createShop(s, dShop.dShopT.buy);
                    Task.syncDelayed(plugin, this::destroyAll, 3L);
                    Task.syncDelayed(plugin, () -> open(p), 1L);
                    return AnvilGUI.Response.close();
                })
                .onClose(player -> Task.syncDelayed(plugin, () -> open(p), 1L))
                .title(conf_msg.SHOPS_MANAGER_NEWSHOP)
                .text(FormatUtils.stripColor(conf_msg.SHOPS_MANAGER_NEWSHOP))
                .plugin(plugin)
                .open(p);

    }


}