package org.k0rv1nes.SkyMineFree;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.List;
import java.util.UUID;


public final class SkyMineFree extends JavaPlugin {


    private Connection connection;



    @Override
    public void onEnable() {
        getLogger().info("SkyMineFCree is enabled");
        saveDefaultConfig();
        setupDatabase();
        getCommand("free").setExecutor(this);
    }

    @Override
    public void onDisable() {
        getLogger().info("SkyMineFree is disabled");
        closeDatabase();
    }


    private void setupDatabase() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + getDataFolder() + "/data.db");
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS rewards (uuid TEXT PRIMARY KEY, ip TEXT);");
            }
        } catch (SQLException e) {
            getLogger().severe("Не удалось подключиться к базе данных: " + e.getMessage());
        }
    }

    private void closeDatabase(){
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            getLogger().severe("Не удалось закрыть соединение с базой данных: " + e.getMessage());
        }
    }


    private boolean hasReceivedReward(UUID uuid) {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT 1 FROM rewards WHERE uuid = ?;")){
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()){
                return rs.next();
            }
        } catch (SQLException e) {
            getLogger().severe("Ошибка при проверке награды: " + e.getMessage());
            return true;
        }
    }

    private boolean hasIpReceivedReward(String ip) {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT 1 FROM rewards WHERE ip = ?;")){
            stmt.setString(1, ip);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            getLogger().severe("Ошибка при проверке награды по IP: " + e.getMessage());
            return true;
        }
    }

    private void logReward(UUID uuid, String ip) {
        try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO rewards (uuid, ip) VALUES (?, ?);")){
            stmt.setString(1, uuid.toString());
            stmt.setString(2, ip);
            stmt.executeUpdate();
        } catch (SQLException e) {
            getLogger().severe("Ошибка при логировании награды: " + e.getMessage());
        }
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.only-player-execute")));
            return true;
        }

        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();
        String ip = player.getAddress().getAddress().getHostAddress();

        if(hasReceivedReward(uuid)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.already-received")));
            return true;
        }
        if(hasIpReceivedReward(ip)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.ip-already-received")));
            return true;
        }

        ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
        List<String> commands = getConfig().getStringList("reward-commands");
        for (String cmd : commands) {
            Bukkit.dispatchCommand(console, cmd.replace("%player%", player.getName()));
        }

        List<String> messages = getConfig().getStringList("messages.reward");
        for (String msg : messages) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
        }

        logReward(uuid, ip);
        return true;
    }

}
