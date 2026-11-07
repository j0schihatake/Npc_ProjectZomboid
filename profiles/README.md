# Профили запуска

Два готовых `mods/default.txt`:

- `with-knox/default.txt` — обычный набор с Knox;
- `no-knox/default.txt` — тот же набор без `KnoxEventExpandedNpc` и RU-локализации.

Установка в игру:

```bash
python3 tools/set_profile.py with-knox
python3 tools/set_profile.py no-knox
```

Скрипт делает резервную копию текущего `default.txt` в `default.txt.bak-profile`.
`tools/launch_dev.sh` не изменяется.

Для профиля без Knox нужен полный набор карт:

```bash
./tools/launch_dev.sh --profile full --restart --build --detach --no-debug --nosteam
```
