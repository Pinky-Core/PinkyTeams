package me.pinkycore.pinkyteams.service;

import me.pinkycore.pinkyteams.Database.StorageProvider;
import me.pinkycore.pinkyteams.Utils.Econo;
import org.bukkit.OfflinePlayer;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ClanBankService {
    private static final double EPSILON = 0.000001D;

    private final ClanBalanceStore storage;
    private final PlayerEconomy economy;
    private final Map<String, Object> clanLocks = new ConcurrentHashMap<>();

    public ClanBankService(StorageProvider storage, Econo economy) {
        this(new ClanBalanceStore() {
            public double get(String clan) { return storage.getClanMoney(clan); }
            public void set(String clan, double amount) { storage.setClanMoney(clan, amount); }
        }, new PlayerEconomy() {
            public boolean has(OfflinePlayer player, double amount) { return economy.has(player, amount); }
            public boolean deposit(OfflinePlayer player, double amount) { return economy.deposit(player, amount); }
            public boolean withdraw(OfflinePlayer player, double amount) { return economy.withdraw(player, amount); }
        });
    }

    ClanBankService(ClanBalanceStore storage, PlayerEconomy economy) {
        this.storage = storage;
        this.economy = economy;
    }

    public Result deposit(String clan, OfflinePlayer player, double amount, double maximumBalance) {
        if (!valid(clan, player, amount)) return Result.INVALID_AMOUNT;
        synchronized (lockFor(clan)) {
            double before = storage.get(clan);
            if (maximumBalance > 0 && before + amount > maximumBalance) return Result.BANK_LIMIT;
            if (!economy.has(player, amount)) return Result.PLAYER_FUNDS;
            if (!economy.withdraw(player, amount)) return Result.ECONOMY_ERROR;

            double expected = before + amount;
            storage.set(clan, expected);
            double persisted = storage.get(clan);
            if (!sameMoney(persisted, expected)) {
                economy.deposit(player, amount);
                storage.set(clan, before);
                return Result.STORAGE_ERROR;
            }
            return Result.success(persisted);
        }
    }

    public Result withdraw(String clan, OfflinePlayer player, double amount) {
        if (!valid(clan, player, amount)) return Result.INVALID_AMOUNT;
        synchronized (lockFor(clan)) {
            double before = storage.get(clan);
            if (before + EPSILON < amount) return Result.CLAN_FUNDS;

            double expected = before - amount;
            storage.set(clan, expected);
            double persisted = storage.get(clan);
            if (!sameMoney(persisted, expected)) {
                storage.set(clan, before);
                return Result.STORAGE_ERROR;
            }
            if (!economy.deposit(player, amount)) {
                storage.set(clan, before);
                return Result.ECONOMY_ERROR;
            }
            return Result.success(persisted);
        }
    }

    public Result creditClan(String clan, double amount) {
        if (clan == null || clan.isBlank() || !Double.isFinite(amount) || amount <= 0) return Result.INVALID_AMOUNT;
        synchronized (lockFor(clan)) {
            double before = storage.get(clan), expected = before + amount;
            storage.set(clan, expected);
            double persisted = storage.get(clan);
            return sameMoney(persisted, expected) ? Result.success(persisted) : Result.STORAGE_ERROR;
        }
    }

    private Object lockFor(String clan) {
        return clanLocks.computeIfAbsent(clan.toLowerCase(Locale.ROOT), ignored -> new Object());
    }

    private boolean valid(String clan, OfflinePlayer player, double amount) {
        return clan != null && !clan.isBlank() && player != null && Double.isFinite(amount) && amount > 0;
    }

    private boolean sameMoney(double first, double second) {
        return Double.isFinite(first) && Math.abs(first - second) < EPSILON;
    }

    public enum Status {
        SUCCESS, INVALID_AMOUNT, PLAYER_FUNDS, CLAN_FUNDS, BANK_LIMIT, ECONOMY_ERROR, STORAGE_ERROR
    }

    interface ClanBalanceStore {
        double get(String clan);
        void set(String clan, double amount);
    }

    interface PlayerEconomy {
        boolean has(OfflinePlayer player, double amount);
        boolean deposit(OfflinePlayer player, double amount);
        boolean withdraw(OfflinePlayer player, double amount);
    }

    public static final class Result {
        public static final Result INVALID_AMOUNT = new Result(Status.INVALID_AMOUNT, 0);
        public static final Result PLAYER_FUNDS = new Result(Status.PLAYER_FUNDS, 0);
        public static final Result CLAN_FUNDS = new Result(Status.CLAN_FUNDS, 0);
        public static final Result BANK_LIMIT = new Result(Status.BANK_LIMIT, 0);
        public static final Result ECONOMY_ERROR = new Result(Status.ECONOMY_ERROR, 0);
        public static final Result STORAGE_ERROR = new Result(Status.STORAGE_ERROR, 0);

        private final Status status;
        private final double balance;

        private Result(Status status, double balance) {
            this.status = status;
            this.balance = balance;
        }

        public static Result success(double balance) {
            return new Result(Status.SUCCESS, balance);
        }

        public Status status() { return status; }
        public double balance() { return balance; }
        public boolean successful() { return status == Status.SUCCESS; }
    }
}
