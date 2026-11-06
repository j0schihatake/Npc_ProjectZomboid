# Состояние на 0.8.7

## Проверено запуском (console.txt, 05.11)

- Java-ядро загружено:
  `[PersonalKnox] Java core 0.6.1 loaded; directional world reactions ready.`
- Lua-адаптеры загружены:
  `[PersonalKnox] Lua addon 0.6.1 loaded; Russian dialogues and elevation diagnostics ready.`
- ZombieBuddy добавил JAR в classpath, `PersonalKnoxAPI` экспортирован в Lua.
- Игра дошла до главного меню (`OnMainMenuEnter`), краша и `GameThread exited` в этом запуске нет.
- Ошибок PersonalKnox в console.txt нет; есть только сторонние ERROR шрифтов и FluidContainer.
- Мод не отключался и не конфликтовал с Viewpoint/RealLimbs (25 Java-модов в compat-строке).

Ещё не проверено в мире: NPC-еда, автоперезарядка, режимы патронов, самообслуживание,
A-Life события, задания и сумки — нужен загруженный сейв и игровые сценарии.

## PersonalKnox 0.6.1

- Java-ядро 0.6.1, Lua 0.6.1, JAR пересобран.
- Модели и тесты: NPC/A-Life, фракции, экономика, ресурсные точки и истощение,
  базы 1–7, охрана, набеги 10–30, штурм, кража, задания (журнал/доска/доставка/диалог),
  сопровождение, специалисты, радист/эфир/перехват, поездки, сумки ресурсов и станки.
- Сумки: MEDICAL/FOOD/AMMO, до 6 единиц, сдача в экономику; 3 станка, крафт только
  работником профессии.
- Настройки: 22, включая размер набега 10–30 и частоту.

## PersonalWorldGen

- Профиль карт, `ProtectedCells`, `SitePlanner`, `GenerationQueue`, `WorldGenSession`,
  `CampPlanner`, `RoadPlanner`, `BuildingPlanner`/`BuildingPlan`, `PlanValidator`.
- `WorldGenSession.planSettlement` проверяет лагерь+дом+дорогу и ставит шаги в очередь.
- Тесты: защищённые клетки, поиск, очередь, пауза, профили, бюджет времени, поселение.
- Игровой конвейер (реальные `BuildingDef`/`RoomDef`) ещё не подключён.

## Известные проблемы

- FPS в тяжёлой сцене упирается в GPU Viewpoint: тени/occlusion/gbuffer/sky ~70 мс;
  наш Lua — максимум ~9 мс.
- Сторонний `AutoReload` ещё включён; отключить после проверки нашей версии.
- `SPNCC` отключён в default и сохранениях, резервные копии `*.bak-spncc`.
- Ошибки шрифтов `mainfont_0.png` и FluidContainer — не наши, но шумят в логе.

## Следующие шаги

1. Загрузить сейв с 0.6.1 и проверить `[npc-eat]`, `[npc-care]`, `[reload]`, `[perf]`, сумки.
2. Подключить мирген к реальным `BuildingDef`/`RoomDef` и commit в мир.
3. Реализовать окна заданий/диалога и живой эфир (модели уже есть).
4. После проверки отключить сторонний `AutoReload`.

## Запуск без таймаута

Загрузка B42 с 1497 NPC, 798 группами и множеством карт может идти больше 5 минут.
`timeout 300 ./projectzomboid.sh` убивает игру посреди загрузки — в логе это выглядит
как «вылет» без `hs_err` и без `Exception`. Запускать без `timeout`
через `tools/launch_dev.sh --restart --build --detach`: он сам грузит тестовый мир.
Его `--scenario-timeout` ограничивает ожидание результата, а не жизнь игры.

## Минимальный тестовый запуск

Использовать `tools/launch_dev.sh`.
Скрипт включает моды в отдельном dev-cache, загружает совместимую копию мира,
проходит старт/Knox и закрывает стартовые окна. Успех — код 0 и `scenario_ok`.
`tools/launch_test.sh` вызывает тот же launcher, обычный default.txt не заменяется.
Полная инструкция: [DEEPSEEK_DEV_LAUNCH.md](DEEPSEEK_DEV_LAUNCH.md).

## 0.8.7, что проверено и что нет

- Java-ядро 0.8.7, Lua 0.8.7, JAR установлен; тесты и синтаксис Lua проходят.
- F10-спавн тел подтверждён в 0.8.4: `bodies=1`, `bodies=2`, без ошибок адаптера.
- Одежда: 0.8.7 копирует `HumanVisual` игрока; в игре ещё не подтверждено.
- `NpcReload`/`AutoReload`: убрана ошибка `Object tried to call nil` в `Util.countAmmo`.
- Причина ложных «вылетов»: запуск через `timeout 300`; загрузка сейва дольше 5 минут.
  Для текущей проверки использовать автоматический `tools/launch_dev.sh`, описанный выше.
- Knox пока обязателен: своего pathfinding/боя/приказов/группы нет.

## Dev-сценарий и F10 (проверено 06.11)

- `tools/launch_dev.sh --detach` теперь сам проходит dev-сценарий; только меню — `--menu-only`.
- F10 в игре создаёт своих NPC:
  `[PersonalKnox] Own body visual copied from player`,
  `[own-debug] id=debug#1 bodies=1`, `debug#2 bodies=2`.
- Тела используют внешность игрока; позиции разносятся по сетке 0.9 м.
- В dev-профиле сценарий завершился `status=OK reason=world_player_api_ticks_knox_saved`.
- Прежние случаи пустого default.txt исправлены: launcher создаёт маркер первого запуска B42
  и каждый раз готовит профиль, включая служебный dev-сценарий. Ручная расстановка галочек не требуется.

## Dev-сценарий 0.9.31 — OK (06.11)

- `tools/launch_dev.sh` без параметров: собрал JAR, перезапустил игру, прошёл сценарий.
- Результат:
  `OK: Sandbox loaded, living player, PersonalKnox API and game updates verified.`
- Мир: `PersonalKnox-scenario-4fc588c30e7e391e2a9d6cfecf846e40`
- В логе: `Java core 0.9.31`, `Lua addon 0.9.31`.
- `KnoxBridge` работает: `knox=ok world=0 meta=1645 safehouses=843`.
- `OwnNpc`: `members=0 active=0 bodies=0` — в сценарии нет группы игрока, тел нет.
- F10 в сценарии не проверен: команда ввода была прервана.


## Повторная проверка 0.9.31 после исправления NPC

Первичный OK мира `PersonalKnox-scenario-4fc588c30e7e391e2a9d6cfecf846e40`
подтверждал загрузку, но сопровождался исключениями OwnNpc.lua:32: undefined pz.
Исправлено в 0bd38a9. Верификатор теперь отклоняет такие исключения и не считает
сырой Lua OK достаточным.

Повторный запуск обычным `tools/launch_dev.sh` прошёл в мире
`PersonalKnox-scenario-223bffcee59710204d2c88aedd51a469`: Java/Lua 0.9.31,
закрыто стартовое окно (остальные панели уже скрыты), Knox сгенерирован/
экспортирован, копия сохранена, код launcher 0. Исключений PersonalKnox в журнале
этого прогона нет, хеши JAR совпадают с записанными при запуске. Состав/бой
собственных NPC этим сценарием не проверялись — собственный отряд пуст.

### F10 в сценарии 0.9.31

- В живом мире F10 дважды создал тела:
  `Own body visual copied from player`, `own-debug id=debug#1 bodies=1`, `debug#2 bodies=2`.
- Ошибок `Own body`/`dress failed` нет; игра осталась на паузе, поэтому камера показывает только игрока.

### 0.9.33 — тела остаются, сценарий OK

- `Own body debug#1 at 7990.0,11756.0,0.0`, `debug#2 at 7991.0,11756.0,0.0`.
- `bodies=1`, затем `bodies=2`; `Own body visual copied from player` дважды.
- Сценарий: `OK: Sandbox loaded...`, мир `PersonalKnox-scenario-e8cf63eb82d2241ad2ef165e753fad9c`.

### 0.9.34 — сценарий OK

- `OK: Sandbox loaded...`, мир `PersonalKnox-scenario-56591c2d1265f8fb8d40730d2aea4708`.
- `setVisible(true)` добавлен; F10 в этом прогоне не сработал из-за паузы/открытого окна персонажа.

### Пауза и видимые тела

- В настройках игры отключена пауза при сворачивании: **F2 нужен только если на экране видна плашка «ИГРА ПРИОСТАНОВЛЕНА»**.
- 0.9.34: после снятия паузы и двух F10 в мире видно тело своего NPC у дома (синяя кепка, одежда скопирована с игрока).
- `setVisible(boolean)` у `IsoPlayer` отсутствует (`NoSuchMethodException`); вызов убран в 0.9.35.

### Viewpoint: O/Щ

- При тестах переключать режим Viewpoint клавишей **O** (в русской раскладке **Щ**), туда и обратно.
- В первом лице отдельно проверять, что вид не ломается, камера не проваливается и FPS не падает.
- Автоматические нажатия в текущем окружении не доходят до окна игры; переключение проверять вручную.

### 0.9.40 — сценарий и отображение

- `OK: Sandbox loaded...`, мир `PersonalKnox-scenario-25424a7f05a7deabe94657cccda6ea61`.
- `[dev-scenario] id=debug#1 ... bodyType=zombie.characters.IsoPlayer$PersonalKnoxNpc model=true render=true culled=false alpha=1.0`
- Это подтверждает подкласс IsoPlayer, проходящий фильтр изометрии; FPS/изометрия проверены Codex.
