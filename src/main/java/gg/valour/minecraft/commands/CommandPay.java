package gg.valour.minecraft.commands;

import gg.valour.minecraft.ValourEconomy;
import gg.valour.minecraft.ValourLink;
import org.apache.commons.lang.math.RandomUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.math.RoundingMode;
import java.util.Random;
import java.util.UUID;

public class CommandPay implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof  Player)) {
            sender.sendMessage("Only a player can send currency!");
            return false;
        }

        if (args.length < 2) {
            sender.sendMessage("Usage: /pay <player> <amount>");
            return false;
        }

        Player player = (Player) sender;

        var from = player.getUniqueId().toString();

        var to = "";
        var target = Bukkit.getPlayer(args[0]);
        if (target != null) {
            to = target.getUniqueId().toString();
        } else {
            var offlinePlayer = Bukkit.getOfflinePlayer(args[0]);
            if (offlinePlayer != null) {
                to = offlinePlayer.getUniqueId().toString();
            }
        }

        if ("".equals(to)) {
            player.sendMessage("Target not found.");
            return true;
        }

        try {
            var validAmount = Double.parseDouble(args[1]);
            ValourEconomy.Instance.DoTransaction(from, to, validAmount)
                    .thenApply(result -> {

                        if (result.Success) {
                            player.sendMessage("Sent " + ValourEconomy.Instance.ValourCurrency.symbol + validAmount);
                            if (target != null) {
                                target.sendMessage(player.getDisplayName() + " sent you " + ValourEconomy.Instance.ValourCurrency.symbol + validAmount);
                            }
                        } else {
                            player.sendMessage("Error sending transaction: " + result.Message);
                        }

                        return result;
                    });

        } catch (Exception ex) {
            player.sendMessage("Unable to parse amount.");
        }

        return true;
    }

}
