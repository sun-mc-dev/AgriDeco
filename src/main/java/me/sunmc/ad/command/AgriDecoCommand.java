package me.sunmc.ad.command;

import me.sunmc.ad.AgriDeco;
import me.sunmc.ad.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public final class AgriDecoCommand implements TabExecutor {

    private final AgriDeco plugin;

    public AgriDecoCommand(AgriDeco plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, String @NotNull [] args) {
        if (args.length == 0) {
            plugin.getMessagesManager().send(sender, "invalid_command");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "reload" -> handleReload(sender);
            case "give" -> handleGive(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "gui" -> handleGui(sender);
            default -> plugin.getMessagesManager().send(sender, "invalid_command");
        }
        return true;
    }

    private void handleReload(@NotNull CommandSender sender) {
        if (!hasAdmin(sender)) return;
        try {
            plugin.reload();
            plugin.getMessagesManager().send(sender, "plugin_reload");
        } catch (Exception ex) {
            plugin.getSLF4JLogger().error("Reload error", ex);
            plugin.getMessagesManager().send(sender, "invalid_configuration");
        }
    }

    private void handleGui(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can open the GUI.");
            return;
        }
        if (!hasAdmin(sender)) return;
        plugin.getFoliaScheduler().runAt(player.getLocation(),
                () -> plugin.getGuiManager().openMain(player));
    }

    private void handleGive(@NotNull CommandSender sender, String[] args) {
        if (!hasAdmin(sender)) return;
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /agrideco give <furniture|crop> <id> [player]");
            return;
        }

        String type = args[1].toLowerCase();
        String id = args[2];

        Player target = resolveTarget(sender, args, 3);
        if (target == null) return;

        switch (type) {
            case "furniture" -> {
                var def = plugin.getConfigManager().getFurnitureDefs().get(id);
                if (def == null) {
                    plugin.getMessagesManager().send(sender, "give_invalid_id",
                            "{type}", "furniture", "{id}", id);
                    return;
                }
                var base = plugin.getIntegrations().getMmoItem(def.getId());
                if (base == null) {
                    plugin.getMessagesManager().send(sender, "give_item_error");
                    return;
                }
                var tagged = ItemUtil.tagFurniture(base, id);
                plugin.getFoliaScheduler().runAt(target.getLocation(),
                        () -> target.getInventory().addItem(tagged));
                plugin.getMessagesManager().send(sender, "give_success",
                        "{type}", "furniture", "{id}", id, "{player}", target.getName());
            }
            case "crop" -> {
                var def = plugin.getConfigManager().getCropDefs().get(id);
                if (def == null) {
                    plugin.getMessagesManager().send(sender, "give_invalid_id",
                            "{type}", "crop", "{id}", id);
                    return;
                }
                var base = plugin.getIntegrations().getMmoItem(def.getSeedId());
                if (base == null) {
                    plugin.getMessagesManager().send(sender, "give_item_error");
                    return;
                }
                var tagged = ItemUtil.tagCrop(base, id);
                plugin.getFoliaScheduler().runAt(target.getLocation(),
                        () -> target.getInventory().addItem(tagged));
                plugin.getMessagesManager().send(sender, "give_success",
                        "{type}", "crop seed", "{id}", id, "{player}", target.getName());
            }
            default -> sender.sendMessage("§cType must be 'furniture' or 'crop'.");
        }
    }

    private void handleRemove(@NotNull CommandSender sender, String[] args) {
        if (!hasAdmin(sender)) return;
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /agrideco remove <furniture|crop> <uuid>");
            return;
        }

        String type = args[1].toLowerCase();
        UUID uuid;
        try {
            uuid = UUID.fromString(args[2]);
        } catch (IllegalArgumentException ex) {
            plugin.getMessagesManager().send(sender, "remove_invalid_uuid",
                    "{uuid}", args[2]);
            return;
        }

        // Console has no location, so we need a surrogate player or run on global scheduler.
        // For Folia safety the actual removal is dispatched to the region owning that object.
        switch (type) {
            case "furniture" -> {
                var fm = plugin.getFurnitureManager();
                var pf = fm != null ? fm.getByUuid(uuid) : null;
                if (pf == null) {
                    plugin.getMessagesManager().send(sender, "remove_not_found",
                            "{type}", "furniture", "{uuid}", uuid.toString());
                    return;
                }
                plugin.getFoliaScheduler().runAt(pf.getLocation(), () -> {
                    // Use a "console" player surrogate: pass null and let FurnitureManager
                    // handle it — or, if sender is a player, pass them directly.
                    boolean ok = fm.adminRemove(uuid);
                    if (ok) plugin.getMessagesManager().send(sender, "remove_success",
                            "{type}", "furniture", "{uuid}", uuid.toString());
                    else plugin.getMessagesManager().send(sender, "remove_not_found",
                            "{type}", "furniture", "{uuid}", uuid.toString());
                });
            }
            case "crop" -> {
                var cm = plugin.getCropManager();
                var crop = cm != null ? cm.getByUuid(uuid) : null;
                if (crop == null) {
                    plugin.getMessagesManager().send(sender, "remove_not_found",
                            "{type}", "crop", "{uuid}", uuid.toString());
                    return;
                }
                plugin.getFoliaScheduler().runAt(crop.getLocation(), () -> {
                    boolean ok = cm.adminRemove(uuid);
                    if (ok) plugin.getMessagesManager().send(sender, "remove_success",
                            "{type}", "crop", "{uuid}", uuid.toString());
                    else plugin.getMessagesManager().send(sender, "remove_not_found",
                            "{type}", "crop", "{uuid}", uuid.toString());
                });
            }
            default -> sender.sendMessage("§cType must be 'furniture' or 'crop'.");
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd,
                                      @NotNull String alias, String @NotNull [] args) {
        if (!sender.hasPermission(plugin.getConfigManager().getReloadPermission()))
            return List.of();

        return switch (args.length) {
            case 1 -> filter(List.of("reload", "give", "remove", "gui"), args[0]);
            case 2 -> switch (args[0].toLowerCase()) {
                case "give", "remove" -> filter(List.of("furniture", "crop"), args[1]);
                default -> List.of();
            };
            case 3 -> {
                if (args[0].equalsIgnoreCase("give")) yield switch (args[1].toLowerCase()) {
                    case "furniture" -> filter(new ArrayList<>(
                            plugin.getConfigManager().getFurnitureDefs().keySet()), args[2]);
                    case "crop" -> filter(new ArrayList<>(
                            plugin.getConfigManager().getCropDefs().keySet()), args[2]);
                    default -> List.of();
                };
                yield List.of();
            }
            case 4 -> args[0].equalsIgnoreCase("give")
                    ? plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[3].toLowerCase()))
                    .collect(Collectors.toList())
                    : List.of();
            default -> List.of();
        };
    }

    private boolean hasAdmin(@NotNull CommandSender sender) {
        if (sender.hasPermission(plugin.getConfigManager().getReloadPermission())) return true;
        plugin.getMessagesManager().send(sender, "no_permission");
        return false;
    }

    /**
     * Resolves the target player from args[argIndex], falling back to the sender
     * if they are a player, or complaining if neither is available.
     */
    private @Nullable Player resolveTarget(CommandSender sender, String @NotNull [] args, int argIndex) {
        if (args.length > argIndex) {
            var p = Bukkit.getPlayer(args[argIndex]);
            if (p == null) {
                plugin.getMessagesManager().send(sender, "player_not_found");
                return null;
            }
            return p;
        }
        if (sender instanceof Player p) return p;
        sender.sendMessage("§cConsole must specify a player name.");
        return null;
    }

    private List<String> filter(@NotNull List<String> opts, String prefix) {
        return opts.stream()
                .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }
}