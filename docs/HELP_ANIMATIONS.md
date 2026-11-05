# Анимации и голоса для ситуаций помощи

Используем штатные анимации и звуки Project Zomboid; свои файлы не добавляем.
Пути — от `projectzomboid/media`.

## Анимации

| Ситуация | Штатный набор |
|---|---|
| Схватить/удерживать зомби или NPC | `AnimSets/player/grappleGrab/GrappleGrab.xml`, `player/grappleGrab.xml` |
| Тянуть за воротник, поднимать, вытаскивать | `AnimSets/player/ext/PullAtCollar*.xml`, `PullAtCollar2H/Sneak/Rifle`, `sitext/Pull_At_Colar_1H/2H.xml` |
| Тащить раненого товарища | `AnimSets/player/draggingBody-*/GrappleBwdDrag*.xml` |
| Бинтовать | `AnimSets/player/actions/Bandage*.xml` (по частям тела) |
| Толкать, бить, добивать | `AnimSets/player/shove/*`, `shoveAim/*` |
| Встать с земли | `AnimSets/player/climbwindow/getupfront|getupback.xml`; для зомби — `AnimSets/zombie/getup-*` |
| Присесть/работать | `AnimSets/player/actions/BuildKneeling.xml` |

Если точной анимации нет (разжим капкана, подъём двери), берём ближайшую
(`PullAtCollar` + `Shove`) внутри `ISBaseTimedAction`: без телепорта, скольжения,
T-позы и подмены боевого движка. Длительность и прерывание — как у штатных действий.

## Голоса

Штатные звуки: `media/scripts/generated/sounds/player/sounds_player_voice_male.txt`
и `..._female.txt`.

- базовый голос: `VoiceMale` / `VoiceFemale`;
- предупреждение/окрик: `VoiceMaleShoutHey` / `VoiceFemaleShoutHey`;
- боль: `VoiceMalePainFromHitBlunt` / `VoiceFemalePainFromHitBlunt` (и варианты Bite/Scratch/Laceration/Fall);
- усилие: `VoiceMaleMeleeAttack`, `VoiceMaleMeleeShove`, `VoiceMaleMeleeStomp`;
- шёпот: `VoiceMaleWhisperHey/Psst`;
- смерть: `VoiceMaleDeathAlone/Eaten`.

Правила: голос только вместе с текстом, не чаще текстовых пауз, не перекрывает
чужие моды, отключается настройкой «Голоса NPC». Громкость и дистанция — штатные.
