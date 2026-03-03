package me.sunmc.ad.gui;

import me.sunmc.ad.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public abstract class BaseGui implements InventoryHolder {

    protected static final ItemStack FILLER = buildItem(
            Material.GRAY_STAINED_GLASS_PANE, "&7", List.of());
    protected final Inventory inv;
    private final Map<Integer, Consumer<InventoryClickEvent>> actions = new HashMap<>();

    protected BaseGui(String title, int size) {
        this.inv = Bukkit.createInventory(this, size, ColorUtil.component(title));
    }

    protected static @NotNull ItemStack item(Material mat, String name, String... lore) {
        return buildItem(mat, name, List.of(lore));
    }

    private static @NotNull ItemStack buildItem(Material mat, String name, @NotNull List<String> lore) {
        var stack = new ItemStack(mat);
        var meta = stack.getItemMeta();
        meta.displayName(ColorUtil.component(name));
        if (!lore.isEmpty())
            meta.lore(lore.stream().map(ColorUtil::component).toList());
        stack.setItemMeta(meta);
        return stack;
    }

    protected final void set(int slot, ItemStack item, Consumer<InventoryClickEvent> action) {
        inv.setItem(slot, item);
        if (action != null) actions.put(slot, action);
        else actions.remove(slot);
    }

    protected final void set(int slot, ItemStack item) {
        set(slot, item, null);
    }

    protected final void fill(int from, int to) {
        for (int i = from; i <= to; i++) set(i, FILLER);
    }

    public final void onClick(@NotNull InventoryClickEvent e) {
        e.setCancelled(true);
        var a = actions.get(e.getSlot());
        if (a != null) a.accept(e);
    }

    public abstract void open(Player player);

    @Override
    public final @NotNull Inventory getInventory() {
        return inv;
    }
}
