# Map Detail Shop Editor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use subagent-driven development or executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove `ShopEditTool` and move shop editing into the FPSMatch map detail screen, opening shops by explicit map/team identity instead of `ItemStack` NBT.

**Architecture:** The map detail UI becomes the only shop editor entry point. Server-side services expose editable shop teams for a map and open the editor after permission and capability validation. The editor container stores `gameType`, `mapName`, and `teamName` directly, so no shop editing path depends on an item stack.

**Tech Stack:** Minecraft Forge 1.20.1, Java 17, Forge menus/screens, FPSMatch packet register, Modern UI screen patterns already used by map selection screens.

---

## Files and Responsibilities

- `FPSMatch/src/main/java/com/phasetranscrystal/fpsmatch/common/item/ShopEditTool.java`
  - Delete this file.
- `FPSMatch/src/main/java/com/phasetranscrystal/fpsmatch/common/item/FPSMItems.java` or existing item register file that declares `SHOP_EDIT_TOOL`
  - Remove the shop edit tool registry object and imports.
- `FPSMatch/src/main/java/com/phasetranscrystal/fpsmatch/common/client/screen/EditorShopContainer.java`
  - Replace the `ItemStack` constructor model with explicit `gameType`, `mapName`, `teamName` fields.
  - Keep item slot editing behavior intact.
- `FPSMatch/src/main/java/com/phasetranscrystal/fpsmatch/common/client/screen/EditorShopScreen.java`
  - Read explicit shop identity from the container and fix hover/click coordinate handling if needed.
- `FPSMatch/src/main/java/com/phasetranscrystal/fpsmatch/common/client/screen/mapselect/FPSMMapDetailScreen.java`
  - Add an edit shop button or unsupported message entry point.
- `FPSMatch/src/main/java/com/phasetranscrystal/fpsmatch/common/client/screen/mapselect/FPSMMapShopScreen.java`
  - Create this screen to list editable shop teams for the selected map.
- `FPSMatch/src/main/java/com/phasetranscrystal/fpsmatch/common/mapselect/MapRoomQueryService.java`
  - Add methods to list shop-capable teams and check whether a selected map supports shop editing.
- `FPSMatch/src/main/java/com/phasetranscrystal/fpsmatch/common/packet/mapselect/EditableShopInfo.java`
  - Create this record for network/screen data: `gameType`, `mapName`, `teamName`, `displayName`.
- `FPSMatch/src/main/java/com/phasetranscrystal/fpsmatch/common/packet/shop/OpenShopEditorC2SPacket.java`
  - Replace or supersede `OpenEditorC2SPacket`; carries explicit `gameType`, `mapName`, `teamName`.
- `FPSMatch/src/main/java/com/phasetranscrystal/fpsmatch/FPSMatch.java`
  - Register the new packet and remove the old open-editor packet if unused.
- `FPSMatch/src/main/resources/assets/fpsmatch/lang/en_us.json`
- `FPSMatch/src/main/resources/assets/fpsmatch/lang/zh_cn.json`
  - Add labels for edit shop button, unsupported message, empty shop list, invalid shop messages.

---

## Task 1: Data model and server query helpers

**Files:**
- Create: `FPSMatch/src/main/java/com/phasetranscrystal/fpsmatch/common/packet/mapselect/EditableShopInfo.java`
- Modify: `FPSMatch/src/main/java/com/phasetranscrystal/fpsmatch/common/mapselect/MapRoomQueryService.java`

- [ ] Add `EditableShopInfo` record with buffer encode/decode.

```java
package com.phasetranscrystal.fpsmatch.common.packet.mapselect;

import net.minecraft.network.FriendlyByteBuf;

public record EditableShopInfo(String gameType, String mapName, String teamName, String displayName) {
    public static EditableShopInfo decode(FriendlyByteBuf buf) {
        return new EditableShopInfo(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(gameType);
        buf.writeUtf(mapName);
        buf.writeUtf(teamName);
        buf.writeUtf(displayName);
    }
}
```

- [ ] Add query helpers in `MapRoomQueryService`.

Expected behavior:
- `listEditableShops(String gameType, String mapName)` returns all teams whose team capability exposes an initialized `ShopCapability`.
- `supportsShopEditing(String gameType, String mapName)` returns true when the list is non-empty.
- Results are stable-sorted by display name/team name.
- If FPSMCore is not initialized or the map is missing, return an empty list.

- [ ] Run targeted compile.

```powershell
.\gradlew.bat compileJava --console=plain
```

Expected: Gradle reports `BUILD SUCCESSFUL`.

---

## Task 2: Rewrite shop editor container away from ItemStack

**Files:**
- Modify: `FPSMatch/src/main/java/com/phasetranscrystal/fpsmatch/common/client/screen/EditorShopContainer.java`
- Modify: `FPSMatch/src/main/java/com/phasetranscrystal/fpsmatch/common/client/screen/EditorShopScreen.java`

- [ ] Replace the server constructor with explicit identity.

Required server constructor shape:

```java
public EditorShopContainer(int windowId, Inventory playerInventory, String gameType, String mapName, String teamName)
```

- [ ] Replace the client buffer constructor to read identity strings.

Required client constructor shape:

```java
public EditorShopContainer(int windowId, Inventory playerInventory, FriendlyByteBuf data)
```

It must read:

```java
this(windowId, playerInventory, data.readUtf(), data.readUtf(), data.readUtf());
```

- [ ] Replace every `ShopEditTool.getShop(itemStack)` dependency with a private method that resolves shop by `gameType`, `mapName`, `teamName`.

Resolution rules:
- Use `MapRoomQueryService.findMap(gameType, mapName)`.
- Find the matching team by name.
- Read team capability.
- Return only initialized shop capability.
- If missing, expose an empty shop state and avoid crashes.

- [ ] Keep existing slot save behavior intact.

- [ ] In `EditorShopScreen`, display the explicit team/shop identity in title/subtitle if existing style allows it.

- [ ] Fix hover coordinate logic so slot hit testing compares screen-space mouse coordinates against screen-space slot bounds.

- [ ] Run compile.

```powershell
.\gradlew.bat compileJava --console=plain
```

Expected: Gradle reports `BUILD SUCCESSFUL`.

---

## Task 3: Add explicit open-shop packet

**Files:**
- Create or replace: `FPSMatch/src/main/java/com/phasetranscrystal/fpsmatch/common/packet/shop/OpenShopEditorC2SPacket.java`
- Modify: `FPSMatch/src/main/java/com/phasetranscrystal/fpsmatch/FPSMatch.java`
- Remove if unused: `FPSMatch/src/main/java/com/phasetranscrystal/fpsmatch/common/packet/shop/OpenEditorC2SPacket.java`

- [ ] Create packet fields: `gameType`, `mapName`, `teamName`.

- [ ] Encode/decode all three strings with `FriendlyByteBuf#writeUtf` and `readUtf`.

- [ ] In `handle`, enqueue server work and validate:
  - sender is not null
  - sender has permission level 2 or matches existing map-management permission convention
  - map exists
  - selected team exists
  - selected team has initialized shop capability

- [ ] On validation failure, send `Component.translatable("message.fpsm.shop_editor.invalid")` or a more specific translation key to the player.

- [ ] On validation success, call `NetworkHooks.openScreen` using:

```java
new SimpleMenuProvider(
    (windowId, inv, p) -> new EditorShopContainer(windowId, inv, gameType, mapName, teamName),
    Component.translatable("gui.fpsm.shop_editor.title")
)
```

and write buffer strings in the same order:

```java
buf.writeUtf(gameType);
buf.writeUtf(mapName);
buf.writeUtf(teamName);
```

- [ ] Register `OpenShopEditorC2SPacket` in `FPSMatch#registerPackets` and remove `OpenEditorC2SPacket` registration when no longer used.

- [ ] Run compile.

```powershell
.\gradlew.bat compileJava --console=plain
```

Expected: Gradle reports `BUILD SUCCESSFUL`.

---

## Task 4: Add shop editing entry to map detail screen

**Files:**
- Modify: `FPSMatch/src/main/java/com/phasetranscrystal/fpsmatch/common/client/screen/mapselect/FPSMMapDetailScreen.java`
- Create: `FPSMatch/src/main/java/com/phasetranscrystal/fpsmatch/common/client/screen/mapselect/FPSMMapShopScreen.java`
- Modify language files:
  - `FPSMatch/src/main/resources/assets/fpsmatch/lang/en_us.json`
  - `FPSMatch/src/main/resources/assets/fpsmatch/lang/zh_cn.json`

- [ ] Add an `Edit Shop` button on map detail screen near existing management/settings buttons.

Button behavior:
- If the current map has one or more editable shops, open `FPSMMapShopScreen`.
- If the current mode/map has no editable shop teams, either hide the button or open `FPSMMapShopScreen` showing unsupported text. Prefer showing the button only when shop editing is supported if the data is available locally; otherwise show the unsupported screen.

- [ ] Create `FPSMMapShopScreen` following existing mapselect screen style.

Screen content:
- Title: `gui.fpsm.map_shop.title`
- Subtitle: selected game type and map name
- List rows for `EditableShopInfo`
- Each row has team display text and an edit button
- Empty state text: `gui.fpsm.map_shop.unsupported`
- Back button returns to `FPSMMapDetailScreen`

- [ ] On edit button click, send `OpenShopEditorC2SPacket(gameType, mapName, teamName)`.

- [ ] Add translation keys:

```json
"gui.fpsm.map_detail.edit_shop": "Edit Shop",
"gui.fpsm.map_shop.title": "Edit Shop",
"gui.fpsm.map_shop.unsupported": "This mode does not support shop editing.",
"gui.fpsm.map_shop.edit": "Edit",
"message.fpsm.shop_editor.invalid": "Selected shop is no longer available."
```

Chinese equivalents:

```json
"gui.fpsm.map_detail.edit_shop": "编辑商店",
"gui.fpsm.map_shop.title": "编辑商店",
"gui.fpsm.map_shop.unsupported": "该模式不支持编辑商店。",
"gui.fpsm.map_shop.edit": "编辑",
"message.fpsm.shop_editor.invalid": "所选商店已不可用。"
```

- [ ] Run compile.

```powershell
.\gradlew.bat compileJava --console=plain
```

Expected: Gradle reports `BUILD SUCCESSFUL`.

---

## Task 5: Delete ShopEditTool and old ItemStack open path

**Files:**
- Delete: `FPSMatch/src/main/java/com/phasetranscrystal/fpsmatch/common/item/ShopEditTool.java`
- Modify: item registry file containing `SHOP_EDIT_TOOL`
- Modify: any creative tab/lang/model references for shop edit tool
- Delete if unused: `FPSMatch/src/main/java/com/phasetranscrystal/fpsmatch/common/packet/shop/OpenEditorC2SPacket.java`

- [ ] Remove the shop edit tool registry entry.

- [ ] Remove all imports/references to `ShopEditTool`.

Search command:

```powershell
rg "ShopEditTool|SHOP_EDIT_TOOL|shop_edit_tool|OpenEditorC2SPacket|writeItem\(|readItem\(" FPSMatch/src/main/java FPSMatch/src/main/resources -n
```

Expected after cleanup:
- No `ShopEditTool` references.
- No `SHOP_EDIT_TOOL` references.
- No shop editor open path based on `writeItem/readItem`.

- [ ] Delete stale asset/lang/model entries if they only existed for the removed tool.

- [ ] Run compile.

```powershell
.\gradlew.bat compileJava --console=plain
```

Expected: Gradle reports `BUILD SUCCESSFUL`.

---

## Task 6: Full verification and repository updates

**Files:**
- No new feature files unless needed for fixes found during verification.

- [ ] Run full build.

```powershell
.\gradlew.bat build --console=plain
```

Expected: Gradle reports `BUILD SUCCESSFUL`.

- [ ] Run focused scans.

```powershell
rg "ShopEditTool|SHOP_EDIT_TOOL|shop_edit_tool|OpenEditorC2SPacket|EditorShopContainer\(windowId, inv, player.getMainHandItem\)|writeItem\(|readItem\(" FPSMatch/src/main/java FPSMatch/src/main/resources -n
```

Expected:
- No removed shop edit tool references.
- No item-stack based shop editor open path.

- [ ] Run diff whitespace check.

```powershell
git -C FPSMatch diff --check
git diff --check
```

Expected: no whitespace errors.

- [ ] Commit FPSMatch changes.

```powershell
git -C FPSMatch add src/main/java src/main/resources
git -C FPSMatch commit -m "feat: edit shops from map detail screen"
git -C FPSMatch push origin master
```

- [ ] Commit BlockOffensive submodule pointer.

```powershell
git add FPSMatch
git commit -m "chore: update fpsmatch shop editor flow"
git push origin master
```

---

## Self-Review

- Spec coverage: Plan deletes `ShopEditTool`, exposes editing from the room/map detail screen, only enables editing for shop-capable teams, shows unsupported UI when no shops exist, and removes `ItemStack`-based editor opening.
- Placeholder scan: No TBD/TODO placeholders remain.
- Type consistency: `EditableShopInfo`, `OpenShopEditorC2SPacket`, and `EditorShopContainer` consistently use `gameType`, `mapName`, `teamName`.
