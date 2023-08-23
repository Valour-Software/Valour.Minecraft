package gg.valour.minecraft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;
import gg.valour.minecraft.commands.CommandBalance;
import gg.valour.minecraft.commands.CommandLink;
import gg.valour.minecraft.commands.CommandPay;
import gg.valour.minecraft.commands.CommandValourId;
import gg.valour.minecraft.listeners.ChatListener;
import gg.valour.minecraft.models.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
    public ValourEconomy Economy;
    public HttpClient Http;
    private HubConnection _signalR;
    private FileConfiguration _config;
    public final Gson Gson = new GsonBuilder().create();
    public final String BaseUrl = "https://app.valour.gg/api/";
    private final String _hubUrl = "https://app.valour.gg/hubs/core";

    private String _primaryNode;

    public String[] BaseHeaders;

    // Authorization token for Valour, critical
    public AuthToken ValourAuth;
    public long PlanetId;
    public long ChannelId;
    public long MemberId;
    public long HoldingAccountId;

    public HashMap<String, Double> LocalEcoAccounts = new HashMap<String, Double>();
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
        LoadValourMapping();
    }

    @Override
    public void onEnable() {
        Instance = this;

        LogToConsole("Setting up config.");
        SetupConfig();
        LogToConsole("Attempting to get token...");
        SetupHttp();

        LogToConsole("Linking Economy...");
        Economy = new ValourEconomy(this);

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
        this.getCommand("valourlink").setExecutor(new CommandLink());
        this.getCommand("valourid").setExecutor(new CommandValourId());
        this.getCommand("balance").setExecutor(new CommandBalance());
        this.getCommand("pay").setExecutor(new CommandPay());
    }

    private void SetupConfig(){
        _config = this.getConfig();
        _config.addDefault("valourEmail", "you@gmail.com");
        _config.addDefault("valourPassword", "p@ssword");
        _config.addDefault("channelId", 0);
        _config.addDefault("planetId", 0);
        _config.addDefault("memberId", 0);
        _config.addDefault("ecoHoldingId", 0);
        _config.options().copyDefaults(true);

        ChannelId = _config.getLong("channelId");
        PlanetId = _config.getLong("planetId");
        MemberId = _config.getLong("memberId");
        HoldingAccountId = _config.getLong("ecoHoldingId");

        saveConfig();
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
                    .uri(new URI(BaseUrl + "chatchannels/" + message.channelId + "/messages"))
                    .headers(BaseHeaders)
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(Gson.toJson(message)))
                    .build();

            return Http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
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
                    .uri(new URI(BaseUrl + "users/" + userId))
                    .headers(BaseHeaders)
                    .GET()
                    .build();

            return Http.sendAsync(authRequest, HttpResponse.BodyHandlers.ofString())
                    .thenApply((result) -> {
                       var user = Gson.fromJson(result.body(), User.class);
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
        Http = HttpClient.newBuilder().build();

        var authPayload = new TokenRequest();
        authPayload.email = _config.getString("valourEmail");
        authPayload.password = _config.getString("valourPassword");

        try {
            // Initial auth request to get token
            HttpRequest authRequest = HttpRequest.newBuilder()
                    .uri(new URI(BaseUrl + "users/token"))
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(Gson.toJson(authPayload)))
                    .build();

            /*
            _http.sendAsync(authRequest, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        LogToConsole(response.body());
                        return response;
                    });
             */

            var response = Http.send(authRequest, HttpResponse.BodyHandlers.ofString());
            LogToConsole("Token response: " + response.statusCode());
            if (response.statusCode() != 200) {
                LogToConsole("Failed to negotiate Valour token. Check config.");
                return;
            }

            ValourAuth = Gson.fromJson(response.body(), AuthToken.class);
            LogToConsole("Loaded token for user " + ValourAuth.userId);

            // Get primary Valour node for future requests
            HttpRequest nodeRequest = HttpRequest.newBuilder()
                    .uri(new URI(BaseUrl + "node/name"))
                    .setHeader("authorization", ValourAuth.id)
                    .GET()
                    .build();

            var nodeResponse = Http.send(nodeRequest, HttpResponse.BodyHandlers.ofString());
            if (nodeResponse.statusCode() != 200) {
                LogToConsole("Failed to get primary node. Falling back on 'emma', but please note this may cause issues.");
                _primaryNode = "emma";
            }
            else {
                _primaryNode = nodeResponse.body();
                LogToConsole("Primary node found: " + _primaryNode);
            }

            // Setup base headers for future requests
            BaseHeaders = new String[]{
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

        _signalR.onClosed((ex) -> {
            LogToConsole("Valour SignalR connection closed. Reconnecting...");
            var restartError = _signalR.start().blockingGet();
            if (restartError != null) {
                LogToConsole(restartError.getMessage());
            }
        });

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

                // Prevent hijacking
                _codeToUUID.remove(split[1]);

                UUIDToValourMap.put(uuid, message.authorUserId);

                GetUserAsync(message.authorUserId);

                SendValourMessage("[ValourLink] Linked to MC user " + uuid);

                SaveValourMapping();
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

    public void SaveValourMapping() {
        try {
            FileOutputStream output = new FileOutputStream("linkData.json");
            BukkitObjectOutputStream bOut = new BukkitObjectOutputStream(output);
            bOut.writeObject(UUIDToValourMap);
            bOut.close();
            LogToConsole("Saved updated Valour link data.");
        } catch (Exception ex) {
            LogToConsole("Critical error saving Valour link data!");
            LogToConsole(ex.getMessage());
        }
    }

    public void LoadValourMapping() {
        try {
            if (!new File("linkData.json").exists()) {
                LogToConsole("No Valour link data found. Skipping load...");
                return;
            }

            FileInputStream input = new FileInputStream("linkData.json");
            BukkitObjectInputStream bIn = new BukkitObjectInputStream(input);
            UUIDToValourMap = (HashMap<String, Long>) bIn.readObject();

            LogToConsole("Loaded " + UUIDToValourMap.size() + " linked Valour ids.");
        } catch (Exception ex) {
            LogToConsole("Critical error loading Valour link data!");
            LogToConsole(ex.getMessage());
        }
    }

    public void SaveLocalAccounts() {
        try {
            FileOutputStream output = new FileOutputStream("localAccountData.json");
            BukkitObjectOutputStream bOut = new BukkitObjectOutputStream(output);
            bOut.writeObject(LocalEcoAccounts);
            bOut.close();
            LogToConsole("Saved updated Valour local eco account data.");
        } catch (Exception ex) {
            LogToConsole("Critical error saving Valour local eco account data!");
            LogToConsole(ex.getMessage());
        }
    }

    public void LoadLocalAccounts() {
        try {
            if (!new File("localAccountData.json").exists()) {
                LogToConsole("No Valour local eco account data found. Skipping load...");
                return;
            }

            FileInputStream input = new FileInputStream("localAccountData.json");
            BukkitObjectInputStream bIn = new BukkitObjectInputStream(input);
            LocalEcoAccounts = (HashMap<String, Double>) bIn.readObject();

            LogToConsole("Loaded " + LocalEcoAccounts.size() + " local eco accounts.");
        } catch (Exception ex) {
            LogToConsole("Critical error loading Valour local eco data!");
            LogToConsole(ex.getMessage());
        }
    }
}
