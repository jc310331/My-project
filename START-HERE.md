# Project Socrates 0.4.0-preview.1 — continuation handoff

Read `PROGRESS.md`, `source/README.md`, `source/docs/TEST-RESULTS-0.4.0-preview.1.md`, and
`source/docs/ARCHITECTURE.md` first. This continues the supplied 0.3.0 source;
its exact import and original evidence are preserved in Git baseline 824bd45.
Do not restart from the old 0.2 prototype.

Minecraft Java 1.20.1, Forge 47.4.10, Java 17; single-player, Windows Prism target.
Forge owns validated world actions; Python owns planning, conversation and SQLite.
Keep credentials outside source and the authenticated bridge on loopback only.
Never use the user's main modpack or survival world. Use disposable instances.

0.3.2 fixes goal scheduler phase starvation, guards every pickup against removed
items, rejects provider redirects, and returns proper errors for malformed auth.
Actual build, Python tests, engine regressions and graphical production-client
checks have been run; consult the dated result report for exact outcomes and limits.

`compiled/` includes the original 0.3.0 JAR and the new 0.3.2 production JAR.
`dev/` contains checksum-verified cloud toolchain and disposable client helpers.
The portable handoff includes complete source/evidence; the separate Git bundle
preserves commit history. No credentials, databases or test worlds belong in it.
No unattended work occurs between sessions without a persistent runner.

0.3.2 adds the MCA-style right-click interaction menu and owner-scoped packets.
See source/docs/TEST-RESULTS-0.4.0-preview.1.md for this release. The previous 0.3.1 report
and artifacts remain historical evidence; user reports its PC smoke test looked good.

The new capability preview adds shared inventory/gear, bounded oak harvesting, basic recipe-validated kit crafting and an authored personal objective. It is not full autonomous survival. Read source/docs/PLAYER2-EVALUATION.md before expanding broad mining/building: matching Forge branches may provide a better backend, subject to runtime, licensing and pricing checks.
