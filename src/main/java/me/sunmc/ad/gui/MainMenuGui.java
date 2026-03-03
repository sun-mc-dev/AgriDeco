package me.sunmc.ad.gui;

import me.sunmc.ad.AgriDeco;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class MainMenuGui extends BaseGui {

    private final AgriDeco plugin;

    public MainMenuGui(AgriDeco plugin) {
        super("&8AgriDeco &7— Main Menu", 27);
        this.plugin = plugin;
        build();
    }

    private void build() {
        fill(0, 26);

        int fc = plugin.getConfigManager().getFurnitureDefs().size();
        int cc = plugin.getConfigManager().getCropDefs().size();

        set(11,
                item(Material.CHEST,
                        "&6&lFurniture Browser",
                        "&7" + fc + " definition(s) available.",
                        "",
                        "&eClick &7to browse and give items."),
                e -> plugin.getGuiManager().openFurniture((Player) e.getWhoClicked(), 0));

        set(13,
                item(Material.BOOK,
                        "&b&lAgriDeco v" + plugin.getPluginMeta().getVersion(),
                        "&7Furniture defs: &e" + fc,
                        "&7Crop defs: &e" + cc,
                        "",
                        "&7Storage: &e" + plugin.getDatabaseManager().getStorageType()));

        set(15,
                item(Material.WHEAT,
                        "&a&lCrop Browser",
                        "&7" + cc + " definition(s) available.",
                        "",
                        "&eClick &7to browse and give seeds."),
                e -> plugin.getGuiManager().openCrops((Player) e.getWhoClicked(), 0));

        set(26,
                item(Material.BARRIER, "&c&lClose"),
                e -> e.getWhoClicked().closeInventory());
    }

    @Override
    public void open(@NotNull Player player) {
        player.openInventory(inv);
    }
}
