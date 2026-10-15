# Python planner (development)

Python 3.10 or newer; no pip packages required.

Run tests: `python -m unittest -v` from this directory.
Start: `python service.py --instance "C:\path\to\Prism\instances\Socrates Test\.minecraft"`.
Use the actual `.minecraft` folder opened by Prism's Folder button.

The service generates `config/socrates/bridge.json` with a random local token and
`config/socrates/memory.sqlite3`. Do not share either file. It listens only on
127.0.0.1:8765. Do not expose/forward that port. Stop with Ctrl+C.

Default provider is offline: supported instructions, explicit remembered facts and the authored travel-kit personal objective.
Optional PowerShell environment configuration before starting:

```
$env:SOCRATES_PROVIDER = "gemini"  # or "openai"
$env:SOCRATES_MODEL = "YOUR_AVAILABLE_MODEL_ID"
$env:GEMINI_API_KEY = "YOUR_KEY"   # or OPENAI_API_KEY for OpenAI
```

Never put keys into Java, the ZIP, a skin filename, chat, or a committed file.
Cloud mode sends your instruction, a bounded live state snapshot and up to eight remembered facts to the chosen
provider. Local mode makes no provider requests. API use can incur charges.
Use a model ID available to your own account; this project does not assume access.

Current provider adapter references:
https://ai.google.dev/api/generate-content
https://platform.openai.com/docs/api-reference/responses

Tests use mocked cloud responses. No live Gemini/OpenAI call has been verified.
SQLite exchanges are **proposals**, not claims that Minecraft completed an action.

The 0.4.0 preview adds bounded `get me 15 oak logs` requests and a personal kit/wood-reserve objective. Autonomy uses local authored decisions. SQLite mirrors observed oak outcomes; it does not make unobserved proposals into completed work.
