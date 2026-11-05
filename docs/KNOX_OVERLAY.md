# Аудит Java/Lua-надслойки над Knox

## Что уже на Java

- `CompanionBrain`, `PlayerCare`, `UpkeepPolicy`, `ReloadPolicy`, `SupplyEligibility`,
  `WeaponChoice`, `GroupDecision`, `LineOfFire`, `ShotPriority`, `Survey`,
  `Diplomacy`, `BetrayalRisk`, `TransportPolicy`, `EvacuationPolicy`, `SurrenderPolicy`,
  `ReactionDirector`, `ThreatDirection`, `LootBag`, `PersonalKnoxAPI`.
- A-Life, задания, экономика, радиомодели, свой NPC-слой.

## Что осталось в Lua

- доступ к Knox: `KnoxEventLoader.getAPI("npc")`, `getGroup`, `sendToGroup`,
  `tellNpcFollow`, `getSafehouseNpcMembers`, поведенческие деревья;
- тела/анимации/`ISTimedAction`, контекстное меню, UI, переводы;
- циклы опроса (`Care`, `Upkeep`, `NpcReload`, `NpcEat`, ...).

## Вывод

Складывать новый Java-слой поверх старого Lua-надслоя не стоит: получится два
источника истины (Knox + наш слой). Правильный путь:

1. Java-`KnoxBridge` только для чтения группы/safehouse (переходный период).
2. Свой `OwnNpcLayer` забирает тела, приказы, бой, нужды, сохранение.
3. Lua остаётся тонким: события, меню, анимации, UI.
4. После паритета — переключатель `Knox / свои` и удаление Lua-циклов.

## Ближайшие шаги

- `KnoxBridge` (reflection, без compile-зависимости): состав группы, safehouse, spawn.
- Перевести `roster`/`members` в Java, Lua только передаёт player.
- Перевести `NpcReload`/`Upkeep` решения в Java, Lua выполняет `ISTimedAction`.
- Затем свой бой/двери/лестницы и отключение Knox.
