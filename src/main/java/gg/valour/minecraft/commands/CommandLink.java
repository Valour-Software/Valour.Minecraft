package gg.valour.minecraft.commands;

import gg.valour.minecraft.ValourLink;
import org.apache.commons.lang.math.RandomUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Random;
import java.util.UUID;

public class CommandLink implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            var rand = RandomUtils.nextInt(1000);
            var code = sender.getName() + "-" + rand;

            ValourLink.Instance.AddLinkCode(code, player.getUniqueId().toString());

            player.sendMessage("Please send the command '/mclink " + code + "' in the linked Valour channel");
        }

        return true;
    }

}
