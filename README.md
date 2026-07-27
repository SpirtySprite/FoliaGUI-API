# FoliaGUI-API

A thread-safe inventory GUI library for [Folia](https://papermc.io/software/folia) and regular Paper. It is a pure library: no commands, no `plugin.yml`, no config file. You add it as a dependency, call `FoliaGUI.init(this)` once, and build menus with a fluent API from there.

Every operation that touches an inventory (open, close, update, title change, animation) is dispatched through a Folia-safe scheduler. Your GUIs work correctly under Folia's regionised threading and on classic single-thread Paper, using the exact same code.

## Table of contents

- [Requirements](#requirements)
- [Installation](#installation)
- [Quick start](#quick-start)
- [Core concepts](#core-concepts)
- [Guide](#guide)
  - [Basic GUI](#basic-gui)
  - [Typed GUIs](#typed-guis)
  - [Paginated GUI](#paginated-gui)
  - [Searchable paginated GUI](#searchable-paginated-gui)
  - [Scrolling GUI](#scrolling-gui)
  - [Storage GUI](#storage-gui)
  - [Layout filler](#layout-filler)
  - [Interaction modifiers](#interaction-modifiers)
  - [Click types, cooldowns, and permissions](#click-types-cooldowns-and-permissions)
  - [Confirmation and alert dialogs](#confirmation-and-alert-dialogs)
  - [Loading content asynchronously](#loading-content-asynchronously)
  - [Anvil text input](#anvil-text-input)
  - [Sign text input](#sign-text-input)
  - [Chat text input](#chat-text-input)
  - [Merchant trade window](#merchant-trade-window)
  - [Navigation and themes](#navigation-and-themes)
  - [Animation and auto-refresh](#animation-and-auto-refresh)
  - [Cycling items](#cycling-items)
  - [Item builders](#item-builders)
  - [Item metadata: NBT, enchants, attributes, and everything else](#item-metadata-nbt-enchants-attributes-and-everything-else)
  - [Saving and loading item stacks](#saving-and-loading-item-stacks)
  - [Custom events](#custom-events)
  - [GuiManager](#guimanager)
  - [Text utility](#text-utility)
  - [Force-open dialogs](#force-open-dialogs)
- [Lifecycle](#lifecycle)
- [Threading model](#threading-model)
- [Package overview](#package-overview)
- [Testing](#testing)
- [Building from source](#building-from-source)
- [License](#license)

## Requirements

- Java 21 or newer
- Folia or Paper, version 1.21.x

---

## Installation

**1. Depend on the core library:**

```xml
	<repositories>
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
	</repositories>

	<dependency>
	    <groupId>com.github.SpirtySprite</groupId>
	    <artifactId>FoliaGUI-API</artifactId>
	    <version>1.0.0</version>
	</dependency
```

**2. Mark your plugin Folia-ready** — required or it won't load on Folia:

```yaml
name: YourPlugin
main: com.yourplugin.YourPlugin
version: 1.0.0
api-version: '1.20'
folia-supported: true
```

This library ships no `plugin.yml`, so it needs to end up on your plugin's classpath somehow. Two options:

1. **Shade it into your plugin jar** with the Maven Shade plugin. If you do this, relocate the `com.foliagui` package to something unique to your plugin (for example `com.yourplugin.libs.foliagui`). FoliaGUI keeps a small amount of process-wide state (the registered listener, the open-GUI registry). If two plugins on the same server both shade in an *unrelocated* copy, they will fight over that state and one of them will end up initialised against the wrong plugin instance.
2. **Ship it as its own library plugin** that calls `FoliaGUI.init(this)` in its `onEnable`, and have your other plugins depend on it via `depend` or `softdepend` in their own `plugin.yml`.

## Quick start

Initialise once, in your plugin's `onEnable`:

```java
public final class MyPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        FoliaGUI.init(this);
    }

    @Override
    public void onDisable() {
        FoliaGUI.shutdown();
    }
}
```

Build a menu and open it. Every method below is safe to call from any thread:

```java
Gui gui = Gui.builder()
        .rows(3)
        .title("&8Main Menu")
        .create();

gui.filler().fillBorder(
        ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").asGuiItem());

gui.setItem(2, 5, ItemBuilder.of(Material.DIAMOND)
        .name("&bClick me")
        .lore("&7A shiny reward")
        .glow(true)
        .asGuiItem(event -> {
            Player player = (Player) event.getWhoClicked();
            player.sendMessage("You clicked the diamond!");
        }));

gui.open(player);
```

## Core concepts

A few rules apply across the whole library:

- **One GUI instance per viewer.** A `BaseGui` is designed to be looked at by one player at a time. If you want the same menu shown to several players, build one instance per player (a small factory method is the usual pattern).
- **Item mutators are map-only.** Calling `setItem`, `addItem`, `removeItem`, or anything under `filler()` just updates an internal map. The change becomes visible the next time you call `update()` or `open()`. This is deliberate: it lets you build a whole screen's worth of items without triggering a redraw after every single call.
- **Everything else is Folia-safe.** `open`, `close`, `update`, `updateTitle`, and `updateItem` route through the scheduler automatically, so you never need to check what thread you're on before calling them.
- **Colour strings accept both formats.** Anywhere a `String` title, name, or lore line is accepted, you can use legacy `&`-codes (`&aHello`) or call the `*Mini` variant for MiniMessage tags (`<gradient:#ff0000:#0000ff>Hello</gradient>`).

## Guide

### Basic GUI

`Gui` is a plain chest-style menu, sized by row count (1 to 6):

```java
Gui gui = Gui.builder()
        .rows(3)
        .title("&8Menu")
        .onOpen(event -> player.sendMessage("Opened!"))
        .onClose(event -> player.sendMessage("Closed!"))
        .create();

gui.setItem(1, 1, ItemBuilder.of(Material.PAPER).name("&fHello").asGuiItem());
gui.addItem(ItemBuilder.of(Material.EMERALD).asGuiItem()); // fills the next empty slot
gui.removeItem(1, 1);
gui.updateItem(4, ItemBuilder.of(Material.GOLD_INGOT).build()); // pushes to viewers immediately
```

### Typed GUIs

Non-chest inventory shapes are available through `GuiType`:

```java
Gui gui = Gui.builder()
        .type(GuiType.HOPPER) // also WORKBENCH, DISPENSER, BREWING
        .title("&6A Hopper Window")
        .create();
```

### Paginated GUI

Static items placed with `setItem` stay on every page. Items added with `addPageItem` flow through whatever slots are left empty, and are paged automatically:

```java
PaginatedGui gui = PaginatedGui.builder()
        .rows(6)
        .title("&8Shop")
        .create();

gui.setItem(6, 3, ItemBuilder.of(Material.ARROW).name("&aPrevious")
        .asGuiItem(event -> gui.previous()));
gui.setItem(6, 7, ItemBuilder.of(Material.ARROW).name("&aNext")
        .asGuiItem(event -> gui.next()));

for (Material material : Material.values()) {
    if (material.isItem()) {
        gui.addPageItem(ItemBuilder.of(material).name("&f" + material.name()).asGuiItem());
    }
}

gui.open(player);          // opens on the current page
gui.open(player, 3);       // or jump straight to page 3
gui.openLastPage();        // or jump to whatever the last page turns out to be
gui.promptJumpToPage(player); // ask via chat for a page number and jump there
```

### Searchable paginated GUI

`SearchablePaginatedGui` keeps a master list of items separate from what's currently displayed, so it can filter down to whatever matches a search term:

```java
SearchablePaginatedGui gui = new SearchablePaginatedGui(6, Text.of("&8Items"), 0);

for (Material material : Material.values()) {
    if (material.isItem()) {
        gui.addSearchableItem(ItemBuilder.of(material).asGuiItem(), material.name());
    }
}

gui.setItem(6, 5, ItemBuilder.of(Material.COMPASS).name("&eSearch")
        .asGuiItem(event -> gui.promptSearch((Player) event.getWhoClicked())));
```

Clicking the compass calls `promptSearch`, which uses [chat text input](#chat-text-input) to ask for a term, then filters the list down to matches. Search `"clear"` to reset it. The matching logic is a case-insensitive substring match by default and can be overridden with `.matcher(BiPredicate<GuiItem, String>)`.

### Scrolling GUI

Unlike `PaginatedGui`, which jumps a whole page at a time, `ScrollingGui` slides its content one row or column at a time, filling whatever slots are left empty:

```java
ScrollingGui gui = ScrollingGui.builder()
        .rows(6)
        .title("&8Warps")
        .scrollType(ScrollType.VERTICAL) // or HORIZONTAL
        .create();

gui.filler().fillColumn(9,
        ItemBuilder.of(Material.BLACK_STAINED_GLASS_PANE).name(" ").asGuiItem());

warps.forEach(warp -> gui.addContent(
        ItemBuilder.of(Material.ENDER_PEARL).name("&a" + warp.name())
                .asGuiItem(event -> warp.teleport((Player) event.getWhoClicked()))));

gui.setItem(6, 9, ItemBuilder.of(Material.ARROW).name("&aScroll down")
        .asGuiItem(event -> gui.scrollNext()));

gui.open(player);
gui.scrollToEnd(); // or jump straight to the last window of content
```

### Storage GUI

A chest-like menu where players can freely deposit and withdraw items. All interaction modifiers are cleared by default, unlike a plain `Gui`, which locks everything down:

```java
StorageGui gui = StorageGui.builder()
        .rows(3)
        .title("&8Deposit box")
        .onClose(event -> {
            StorageGui closed = (StorageGui) event.getInventory().getHolder();
            persist(event.getPlayer().getUniqueId(), closed.getStoredItems());
        })
        .create();

gui.open(player);
```

### Layout filler

`gui.filler()` gives you a handful of helpers for painting decoration without touching real content slots. Each one only fills slots that are still empty:

```java
gui.filler().fill(pane);                    // every empty slot
gui.filler().fillBorder(pane);               // just the outer ring
gui.filler().fillTop(pane);                  // top row
gui.filler().fillBottom(pane);               // bottom row
gui.filler().fillRow(3, pane);
gui.filler().fillColumn(5, pane);
gui.filler().fillBetween(10, 20, pane);      // inclusive flat-slot range
gui.filler().fillCorners(pane);              // just the four corner slots

gui.filler().pattern(
        Map.of('X', border, 'O', center),
        "XXXXXXXXX",
        "X       X",
        "XXXXOXXXX");
```

### Interaction modifiers

By default, every `Gui` blocks all item movement so its layout can't be disturbed. Relax individual rules when you need to:

```java
Gui gui = Gui.builder()
        .rows(3)
        .title("&8Half-locked")
        .enableInteraction(InteractionModifier.PREVENT_ITEM_TAKE) // allow taking items out
        .create();
```

The six modifiers are `PREVENT_ITEM_PLACE`, `PREVENT_ITEM_TAKE`, `PREVENT_ITEM_SWAP`, `PREVENT_ITEM_DROP`, `PREVENT_ITEM_DRAG`, and `PREVENT_OTHER_ACTIONS`. `StorageGui` clears all of them for you.

One important detail: a slot holding a `GuiItem` (a button, a piece of border decoration) is **always** protected from being taken, swapped, dropped, or dragged, no matter what the modifiers say. The modifiers only govern truly empty slots, which is what makes `StorageGui`'s free area work while its border decoration stays put. If you want a specific `GuiItem` to be movable anyway (a claimable reward, a pre-filled trade slot), call `.editable(true)` on it.

### Click types, cooldowns, and permissions

`GuiItem` supports more than one flat action:

```java
GuiItem item = ItemBuilder.of(Material.NOTE_BLOCK).name("&dClick me").asGuiItem();

item.onLeftClick(event -> player.sendMessage("Left click"));
item.onRightClick(event -> player.sendMessage("Right click"));
item.onShiftClick(event -> player.sendMessage("Shift click"));
item.onNumberKey(event -> player.sendMessage("Hotbar swap key pressed"));

item.cooldown(20); // a fast repeat click within 20 ticks is dropped, not just delayed
item.onCooldownBlocked(event -> player.sendMessage("&cSlow down!")); // fires instead, while on cooldown

item.requirePermission("myplugin.use", denied ->
        denied.sendMessage("You don't have permission for that."));
```

### Confirmation and alert dialogs

A ready-made yes/no menu:

```java
Confirmation.builder()
        .title("&8Reset your stats?")
        .onConfirm(player -> {
            resetStats(player);
            player.sendMessage("&aDone.");
        })
        .onCancel(player -> player.sendMessage("&7Cancelled."))
        .expireAfter(20 * 10, player -> player.sendMessage("&cTimed out.")) // optional, in ticks
        .open(player);
```

`Alert` is the single-button counterpart, for a message that just needs acknowledging rather than a choice:

```java
Alert.builder()
        .title("&8Maintenance notice")
        .onAcknowledge(player -> player.sendMessage("&7Thanks for reading."))
        .open(player);
```

### Loading content asynchronously

`AsyncContent` opens a GUI immediately (so the player isn't left waiting on a frozen screen), fetches data off the main thread, then swaps it in once ready, hopping back to the player's region thread automatically:

```java
Gui gui = Gui.builder().rows(3).title("&8Leaderboard").create();

AsyncContent.load(gui, player,
        () -> fetchTopPlayersFromDatabase(), // runs off-thread
        results -> {                          // runs back on the player's region thread
            for (int i = 0; i < results.size(); i++) {
                gui.setItem(i, ItemBuilder.skull().owner(results.get(i)).asGuiItem());
            }
            gui.update();
        });
```

### Anvil text input

A virtual anvil used purely as a text prompt, with no NMS or packet libraries involved:

```java
AnvilGui.builder()
        .title("&8Name your pet")
        .text("Fluffy")
        .onComplete((player, input) -> {
            if (input.isBlank()) {
                return AnvilGui.Response.text("&cType something!");
            }
            player.sendMessage("Named: " + input);
            return AnvilGui.Response.close();
        })
        .onClose(player -> player.sendMessage("Naming cancelled"))
        .open(player);
```

`Response.text(...)` keeps the dialog open and replaces the input field, which is how you show a validation hint. `Response.keepOpen()` leaves it untouched, and `Response.close()` ends the dialog.

Add `.forceOpen(true)` to the builder if the player must submit or explicitly cancel rather than dismiss the anvil with Escape; closing it any other way reopens it automatically, the same way [force-open dialogs](#force-open-dialogs) work for regular menus.

### Sign text input

`SignGui` is the same idea as `AnvilGui` — a 4-line text prompt — but built on Paper's virtual sign packets (`Player#openVirtualSign`) instead of a fake anvil inventory. Set all 4 lines at once with `.lines(...)`, or just one with `.line(lineNumber, text)` (1-4) without touching the others:

```java
SignGui.builder()
        .line(1, "&8Type your pet's name below")
        .onComplete((player, lines) -> {
            String name = lines.get(1);
            if (name.isBlank()) {
                player.sendMessage("&cYou didn't type anything.");
                return;
            }
            player.sendMessage("Named: " + name);
        })
        .open(player);
```

A few things behave differently here than with `AnvilGui`, because a sign isn't an inventory:

- **A sign block is genuinely rendered**, faked client-side only for that one player via `sendBlockChange`, at a `Location` you can override with `.position(...)`. By default it's placed 3 blocks beneath the player's feet so solid ground blocks it from view; standing over an open cave, glass floor, or in the air will expose it. Nothing is placed in the real world, no other player ever sees it, and the original block is restored once the dialog ends.
- **Lines that come back unchanged are blanked out.** Whatever you passed to `.lines(...)` is compared against what the player submitted; a line the player left untouched comes back as `""` in the callback instead of your placeholder text, so `onComplete` only reflects what was actually typed or changed.
- **There's no reliable "player pressed Escape" signal** the way `AnvilGui` gets one from `InventoryCloseEvent`, so there's no `onClose` or `forceOpen` here. Instead, `.timeout(ticks)` (default 60 seconds, `0` disables it) auto-reverts the fake sign and drops the session if the player never submits, so it doesn't linger client-side forever.
- **Built on `@ApiStatus.Experimental` Paper API** (`UncheckedSignChangeEvent`), which may change between Paper releases.

### Chat text input

For text longer than an anvil's rename field comfortably shows, `ChatPrompt` closes whatever GUI the player has open, listens for their next chat line, and hands it to a callback:

```java
ChatPrompt.ask(player, "&eType a description, or 'cancel':", 20 * 30, input -> {
    if (input == null) {
        player.sendMessage("Timed out.");
        return;
    }
    player.sendMessage("You wrote: " + input);
});
```

The chat message is cancelled so it never reaches the server's chat log, and the callback runs back on the player's own region thread even though chat events fire off it. Pass `0` as the timeout to wait indefinitely.

### Merchant trade window

A real villager trade window, backed by `Bukkit.createMerchant`, with your own recipes instead of a real villager:

```java
MerchantGui.builder()
        .title("&2Blacksmith")
        .addRecipe(new ItemStack(Material.DIAMOND_SWORD), List.of(new ItemStack(Material.EMERALD, 5)))
        .onTrade((player, recipe) -> player.sendMessage("Thanks for your business!"))
        .onClose(player -> player.sendMessage("Come back soon."))
        .open(player);
```

Vanilla trading mechanics (taking ingredients, giving the result) run exactly as they would with a real villager. `onTrade` fires once vanilla has already applied the trade.

### Navigation and themes

`GuiNavigator` is a small per-player back-stack, so you don't have to hand-wire a "previous screen" button into every menu:

```java
GuiNavigator.open(player, nextMenu); // pushes the player's current GUI, then opens nextMenu
GuiNavigator.back(player);            // pops one level and reopens it, returns false if there's nothing to pop
```

`GuiTheme` bundles the border, back button, and close button most menus repeat, so you configure the look once:

```java
static final GuiTheme THEME = new GuiTheme()
        .border(() -> ItemBuilder.of(Material.BLACK_STAINED_GLASS_PANE).name(" ").asGuiItem());

Gui gui = Gui.builder().rows(3).title("&8Menu").create();
THEME.applyBorder(gui);
gui.setItem(3, 1, THEME.backButton());       // wired to GuiNavigator.back
gui.setItem(3, 9, THEME.closeButton(gui));   // closes this exact GUI
```

### Animation and auto-refresh

For a hand-driven animation loop tied to a GUI and its viewer:

```java
Material[] frames = { Material.WHITE_WOOL, Material.ORANGE_WOOL, Material.MAGENTA_WOOL };
int[] index = {0};

GuiAnimation.play(gui, player, 8, g ->
        g.updateItem(4, ItemBuilder.of(frames[index[0]++ % frames.length]).build()));
```

The animation stops itself automatically once the player is no longer viewing that GUI. For a simpler, ticket-based periodic refresh instead of a manual loop:

```java
gui.setUpdateInterval(20); // rebuild the inventory from the item map once a second while it's open
```

### Cycling items

A button that advances through a fixed list of values on every click, for toggles, difficulty levels, and similar settings:

```java
CycleItem<String> mode = CycleItem.of(List.of("On", "Off", "Auto"), state ->
        ItemBuilder.of(Material.LEVER).name("&bMode: " + state).build());

gui.setItem(1, 1, mode.asGuiItem(gui, Slot.of(1, 1)));
```

### Item builders

`ItemBuilder` covers the common cases (name, lore, enchants, flags, unbreakable, custom model data, glow, hide-tooltip, max stack size, durability, persistent data) and hands off to specialised builders for anything material-specific:

```java
ItemStack potion = ItemBuilder.potion(Material.SPLASH_POTION)
        .base(PotionType.STRONG_HEALING)
        .effect(PotionEffectType.SPEED, 20 * 30, 1)
        .color(Color.AQUA)
        .name("<gradient:#0ff:#00f>Zoom</gradient>") // parsed as MiniMessage via nameMini
        .build();

ItemStack head = ItemBuilder.skull()
        .owner(offlinePlayer)          // or .texture("base64...") or .textureUrl("https://...")
        .name("&fCustom Head")
        .build();

ItemStack banner = ItemBuilder.banner(Material.WHITE_BANNER)
        .pattern(DyeColor.RED, PatternType.STRIPE_BOTTOM)
        .build();

ItemStack rocket = ItemBuilder.firework()
        .power(2)
        .effect(FireworkEffect.builder().withColor(Color.LIME).with(FireworkEffect.Type.BURST).build())
        .build();

ItemStack book = ItemBuilder.book()
        .bookTitle("<gold>Rules")
        .author("<gray>The Server")
        .page("<green>Page one!")
        .build();

GuiItem button = ItemBuilder.of(Material.EMERALD)
        .name("&aBuy")
        .glow(true)
        .asGuiItem(event -> buy((Player) event.getWhoClicked()))
        .clickSound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP);

ItemStack worn = ItemBuilder.of(Material.IRON_PICKAXE)
        .damage(150)
        .maxDamage(250)
        .build();
```

### Item metadata: NBT, enchants, attributes, and everything else

Every item builder is built on `BaseItemBuilder`, which exposes essentially everything `ItemMeta` offers, not just the common cosmetic options. All of it is guarded so calling the wrong method on the wrong material is a no-op, never an exception.

Enchantments, in bulk or one at a time:

```java
ItemStack sword = ItemBuilder.of(Material.DIAMOND_SWORD)
        .enchants(Map.of(Enchantment.SHARPNESS, 5, Enchantment.UNBREAKING, 3))
        .enchant(Enchantment.MENDING, 1, false) // false = respect the enchantment's normal max level
        .build();

boolean sharp = ItemBuilder.of(sword).hasEnchant(Enchantment.SHARPNESS);
Map<Enchantment, Integer> current = ItemBuilder.of(sword).getEnchants();
```

Persistent data (an item's custom NBT), beyond the single-value `setData` you'd use for a simple tag:

```java
NamespacedKey key = new NamespacedKey(plugin, "shop-price");

ItemStack priced = ItemBuilder.of(Material.DIAMOND)
        .setData(key, PersistentDataType.INTEGER, 500)
        .build();

Integer price = ItemBuilder.of(priced).getData(key, PersistentDataType.INTEGER);

ItemBuilder.of(priced).persistentData(container -> {
    // full access to the container for anything not wrapped above, e.g. nested containers
});
```

Resource-pack identity, rarity, and enchanting behaviour:

```java
ItemStack custom = ItemBuilder.of(Material.STONE)
        .itemModel(new NamespacedKey("myplugin", "custom_stone")) // Paper's modern per-item model key
        .customModelData(data -> data.setFloats(List.of(1.0f)))    // the newer component form
        .rarity(ItemRarity.EPIC)
        .enchantable(15)
        .build();
```

Attribute modifiers (bonus attack damage, extra speed, and so on):

```java
AttributeModifier bonus = new AttributeModifier(
        new NamespacedKey(plugin, "sword-bonus"), 4.0, AttributeModifier.Operation.ADD_NUMBER);

ItemStack sword = ItemBuilder.of(Material.DIAMOND_SWORD)
        .attribute(Attribute.ATTACK_DAMAGE, bonus)
        .build();
```

The newer 1.20.5+ data components (food, tool, equippable, jukebox-playable, use-cooldown) all follow the same pattern: you get a `Consumer` that receives the current component already populated with its existing values, so you only need to touch what you're changing:

```java
ItemStack apple = ItemBuilder.of(Material.GOLDEN_APPLE)
        .food(food -> {
            food.setNutrition(20);
            food.setSaturation(10f);
            food.setCanAlwaysEat(true);
        })
        .build();

ItemStack sword = ItemBuilder.of(Material.NETHERITE_SWORD)
        .equippable(equippable -> equippable.setSlot(EquipmentSlot.HAND))
        .build();
```

A few more scattered options: `glider(boolean)`, `fireResistant(boolean)`, `damageResistant(Tag<DamageType>)`, `useRemainder(ItemStack)`, `canDestroy(Material...)` / `canPlaceOn(Material...)` for adventure-mode inventories, `repairCost(int)` on anything with an anvil repair cost, `itemName(String)` for the separate non-renameable display name, and `ItemBuilder.trim(TrimMaterial, TrimPattern)` for armor. And if something genuinely isn't wrapped, `editMeta(Consumer<ItemMeta>)` drops you straight into the raw `ItemMeta`.

### Saving and loading item stacks

`StorageGui` deliberately doesn't persist its contents for you. `ItemStackSerializer` is the other half of that:

```java
String saved = ItemStackSerializer.toBase64(storageGui.getStorageContents());
// store `saved` in a config file, a database column, wherever you like

// later, perhaps after a restart:
storageGui.setStorageContents(ItemStackSerializer.fromBase64(saved));
```

`toBase64Compact`/`fromBase64Compact` do the same job using Paper's native NBT-based `ItemStack` serialization instead of Java object serialization. The result is smaller and faster to (de)serialize; reach for it unless you need the encoded string to stay readable by older saves that already used `toBase64`:

```java
String compact = ItemStackSerializer.toBase64Compact(storageGui.getStorageContents());
storageGui.setStorageContents(ItemStackSerializer.fromBase64Compact(compact));
```

### Custom events

Alongside the per-GUI callbacks (`onOpen`, `onClick`, and so on), FoliaGUI fires three normal Bukkit events so other plugins can observe or react to menu activity without needing direct access to your GUI instances:

```java
@EventHandler
public void onGuiClick(GuiClickEvent event) {
    if (event.getGui() instanceof MyShopGui) {
        // log it, run an anti-cheat check, whatever you need
    }
}
```

`GuiOpenEvent` and `GuiClickEvent` are cancellable and mirror the outcome back onto the underlying `InventoryOpenEvent` or `InventoryClickEvent`. `GuiCloseEvent` is not cancellable, matching vanilla's own `InventoryCloseEvent`.

### GuiManager

A registry, maintained automatically, of who currently has what open:

```java
BaseGui open = GuiManager.getOpenGui(player);
boolean hasOne = GuiManager.hasGuiOpen(player);
int total = GuiManager.openCount();
List<Player> viewers = GuiManager.viewersOf(someGui);
List<BaseGui> shops = GuiManager.openGuisOfType(ShopGui.class);

GuiManager.refresh(someGui);                       // rebuild it for its current viewers
GuiManager.closeAll();                             // everything, e.g. on plugin disable
GuiManager.closeAll(gui -> gui instanceof ShopGui); // just one kind of menu

// true if the player has a Gui, an AnvilGui, a SignGui, a MerchantGui, or a ChatPrompt open, any
// of which would make it a bad time to also open a menu of your own
boolean busy = GuiManager.hasAnyScreenOpen(player);
```

A couple of matching queries live directly on `BaseGui` itself, for when you already hold the instance:

```java
List<Player> viewers = someGui.getViewerPlayers(); // just the real Player viewers, filtered from getViewers()
boolean isThisOneOpen = someGui.isOpenFor(player);
```

### Text utility

`Text` bridges legacy colour codes, MiniMessage, and Adventure components:

```java
Component fromLegacy = Text.of("&aHello");
Component fromMini = Text.mini("<gradient:#f00:#00f>Hello</gradient>");
Component label = Text.label("&aHello"); // also disables the default item-name italic

String backToLegacy = Text.toLegacy(fromMini);

Component templated = Text.of("&aHello, {player}!", Map.of("player", player.getName()));
```

### Force-open dialogs

For a dialog the player must explicitly resolve rather than dismiss with Escape:

```java
Gui dialog = Confirmation.builder()
        .title("&8You must choose one")
        .onConfirm(player -> player.sendMessage("Confirmed."))
        .onCancel(player -> player.sendMessage("Cancelled."))
        .build();
dialog.setForceOpen(true);
dialog.open(player);
```

Closing the dialog through your own code (`gui.close(player)`, which is exactly what the confirm and cancel buttons do internally) still works normally. Only a player closing it themselves (Escape, an inventory swap) triggers the reopen.

## Lifecycle

```java
FoliaGUI.init(this);          // once, in onEnable
FoliaGUI.isInitialised();     // true after a successful init
FoliaGUI.shutdown();          // closes every open GUI, clears all registries, unregisters the listener
String version = FoliaGUI.VERSION; // the library's own version, read from its packaged POM
```

`shutdown()` exists so a plugin reload framework (or a test suite) can tear the library down cleanly and call `init()` again afterwards, rather than being stuck with a permanently-initialised singleton.

Calling almost anything in this library before `init` (or after `shutdown`) throws `FoliaGUINotInitialisedException`, a descriptive subclass of `IllegalStateException`, instead of a bare, unexplained one.

If you would rather look the running instance up as a service than depend on the static class directly:

```java
FoliaGUIService service = Bukkit.getServicesManager().load(FoliaGUIService.class);
```

## Threading model

- `open`, `close`, `update`, `updateTitle`, and `updateItem` are safe to call from any thread. They route to the correct region thread internally, and take a fast path that runs immediately when you're already on the right one.
- Direct item mutators (`setItem`, `addItem`, `removeItem`, everything under `filler()`) only touch internal maps. They take effect the next time you call `update()` or `open()`.
- Click, drag, open, and close callbacks fire on the region thread that owns the acting player, so they can safely touch that player and the GUI directly.
- A single GUI instance is meant for one viewer at a time. For a menu shown to many players, build one instance per player.

## Package overview

| Package | Contents |
|---|---|
| `com.foliagui` | `FoliaGUI` entry point, `FoliaGUIService`, `FoliaGUINotInitialisedException` |
| `com.foliagui.gui` | `BaseGui`, `Gui`, `PaginatedGui`, `SearchablePaginatedGui`, `ScrollingGui`, `StorageGui`, `AnvilGui`, `SignGui`, `MerchantGui`, `ChatPrompt`, `Confirmation`, `Alert`, `AsyncContent`, `GuiManager`, `GuiNavigator`, `GuiTheme`, `CycleItem`, `GuiType`, `ScrollType`, `InteractionModifier`, `GuiFiller` |
| `com.foliagui.item` | `GuiItem`, `GuiAction` |
| `com.foliagui.event` | `GuiOpenEvent`, `GuiClickEvent`, `GuiCloseEvent` |
| `com.foliagui.builder.item` | `ItemBuilder`, `SkullBuilder`, `PotionBuilder`, `BannerBuilder`, `FireworkBuilder`, `BookBuilder`, `BaseItemBuilder` |
| `com.foliagui.builder.gui` | `SimpleGuiBuilder`, `PaginatedGuiBuilder`, `ScrollingGuiBuilder`, `StorageGuiBuilder` |
| `com.foliagui.animation` | `GuiAnimation` |
| `com.foliagui.scheduler` | `Scheduler`, `PaperFoliaScheduler`, `TaskHandle` |
| `com.foliagui.listener` | `GuiListener` (registered automatically by `init`) |
| `com.foliagui.util` | `Text`, `Slot`, `ItemStackSerializer` |

## Testing

The test suite runs against [MockBukkit](https://github.com/MockBukkit/MockBukkit) so it can exercise real click events, real inventory holders, and the Folia-style per-entity scheduler without a live server:

```bash
mvn test
```

## Building from source

```bash
mvn clean install
```

This compiles the library, runs the test suite, and installs the jar (plus sources and javadoc jars) to your local Maven repository.

## License

Do whatever you want with it.
