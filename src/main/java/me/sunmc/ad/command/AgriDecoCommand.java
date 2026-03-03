package me.sunmc.ad.command;

import me.sunmc.ad.AgriDeco;
import me.sunmc.ad.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class AgriDecoCommand implements TabExecutor {

    private final AgriDeco plugin;

    public AgriDecoCommand(AgriDeco plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String @NotNull [] args) {
        if (args.length == 0) {
            plugin.getMessagesManager().send(sender, "invalid_command");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "reload" -> handleReload(sender);
            case "give" -> handleGive(sender, args);
            case "gui" -> handleGui(sender);
            default -> plugin.getMessagesManager().send(sender, "invalid_command");
        }
        return true;
    }

    private void handleGui(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can open the GUI.");
            return;
        }
        if (!player.hasPermission(plugin.getConfigManager().getReloadPermission())) {
            plugin.getMessagesManager().send(sender, "no_permission");
            return;
        }
        plugin.getFoliaScheduler().runAt(player.getLocation(),
                () -> plugin.getGuiManager().openMain(player));
    }

    private void handleReload(@NotNull CommandSender sender) {
        if (!sender.hasPermission(plugin.getConfigManager().getReloadPermission())) {
            plugin.getMessagesManager().send(sender, "no_permission");
            return;
        }
        try {
            plugin.reload();
            plugin.getMessagesManager().send(sender, "plugin_reload");
        } catch (Exception ex) {
            plugin.getSLF4JLogger().error("Reload error", ex);
            plugin.getMessagesManager().send(sender, "invalid_configuration");
        }
    }

    private void handleGive(@NotNull CommandSender sender, String[] args) {
        if (!sender.hasPermission(plugin.getConfigManager().getReloadPermission())) {
            plugin.getMessagesManager().send(sender, "no_permission");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /agrideco give <furniture|crop> <id> [player]");
            return;
        }

        String type = args[1].toLowerCase();
        String id = args[2];

        Player target;
        if (args.length >= 4) {
            target = Bukkit.getPlayer(args[3]);
            if (target == null) {
                plugin.getMessagesManager().send(sender, "player_not_found");
                return;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sender.sendMessage("§cConsole must specify a player name.");
            return;
        }

        Player finalTarget = target;

        switch (type) {
            case "furniture" -> {
                var def = plugin.getConfigManager().getFurnitureDefs().get(id);
                if (def == null) {
                    plugin.getMessagesManager().send(sender, "give_invalid_id", "{type}", "furniture", "{id}", id);
                    return;
                }
                var base = plugin.getIntegrations().getMmoItem(def.getId());
                if (base == null) {
                    plugin.getMessagesManager().send(sender, "give_item_error");
                    return;
                }
                var tagged = ItemUtil.tagFurniture(base, id);
                plugin.getFoliaScheduler().runAt(finalTarget.getLocation(),
                        () -> finalTarget.getInventory().addItem(tagged));
                plugin.getMessagesManager().send(sender, "give_success",
                        "{type}", "furniture", "{id}", id, "{player}", finalTarget.getName());
            }
            case "crop" -> {
                var def = plugin.getConfigManager().getCropDefs().get(id);
                if (def == null) {
                    plugin.getMessagesManager().send(sender, "give_invalid_id", "{type}", "crop", "{id}", id);
                    return;
                }
                var base = plugin.getIntegrations().getMmoItem(def.getSeedId());
                if (base == null) {
                    plugin.getMessagesManager().send(sender, "give_item_error");
                    return;
                }
                var tagged = ItemUtil.tagCrop(base, id);
                plugin.getFoliaScheduler().runAt(finalTarget.getLocation(),
                        () -> finalTarget.getInventory().addItem(tagged));
                plugin.getMessagesManager().send(sender, "give_success",
                        "{type}", "crop seed", "{id}", id, "{player}", finalTarget.getName());
            }
            default -> sender.sendMessage("§cType must be 'furniture' or 'crop'.");
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String alias, String @NotNull [] args) {
        if (!sender.hasPermission(plugin.getConfigManager().getReloadPermission()))
            return List.of();

        return switch (args.length) {
            case 1 -> filter(List.of("reload", "give", "gui"), args[0]);
            case 2 -> args[0].equalsIgnoreCase("give")
                    ? filter(List.of("furniture", "crop"), args[1])
                    : List.of();
            case 3 -> {
                if (!args[0].equalsIgnoreCase("give")) yield List.of();
                yield switch (args[1].toLowerCase()) {
                    case "furniture" ->
                            filter(new ArrayList<>(plugin.getConfigManager().getFurnitureDefs().keySet()), args[2]);
                    case "crop" -> filter(new ArrayList<>(plugin.getConfigManager().getCropDefs().keySet()), args[2]);
                    default -> List.of();
                };
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

    private List<String> filter(@NotNull List<String> opts, String prefix) {
        return opts.stream()
                .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }
}