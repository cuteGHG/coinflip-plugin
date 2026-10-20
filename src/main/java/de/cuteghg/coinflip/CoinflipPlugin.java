package de.cuteghg.coinflip;

import de.cuteghg.coinflip.gui.ConfirmCreateHolder;
import de.cuteghg.coinflip.gui.MainMenuHolder;
import de.cuteghg.coinflip.model.OpenCoinflip;
import de.cuteghg.coinflip.service.CoinflipService;
import de.cuteghg.coinflip.service.RefundLedger;
import de.cuteghg.coinflip.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class CoinflipPlugin extends JavaPlugin implements Listener {
    private final CoinflipService service = new CoinflipService();
    private final SecureRandom random = new SecureRandom();
    private final Set<UUID> awaitingStakeInput = new HashSet<>();
    private final Map<UUID, Integer> rememberedPage = new HashMap<>();

    private RefundLedger refundLedger;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        refundLedger = new RefundLedger(this);
        refundLedger.load();

        if (getCommand("coinflip") != null) {
            getCommand("coinflip").setExecutor(this);
        }

        Bukkit.getPluginManager().registerEvents(this, this);
        for (Player player : Bukkit.getOnlinePlayers()) {
            payPendingRefund(player);
        }
    }

    @Override
    public void onDisable() {
        for (OpenCoinflip flip : service.drainAllOpenFlips()) {
            refundLedger.add(flip.creator(), flip.stake());
        }
        refundLedger.save();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(msg("messages.only-player"));
            return true;
        }
        if (!player.hasPermission("coinflip.use")) {
            player.sendMessage(msg("messages.no-permission"));
            return true;
        }

        openMainMenu(player, rememberedPage.getOrDefault(player.getUniqueId(), 0));
        return true;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        payPendingRefund(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        service.cancelByCreator(event.getPlayer().getUniqueId()).ifPresent(flip -> refundLedger.add(flip.creator(), flip.stake()));
        awaitingStakeInput.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        service.cancelByCreator(event.getEntity().getUniqueId()).ifPresent(flip -> refundLedger.add(flip.creator(), flip.stake()));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        InventoryHolder holder = top.getHolder();

        if (!(holder instanceof MainMenuHolder) && !(holder instanceof ConfirmCreateHolder)) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (holder instanceof MainMenuHolder main) {
            handleMainMenuClick(player, event, main.page());
            return;
        }

        if (holder instanceof ConfirmCreateHolder confirm) {
            handleConfirmClick(player, event, confirm.stake());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof MainMenuHolder || holder instanceof ConfirmCreateHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            // No action needed. Stakes are held server-side and protected.
            rememberedPage.putIfAbsent(player.getUniqueId(), 0);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onStakeInput(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!awaitingStakeInput.contains(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        String raw = event.getMessage().trim().toLowerCase(Locale.ROOT);

        Bukkit.getScheduler().runTask(this, () -> {
            awaitingStakeInput.remove(player.getUniqueId());
            if (raw.equals("cancel") || raw.equals("abbrechen")) {
                player.sendMessage(msg("messages.input-cancelled"));
                openMainMenu(player, rememberedPage.getOrDefault(player.getUniqueId(), 0));
                return;
            }

            int min = getConfig().getInt("stakes.min", 1);
            int max = getConfig().getInt("stakes.max", 2304);
            int amount;
            try {
                amount = Integer.parseInt(raw);
            } catch (NumberFormatException ex) {
                player.sendMessage(msg("messages.invalid-number"));
                openMainMenu(player, rememberedPage.getOrDefault(player.getUniqueId(), 0));
                return;
            }

            if (amount < min || amount > max) {
                player.sendMessage(msg("messages.stake-out-of-range")
                        .replace("%min%", Integer.toString(min))
                        .replace("%max%", Integer.toString(max)));
                openMainMenu(player, rememberedPage.getOrDefault(player.getUniqueId(), 0));
                return;
            }

            openConfirmMenu(player, amount);
        });
    }

    private void handleMainMenuClick(Player player, InventoryClickEvent event, int page) {
        int clickedSlot = event.getRawSlot();
        int createSlot = getConfig().getInt("gui.buttons.create", 45);
        int prevSlot = getConfig().getInt("gui.buttons.prev", 46);
        int refreshSlot = getConfig().getInt("gui.buttons.refresh", 49);
        int nextSlot = getConfig().getInt("gui.buttons.next", 52);
        int closeSlot = getConfig().getInt("gui.buttons.close", 53);

        if (clickedSlot == createSlot) {
            startStakeInput(player, page);
            return;
        }
        if (clickedSlot == prevSlot) {
            openMainMenu(player, Math.max(0, page - 1));
            return;
        }
        if (clickedSlot == refreshSlot) {
            openMainMenu(player, page);
            return;
        }
        if (clickedSlot == nextSlot) {
            openMainMenu(player, page + 1);
            return;
        }
        if (clickedSlot == closeSlot) {
            player.closeInventory();
            return;
        }

        List<Integer> listSlots = listSlots();
        int idxInPage = listSlots.indexOf(clickedSlot);
        if (idxInPage < 0) {
            return;
        }

        List<OpenCoinflip> open = service.listOpenFlips();
        int absolute = page * listSlots.size() + idxInPage;
        if (absolute < 0 || absolute >= open.size()) {
            return;
        }

        OpenCoinflip flip = open.get(absolute);
        if (flip.creator().equals(player.getUniqueId()) && event.getClick().isRightClick()) {
            cancelOwnFlip(player, flip.id(), page);
            return;
        }

        joinFlip(player, flip.id(), page);
    }

    private void handleConfirmClick(Player player, InventoryClickEvent event, int stake) {
        int slot = event.getRawSlot();
        int confirm = getConfig().getInt("gui.confirm.confirm-slot", 11);
        int back = getConfig().getInt("gui.confirm.back-slot", 13);
        int cancel = getConfig().getInt("gui.confirm.cancel-slot", 15);

        if (slot == confirm) {
            createFlip(player, stake);
            return;
        }

        if (slot == back) {
            openMainMenu(player, rememberedPage.getOrDefault(player.getUniqueId(), 0));
            return;
        }

        if (slot == cancel) {
            player.closeInventory();
            player.sendMessage(msg("messages.input-cancelled"));
        }
    }

    private void startStakeInput(Player player, int page) {
        if (service.getCreatorFlip(player.getUniqueId()).isPresent()) {
            player.sendMessage(msg("messages.already-open"));
            return;
        }

        awaitingStakeInput.add(player.getUniqueId());
        rememberedPage.put(player.getUniqueId(), page);
        player.closeInventory();
        player.sendMessage(msg("messages.enter-stake"));
    }

    private void createFlip(Player player, int stake) {
        UUID creator = player.getUniqueId();

        if (service.getCreatorFlip(creator).isPresent()) {
            player.sendMessage(msg("messages.already-open"));
            openMainMenu(player, rememberedPage.getOrDefault(creator, 0));
            return;
        }

        if (!hasDiamonds(player, stake)) {
            player.sendMessage(msg("messages.not-enough-diamonds"));
            openMainMenu(player, rememberedPage.getOrDefault(creator, 0));
            return;
        }

        if (!removeDiamonds(player, stake)) {
            player.sendMessage(msg("messages.transaction-failed"));
            openMainMenu(player, rememberedPage.getOrDefault(creator, 0));
            return;
        }

        try {
            OpenCoinflip flip = service.create(creator, stake);
            player.sendMessage(msg("messages.flip-created")
                    .replace("%stake%", Integer.toString(stake))
                    .replace("%id%", shortId(flip.id())));
        } catch (RuntimeException ex) {
            addDiamonds(player, stake);
            player.sendMessage(msg("messages.transaction-failed"));
        }

        openMainMenu(player, rememberedPage.getOrDefault(creator, 0));
    }

    private void cancelOwnFlip(Player player, UUID flipId, int page) {
        Optional<OpenCoinflip> cancelled = service.cancel(player.getUniqueId(), flipId);
        if (cancelled.isEmpty()) {
            player.sendMessage(msg("messages.cancel-failed"));
            openMainMenu(player, page);
            return;
        }

        addDiamonds(player, cancelled.get().stake());
        player.sendMessage(msg("messages.flip-cancelled")
                .replace("%stake%", Integer.toString(cancelled.get().stake())));
        openMainMenu(player, page);
    }

    private void joinFlip(Player challenger, UUID flipId, int page) {
        UUID challengerId = challenger.getUniqueId();
        Optional<OpenCoinflip> locked = service.lockForJoin(challengerId, flipId);
        if (locked.isEmpty()) {
            challenger.sendMessage(msg("messages.join-failed"));
            openMainMenu(challenger, page);
            return;
        }

        OpenCoinflip flip = locked.get();
        int stake = flip.stake();

        if (!hasDiamonds(challenger, stake) || !removeDiamonds(challenger, stake)) {
            service.abortJoinLock(flip.id());
            challenger.sendMessage(msg("messages.not-enough-diamonds"));
            openMainMenu(challenger, page);
            return;
        }

        Optional<CoinflipService.JoinOutcome> outcome = service.completeJoin(challengerId, flip.id(), random);
        if (outcome.isEmpty()) {
            addDiamonds(challenger, stake);
            service.abortJoinLock(flip.id());
            challenger.sendMessage(msg("messages.join-failed"));
            openMainMenu(challenger, page);
            return;
        }

        CoinflipService.JoinOutcome result = outcome.get();
        payDiamonds(result.winner(), result.totalPot());

        Player creator = Bukkit.getPlayer(result.creator());
        Player winnerPlayer = Bukkit.getPlayer(result.winner());
        String winnerName = winnerPlayer != null ? winnerPlayer.getName() : "Unbekannt";

        if (creator != null) {
            creator.sendMessage(msg("messages.flip-finished")
                    .replace("%winner%", winnerName)
                    .replace("%pot%", Integer.toString(result.totalPot())));
        }
        challenger.sendMessage(msg("messages.flip-finished")
                .replace("%winner%", winnerName)
                .replace("%pot%", Integer.toString(result.totalPot())));

        if (winnerPlayer != null) {
            winnerPlayer.sendMessage(msg("messages.you-won")
                    .replace("%pot%", Integer.toString(result.totalPot())));
        }

        openMainMenu(challenger, page);
        if (creator != null && creator.isOnline()) {
            openMainMenu(creator, rememberedPage.getOrDefault(creator.getUniqueId(), 0));
        }
    }

    private void payPendingRefund(Player player) {
        int pending = refundLedger.consume(player.getUniqueId());
        if (pending <= 0) {
            return;
        }

        addDiamonds(player, pending);
        player.sendMessage(msg("messages.refund-paid").replace("%amount%", Integer.toString(pending)));
    }

    private void payDiamonds(UUID playerId, int amount) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            addDiamonds(player, amount);
            return;
        }
        refundLedger.add(playerId, amount);
    }

    private void openMainMenu(Player player, int page) {
        List<Integer> listSlots = listSlots();
        int size = getConfig().getInt("gui.main.size", 54);
        if (size % 9 != 0 || size < 9 || size > 54) {
            size = 54;
        }

        int pages = Math.max(1, (int) Math.ceil(service.listOpenFlips().size() / (double) listSlots.size()));
        int safePage = Math.max(0, Math.min(page, pages - 1));
        rememberedPage.put(player.getUniqueId(), safePage);

        Inventory inventory = Bukkit.createInventory(
                new MainMenuHolder(safePage),
                size,
                Text.color(getConfig().getString("gui.main.title", "&8Coinflip &7(Page %page%)").replace("%page%", Integer.toString(safePage + 1)))
        );

        fillBackground(inventory, Material.GRAY_STAINED_GLASS_PANE, " ");

        int createSlot = getConfig().getInt("gui.buttons.create", 45);
        int prevSlot = getConfig().getInt("gui.buttons.prev", 46);
        int refreshSlot = getConfig().getInt("gui.buttons.refresh", 49);
        int nextSlot = getConfig().getInt("gui.buttons.next", 52);
        int closeSlot = getConfig().getInt("gui.buttons.close", 53);

        inventory.setItem(createSlot, item(Material.EMERALD, "&aCoinflip erstellen", List.of("&7Einsatz über Chat eingeben")));
        inventory.setItem(prevSlot, item(Material.ARROW, "&eZurück", List.of("&7Vorherige Seite")));
        inventory.setItem(refreshSlot, item(Material.CLOCK, "&bAktualisieren", List.of("&7Offene Coinflips neu laden")));
        inventory.setItem(nextSlot, item(Material.ARROW, "&eWeiter", List.of("&7Nächste Seite")));
        inventory.setItem(closeSlot, item(Material.BARRIER, "&cSchließen", List.of("&7GUI schließen")));

        List<OpenCoinflip> flips = service.listOpenFlips();
        int start = safePage * listSlots.size();
        for (int i = 0; i < listSlots.size(); i++) {
            int absolute = start + i;
            if (absolute >= flips.size()) {
                break;
            }
            OpenCoinflip flip = flips.get(absolute);
            String creatorName = Optional.ofNullable(Bukkit.getOfflinePlayer(flip.creator()).getName()).orElse("Unbekannt");
            List<String> lore = new ArrayList<>();
            lore.add("&7Ersteller: &f" + creatorName);
            lore.add("&7Einsatz: &b" + flip.stake() + " Diamanten");
            lore.add("&7ID: &8" + shortId(flip.id()));
            if (flip.creator().equals(player.getUniqueId())) {
                lore.add(" ");
                lore.add("&eRechtsklick: Coinflip abbrechen");
            } else {
                lore.add(" ");
                lore.add("&aKlicken: Coinflip beitreten");
            }

            inventory.setItem(listSlots.get(i), item(Material.DIAMOND, "&bCoinflip", lore));
        }

        player.openInventory(inventory);
    }

    private void openConfirmMenu(Player player, int stake) {
        Inventory inventory = Bukkit.createInventory(
                new ConfirmCreateHolder(stake),
                27,
                Text.color(getConfig().getString("gui.confirm.title", "&8Coinflip bestätigen"))
        );

        fillBackground(inventory, Material.BLACK_STAINED_GLASS_PANE, " ");

        int confirm = getConfig().getInt("gui.confirm.confirm-slot", 11);
        int back = getConfig().getInt("gui.confirm.back-slot", 13);
        int cancel = getConfig().getInt("gui.confirm.cancel-slot", 15);

        inventory.setItem(confirm, item(Material.LIME_CONCRETE, "&aBestätigen", List.of("&7Einsatz: &b" + stake + " Diamanten")));
        inventory.setItem(back, item(Material.ARROW, "&eZurück", List.of("&7Zur Hauptansicht")));
        inventory.setItem(cancel, item(Material.RED_CONCRETE, "&cAbbrechen", List.of("&7Vorgang beenden")));

        player.openInventory(inventory);
    }

    private List<Integer> listSlots() {
        List<Integer> configured = getConfig().getIntegerList("gui.main.list-slots");
        if (configured == null || configured.isEmpty()) {
            return List.of(10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34);
        }
        return configured;
    }

    private void fillBackground(Inventory inventory, Material material, String name) {
        ItemStack filler = item(material, name, List.of());
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }

        meta.setDisplayName(Text.color(name));
        if (!lore.isEmpty()) {
            List<String> coloredLore = lore.stream().map(Text::color).toList();
            meta.setLore(coloredLore);
        }
        meta.addItemFlags(ItemFlag.values());
        stack.setItemMeta(meta);
        return stack;
    }

    private String msg(String path) {
        return Text.color(getConfig().getString(path, "&cMissing message: " + path));
    }

    private String shortId(UUID id) {
        String s = id.toString();
        return s.substring(0, 8);
    }

    private boolean hasDiamonds(Player player, int amount) {
        return countDiamonds(player) >= amount;
    }

    private int countDiamonds(Player player) {
        int count = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack == null || stack.getType() != Material.DIAMOND) {
                continue;
            }
            count += stack.getAmount();
        }
        return count;
    }

    private boolean removeDiamonds(Player player, int amount) {
        if (amount <= 0) {
            return true;
        }
        if (countDiamonds(player) < amount) {
            return false;
        }

        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack stack = contents[i];
            if (stack == null || stack.getType() != Material.DIAMOND) {
                continue;
            }

            int take = Math.min(remaining, stack.getAmount());
            stack.setAmount(stack.getAmount() - take);
            if (stack.getAmount() <= 0) {
                contents[i] = null;
            }
            remaining -= take;
        }

        player.getInventory().setContents(contents);
        player.updateInventory();
        return remaining == 0;
    }

    private void addDiamonds(Player player, int amount) {
        if (amount <= 0) {
            return;
        }

        ItemStack toAdd = new ItemStack(Material.DIAMOND, amount);
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(toAdd);
        if (!overflow.isEmpty()) {
            for (ItemStack stack : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), stack);
            }
        }
        player.updateInventory();
    }
}
