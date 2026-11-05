# Карты — статический аудит скачанных дополнений

Дата: 2026-11-05. Игра: 42.21.0. Проверка: `python3 tools/check_map_cells.py`
и `python3 tools/check_map_cells_test.py`. Сканер читает `map.info`, заголовки
клеток, `mod.info`, зависимости, хеши Lua-файлов и тайлпаков. Он **не** доказывает
загрузку мира, работу комнат/зданий, поведение NPC и FPS.

## Включённые карты

| Карта | Клетки | Подписка |
|---|---:|---|
| Raven Creek B42 | 116 | да |
| Maplewood | 4 | да |
| Iris Eyot | 6 | да |
| Dinglespurt | 4 | да |
| **Итого** | **130** | |

Пересечений между включёнными картами нет.

## Скачанные кандидаты

Все перечисленные моды лежат в `steamapps/workshop/content/108600`, но **не
подписаны** — Steam может удалить их при следующей очистке. Перед включением
нужна подписка.

| Карта | Клетки | Статический результат | Решение |
|---|---:|---|---|
| Camden County B42 | 607 | конфликтов нет | рекомендуется |
| Falcon Ridge B42 | 38 | «Falcon Ridge» и «Falcon Ridge Road Ext» пересекаются на 2 клетках | отложить |
| Tikitown | 30 | общий путь `MapUIReplacement.lua` с Greenport, содержимое разное | исключить |
| Frogtown | 20 | конфликтов нет | рекомендуется |
| AnruisiTown | 19 | 3 клетки с Camden County | исключить |
| Constown | 18 | 1 клетка с Greenleaf | альтернатива Greenleaf |
| Haven Fall | 16 | общий путь `ALICEPacks_HotbarAttachDefinition.lua` с Iris Eyot | исключить |
| RaccoonCity B42 | 12 | конфликтов нет | рекомендуется (тематическая) |
| muldraughmilitarybaseas24 | 12 | конфликтов нет | рекомендуется |
| Greenleaf | 12 | конфликтов нет | рекомендуется |
| Greenport | 9 | общий путь `MapUIReplacement.lua` с Tikitown | исключить |
| Cathaya Valley 2.0 | 6 (+8 highway) | требует Community Tile Pack, вариант highway пересекает базу | отложить |
| RabbitHashKY | 6 | конфликтов нет | рекомендуется |
| Fort Waterfront B42 | 6 | тайлпак `tkTiles_01` (830) отличается от Community Tile Pack | исключить |
| Fort Benning B42 | 6 | конфликтов нет | рекомендуется |
| ArcadiaRVInterior | 310 | интерьеры машин, не карта мира | не относится |

Community Tile Pack (3628736763) скачан, но не подписан и не включён. Если его
включить ради Cathaya Valley, появится конфликт тайлов с Fort Waterfront B42.

## Рекомендуемый набор

Camden County, Frogtown, Greenleaf, RaccoonCity, muldraughmilitarybaseas24,
RabbitHashKY, Fort Benning B42 — **675 клеток**, вместе с текущими 130 получается
805 клеток. Пересечений клеток, коллизий Lua и тайлпаков в этом наборе нет.

RaccoonCity — тематическая карта; её можно не включать, если нужен только
обычный Knox Country. Falcon Ridge, Constown, Cathaya, Greenport, Tikitown,
Haven Fall, Fort Waterfront и AnruisiTown в набор не входят по причинам выше.

## Следующий шаг

1. Подписать выбранные моды через Steam и дождаться установки.
2. Добавить их в `mods/default.txt` и запустить **новый** мир; существующие
   сохранения не меняются.
3. Проверить загрузку, комнаты/здания, NPC, дороги и FPS по `docs/PERFORMANCE.md`.
4. Не считать статическую проверку доказательством отсутствия вылетов.
