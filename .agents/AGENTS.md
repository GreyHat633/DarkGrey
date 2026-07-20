# DarkGrey Modding Project Rules

## 1. Storage and Downloads
- The user's C drive is practically full and must not be used for downloading or installing new files, libraries, or caches.
- Whenever you need to download files, or install Python packages via `pip install`, you **MUST** target `E:\Java\VibeCoding\ATTACH`.
- For `pip install`, you MUST override the temp directory to prevent C drive exhaustion, e.g. in PowerShell: `$env:TEMP="E:\Java\VibeCoding\ATTACH\tmp"; $env:TMP="E:\Java\VibeCoding\ATTACH\tmp"; pip install --target E:\Java\VibeCoding\ATTACH --cache-dir E:\Java\VibeCoding\ATTACH\pip_cache <package>`
- In Python scripts, add `sys.path.insert(0, r"E:\Java\VibeCoding\ATTACH")` to use the installed packages.
- Always set environment variables for models or caches to point inside `E:\Java\VibeCoding\ATTACH` (e.g., `os.environ["U2NET_HOME"] = r"E:\Java\VibeCoding\ATTACH\.u2net"`).

## 2. Language and Artifacts
- All implementation plans (`implementation_plan.md`) and task-related artifacts MUST be written in Chinese (中文).
