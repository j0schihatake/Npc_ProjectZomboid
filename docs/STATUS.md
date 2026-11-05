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
(`tools/launch_game.sh`), ждать меню и только потом грузить сейв.

## Минимальный тестовый запуск

`tools/launch_test.sh` временно оставляет только ZombieBuddy, Viewpoint, Knox и PersonalKnox,
запускает `-debug -nosteam` и восстанавливает `mods/default.txt` после выхода. Это не
официальный «быстрый режим» движка, но убирает лишние карты/моды из загрузки.

## 0.8.7, что проверено и что нет

- Java-ядро 0.8.7, Lua 0.8.7, JAR установлен; тесты и синтаксис Lua проходят.
- F10-спавн тел подтверждён в 0.8.4: `bodies=1`, `bodies=2`, без ошибок адаптера.
- Одежда: 0.8.7 копирует `HumanVisual` игрока; в игре ещё не подтверждено.
- `NpcReload`/`AutoReload`: убрана ошибка `Object tried to call nil` в `Util.countAmmo`.
- Причина ложных «вылетов»: запуск через `timeout 300`; загрузка сейва дольше 5 минут.
  Запускать `tools/launch_game.sh` без таймаута.
- Knox пока обязателен: своего pathfinding/боя/приказов/группы нет.
