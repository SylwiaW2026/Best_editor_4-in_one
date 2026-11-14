# Native save bridge

## Architecture

The `NativeSaveBridge` JNI layer uses the upstream `d1edit` C++ core from [hooligan97/diablo_character_editor_build](https://github.com/hooligan97/diablo_character_editor_build), pinned as a Git submodule at:

`app/src/main/cpp/vendor/diablo_character_editor_build`

It calls reusable APIs, not the CLI `main()`:
- `MpqArchive::Open` and `ReadArchive(*archive, "hero", ...)` for MPQ open, internal hero extraction, and codec decode.
- `codec_encode` and `MpqWriter::WriteFile("hero", ...)` for updating the hero payload while preserving other archive entries.
- `offsetof(PlayerPack, field)` for patch offsets from the upstream packed structure.

## Raw-data guarantee

`readHero` returns decoded bytes of packed `PlayerPack` as a `ByteArray`. This is the editing session's raw baseline. `patchField` copies it and changes only a supported field's bytes; all remaining bytes, including reserved and unknown data, are retained. The UI should keep this raw snapshot and patch it rather than rebuild a struct from defaults.

Supported `patchField` names: `level`, `class`, `baseStrength`, `baseMagic`, `baseDexterity`, `baseVitality`, `statPoints`, `experience`, `gold`, `hpBase`, `maxHpBase`, `manaBase`, `maxManaBase`, `invBody`, `invList`, `invGrid`, `inventoryCount`, and `spdList`.

The item codec retains each 19-byte native `ItemPack` verbatim, including unknown item IDs and fields the editor does not understand. It reads and writes equipment, inventory records, the inventory grid/count, and belt records. Unchanged item data is byte-preserved; inventory removal/reordering remaps grid references. Catalog entries without a verified native item record are rejected during native save export rather than being written as guessed or invalid items. Native item IDs are shown as `Native item #<id>` because this repository does not include the canonical game item-name and dimension tables; adding or moving an item without verified dimensions is likewise rejected.

## Build and test notes

The Gradle module invokes CMake and the upstream repo is pinned as a submodule. Initialize it after checkout:

```bash
git submodule update --init --recursive
```

The upstream CMake defines a desktop CLI executable in addition to `d1edit`. The Android wrapper excludes the CLI target from the default build and links the library.

This integration has not yet been built with Android NDK or verified against a real `.sv`/`.hsv` in Android runtime. Before release, build supported ABIs and test read → patch → write → reread for single-player, multiplayer, Spawn, and Hellfire saves. Compare untouched raw bytes and write to a backup/copy first.

The C++ code is derived from DevilutionX and has license obligations. Retain upstream notices and review distribution compatibility.