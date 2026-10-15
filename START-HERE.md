# Project Socrates 0.3.1 — continuation handoff

Read `PROGRESS.md`, `source/README.md`, `source/docs/TEST-RESULTS-0.3.1.md`, and
`source/docs/ARCHITECTURE.md` first. This continues the supplied 0.3.0 source;
its exact import and original evidence are preserved in Git baseline 824bd45.
Do not restart from the old 0.2 prototype.

Minecraft Java 1.20.1, Forge 47.4.10, Java 17; single-player, Windows Prism target.
Forge owns validated world actions; Python owns planning, conversation and SQLite.
Keep credentials outside source and the authenticated bridge on loopback only.
Never use the user's main modpack or survival world. Use disposable instances.

0.3.1 fixes goal scheduler phase starvation, guards every pickup against removed
items, rejects provider redirects, and returns proper errors for malformed auth.
Actual build, Python tests, engine regressions and graphical production-client
checks have been run; consult the dated result report for exact outcomes and limits.

`compiled/` includes the original 0.3.0 JAR and the new 0.3.1 production JAR.
`dev/` contains checksum-verified cloud toolchain and disposable client helpers.
The portable handoff includes complete source/evidence; the separate Git bundle
preserves commit history. No credentials, databases or test worlds belong in it.
No unattended work occurs between sessions without a persistent runner.
