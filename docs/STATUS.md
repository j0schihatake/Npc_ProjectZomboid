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

### 0.9.51 — сценарий OK

- Мир `PersonalKnox-scenario-496e2e4087c80793fe8c638e6750702b`.
- `npc_created=debug#1`, `ready=true`, `registered=true`, `squareMember=true`,
  `bodyType=IsoPlayer$PersonalKnoxNpc`, `model=true`, `render=true`, `culled=false`, `alpha=1.0`.
- `status=OK reason=own_npc_registered_model_saved`.
- Авторазбор в этом прогоне не запускался: путь NPC не был заблокирован.

### 0.9.53 — сценарий OK

- Мир `PersonalKnox-scenario-079559e3b4360553eb40e5a826ae5e9a`.
- `status=OK`; NPC создан и зарегистрирован, рендер/модель активны.

### 0.9.54 — сценарий OK

- Мир `PersonalKnox-scenario-f2f6b012bbbf264acb646d7efaa74320`.
- NPC создан и зарегистрирован; F11 доступен для ручной проверки разбора.

### 0.9.57 — сценарий OK

- Мир `PersonalKnox-scenario-a8b9447ac1d04b9b5ef150e7ed8a162f`.
- NPC создан/зарегистрирован; отмена разбора вдали код покрыт тестами.

### 0.9.58 — сценарий OK, F9

- Мир `PersonalKnox-scenario-3af21db35755b4fa76a8c0a584bc3155`.
- F10 создаёт NPC, F9 запускает разбор ближайшего объекта.
- Автоматическая инъекция F9 отклонена политикой окружения; проверять вручную.

### 0.9.67 — штатный запуск без аргументов

- `./tools/launch_dev.sh` без параметров прошёл сценарий:
  `OK`, мир `PersonalKnox-scenario-17d358ffad57644acf0e4cc3d438260a`.
- Скрипт запуска восстановлен из `64a6e7d` и стоит read-only; хеш JAR подтверждён в store.
- Добавлено меню «Свой NPC» по ПКМ.

### 0.9.69 — штатный запуск и отвязка от игрока

- `launch_dev.sh` без параметров: автоподтверждение через `--trust-local-builds`, сценарий `OK`,
  мир `PersonalKnox-scenario-71035bf81df2842f7c627a3e1a59f6d6`.
- Тело больше не получает `playerIndex` игрока (`-1`), поэтому не должно зеркалить движения.
- Меню своих NPC привязано к нажатому телу.

### 0.9.72 — пресет спутников проверен

- Штатный запуск: `OK`, мир `PersonalKnox-scenario-ad5802b68d6959c5af7c1ce7478504af`.
- При старте созданы `debug#1`, `debug#2`; лог:
  `[own-npc] starting companion debug#1`, `... debug#2`;
  `active=2 bodies=2`.
- Затем dev-сценарий добавил `debug#3`: `active=3 bodies=3`.

### 0.9.73 — спутники в разных клетках

- Штатный запуск: `OK`, мир `PersonalKnox-scenario-b2fc68aea2788d7fa13b501318f0661c`.
- `debug#1` 7989,11754; `debug#2` 7989,11756; `debug#3` 7990,11754.
- `active=2 bodies=2`, затем dev-сценарий добавил третьего.
- `build.py` сам регистрирует хеш PersonalKnox в approval store; диалог не появлялся.

### 0.9.74 — сценарий и меню отряда

- Штатный запуск: `OK`, мир `PersonalKnox-scenario-dc3d771807261b62e7f694b1d778709e`.
- Стартовые спутники: 2, разные клетки; меню «Свой отряд» добавлено.
- Игра выключена после проверки.

### 0.9.75 — инструменты и разные клетки проверены

- Штатный запуск: `OK`, мир `PersonalKnox-scenario-618ec713b57bc1a5eb8ff29132e73893`.
- `Own body starting tools added` для каждого спутника.
- `debug#1` 7989,11754; `debug#2` 7989,11756; dev `debug#3` 7990,11754.
- `active=2 bodies=2`, затем `active=3 bodies=3`; игра выключена.

### 0.9.79 — черты читаются Java, старт OK

- Штатный запуск: `OK`, мир `PersonalKnox-scenario-3c33c394f5479b143ca3d7132c3e45da`.
- Lua-ошибок `hasTrait/getTraits` больше нет; старт создал `debug#1`, `debug#2`,
  `active=2 bodies=2`, затем dev `debug#3`.
- Игра выключена после теста.

### 0.9.80 — старт полностью в Java

- Штатный запуск: `OK`, мир `PersonalKnox-scenario-edd1e62d7e127df857f9a949ee9cd37d`.
- `starting companions debug#1;debug#2;` — цикл и выбор числа уже в Java.
- `active=2 bodies=2`; инструменты выданы; затем dev `debug#3`.
- Игра выключена.

### 0.9.81 — команды меню в Java

- Штатный запуск: `OK`, мир `PersonalKnox-scenario-09d067e104ef9268f46ab8361b39359e`.
- Меню вызывает `ownCommand`/`ownCommandAll`; Lua только UI.
- Игра выключена.

### 0.9.82 — bootstrap в Java

- Штатный запуск: `OK`, мир `PersonalKnox-scenario-62ba8d36b012da787c747b562f571f44`.
- Лог: `bootstrap STARTED:debug#1;debug#2;`, `active=2 bodies=2`.
- Java сам решил создать стартовый отряд; игра выключена.

### 0.9.84 — старт и A-Life

- Штатный запуск: `OK`, мир `PersonalKnox-scenario-312aa70328f2e142f288264e1d1ea9dc`.
- `active=2 bodies=2`, затем dev `debug#3`; инструменты выданы.
- `alife-init squads=0` — собственная population-группа пока не входит в A-Life squads,
  это следующий разрыв интеграции; игра выключена.

### 0.9.85 — свои NPC в A-Life

- Штатный запуск: `OK`, мир `PersonalKnox-scenario-c6329a9898ee10f6bf76dceb303109be`.
- Поздний лог: `[alife] squads=3 events={NEAR_SQUAD=3}` — свои NPC попали в A-Life.
- Игра выключена.

### 0.9.86 — A-Life задачи

- Штатный запуск: `OK`, мир `PersonalKnox-scenario-02b02948f0d49ee110cf7217e16564f3`.
- `[alife] squads=3 events={NEAR_SQUAD=3}`; приказы обновляют `NpcState.Task`,
  Java переводит их в A-Life REGROUP/HOLD/SCAVENGE/RAID/TRAVEL.
- Игра выключена.

### 0.9.88 — F8 и сценарий

- Штатный запуск: `OK`, мир `PersonalKnox-scenario-eb892e55cdf0059b81ec0f8fa7f60f4e`.
- F8 печатает `ownSummary` всех своих NPC; игра выключена.

### 0.9.90 — окно F7 и сценарий

- Штатный запуск: `OK`, мир `PersonalKnox-scenario-13663746a5a7811c405d2fd2f828d386`.
- Панель отряда грузится лениво по F7; игра выключена.

### 0.9.96 — NPC одеты

- Штатный запуск: `OK`, мир `PersonalKnox-scenario-a14094b1d2647d6268f520995a9f85fe`.
- `Own body worn clothes applied` для всех трёх; на скриншоте NPC в белых рубашках,
  джинсах и обуви, с разным тоном кожи.
- Игра выключена.

### 0.9.97 — перехват текста

- Штатный запуск: `OK`, мир `PersonalKnox-scenario-7d4e94e75275b1fd978e81b8d9c73bd9`.
- `TextFix` установлен; игра выключена.

### 0.9.99 — сценарий и окно

- Штатный запуск: `OK`, мир `PersonalKnox-scenario-73560d050c59f508ab6c80394657cd20`.
- F6/F7 не доходят до окна в этом окружении (ввод не проходит в игру); код панели
  грузится лениво, ошибок в логе нет.
- Игра выключена.

### 0.10.0 — живое окно и сценарий

- Штатный запуск: `OK`, мир `PersonalKnox-scenario-80130e1c95b6080a9b7780b2e2945742`.
- Окно отряда обновляет строки раз в секунду; игра выключена.

### 0.10.1 — приказ сбора и сценарий

- Штатный запуск: `OK`, мир `PersonalKnox-scenario-3d9b44121e63f7a16fff30851b5587df`.
- ПКМ → «Собирать»: NPC идёт к ближайшему контейнеру; игра выключена.

### 0.10.3 — перенос предметов и сценарий

- Штатный запуск: `OK`, мир `PersonalKnox-scenario-40d13c723156dfe0e51a71b74c916194`.
- `findLoot`/`loot` переносят до 8 предметов; игра выключена.

### 0.10.4 — припасы после сбора и сценарий

- Штатный запуск: `OK`, мир `PersonalKnox-scenario-389d63ddada16e7aabde4205c953a988`.
- `addSupplies` обновляет припасы; игра выключена.

### 0.11.0 — Knox необязателен, сценарий OK

- Штатный запуск: `OK`, мир `PersonalKnox-scenario-d681274291582734cf805be2fe2b94f5`.
- `mod.info` больше не требует Knox; `pcall`-загрузка не сломала сценарий.
- A-Life — наш `personal.knox.alife`, не код Knox.
- Игра выключена.

### 0.11.1 — guards Knox и сценарий

- Штатный запуск: `OK`, мир `PersonalKnox-scenario-18a54f5ac31c1039325374c97b4f59b4`.
- `Barricade/BaseRoutine/Call` не падают без Knox; игра выключена.

### 0.11.2 — свой состав в player-squad

- Штатный запуск: `OK`, мир `PersonalKnox-scenario-254ec6c71d0cb2fcaea63c22ff333b38`.
- `player-squad` считает `ownActive`; игра выключена.

### 0.11.3 — HOLD/REST и сценарий

- Штатный запуск: `OK`, мир `PersonalKnox-scenario-ad3bf61094a80a106ef0fd4cb07d06f7`.
- HOLD/REST держат точку; игра выключена.

### 0.11.3 — запуск без Knox

- Временно профиль без KnoxEventExpandedNpc: `ZombieBuddy, Viewpoint, PersonalKnox`.
- Лог: `PersonalKnox Java core 0.11.3 loaded`, `Lua addon 0.11.3 loaded`;
  единственное — ожидаемый `WARN require("KnoxEventLoader") failed`.
- Игра выключена, dev-профиль с Knox восстановлен.

### 0.11.6 — панель создаётся, но не видна

- Штатный запуск: `OK`, мир `PersonalKnox-scenario-8ca8b44693a08bb33fa7bb56d6dd23b8`.
- Лог: `own-panel opened x=80 y=80 w=560 h=92`, но на экране окна нет;
  значит, проблема в отрисовке/UI-слое, а не в загрузке.
- Игра выключена.

### 0.11.7 — панель visible, но не рисуется

- Штатный запуск: `OK`, мир `PersonalKnox-scenario-40ec7dbde7baa81ea24b49cd3e575dff`.
- `[own-panel] opened ...`, `[own-panel] update visible=true`; на экране окна всё ещё нет.
- Значит, проблема в UIManager/отрисовке B42, а не в видимости флага. Игра выключена.

### 0.11.9 — панель без ошибки, но сценарий её закрывает

- Штатный запуск: `OK`, мир `PersonalKnox-scenario-def52b03152ce53257609f58a0a84a7f`.
- `[own-panel] java=true opened ...`, `update visible=true`; на скриншоте панели нет.
- Вероятно, dev-сценарий закрывает стартовые окна после OnGameStart. Игра выключена.

### 0.12.1 — панель всё ещё не видна

- Штатный запуск: `OK`, мир `PersonalKnox-scenario-5ce193d89dc19808d6028b1ae7ca542d`.
- Панель создаётся (`java=true`), `getIsVisible` true, но не рисуется; повторное
  открытие не срабатывает, потому что флаг видимости остаётся true.
- Игра выключена.

### 0.12.3 — панель создаётся без ошибки, но не рисуется

- Штатный запуск: `OK`, мир `PersonalKnox-scenario-c1184acda5e7bfe5dc022889501ee86d`.
- `[own-panel] java=true`, `update visible=true`; на экране окна нет.
- Игра выключена.

### 0.12.5 — панель пересоздаётся, но не рисуется

- Штатный запуск: `OK`, мир `PersonalKnox-scenario-2badfa10ff4cef25ca413c16431a6817`.
- Второй `[own-panel] opened ... h=164` после 5 c; на экране окна нет.
- Игра выключена.

### 0.13.4 — in-game tests 12/12

- Включён `inGameTests`, штатный запуск: `OK`, мир
  `PersonalKnox-scenario-1aeee720f725b47b08cbad79cc21a3ac`.
- Лог: `summary passed=12 failed=0`; игра выключена, тесты снова отключены.

### 0.13.5 — in-game tests 13/13

- Штатный запуск: `OK`, мир `PersonalKnox-scenario-267fe4f10ab84efc10b1199bab51339f`.
- `summary passed=13 failed=0`, включая `remove`; тестовый NPC удаляется.
- Игра выключена, тесты в ModOptions отключены.

### 0.13.6 — in-game tests 14/14

- Штатный запуск: `OK`, мир `PersonalKnox-scenario-2d695df55634356231a3e547fae9178c`.
- `summary passed=14 failed=0`, включая damage и remove; игра выключена.

### 0.13.9 — in-game tests 15/15, обувь

- `worn=Tshirt_WhiteLongSleeve,Shoes_Black,Trousers`.
- Тест нашёл отсутствие `Base.Shoes`; заменено на `Base.Shoes_Black`.
- Итог: `summary passed=15 failed=0`; игра выключена.

### 0.13.10 — один прогон тестов

- `python3 tools/run_ingame_tests.py`: включает тесты, запускает штатный скрипт,
  собирает `[ingame-test]`, пишет `build/ingame-tests.log`, закрывает игру и
  возвращает код 0/1.
- Прогон: `passed=15 failed=0`; игра выключена.

### 0.13.16 — без Knox и внешний вид

- Мир без Knox: `knox=unavailable`, `status=OK`, 16 тел; движок не падает.
- Штатный мир: игрок одет, NPC одеты, 15/15.
- В тестовом сохранении без Knox игрок и один debug-NPC были раздеты —
  это сохранение/тестовое тело, не откат Knox; штатный мир чистый.
- Игра выключена, профиль с Knox восстановлен.

### 0.13.18 — профиль без Knox, 16/16

- `profiles/no-knox` установлен в игру.
- Сценарий: `knox=unavailable`, `status=OK`.
- `summary passed=16 failed=0`, `player-clothed` PASS.
- Игра выключена.

### 0.14.0 — без loadModAfter Knox

- `mod.info` больше не ссылается на Knox.
- Профиль без Knox: `knox=unavailable`, **16/16**, игрок одет.
- Игра выключена, тесты отключены.

### 0.14.1 — 17/17 без Knox

- `classFilesInstalled` выставляется своим модом; care/upkeep снова работают без Knox.
- Профиль без Knox: `summary passed=17 failed=0`.
- Игра выключена.

### 0.14.2 — REST в меню, 18/18

- ПКМ по своему NPC: добавлены «Отдыхать» и «Состояние».
- Профиль без Knox: `summary passed=18 failed=0`.
- Игра выключена.

### 0.14.3 — диалог и 19/19

- ПКМ → «Поговорить»: `ownSpeakById`; in-game `speak` PASS.
- Без Knox: `summary passed=19 failed=0`; игра выключена.

### 0.15.0 — база отряда, 21/21

- Меню отряда: «База здесь» — сохраняет точку и ставит всех на охрану.
- Без Knox: `summary passed=21 failed=0`; игра выключена.

### 0.15.1 — бой: огнестрел/ближний

- NPC тратит патроны, урон 15/10; зарядка проверяется.
- Без Knox: 21/21; игра выключена.

### 0.15.3 — 30/30 in-game

- Профиль без Knox: `summary passed=30 failed=0`.
- Игра выключена, тесты отключены.

### 0.15.4 — урон и 34/34

- Урон зависит от оружия, состояния и навыка.
- Профиль без Knox: `summary passed=34 failed=0`.
- Игра выключена, тесты отключены.

### 0.16.0 — одежда как у игрока

- Настройка включена на прогоне: `clothes match player: 3`.
- Без Knox: 34/34; игра выключена.

### 0.17.0 — фракции и точки, 35/35

- `factions count=4`, `summary passed=35 failed=0`.
- Игра выключена, тесты отключены.

### 0.17.2 — настройки фракций

- Числа групп/точек задаются в Mod Options, не хардкодятся.
- Без Knox: 35/35; игра выключена.

### 0.18.0 — отладка боя

- Тестовый зомби создаётся (`health=100`), но in-game тест урона не сработал.
- Стабильный набор 35/35; бой с настоящим зомби — в работе.
- Игра выключена.

### 0.18.2 — 38/38

- Самообслуживание/прочность проверены в игре; игра выключена.

### 0.18.3 — стабильные 38

- Бой с настоящим зомби не подтверждён; тест исключён.
- Набор 38/38; игра выключена.

### 0.18.4 — бой с зомби подтверждён

- `own-combat target=IsoZombie damage=10.0`; `PASS combat`.
- Без Knox: 39/39; игра выключена.

### 0.18.5 — 41/41

- Командные действия отряда проверены; игра выключена.

### 0.18.7 — 43/43

- Отход лицом к противнику: основа и проверки; игра выключена.

### 0.19.1 — 43/43, тестовый зомби убирается

- Бой с зомби проходит; после теста зомби удаляется.
- Игра выключена.
