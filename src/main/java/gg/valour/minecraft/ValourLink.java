package gg.valour.minecraft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;
import gg.valour.minecraft.listeners.ChatListener;
import gg.valour.minecraft.models.*;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class ValourLink extends JavaPlugin {

    public static ValourLink Instance;
    public static ValourEconomy Economy;
    private HttpClient _http;
    private HubConnection _signalR;
    private FileConfiguration _config;
    private final Gson _gson = new GsonBuilder().create();
    private final String _baseUrl = "https://app.valour.gg/api/";
    private final String _hubUrl = "https://app.valour.gg/hubs/core";

    private String _primaryNode;

    private String[] _baseHeaders;

    // Authorization token for Valour, critical
    public AuthToken ValourAuth;
    public Currency ValourCurrency;
    public long PlanetId;
    public long ChannelId;
    public long MemberId;

    public HashMap<String, Long> UUIDToValourMap = new HashMap<String, Long>();

    public HashMap<String, User> UserIdMap = new HashMap<String, User>();

    private HashMap<String, String> _codeToUUID = new HashMap<String, String>();

    public void AddLinkCode(String code, String playerUUID) {
        _codeToUUID.put(code, playerUUID);
    }

    private User GetCachedUser(long userId) {
        return UserIdMap.getOrDefault(String.valueOf(userId), null);
    }

    private void SetCachedUser(User user){
        UserIdMap.put(String.valueOf(user.id), user);
    }

    public void LogToConsole(String message){
        getLogger().info(message);
    }

    @Override
    public void onLoad() {
        LogToConsole("ValourLink has been loaded.");
    }

    @Override
    public void onEnable() {
        Instance = this;

        LogToConsole("Setting up config.");
        SetupConfig();
        LogToConsole("Attempting to get token...");
        SetupHttp();

        LogToConsole("Connecting Economy...");
        SetupEconomy();

        LogToConsole("ValourLink has been enabled.");
        LogToConsole("Connecting to SignalR...");
        SetupSignalR();

        var connectResult = ConnectToChannel(ChannelId);
        if (!connectResult.Success) {
            LogToConsole("Failed to connect to channel " + ChannelId);
            LogToConsole("Chat sync will not work!");
            LogToConsole(connectResult.Message);
        } else {
            LogToConsole("Connected to channel " + ChannelId);
        }

        getServer().getPluginManager().registerEvents(new ChatListener(this), this);

        SetupCommands();
    }

    @Override
    public void onDisable() {
        LogToConsole("ValourLink has been disabled.");
    }

    private void SetupCommands() {
        this.getCommand("valourlink").setExecutor(new ValourLink());
    }

    private void SetupConfig(){
        _config = this.getConfig();
        _config.addDefault("valourEmail", "you@gmail.com");
        _config.addDefault("valourPassword", "p@ssword");
        _config.addDefault("channelId", 0);
        _config.addDefault("planetId", 0);
        _config.addDefault("memberId", 0);
        _config.options().copyDefaults(true);

        ChannelId = _config.getLong("channelId");
        PlanetId = _config.getLong("planetId");
        MemberId = _config.getLong("memberId");

        saveConfig();
    }

    private void SetupEconomy() {
        getServer().getServicesManager().register(Economy.class, Economy = new ValourEconomy(), this, ServicePriority.Highest);
        LogToConsole("Getting planet currency...");
        LoadCurrency();
    }

    private void LoadCurrency() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(_baseUrl + "eco/currencies/byPlanet/" + PlanetId))
                    .headers(_baseHeaders)
                    .header("content-type", "application/json")
                    .GET()
                    .build();

            var result = _http.send(request, HttpResponse.BodyHandlers.ofString());
            if (result.statusCode() != 200) {
                LogToConsole("Error getting planet currency!");
                LogToConsole("Economy features will be broken!");
                return;
            }

            ValourCurrency = _gson.fromJson(result.body(), Currency.class);
            LogToConsole("Loaded currency " + ValourCurrency.name + " successfully!");

        } catch (Exception ex) {
            LogToConsole("Error getting planet currency!");
            LogToConsole("Economy features will be broken!");
            LogToConsole(ex.getMessage());
        }
    }

    public Future<TaskResult> SendValourMessage(String content) {

        PlanetMessage message = new PlanetMessage();
        message.planetId = PlanetId;
        message.channelId = ChannelId;
        message.authorUserId = ValourAuth.userId;
        message.authorMemberId = MemberId;
        message.fingerprint = UUID.randomUUID().toString();
        message.content = content;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(_baseUrl + "chatchannels/" + message.channelId + "/messages"))
                    .headers(_baseHeaders)
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(_gson.toJson(message)))
                    .build();

            return _http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply((result) -> {
                        var res = new TaskResult();
                        res.Message = result.body();

                        if (result.statusCode() == 200) {
                            res.Success = true;
                        }

                        return res;
                    });

        } catch (Exception ex) {
            LogToConsole("Error sending message to Valour");
            LogToConsole(ex.getMessage());

            var result = new TaskResult();
            result.Message = "Error sending message to Valour";
            return CompletableFuture.supplyAsync(() ->  { return result; });
        }
    }

    public Future<User> GetUserAsync(long userId) {

        var cached = GetCachedUser(userId);
        if (cached != null) {
            return CompletableFuture.supplyAsync(() ->  { return cached; });
        }

        try {
            HttpRequest authRequest = HttpRequest.newBuilder()
                    .uri(new URI(_baseUrl + "users/" + userId))
                    .headers(_baseHeaders)
                    .GET()
                    .build();

            return _http.sendAsync(authRequest, HttpResponse.BodyHandlers.ofString())
                    .thenApply((result) -> {
                       var user = _gson.fromJson(result.body(), User.class);
                       SetCachedUser(user);
                       return user;
                    });

        } catch (Exception ex) {
            LogToConsole("Failed to fetch user " + userId);
            LogToConsole(ex.getMessage());
            return null;
        }
    }

    private void SetupHttp(){
        // Setup http client
        _http = HttpClient.newBuilder().build();

        var authPayload = new TokenRequest();
        authPayload.email = _config.getString("valourEmail");
        authPayload.password = _config.getString("valourPassword");

        try {
            // Initial auth request to get token
            HttpRequest authRequest = HttpRequest.newBuilder()
                    .uri(new URI(_baseUrl + "users/token"))
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(_gson.toJson(authPayload)))
                    .build();

            /*
            _http.sendAsync(authRequest, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        LogToConsole(response.body());
                        return response;
                    });
             */

            var response = _http.send(authRequest, HttpResponse.BodyHandlers.ofString());
            LogToConsole("Token response: " + response.statusCode());
            if (response.statusCode() != 200) {
                LogToConsole("Failed to negotiate Valour token. Check config.");
                return;
            }

            ValourAuth = _gson.fromJson(response.body(), AuthToken.class);
            LogToConsole("Loaded token for user " + ValourAuth.userId);

            // Get primary Valour node for future requests
            HttpRequest nodeRequest = HttpRequest.newBuilder()
                    .uri(new URI(_baseUrl + "node/name"))
                    .setHeader("authorization", ValourAuth.id)
                    .GET()
                    .build();

            var nodeResponse = _http.send(nodeRequest, HttpResponse.BodyHandlers.ofString());
            if (nodeResponse.statusCode() != 200) {
                LogToConsole("Failed to get primary node. Falling back on 'emma', but please note this may cause issues.");
                _primaryNode = "emma";
            }
            else {
                _primaryNode = nodeResponse.body();
                LogToConsole("Primary node found: " + _primaryNode);
            }

            // Setup base headers for future requests
            _baseHeaders = new String[]{
                "authorization", ValourAuth.id,
                "x-server-select", _primaryNode
            };

        } catch (Exception ex){
            LogToConsole("Error building auth request.");
            LogToConsole(ex.getMessage());
        }
    }

    private TaskResult ConnectToChannel(long channelId) {
        var result = _signalR.invoke(TaskResult.class, "JoinChannel", channelId);
        return result.blockingGet();
    }

    private void SetupSignalR() {
        _signalR = HubConnectionBuilder.create(_hubUrl)
                .withHeader("authorization", ValourAuth.id)
                .withHeader("x-server-select", _primaryNode)
                .build();

        var startError = _signalR.start().blockingGet();
        if (startError != null) {
            LogToConsole(startError.getMessage());
        }

        // Authenticate SignalR
        var authTask = _signalR.invoke(TaskResult.class, "Authorize", ValourAuth.id);
        var authResult = authTask.blockingGet();
        LogToConsole(authResult.Message);

        _signalR.on("Relay", this::OnValourMessage, PlanetMessage.class);

        _signalR.on("Channel-Watching-Update", (update) -> {
        }, ChannelWatchingUpdate.class);

        _signalR.on("Channel-CurrentlyTyping-Update", (update) -> {
        }, ChannelTypingUpdate.class);
    }

    private void OnValourMessage(PlanetMessage message) {
        try {
            if (message.authorUserId == ValourAuth.userId) {
                return;
            }

            if (message.content.startsWith("/mclink")) {
                var split = message.content.split(" ");
                if (split.length < 2) {
                    SendValourMessage("[ValourLink] Failed! Include your code!");
                    return;
                }

                var uuid = _codeToUUID.getOrDefault(split[1], "FAIL");
                if (uuid.equals("FAIL")) {
                    SendValourMessage("[ValourLink] Failed! Code not found!");
                    return;
                }

                UUIDToValourMap.put(uuid, message.authorUserId);

                GetUserAsync(message.authorUserId);

                SendValourMessage("[ValourLink] Linked to MC user " + uuid);
            }

            var user = GetUserAsync(message.authorUserId).get();

            var broadcast = "[" + ChatColor.AQUA + "Valour" + ChatColor.WHITE + "] " + user.name + ": " + message.content;

            LogToConsole("[Valour Chat] " + user.name + ": " + message.content);
            Bukkit.broadcastMessage(broadcast);

        } catch (Exception ex) {
            LogToConsole("Error fetching user " + message.authorUserId);
            LogToConsole(ex.getMessage());
        }
    }
}
