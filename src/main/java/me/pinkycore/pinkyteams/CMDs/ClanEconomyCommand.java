package me.pinkycore.pinkyteams.CMDs;

import me.pinkycore.pinkyteams.PinkyTeams;
import me.pinkycore.pinkyteams.Utils.ClanPermission;
import me.pinkycore.pinkyteams.Utils.LangManager;
import me.pinkycore.pinkyteams.Utils.MSG;
import me.pinkycore.pinkyteams.api.event.ClanBankTransactionEvent;
import me.pinkycore.pinkyteams.service.ClanBankService;
import me.pinkycore.pinkyteams.service.BankAuditService;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.Locale;

final class ClanEconomyCommand {
    private final PinkyTeams plugin;
    private final LangManager lang;
    private final PermissionCheck permissionCheck;

    ClanEconomyCommand(PinkyTeams plugin, LangManager lang, PermissionCheck permissionCheck) {
        this.plugin = plugin;
        this.lang = lang;
        this.permissionCheck = permissionCheck;
    }

    void execute(Player player, String clanName, String[] args) {
        FileConfiguration config = plugin.getFH().getConfig();
        if (!config.getBoolean("economy.enabled", true)) {
            send(player, "user.economy_disabled");
            return;
        }
        if (clanName == null || clanName.isEmpty()) {
            send(player, "user.no_clan");
            return;
        }
        if (args.length == 1) {
            player.sendMessage(MSG.color(lang.getMessageWithPrefix("user.economy_balance")
                .replace("{balance}", formatMoney(plugin.getStorageProvider().getClanMoney(clanName)))));
            return;
        }
        if (args.length == 2 && (args[1].equalsIgnoreCase("history") || args[1].equalsIgnoreCase("log"))) {
            if (!permissionCheck.test(player, clanName, ClanPermission.BANK_WITHDRAW)) return;
            var entries=plugin.getBankAuditService().recent(clanName,plugin.getConfig().getInt("economy.bank.audit-display-limit",10));
            player.sendMessage(MSG.color("&d&lBank audit &7("+clanName+")"));
            if(entries.isEmpty()){player.sendMessage(MSG.color("&7No transactions."));return;}
            for(var entry:entries) player.sendMessage(MSG.color("&7- &f"+entry.actor()+" &d"+entry.type()+" &f$"+formatMoney(entry.amount())+" &7-> $"+formatMoney(entry.balance())));
            return;
        }
        if (args.length != 3) {
            send(player, "user.economy_usage");
            return;
        }

        String action = args[1].toLowerCase(Locale.ROOT);
        boolean deposit = action.equals("deposit") || action.equals("depositar");
        boolean withdraw = action.equals("withdraw") || action.equals("retirar");
        if (!deposit && !withdraw) {
            send(player, "user.economy_usage");
            return;
        }

        Double amount = parsePositiveAmount(args[2]);
        if (amount == null) {
            send(player, "user.economy_invalid_amount");
            return;
        }

        ClanBankService.Result result;
        double maximum = 0;
        if (deposit) {
            if (!permissionCheck.test(player, clanName, ClanPermission.BANK_DEPOSIT)) return;
            maximum = config.getDouble("economy.bank.max-balance", 0.0);
            result = plugin.getClanBankService().deposit(clanName, player, amount, maximum);
        } else {
            if (!permissionCheck.test(player, clanName, ClanPermission.BANK_WITHDRAW)) return;
            result = plugin.getClanBankService().withdraw(clanName, player, amount);
        }
        sendResult(player, clanName, result, amount, maximum, deposit);
    }

    private void sendResult(Player player, String clanName, ClanBankService.Result result,
                            double amount, double maximum, boolean deposit) {
        String key = switch (result.status()) {
            case SUCCESS -> deposit ? "user.economy_deposit_success" : "user.economy_withdraw_success";
            case PLAYER_FUNDS -> "user.economy_not_enough_player";
            case CLAN_FUNDS -> "user.economy_not_enough_clan";
            case BANK_LIMIT -> "user.economy_bank_limit";
            case INVALID_AMOUNT -> "user.economy_invalid_amount";
            default -> "user.economy_transaction_error";
        };
        player.sendMessage(MSG.color(lang.getMessageWithPrefix(key)
            .replace("{amount}", formatMoney(amount))
            .replace("{balance}", formatMoney(result.balance()))
            .replace("{limit}", formatMoney(maximum))));
        if (result.successful()) {
            plugin.getBankAuditService().record(clanName,player.getName(),deposit?BankAuditService.Type.DEPOSIT:BankAuditService.Type.WITHDRAW,amount,result.balance());
            Bukkit.getPluginManager().callEvent(new ClanBankTransactionEvent(player, clanName, amount,
                result.balance(), deposit ? ClanBankTransactionEvent.Type.DEPOSIT : ClanBankTransactionEvent.Type.WITHDRAW));
        }
    }

    private void send(Player player, String key) {
        player.sendMessage(MSG.color(lang.getMessageWithPrefix(key)));
    }

    private static Double parsePositiveAmount(String raw) {
        if (raw == null || raw.trim().isEmpty() || raw.matches(".*[A-Za-z].*")) return null;
        String value = raw.trim().replace(" ", "").replace("$", "").replaceAll("[^0-9,\\.]", "");
        if (value.matches("^\\d{1,3}(,\\d{3})+$")) value = value.replace(",", "");
        else if (value.matches("^\\d{1,3}(\\.\\d{3})+$")) value = value.replace(".", "");
        else if (value.contains(",") && !value.contains(".")) value = value.replace(",", ".");
        else if (value.contains(",") && value.contains(".")) value = value.replace(",", "");
        if (!value.matches("^[0-9]+(\\.[0-9]+)?$")) return null;
        try {
            double amount = Double.parseDouble(value);
            return Double.isFinite(amount) && amount > 0 ? amount : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String formatMoney(double amount) {
        return new DecimalFormat("#,##0.##").format(amount);
    }

    @FunctionalInterface
    interface PermissionCheck {
        boolean test(Player player, String clanName, ClanPermission permission);
    }
}
