# Changelog

## 1.2.0 — Modernization

- Upgraded the public project version from 1.1.0 to 1.2.0.
- Restored the single-pass EMC graph propagation loop and removed a decompiler-induced nested loop.
- Replaced raw Brigadier builder casts in the EMC, knowledge, and custom EMC removal commands with typed builders.
- Preserved the original ProjectE command names, permissions, EMC semantics, and item/tag forms.
- Reduced raw FastUtil map construction in the EMC mapper and recipe pipeline.
- Kept Minecraft 26.2, Fabric API 0.161.0+26.2, Java 25, and the existing optional integrations unchanged.

This is an unofficial Fabric port of ProjectE. It contains substantial AI-assisted porting and repair work and is not reviewed or endorsed by upstream ProjectE maintainers.
