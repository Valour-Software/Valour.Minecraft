package gg.valour.minecraft.listeners;

import gg.valour.minecraft.ValourLink;
import gg.valour.minecraft.models.PlanetMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.UUID;

public class ChatListener implements Listener {

    private final ValourLink _valourLink;

    public ChatListener(ValourLink valourLink) {
        _valourLink = valourLink;
    }

    @EventHandler
    public void OnMinecraftChat(AsyncPlayerChatEvent event) {
        var message = "(MC)[" + event.getPlayer().getPlayerListName() + "]: " + event.getMessage();

        try {
            var task = _valourLink.SendValourMessage(message);
            var result =  task.get();
            if (!result.Success) {
                _valourLink.LogToConsole("Error sending Valour message.");
                _valourLink.LogToConsole(result.Message);
            }
        } catch (Exception ex) {
            _valourLink.LogToConsole("Error sending Valour message.");
            _valourLink.LogToConsole(ex.getMessage());
        }
    }
}
