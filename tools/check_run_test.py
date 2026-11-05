from check_run import summarize

log = '''
[PersonalKnox] Java core 0.4.6 loaded; directional world reactions ready.
[PersonalKnox] Lua addon loaded; Russian dialogues and elevation diagnostics ready.
[PersonalKnox][capabilities] luaNpcTransferAction=false luaBehaviorNode=true luaSubTreeNode=true
[PersonalKnox][care] action=bandage ok=true bleeding=1 pain=10.0 thirst=0.10 hunger=0.20
[PersonalKnox][upkeep] ammo=Base.Bullets9mm add=30
[PersonalKnox][probe] npc=3129 windows=12
'''
report = summarize(log)
assert report['core'] == '0.4.6'
assert report['lua'] and report['capabilities'] == 1
assert report['care'] == 1 and report['upkeep'] == 1 and report['probe'] == 1
assert not report['errors'] and not report['exited']
bad = summarize('[PersonalKnox] Java core 0.4.5 loaded\n[PersonalKnox] Care error: boom')
assert bad['core'] == '0.4.5' and bad['errors']
print('Run-report checks passed: version, Lua, capabilities, care, upkeep, probe, errors.')
