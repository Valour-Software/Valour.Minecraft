package gg.valour.minecraft.commands;

import gg.valour.minecraft.ValourLink;
import org.apache.commons.lang.math.RandomUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Random;
import java.util.UUID;

public class CommandValourId implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            var valourId = ValourLink.Instance.UUIDToValourMap.getOrDefault(player.getUniqueId().toString(), 0L);
            if (valourId == 0L) {
                player.sendMessage("Your Valour account is not linked.");
                return true;
            }

            player.sendMessage("Your Valour id is " + valourId);
        }

        return true;
    }

}
