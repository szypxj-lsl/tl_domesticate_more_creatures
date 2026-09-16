# TL Domesticate More Creatures Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rename the mod to `tl_domesticate_more_creatures` and add native taming, torpor, narcotic arrow, pet AI/friendly-fire, and optional tl_marking command integration while preserving existing level/attribute/talent/breeding features.

**Architecture:** Keep existing progression systems, migrate namespace/package, then add focused `domestication`, `torpor`, `command`, and `compat.tlmarking` units. Persist new owner/taming/torpor state in entity persistent NBT, use lazy torpor recovery, and isolate optional tl_marking references behind a guarded compatibility bootstrap.

**Tech Stack:** Java 17, Minecraft 1.20.1, Forge 47.4.10, ForgeGradle 6, Parchment 2023.09.03-1.20.1.

**Spec:** `docs/superpowers/specs/2026-08-20-tdmc-domestication-command-design.md`

## Global Constraints

- Mod ID must be `tl_domesticate_more_creatures` for all new resources, config, network, language, and registry IDs.
- Old `tl_biological_attribute_panel` is read-only compatibility; FindMe Salvation support is removed.
- No global per-tick entity scanning.
- Player-visible new text uses I18N.
- Existing level, talent, inheritance, taming-bonus, and performance fixes must be preserved.
- tl_marking is optional; core mod must load without it.

---

### Task 1: Namespace and migration foundation
- [x] Rename Java package/main class and resources/config IDs.
- [x] Add old ProgressData NBT migration and MissingMappings item remaps.
- [x] Update command root/network/creative tab/language namespaces.
- [x] Verify no unintended old namespace remains outside migration constants/docs.

### Task 2: Torpor data and formula
- [x] Add torpor to default stat definitions and configuration.
- [x] Add TorporData/TorporService with lazy recovery and unconscious latch-to-zero semantics.
- [x] Add player and non-player filter handling.
- [x] Add regression checks for formula and state transitions.

### Task 3: Narcotic effect and arrow
- [x] Register narcotic MobEffect and narcotic arrow item.
- [x] Apply 25 * amplifier-level torpor per second and pause recovery while active.
- [x] Add arrow recipe/model/lang and configurable duration/amplifier.

### Task 4: Native taming rules and ownership
- [x] Implement TamingRuleManager for annotated TOML.
- [x] Replace FindMe bridge with custom DomesticationData and unified PetOwnershipService.
- [x] Implement FEED/KNOCKOUT interactions, player locking, progress, wake reset, damage efficiency, and successful ownership.
- [x] Keep standard TamableAnimal compatibility.

### Task 5: Pet AI, breeding ownership, friendly fire
- [x] Add lightweight PetAiService target/follow overrides without deleting special attack goals.
- [x] Add FriendlyFireService with owner/projectile attribution.
- [x] Apply same-owner offspring ownership without taming bonus.

### Task 6: Snapshot/UI integration
- [x] Extend panel/inspect snapshots with torpor and taming data.
- [x] Add torpor current/max bars; add taming method/foods/progress only to inspect overlay.
- [x] Narrow and lengthen spyglass overlay.

### Task 7: Optional tl_marking command integration
- [x] Add compileOnly local tl_marking dependency and optional mods.toml entry.
- [x] Implement pet selection packets/state and Shift+middle multi-select.
- [x] Translate new/updated tl_marking pings into move/attack/manual commands on server.
- [x] Implement command persistence/termination and 64-block range.

### Task 8: Verification and packaging
- [x] Scan for old namespace and FindMe leftovers.
- [x] Validate JSON/TOML/resources and ZIP structure.
- [x] Attempt Gradle compile/build; if wrapper download remains blocked, report exact blocker and perform available static checks.
- [x] Package full source and changed-files archive.
