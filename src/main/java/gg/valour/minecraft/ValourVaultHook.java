package gg.valour.minecraft;

import gg.valour.minecraft.models.EcoAccount;
import net.milkbowl.vault.economy.AbstractEconomy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.List;

public class ValourVaultHook extends AbstractEconomy {
    private final ValourLink _valourLink;
    private final ValourEconomy _economy;

    public ValourVaultHook(ValourEconomy eco) {
        _valourLink = ValourLink.Instance;
        _economy = eco;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getName() {
        return "Valour Economy";
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        return _economy.ValourCurrency.decimalPlaces;
    }

    @Override
    public String format(double v) {
        return _economy.ValourCurrency.symbol + v;
    }

    @Override
    public String currencyNamePlural() {
        return _economy.ValourCurrency.pluralName;
    }

    @Override
    public String currencyNameSingular() {
        return _economy.ValourCurrency.name;
    }

    @Override
    public boolean hasAccount(String s) {
        try {
            var player = Bukkit.getOfflinePlayer(s);
            var account = _economy.GetAccountByUserId(player.getUniqueId().toString()).get();
            return account != null;
        } catch (Exception ex) {
            _valourLink.LogToConsole("Unexpected error in Valour Vault Hook!");
            _valourLink.LogToConsole(ex.getMessage());
            return false;
        }
    }

    @Override
    public boolean hasAccount(OfflinePlayer offlinePlayer) {
        try {
            var account = _economy.GetAccountByUserId(offlinePlayer.getUniqueId().toString()).get();
            return account != null;
        } catch (Exception ex) {
            _valourLink.LogToConsole("Unexpected error in Valour Vault Hook!");
            _valourLink.LogToConsole(ex.getMessage());
            return false;
        }
    }

    @Override
    public boolean hasAccount(String s, String s1) {
        try {
            var player = Bukkit.getOfflinePlayer(s);
            var account = _economy.GetAccountByUserId(player.getUniqueId().toString()).get();
            return account != null;
        } catch (Exception ex) {
            _valourLink.LogToConsole("Unexpected error in Valour Vault Hook!");
            _valourLink.LogToConsole(ex.getMessage());
            return false;
        }
    }

    @Override
    public boolean hasAccount(OfflinePlayer offlinePlayer, String s) {
        try {
            var account = _economy.GetAccountByUserId(offlinePlayer.getUniqueId().toString()).get();
            return account != null;
        } catch (Exception ex) {
            _valourLink.LogToConsole("Unexpected error in Valour Vault Hook!");
            _valourLink.LogToConsole(ex.getMessage());
            return false;
        }
    }

    @Override
    public double getBalance(String s) {
        try {
            // Towny support
            if (s.startsWith("town-") || s.startsWith("nation-")) {
                return _valourLink.LocalEcoAccounts.getOrDefault(s, 0d);
            }

            var player = Bukkit.getOfflinePlayer(s);
            var account = _economy.GetAccountByUserId(player.getUniqueId().toString()).get();
            if (account == null) {
                return 0;
            }
            return account.balanceValue.doubleValue();
        } catch (Exception ex) {
            _valourLink.LogToConsole("Unexpected error in Valour Vault Hook!");
            _valourLink.LogToConsole(ex.getMessage());
            return 0;
        }
    }

    @Override
    public double getBalance(OfflinePlayer offlinePlayer) {
        try {
            var account = _economy.GetAccountByUserId(offlinePlayer.getUniqueId().toString()).get();
            if (account == null) {
                return 0;
            }
            return account.balanceValue.doubleValue();
        } catch (Exception ex) {
            _valourLink.LogToConsole("Unexpected error in Valour Vault Hook!");
            _valourLink.LogToConsole(ex.getMessage());
            return 0;
        }
    }

    @Override
    public double getBalance(String s, String s1) {
        try {
            // Towny support
            if (s.startsWith("town-") || s.startsWith("nation-")) {
                return _valourLink.LocalEcoAccounts.getOrDefault(s, 0d);
            }

            var player = Bukkit.getOfflinePlayer(s);
            var account = _economy.GetAccountByUserId(player.getUniqueId().toString()).get();

            if (account == null) {
                return 0;
            }
            return account.balanceValue.doubleValue();
        } catch (Exception ex) {
            _valourLink.LogToConsole("Unexpected error in Valour Vault Hook!");
            _valourLink.LogToConsole(ex.getMessage());
            return 0;
        }
    }

    @Override
    public double getBalance(OfflinePlayer offlinePlayer, String s) {
        try {
            var account = _economy.GetAccountByUserId(offlinePlayer.getUniqueId().toString()).get();
            if (account == null) {
                return 0;
            }
            return account.balanceValue.doubleValue();
        } catch (Exception ex) {
            _valourLink.LogToConsole("Unexpected error in Valour Vault Hook!");
            _valourLink.LogToConsole(ex.getMessage());
            return 0;
        }
    }

    @Override
    public boolean has(String s, double v) {
        try {
            // Towny support
            if (s.startsWith("town-") || s.startsWith("nation-")) {
                var balance = _valourLink.LocalEcoAccounts.getOrDefault(s, 0d);
                return balance >= v;
            }

            var player = Bukkit.getOfflinePlayer(s);
            var account = _economy.GetAccountByUserId(player.getUniqueId().toString()).get();
            if (account == null) {
                return false;
            }
            return account.balanceValue.doubleValue() >= v;
        } catch (Exception ex) {
            _valourLink.LogToConsole("Unexpected error in Valour Vault Hook!");
            _valourLink.LogToConsole(ex.getMessage());
            return false;
        }
    }

    @Override
    public boolean has(OfflinePlayer offlinePlayer, double v) {
        try {
            var account = _economy.GetAccountByUserId(offlinePlayer.getUniqueId().toString()).get();
            if (account == null) {
                return false;
            }
            return account.balanceValue.doubleValue() >= v;
        } catch (Exception ex) {
            _valourLink.LogToConsole("Unexpected error in Valour Vault Hook!");
            _valourLink.LogToConsole(ex.getMessage());
            return false;
        }
    }

    @Override
    public boolean has(String s, String s1, double v) {
        try {
            // Towny support
            if (s.startsWith("town-") || s.startsWith("nation-")) {
                var balance = _valourLink.LocalEcoAccounts.getOrDefault(s, 0d);
                return balance >= v;
            }

            var player = Bukkit.getOfflinePlayer(s);
            var account = _economy.GetAccountByUserId(player.getUniqueId().toString()).get();
            if (account == null) {
                return false;
            }
            return account.balanceValue.doubleValue() >= v;
        } catch (Exception ex) {
            _valourLink.LogToConsole("Unexpected error in Valour Vault Hook!");
            _valourLink.LogToConsole(ex.getMessage());
            return false;
        }
    }

    @Override
    public boolean has(OfflinePlayer offlinePlayer, String s, double v) {
        try {
            var account = _economy.GetAccountByUserId(offlinePlayer.getUniqueId().toString()).get();
            if (account == null) {
                return false;
            }
            return account.balanceValue.doubleValue() >= v;
        } catch (Exception ex) {
            _valourLink.LogToConsole("Unexpected error in Valour Vault Hook!");
            _valourLink.LogToConsole(ex.getMessage());
            return false;
        }
    }

    private EconomyResponse DoWithdraw(EcoAccount acc, double v) {
        try {
            if (acc == null) {
                return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Account not found");
            }

            var result = ValourEconomy.Instance.DoTransaction(acc, _economy.HoldingsAccount, v).get();

            if (!result.Success) {
                _valourLink.LogToConsole("Failed transaction: " + result.Message);
                return new EconomyResponse(0, acc.balanceValue.doubleValue(), EconomyResponse.ResponseType.FAILURE, result.Message);
            } else {
                return new EconomyResponse(v, acc.balanceValue.doubleValue() - v, EconomyResponse.ResponseType.SUCCESS, null);
            }

        } catch (Exception ex) {
            _valourLink.LogToConsole("Unexpected error in Valour Vault Hook!");
            _valourLink.LogToConsole(ex.getMessage());
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "An unexpected error occurred.");
        }
    }

    @Override
    public EconomyResponse withdrawPlayer(String s, double v) {
        try {
            // Towny support
            if (s.startsWith("town-") || s.startsWith("nation-")) {
                var newBalance = 0d;
                if (_valourLink.LocalEcoAccounts.containsKey(s)) {
                    var current = _valourLink.LocalEcoAccounts.get(s);
                    if (current < v) {
                        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Balance too low");
                    }

                    newBalance = current - v;
                    _valourLink.LocalEcoAccounts.put(s, newBalance);
                } else {
                    newBalance = v;
                    _valourLink.LocalEcoAccounts.put(s, newBalance);
                }

                _valourLink.SaveLocalAccounts();

                return new EconomyResponse(v, newBalance, EconomyResponse.ResponseType.SUCCESS, null);
            }

            var player = Bukkit.getOfflinePlayer(s);
            var acc = ValourEconomy.Instance.GetAccountByUserId(player.getUniqueId().toString()).get();
            return DoWithdraw(acc, v);
        } catch (Exception ex) {
            _valourLink.LogToConsole("Unexpected error in Valour Vault Hook!");
            _valourLink.LogToConsole(ex.getMessage());
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "An unexpected error occurred.");
        }
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer offlinePlayer, double v) {
        try {
            var acc = ValourEconomy.Instance.GetAccountByUserId(offlinePlayer.getUniqueId().toString()).get();
            return DoWithdraw(acc, v);
        } catch (Exception ex) {
            _valourLink.LogToConsole("Unexpected error in Valour Vault Hook!");
            _valourLink.LogToConsole(ex.getMessage());
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "An unexpected error occurred.");
        }
    }

    @Override
    public EconomyResponse withdrawPlayer(String s, String s1, double v) {
        try {
            // Towny support
            if (s.startsWith("town-") || s.startsWith("nation-")) {
                var newBalance = 0d;
                if (_valourLink.LocalEcoAccounts.containsKey(s)) {
                    var current = _valourLink.LocalEcoAccounts.get(s);
                    if (current < v) {
                        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Balance too low");
                    }

                    newBalance = current - v;
                    _valourLink.LocalEcoAccounts.put(s, newBalance);
                } else {
                    newBalance = v;
                    _valourLink.LocalEcoAccounts.put(s, newBalance);
                }

                _valourLink.SaveLocalAccounts();

                return new EconomyResponse(v, newBalance, EconomyResponse.ResponseType.SUCCESS, null);
            }

            var player = Bukkit.getOfflinePlayer(s);
            var acc = ValourEconomy.Instance.GetAccountByUserId(player.getUniqueId().toString()).get();
            return DoWithdraw(acc, v);
        } catch (Exception ex) {
            _valourLink.LogToConsole("Unexpected error in Valour Vault Hook!");
            _valourLink.LogToConsole(ex.getMessage());
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "An unexpected error occurred.");
        }
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer offlinePlayer, String s, double v) {
        try {
            var acc = ValourEconomy.Instance.GetAccountByUserId(offlinePlayer.getUniqueId().toString()).get();
            return DoWithdraw(acc, v);
        } catch (Exception ex) {
            _valourLink.LogToConsole("Unexpected error in Valour Vault Hook!");
            _valourLink.LogToConsole(ex.getMessage());
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "An unexpected error occurred.");
        }
    }

    private EconomyResponse DoDeposit(EcoAccount acc, double v) {
        try {
            if (acc == null) {
                return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Account not found");
            }

            var result = ValourEconomy.Instance.DoTransaction(_economy.HoldingsAccount, acc, v).get();
            if (!result.Success) {
                _valourLink.LogToConsole("Failed Deposit: " + result.Message);
                return new EconomyResponse(0, acc.balanceValue.doubleValue(), EconomyResponse.ResponseType.FAILURE, result.Message);
            } else {
                return new EconomyResponse(v, acc.balanceValue.doubleValue() + v, EconomyResponse.ResponseType.SUCCESS, null);
            }

        } catch (Exception ex) {
            _valourLink.LogToConsole("Unexpected error in Valour Vault Hook!");
            _valourLink.LogToConsole(ex.getMessage());
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "An unexpected error occurred.");
        }
    }

    @Override
    public EconomyResponse depositPlayer(String s, double v) {
        try {

            // Towny support
            if (s.startsWith("town-") || s.startsWith("nation-")) {
                var newBalance = 0d;
                if (_valourLink.LocalEcoAccounts.containsKey(s)) {
                    var current = _valourLink.LocalEcoAccounts.get(s);
                    newBalance = current + v;
                    _valourLink.LocalEcoAccounts.put(s, newBalance);
                } else {
                    newBalance = v;
                    _valourLink.LocalEcoAccounts.put(s, newBalance);
                }

                _valourLink.SaveLocalAccounts();

                return new EconomyResponse(v, newBalance, EconomyResponse.ResponseType.SUCCESS, null);
            }

            var player = Bukkit.getOfflinePlayer(s);
            var acc = ValourEconomy.Instance.GetAccountByUserId(player.getUniqueId().toString()).get();
            return DoDeposit(acc, v);
        } catch (Exception ex) {
            _valourLink.LogToConsole("Unexpected error in Valour Vault Hook!");
            _valourLink.LogToConsole(ex.getMessage());
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "An unexpected error occurred.");
        }
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer offlinePlayer, double v) {
        try {
            var acc = ValourEconomy.Instance.GetAccountByUserId(offlinePlayer.getUniqueId().toString()).get();
            return DoDeposit(acc, v);
        } catch (Exception ex) {
            _valourLink.LogToConsole("Unexpected error in Valour Vault Hook!");
            _valourLink.LogToConsole(ex.getMessage());
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "An unexpected error occurred.");
        }
    }

    @Override
    public EconomyResponse depositPlayer(String s, String s1, double v) {
        try {
            // Towny support
            if (s.startsWith("town-") || s.startsWith("nation-")) {
                var newBalance = 0d;
                if (_valourLink.LocalEcoAccounts.containsKey(s)) {
                    var current = _valourLink.LocalEcoAccounts.get(s);
                    newBalance = current + v;
                    _valourLink.LocalEcoAccounts.put(s, newBalance);
                } else {
                    newBalance = v;
                    _valourLink.LocalEcoAccounts.put(s, newBalance);
                }

                _valourLink.SaveLocalAccounts();

                return new EconomyResponse(v, newBalance, EconomyResponse.ResponseType.SUCCESS, null);
            }

            var player = Bukkit.getOfflinePlayer(s);
            var acc = ValourEconomy.Instance.GetAccountByUserId(player.getUniqueId().toString()).get();
            return DoDeposit(acc, v);
        } catch (Exception ex) {
            _valourLink.LogToConsole("Unexpected error in Valour Vault Hook!");
            _valourLink.LogToConsole(ex.getMessage());
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "An unexpected error occurred.");
        }
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer offlinePlayer, String s, double v) {
        try {
            var acc = ValourEconomy.Instance.GetAccountByUserId(offlinePlayer.getUniqueId().toString()).get();
            return DoDeposit(acc, v);
        } catch (Exception ex) {
            _valourLink.LogToConsole("Unexpected error in Valour Vault Hook!");
            _valourLink.LogToConsole(ex.getMessage());
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "An unexpected error occurred.");
        }
    }

    @Override
    public EconomyResponse createBank(String s, String s1) {
        return null;
    }

    @Override
    public EconomyResponse createBank(String s, OfflinePlayer offlinePlayer) {
        return null;
    }

    @Override
    public EconomyResponse deleteBank(String s) {
        return null;
    }

    @Override
    public EconomyResponse bankBalance(String s) {
        return null;
    }

    @Override
    public EconomyResponse bankHas(String s, double v) {
        return null;
    }

    @Override
    public EconomyResponse bankWithdraw(String s, double v) {
        return null;
    }

    @Override
    public EconomyResponse bankDeposit(String s, double v) {
        return null;
    }

    @Override
    public EconomyResponse isBankOwner(String s, String s1) {
        return null;
    }

    @Override
    public EconomyResponse isBankOwner(String s, OfflinePlayer offlinePlayer) {
        return null;
    }

    @Override
    public EconomyResponse isBankMember(String s, String s1) {
        return null;
    }

    @Override
    public EconomyResponse isBankMember(String s, OfflinePlayer offlinePlayer) {
        return null;
    }

    @Override
    public List<String> getBanks() {
        return null;
    }

    @Override
    public boolean createPlayerAccount(String s) {
        return false;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer offlinePlayer) {
        return false;
    }

    @Override
    public boolean createPlayerAccount(String s, String s1) {
        return false;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer offlinePlayer, String s) {
        return false;
    }
}
