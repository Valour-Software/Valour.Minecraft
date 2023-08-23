package gg.valour.minecraft.commands;

import gg.valour.minecraft.ValourEconomy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.math.RoundingMode;

public class CommandBalance implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 0) {
            if (sender instanceof Player) {
                Player player = (Player) sender;

                /* Fancy async */
                ValourEconomy.Instance.GetAccountByUserId(player.getUniqueId().toString())
                        .thenApply(acc -> {
                            if (acc != null) {
                                var currency = ValourEconomy.Instance.ValourCurrency;
                                player.sendMessage("Your balance is " + currency.symbol + acc.balanceValue.setScale(currency.decimalPlaces, RoundingMode.HALF_UP));
                            } else {
                                player.sendMessage("Failed to find account.");
                            }

                            return acc;
                        });
            }
        } else {
            var uuid = "";
            var target = Bukkit.getPlayer(args[0]);
            if (target != null) {
                uuid = target.getUniqueId().toString();
            } else {
                var offlinePlayer = Bukkit.getOfflinePlayer(args[0]);
                if (offlinePlayer != null) {
                    uuid = offlinePlayer.getUniqueId().toString();
                }
            }

            if ("".equals(uuid)) {
                target.sendMessage("Target not found.");
                return true;
            }

            /* Fancy async */
            ValourEconomy.Instance.GetAccountByUserId(uuid)
                    .thenApply(acc -> {
                        if (acc != null) {
                            var currency = ValourEconomy.Instance.ValourCurrency;
                            sender.sendMessage(args[0] + "'s balance is " + currency.symbol + acc.balanceValue.setScale(currency.decimalPlaces, RoundingMode.HALF_UP));
                        } else {
                            sender.sendMessage("Failed to find account.");
                        }

                        return acc;
                    });
        }

        return true;
    }

}
