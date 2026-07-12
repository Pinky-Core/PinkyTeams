package me.pinkycore.pinkyteams.service;

import me.pinkycore.pinkyteams.Database.StorageProvider;

public final class ClanAllianceService {
    private final StorageProvider storage;

    public ClanAllianceService(StorageProvider storage) {
        this.storage = storage;
    }

    public Result request(String source, String target) {
        if (same(source, target)) return Result.SAME_CLAN;
        if (!storage.clanExists(target)) return Result.CLAN_NOT_FOUND;
        if (storage.areClansAllied(source, target)) return Result.ALREADY_ALLIED;
        if (storage.getPendingAlliances(target).stream().anyMatch(source::equalsIgnoreCase)) return Result.ALREADY_PENDING;
        storage.addPendingAlliance(source, target);
        return Result.SUCCESS;
    }

    public Result accept(String receivingClan, String requester) {
        if (same(receivingClan, requester)) return Result.SAME_CLAN;
        if (!hasPending(receivingClan, requester)) return Result.NO_PENDING;
        storage.createAlliance(requester, receivingClan, false);
        storage.removePendingAlliance(requester, receivingClan);
        storage.reloadCache();
        return Result.SUCCESS;
    }

    public Result decline(String receivingClan, String requester) {
        if (same(receivingClan, requester)) return Result.SAME_CLAN;
        if (!hasPending(receivingClan, requester)) return Result.NO_PENDING;
        storage.removePendingAlliance(requester, receivingClan);
        return Result.SUCCESS;
    }

    public Result remove(String source, String target) {
        if (same(source, target)) return Result.SAME_CLAN;
        boolean allied = storage.getClanAlliances(source).stream().anyMatch(target::equalsIgnoreCase);
        if (!allied) return Result.NOT_ALLIED;
        storage.removeAlliance(source, target);
        return Result.SUCCESS;
    }

    public void setFriendlyFire(String clanName, boolean enabled) {
        storage.setFriendlyFireAlliesEnabled(clanName, enabled);
    }

    private boolean hasPending(String receivingClan, String requester) {
        return storage.getPendingAlliances(receivingClan).stream().anyMatch(requester::equalsIgnoreCase);
    }

    private boolean same(String first, String second) {
        return first != null && second != null && first.equalsIgnoreCase(second);
    }

    public enum Result {
        SUCCESS, SAME_CLAN, CLAN_NOT_FOUND, ALREADY_ALLIED, ALREADY_PENDING, NO_PENDING, NOT_ALLIED
    }
}
