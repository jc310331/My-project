# Socrates 0.3.2 — 2026-10-15

## Build and automated checks

Forge 47.4.10, Minecraft 1.20.1, Temurin Java 17.0.16+8, Gradle 8.8.
`bash dev/gradle-cloud.sh build runGameTestServer -PbridgeTests -x downloadAssets`
completed successfully; **13 required actual Forge engine tests passed**.
`python3 -m unittest discover -s source/ai -v`: **18 tests passed**.
Evidence: `evidence/0.3.2/validation-topics.log`, `python-final.log`.

Four new engine tests cover owner actions without OP using a survival player,
server ownership/dimension/range/payload rejection, exact unload counts and repeat
unload without duplication, talk validation and local skin path validation. The
Python addition checks that topic discussion never creates a world-action goal.
Existing authenticated bridge/memory, navigation and collection regressions pass.
An earlier test incorrectly used a creative mock player and failed its fixture
assertion. It was corrected to a non-OP survival mock; failed logs are preserved.

## Installed production client

Disposable Linux Forge client at `/workspace/socrates-client`, dummy Xorg :99,
Mesa software OpenGL; no user PC/world access. Installed final production JAR
SHA-256: `3ec6c7beb726a589351aaddabdec2ad433c3af5bf99c86515c1f9039ea9df267`. Its hash matches the release artifact.
Final session log: `evidence/0.3.2/production-release.log`.

Verified by mouse/keyboard and screenshots on this final JAR:

- Saved test world reopened, Socrates/equipment/skin present; right-click owner menu opens.
- Talk / Topics page, ability reply via asynchronous Java/Python bridge.
- Remember a fact prefills the message field; typing and Enter saves the whole
  fact. Closing/reopening and choosing Memories recalls it from SQLite.
- Topic layout remains usable at 1280x480 (GUI scale 2), with scrolling transcript.
- Toss one oak log using Q; Collect drops gathers it. Status automatically changes
  Items 0 to 1 without extra action messages. Unload returns the log to the owner,
  count returns to zero; hotbar returns from two to three. Stay cancels task.
- World save confirmed all dimensions saved before client shutdown.

Earlier 0.3.2 production builds also exercised follow/stay, set home, resident,
explore, autonomy on then Stay/off, appearance PNG application and username
restoration, empty unload, normal and compact appearance screens. Those tests
preceded the final quiet-refresh/topic refinements; they are not claimed as
repeated on the final artifact. Screenshots/logs retained. A text focus bug that
dropped initial words after clicking Talk was reproduced and fixed.

## Scope and limits

MCA: Conversations 1.8.0 was inspected statically as a user reference, never run.
See CONVERSATION-REFERENCE.md. Topics and offline replies are independent original
work. No MCA dependency, reference code, assets or dialogue text were added.
Fact memory persists; screen transcript does not survive closing the screen.
Offline topics are small scripted responses, not general AI. Open-ended cloud
providers still need credentials; no live Gemini/OpenAI requests were tested.
Branching conversations, relationship progression, moods and gossip are planned.

User reports their previous PC test looked good; that does not verify this new
release or establish its exact modpack composition. Windows Prism, Create 6.0.8,
Embeddium and Oculus compatibility remain unverified for 0.3.2. Full animations,
complex terrain and long autonomy remain unverified. Collect still means dropped
items only; no mining/crafting/building/combat/Create machinery support.
Goal rows still record proposals, not completed work.
