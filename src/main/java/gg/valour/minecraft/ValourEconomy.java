package gg.valour.minecraft;

import gg.valour.minecraft.models.Currency;
import gg.valour.minecraft.models.EcoAccount;
import gg.valour.minecraft.models.TaskResult;
import gg.valour.minecraft.models.Transaction;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.ServicePriority;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ValourEconomy {
    public static ValourEconomy Instance;
    private  ValourLink Link;

    public Currency ValourCurrency;

    // BALANCE DOES NOT UPDATE OR REFLECT REALITY!
    public EcoAccount HoldingsAccount;

    public ValourEconomy(ValourLink link) {
        Instance = this;
        Link = link;

        SetupEconomy();
    }

    private void SetupEconomy() {
        Link.getServer().getServicesManager().register(Economy.class, new ValourVaultHook(this), Link, ServicePriority.Highest);
        Link.LogToConsole("Getting planet currency...");
        LoadCurrency();

        try {
            HoldingsAccount = GetAccountById(Link.HoldingAccountId).get();
            Link.LogToConsole("Loaded holdings account " + HoldingsAccount.name + " (" + HoldingsAccount.id + ")");
        } catch (Exception ex) {
            Link.LogToConsole("Failed to load holdings account. This will break some vault-based eco features!");
            Link.LogToConsole("Valour is a closed economy, which means there must be an account to handle server transactions.");
            Link.LogToConsole(ex.getMessage());
        }
    }

    public CompletableFuture<EcoAccount> GetAccountById(long accountId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(Link.BaseUrl + "eco/accounts/" + accountId))
                    .headers(Link.BaseHeaders)
                    .header("content-type", "application/json")
                    .GET()
                    .build();

            return Link.Http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() != 200) {
                            return null;
                        }
                        return Link.Gson.fromJson(response.body(), EcoAccount.class);
                    });

        } catch (Exception ex) {
            Link.LogToConsole("Error getting account.");
            Link.LogToConsole(ex.getMessage());
            return CompletableFuture.supplyAsync(() -> { return null; });
        }
    }

    public CompletableFuture<EcoAccount> GetAccountByUserId(String uuid) {

        var valourId = Link.UUIDToValourMap.getOrDefault(uuid, null);
        if (valourId == null) {
            return CompletableFuture.supplyAsync(() -> { return null; });
        }

        return GetAccountByUserId(valourId);
    }

    public CompletableFuture<EcoAccount> GetAccountByUserId(long valourId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(Link.BaseUrl + "eco/accounts/planet/" + Link.PlanetId + "/byuser/" + valourId))
                    .headers(Link.BaseHeaders)
                    .header("content-type", "application/json")
                    .GET()
                    .build();

            return Link.Http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() != 200) {
                            return null;
                        }
                        return Link.Gson.fromJson(response.body(), EcoAccount.class);
                    });

        } catch (Exception ex) {
            Link.LogToConsole("Error getting user account.");
            Link.LogToConsole(ex.getMessage());
            return CompletableFuture.supplyAsync(() -> { return null; });
        }
    }

    public CompletableFuture<TaskResult> DoTransaction(String from, long to, double amount) {
        // Get both user accounts
        var fromTask = GetAccountByUserId(from);
        var toTask = GetAccountByUserId(to);

        try {
            CompletableFuture.allOf(fromTask, toTask).get();
        } catch (Exception ex) {
            var res = new TaskResult();
            res.Message = "Error getting accounts.";
            return CompletableFuture.supplyAsync(() -> res);
        }

        var fromAcc = fromTask.join();
        var toAcc = toTask.join();

        if (fromAcc == null || toAcc == null) {
            var res = new TaskResult();
            res.Message = "Error getting accounts.";
            return CompletableFuture.supplyAsync(() -> res);
        }

        return DoTransaction(fromAcc, toAcc, amount);
    }

    public CompletableFuture<TaskResult> DoTransaction(String from, String to, double amount) {
        // Get both user accounts
        var fromTask = GetAccountByUserId(from);
        var toTask = GetAccountByUserId(to);

        try {
            CompletableFuture.allOf(fromTask, toTask).get();
        } catch (Exception ex) {
            var res = new TaskResult();
            res.Message = "Error getting accounts.";
            return CompletableFuture.supplyAsync(() -> res);
        }

        var fromAcc = fromTask.join();
        var toAcc = toTask.join();

        if (fromAcc == null || toAcc == null) {
            var res = new TaskResult();
            res.Message = "Error getting accounts.";
            return CompletableFuture.supplyAsync(() -> res);
        }

        return DoTransaction(fromAcc, toAcc, amount);
    }

    public CompletableFuture<TaskResult> DoTransaction(EcoAccount fromAcc, EcoAccount toAcc, double amount) {
        Transaction transaction = new Transaction();

        transaction.planetId = Link.PlanetId;
        transaction.userFromId = fromAcc.userId;
        transaction.accountFromId = fromAcc.id;
        transaction.userToId = toAcc.userId;
        transaction.accountToId = toAcc.id;

        transaction.description = "Minecraft ValourLink payment";

        transaction.amount = new BigDecimal(amount).setScale(ValourCurrency.decimalPlaces, RoundingMode.HALF_UP);

        transaction.fingerprint = "MC-" + UUID.randomUUID().toString();

        transaction.forcedBy = Link.ValourAuth.userId;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(Link.BaseUrl + "eco/transactions"))
                    .headers(Link.BaseHeaders)
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(Link.Gson.toJson(transaction)))
                    .build();

            return Link.Http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        var result = new TaskResult();
                        result.Message = response.body();
                        if (response.statusCode() == 201) {
                            result.Success = true;
                        }
                        return result;
                    });

        } catch (Exception ex) {
            Link.LogToConsole("Error sending transaction.");
            Link.LogToConsole(ex.getMessage());
            TaskResult res = new TaskResult();
            res.Message = "Error sending transaction.";
            return CompletableFuture.supplyAsync(() -> res);
        }
    }

    private void LoadCurrency() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(Link.BaseUrl + "eco/currencies/byPlanet/" + Link.PlanetId))
                    .headers(Link.BaseHeaders)
                    .header("content-type", "application/json")
                    .GET()
                    .build();

            var result = Link.Http.send(request, HttpResponse.BodyHandlers.ofString());
            if (result.statusCode() != 200) {
                Link.LogToConsole("Error getting planet currency!");
                Link.LogToConsole("Economy features will be broken!");
                return;
            }

            ValourCurrency = Link.Gson.fromJson(result.body(), Currency.class);
            Link.LogToConsole("Loaded currency " + ValourCurrency.name + " successfully!");

        } catch (Exception ex) {
            Link.LogToConsole("Error getting planet currency!");
            Link.LogToConsole("Economy features will be broken!");
            Link.LogToConsole(ex.getMessage());
        }
    }


}
