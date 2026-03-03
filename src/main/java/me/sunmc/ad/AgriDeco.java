package me.sunmc.ad;

import me.sunmc.ad.command.AgriDecoCommand;
import me.sunmc.ad.config.ConfigManager;
import me.sunmc.ad.config.MessagesManager;
import me.sunmc.ad.database.DataRepository;
import me.sunmc.ad.database.DatabaseManager;
import me.sunmc.ad.gui.GuiManager;
import me.sunmc.ad.integration.IntegrationManager;
import me.sunmc.ad.listener.*;
import me.sunmc.ad.manager.CropManager;
import me.sunmc.ad.manager.FurnitureManager;
import me.sunmc.ad.packet.PacketHandler;
import me.sunmc.ad.util.scheduler.FoliaScheduler;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class AgriDeco extends JavaPlugin {

    private static AgriDeco instance;

    private FoliaScheduler foliaScheduler;
    private ConfigManager configManager;
    private MessagesManager messagesManager;
    private DatabaseManager databaseManager;
    private DataRepository dataRepository;
    private PacketHandler packetHandler;
    private IntegrationManager integrationManager;
    private FurnitureManager furnitureManager;
    private CropManager cropManager;
    private GuiManager guiManager;

    public static AgriDeco getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        instance = this;
        Keys.init(this);
    }

    @Override
    public void onEnable() {
        foliaScheduler = new FoliaScheduler(this);
        configManager = new ConfigManager(this);
        messagesManager = new MessagesManager(this);
        packetHandler = new PacketHandler(this);
        integrationManager = new IntegrationManager(this);
        furnitureManager = new FurnitureManager(this);
        cropManager = new CropManager(this);
        guiManager = new GuiManager(this);

        var pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerInteractListener(this), this);
        pm.registerEvents(new BlockBreakListener(this), this);
        pm.registerEvents(new ChunkLoadListener(this), this);
        pm.registerEvents(new PlayerQuitListener(this), this);
        pm.registerEvents(new InventoryClickListener(this), this);

        var cmd = Objects.requireNonNull(getCommand("agrideco"));
        var executor = new AgriDecoCommand(this);
        cmd.setExecutor(executor);
        cmd.setTabCompleter(executor);

        databaseManager = new DatabaseManager(this);
        databaseManager.initAsync().whenComplete((v, ex) -> {
            if (ex != null) {
                getSLF4JLogger().error("Database init failed — disabling plugin.", ex);
                foliaScheduler.runGlobal(() -> getServer().getPluginManager().disablePlugin(this));
                return;
            }
            dataRepository = new DataRepository(this);
            getSLF4JLogger().info("Database connected. Loading world data…");
            foliaScheduler.runGlobal(() -> {
                furnitureManager.loadFromDatabase();
                cropManager.loadFromDatabase();
            });
        });

        getSLF4JLogger().info("AgriDeco v{} started on Folia!", getPluginMeta().getVersion());
    }

    @Override
    public void onDisable() {
        if (cropManager != null) cropManager.shutdown();
        if (furnitureManager != null) furnitureManager.shutdown();
        if (databaseManager != null) databaseManager.shutdown();
        if (packetHandler != null) packetHandler.shutdown();
        getSLF4JLogger().info("AgriDeco disabled.");
    }

    public void reload() {
        reloadConfig();
        configManager.reload();
        messagesManager.reload();
        if (furnitureManager != null) furnitureManager.reload();
        if (cropManager != null) cropManager.reload();
    }

    public FoliaScheduler getFoliaScheduler() {
        return foliaScheduler;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessagesManager getMessagesManager() {
        return messagesManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public DataRepository getDataRepository() {
        return dataRepository;
    }

    public PacketHandler getPacketHandler() {
        return packetHandler;
    }

    public IntegrationManager getIntegrations() {
        return integrationManager;
    }

    public FurnitureManager getFurnitureManager() {
        return furnitureManager;
    }

    public CropManager getCropManager() {
        return cropManager;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }
}