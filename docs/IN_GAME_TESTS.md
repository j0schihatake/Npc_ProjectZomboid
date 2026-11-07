# Внутриигровые тесты PersonalKnox

Набор проверяет не Java-модели, а реальный слой NPC в запущенной игре.

## Запуск

- Mod Options → PersonalKnox → **Внутриигровые тесты NPC** (автозапуск через 2 с);
- либо **F5** в мире.

## Что проверяется

1. `ownCreateCompanion` возвращает id;
2. `ownBodiesSync` создаёт тело;
3. тело реально в `square.getMovingObjects`;
4. `ownSummary` содержит id;
5. в инвентаре есть инструменты;
6. FOLLOW/HOLD/LOOT меняют приказ;
7. `ownTick` не падает;
8. `ownTargetZSnapshot` не пуст;
9. `ownBodyInspect` отвечает;
10. A-Life видит отряд.

## Формат лога

```
[PersonalKnox][ingame-test] PASS follow-order
[PersonalKnox][ingame-test] FAIL body-registered :: false
[PersonalKnox][ingame-test] summary passed=12 failed=0
```

## Когда прогонять

- после крупного пула доработок;
- перед обновлением игры;
- после смены B42/Viewpoint/Knox.

Если `failed > 0`, общий лог ошибок — `console.txt`, строки `ingame-test`.
