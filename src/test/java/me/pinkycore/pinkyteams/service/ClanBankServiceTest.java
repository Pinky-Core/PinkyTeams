package me.pinkycore.pinkyteams.service;

import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class ClanBankServiceTest {
    @Test
    void depositMovesMoneyToClan() {
        FakeStore store = new FakeStore(100);
        FakeEconomy economy = new FakeEconomy(80);
        ClanBankService service = new ClanBankService(store, economy);

        ClanBankService.Result result = service.deposit("Pinky", nullPlayer(), 30, 0);

        assertEquals(ClanBankService.Status.SUCCESS, result.status());
        assertEquals(130, store.balance, 0.000001);
        assertEquals(50, economy.balance, 0.000001);
    }

    @Test
    void failedStorageDepositRefundsPlayer() {
        FakeStore store = new FakeStore(100);
        store.rejectWrites = true;
        FakeEconomy economy = new FakeEconomy(80);
        ClanBankService service = new ClanBankService(store, economy);

        ClanBankService.Result result = service.deposit("Pinky", nullPlayer(), 30, 0);

        assertEquals(ClanBankService.Status.STORAGE_ERROR, result.status());
        assertEquals(100, store.balance, 0.000001);
        assertEquals(80, economy.balance, 0.000001);
    }

    @Test
    void failedPlayerDepositRestoresClanBalance() {
        FakeStore store = new FakeStore(100);
        FakeEconomy economy = new FakeEconomy(0);
        economy.rejectDeposits = true;
        ClanBankService service = new ClanBankService(store, economy);

        ClanBankService.Result result = service.withdraw("Pinky", nullPlayer(), 30);

        assertEquals(ClanBankService.Status.ECONOMY_ERROR, result.status());
        assertEquals(100, store.balance, 0.000001);
    }

    @Test
    void enforcesMaximumBalance() {
        FakeStore store = new FakeStore(100);
        FakeEconomy economy = new FakeEconomy(80);
        ClanBankService service = new ClanBankService(store, economy);

        ClanBankService.Result result = service.deposit("Pinky", nullPlayer(), 30, 120);

        assertEquals(ClanBankService.Status.BANK_LIMIT, result.status());
        assertEquals(100, store.balance, 0.000001);
        assertEquals(80, economy.balance, 0.000001);
    }

    private OfflinePlayer nullPlayer() {
        return mock(OfflinePlayer.class);
    }

    private static final class FakeStore implements ClanBankService.ClanBalanceStore {
        double balance;
        boolean rejectWrites;
        FakeStore(double balance) { this.balance = balance; }
        public double get(String clan) { return balance; }
        public void set(String clan, double amount) { if (!rejectWrites) balance = amount; }
    }

    private static final class FakeEconomy implements ClanBankService.PlayerEconomy {
        double balance;
        boolean rejectDeposits;
        FakeEconomy(double balance) { this.balance = balance; }
        public boolean has(OfflinePlayer player, double amount) { return balance >= amount; }
        public boolean deposit(OfflinePlayer player, double amount) {
            if (rejectDeposits) return false;
            balance += amount;
            return true;
        }
        public boolean withdraw(OfflinePlayer player, double amount) {
            if (balance < amount) return false;
            balance -= amount;
            return true;
        }
    }
}
