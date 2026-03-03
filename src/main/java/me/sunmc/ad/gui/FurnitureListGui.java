package me.sunmc.ad.gui;

import me.sunmc.ad.AgriDeco;
import me.sunmc.ad.util.ColorUtil;
import me.sunmc.ad.util.ItemUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class FurnitureListGui extends BaseGui {

    private static final int PAGE_SIZE = 45;
    private static final int ROW5 = 45;

    private final AgriDeco plugin;
    private final int page;
    private final List<String> ids;

    public FurnitureListGui(@NotNull AgriDeco plugin, int page) {
        super("&8Furniture &7— Page " + (page + 1), 54);
        this.plugin = plugin;
        this.page = page;
        this.ids = List.copyOf(plugin.getConfigManager().getFurnitureDefs().keySet());
        build();
    }

    private void build() {
        fill(ROW5, 53);

        int start = page * PAGE_SIZE;
        int totalPages = Math.max(1, (int) Math.ceil((double) ids.size() / PAGE_SIZE));

        for (int i = 0; i < PAGE_SIZE; i++) {
            int idx = start + i;
            if (idx >= ids.size()) break;
            String id = ids.get(idx);
            var def = plugin.getConfigManager().getFurnitureDefs().get(id);
            if (def == null) continue;

            var raw = plugin.getIntegrations().getMmoItem(def.getId());
            var display = raw != null ? raw.clone() : item(Material.CHEST, "&e" + id);

            var meta = display.getItemMeta();
            List<Component> lore = new ArrayList<>(meta.lore() == null ? List.of() : Objects.requireNonNull(meta.lore()));
            lore.add(Component.empty());
            lore.add(ColorUtil.component("&7Config ID:  &e" + id));
            lore.add(ColorUtil.component("&7Type:       &b" + def.getFurnitureType()));
            lore.add(ColorUtil.component("&7Placement:  &b" + def.getPlacementType()));
            lore.add(Component.empty());
            lore.add(ColorUtil.component("&eClick &7to receive this furniture item."));
            meta.lore(lore);
            display.setItemMeta(meta);

            set(i, display, e -> giveItem(e, id, def.getId()));
        }

        if (page > 0)
            set(ROW5, item(Material.ARROW, "&aPrevious Page", "&7Page " + page),
                    e -> plugin.getGuiManager().openFurniture((Player) e.getWhoClicked(), page - 1));

        set(ROW5 + 3,
                item(Material.DARK_OAK_DOOR, "&7← Back to Main Menu"),
                e -> plugin.getGuiManager().openMain((Player) e.getWhoClicked()));

        set(ROW5 + 4,
                item(Material.PAPER,
                        "&bPage &e" + (page + 1) + " &bof &e" + totalPages,
                        "&7Showing " + Math.min(PAGE_SIZE, ids.size() - page * PAGE_SIZE) + " of " + ids.size()));

        set(ROW5 + 5,
                item(Material.BARRIER, "&cClose"),
                e -> e.getWhoClicked().closeInventory());

        if (page < totalPages - 1)
            set(53, item(Material.ARROW, "&aNext Page", "&7Page " + (page + 2)),
                    e -> plugin.getGuiManager().openFurniture((Player) e.getWhoClicked(), page + 1));
    }

    private void giveItem(org.bukkit.event.inventory.@NotNull InventoryClickEvent e,
                          String configId, String mmoId) {
        var player = (Player) e.getWhoClicked();
        player.closeInventory();
        var base = plugin.getIntegrations().getMmoItem(mmoId);
        if (base == null) {
            plugin.getMessagesManager().send(player, "give_item_error");
            return;
        }
        var tagged = ItemUtil.tagFurniture(base, configId);
        plugin.getFoliaScheduler().runAt(player.getLocation(),
                () -> player.getInventory().addItem(tagged));
        plugin.getMessagesManager().send(player, "give_success",
                "{type}", "furniture", "{id}", configId, "{player}", player.getName());
    }

    @Override
    public void open(@NotNull Player player) {
        player.openInventory(inv);
    }
}
