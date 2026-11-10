-- Launcher-only harness. Never enabled in the normal mod profile.
local function read()
    local r=getFileReader('PersonalKnox-dev-scenario.txt',false)
    if not r then return end
    local a={};for i=1,5 do a[i]=r:readLine() end;r:close()
    if a[1]~='PersonalKnoxDevScenario1' or not a[2] or not a[2]:match('^PersonalKnox%-scenario%-%x+$')
        or not a[3] or #a[3]~=32 or not a[3]:match('^%x+$') then return end
    local seconds=tonumber(a[4])
    if not seconds or seconds<2 or seconds>300 then return end
    return {world=a[2],nonce=a[3],seconds=seconds,state=a[5]}
end
local config=read()
if not config or (config.state~='armed' and config.state~='loading') then return end
local function state(value)
    local w=getFileWriter('PersonalKnox-dev-scenario.txt',true,false)
    w:write(table.concat({'PersonalKnoxDevScenario1',config.world,config.nonce,tostring(config.seconds),value},'\n')..'\n')
    w:close();config.state=value
end
local function result(status,reason)
    local current=read()
    if not current or current.nonce~=config.nonce then return end
    state(status)
    local w=getFileWriter('PersonalKnox-dev-scenario-result.json',true,false)
    w:write('{"nonce":"'..config.nonce..'","world":"'..config.world..'","status":"'..status..'","reason":"'..reason..'"}\n')
    w:close()
    print('[PersonalKnox][dev-scenario] run='..config.nonce..' status='..status..' reason='..reason)
end
local function inWorld()
    local w=getWorld()
    return w and w:getGameMode()=='Sandbox' and w:getWorld()==config.world
end
local function closeStartupUI()
    local closed=0
    local function hide(panel)
        if panel and panel:isVisible() then
            panel:setVisible(false)
            closed=closed+1
        end
    end
    local count=getNumActivePlayers and math.min(getNumActivePlayers(),4) or 1
    for playerNum=0,count-1 do
        local inventory=getPlayerInventory and getPlayerInventory(playerNum)
        local loot=getPlayerLoot and getPlayerLoot(playerNum)
        for _,page in pairs({inventory=inventory,loot=loot}) do
            hide(page.infoRichText) -- Inventory help popup, never arbitrary modal dialogs.
            hide(page)
        end
    end
    local guide=SurvivalGuide and SurvivalGuide.instance
    if guide and guide:isVisible() then guide:onClose();closed=closed+1 end
    if closed>0 then
        print('[PersonalKnox][dev-scenario] run='..config.nonce..' startup_ui_closed='..closed)
    end
end
if config.state=='armed' then
    Events.OnMainMenuEnter.Add(function()
        local current=read()
        if not current or current.nonce~=config.nonce or current.state~='armed' then return end
        local menu=MainScreen and MainScreen.instance
        if not menu or menu.inGame or menu.checkSavefileModal or menu.termsOfServiceDialog or menu.tutorialButton then
            result('FAIL','menu_blocked');return
        end
        state('loading')
        local ok=pcall(function()
            getWorld():setGameMode(GameMode.SANDBOX)
            getWorld():setWorld(config.world)
            MainScreen.instance:setDefaultSandboxVars()
            MainScreen.continueLatestSaveAux(false,false)
        end)
        if not ok then result('FAIL','load_failed') end

        local procGenPoll
        procGenPoll=function()
            if config.state~='loading' then Events.OnRenderTick.Remove(procGenPoll);return end
            local ui=NpcProcGenWorldMap_instance
            if not ui then return end
            local ok,generated=pcall(function()
                local proc=KnoxEventLoader and KnoxEventLoader.getAPI('procGen')
                if not proc then return false end
                if not proc:isGenerated() then
                    if not ui.controlWindow.procgenStarted then
                        ui.controlWindow.procgenStarted=true
                        ui.controlWindow:doGenerate()
                    end
                    return false
                end
                return true
            end)
            if not ok then Events.OnRenderTick.Remove(procGenPoll);result('FAIL','knox_generate_failed');return end
            if generated then
                pcall(function() ui:close() end)
                Events.OnRenderTick.Remove(procGenPoll)
            end
        end
        Events.OnRenderTick.Add(procGenPoll)

    end)
end
Events.OnGameStart.Add(function()
    if not inWorld() then return end
    local current=read()
    if not current or current.nonce~=config.nonce or current.state~='loading' then return end
    setGameSpeed(1)
    local started=getTimestampMs();local ticks=0
    local npcReader=getFileReader('PersonalKnox-dev-npc.txt',false)
    local testNpc=npcReader and npcReader:readLine()==config.nonce
    local restoreNpc=testNpc and npcReader:readLine()=='own-restore'
    if npcReader then npcReader:close() end
    local restoredIds=nil
    local npcId=nil
    local npcReadyTicks=0
    local npcStarted=nil

    local nextUI=0
    local cleanUI
    cleanUI=function()
        if not inWorld() or config.state~='loading' then
            Events.OnTickEvenPaused.Remove(cleanUI);return
        end
        local now=getTimestampMs()
        if now-started>math.max(config.seconds,30)*1000 then
            Events.OnTickEvenPaused.Remove(cleanUI);return
        end
        if now<nextUI then return end
        nextUI=now+500
        local ok=pcall(closeStartupUI)
        if not ok then
            Events.OnTickEvenPaused.Remove(cleanUI)
            result('FAIL','startup_ui_cleanup_failed')
        end
    end
    Events.OnTickEvenPaused.Add(cleanUI)
    local knoxReady=false
    local generated=false
    local nextSetup=0
    local setup
    setup=function()
        if not inWorld() or config.state~='loading' then Events.OnTickEvenPaused.Remove(setup);return end
        local now=getTimestampMs()
        if now<nextSetup then return end
        nextSetup=now+500
        if not KnoxEventLoader then
            knoxReady=true
            print('[PersonalKnox][dev-scenario] run='..config.nonce..' knox=unavailable')
            Events.OnTickEvenPaused.Remove(setup)
            return
        end
        local ok=pcall(function()
            local ui=NpcProcGenWorldMap_instance
            if ui then
                local proc=KnoxEventLoader.getAPI('procGen')
                if not proc then error('procGen unavailable') end
                if not proc:isGenerated() then
                    if not generated then
                        generated=true
                        ui.controlWindow:doGenerate()
                        print('[PersonalKnox][dev-scenario] run='..config.nonce..' knox_generate=requested')
                    end
                    return
                end
                if proc:getSurvivorCount()<=0 then error('No generated Knox survivors') end
                ui:close() -- The actual Place NPCs & Close handler exports the simulation.
                print('[PersonalKnox][dev-scenario] run='..config.nonce..' knox_export=completed')
            end
            knoxReady=true
            Events.OnTickEvenPaused.Remove(setup)
        end)
        if not ok then
            Events.OnTickEvenPaused.Remove(setup)
            result('FAIL','knox_setup_failed')
        end
    end
    Events.OnTickEvenPaused.Add(setup)
    local poll
    poll=function()
        if config.state~='loading' then Events.OnTick.Remove(poll);return end
        local player=getSpecificPlayer(0)
        if not player then return end
        if not inWorld() then Events.OnTick.Remove(poll);result('FAIL','world_changed');return end
        if player:isDead() then Events.OnTick.Remove(poll);result('FAIL','player_dead');return end
        ticks=ticks+1
        if testNpc and knoxReady then
            local ok,err=pcall(function()
                local api=PersonalKnoxAPI.getInstance()
                if not npcId then
                    npcStarted=getTimestampMs()
                    if restoreNpc then
                        local snapshot=player:getModData().PersonalKnoxOwnNpc
                        restoredIds={}
                        for line in tostring(snapshot or ''):gmatch('[^\n]+') do
                            local id=line:match('^([^|]+)|')
                            if not id then error('Invalid saved NPC row') end
                            table.insert(restoredIds,id)
                        end
                        if #restoredIds==0 then error('No saved NPCs; restore must never create a replacement') end
                        if api:ownActive()~=#restoredIds then error('Restored NPC count differs from saved snapshot') end
                        api:ownBodiesSync()
                        npcId=restoredIds[1]
                        print('[PersonalKnox][dev-scenario] npc_restored='..table.concat(restoredIds,','))
                    else
                        npcId=api:ownCreateCompanion(player)
                    end
                    print('[PersonalKnox][dev-scenario] run='..config.nonce..(restoreNpc and ' npc_restore_id=' or ' npc_created=')..tostring(npcId))
                end
                local ready=api:ownBodyReady(npcId)
                if restoredIds then
                    if api:ownActive()~=#restoredIds then error('Duplicate or missing restored NPC') end
                    for _,id in ipairs(restoredIds) do ready=api:ownBodyReady(id) and ready end
                end
                if ready then npcReadyTicks=npcReadyTicks+1 else npcReadyTicks=0 end
                if getTimestampMs()-npcStarted>15000 and npcReadyTicks<3 then
                    error(api:ownBodyInspect(npcId))
                end
            end)
            if not ok then
                print('[PersonalKnox][dev-scenario] npc_failure='..tostring(err))
                Events.OnTick.Remove(poll);result('FAIL','own_npc_spawn_failed');return
            end
            if npcReadyTicks<3 then return end
        end
        if not knoxReady or getTimestampMs()-started<config.seconds*1000 then return end
        Events.OnTick.Remove(poll)
        local ok,api=pcall(function() return PersonalKnoxAPI and PersonalKnoxAPI.getInstance() end)
        if not ok or not api or ticks<2 then result('FAIL','api_or_ticks_missing');return end
        local cleared=pcall(closeStartupUI)
        Events.OnTickEvenPaused.Remove(cleanUI)
        if not cleared then result('FAIL','startup_ui_cleanup_failed');return end
        local saved=pcall(function()
            if testNpc then
                local data=player:getModData()
                data.PersonalKnoxOwnNpc=api:ownSnapshot()
                data.PersonalKnoxOwnPopulation=api:ownPopulationSnapshot()
                data.PersonalKnoxTargetZ=api:ownTargetZSnapshot()
            end
            saveGame()
        end)
        if not saved then result('FAIL','save_failed');return end
        if testNpc then
            print('[PersonalKnox][dev-scenario] run='..config.nonce..' '..PersonalKnoxAPI.getInstance():ownBodyInspect(npcId))
        end
        result('OK',restoreNpc and 'own_npc_restored_model_saved' or (testNpc and 'own_npc_registered_model_saved' or 'world_player_api_ticks_knox_saved'))
    end
    Events.OnTick.Add(poll)
    print('[PersonalKnox][dev-scenario] run='..config.nonce..' world_started=true')
end)
