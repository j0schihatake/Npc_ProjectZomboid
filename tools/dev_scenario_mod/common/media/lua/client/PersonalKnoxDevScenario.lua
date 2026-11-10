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
print('[PersonalKnox][dev-scenario] loaded state='..tostring(config.state)..' world='..tostring(config.world))
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
-- The window can lose focus and pause before OnGameStart; keep the dev load alive.
local earlyPulse
earlyPulse=function()
    if config.state~='loading' then Events.OnTickEvenPaused.Remove(earlyPulse);return end
    if getGameSpeed and getGameSpeed()==0 then
        pcall(function()
            setGameSpeed(1)
            if getGameTime and getGameTime() then getGameTime():setMultiplier(1) end
        end)
    end
    local demo=ISDemoPopup and ISDemoPopup.instance
    if demo then pcall(function() demo:removeSelf() end) end
end
Events.OnTickEvenPaused.Add(earlyPulse)
local function inWorld()
    local w=getWorld()
    if not w then return false end
    local mode=tostring(w:getGameMode()):lower()
    if mode~='sandbox' then return false end
    local name=tostring(w:getWorld())
    return name==config.world or (config.nonce and name:find(config.nonce,1,true)~=nil)
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
    local demo=ISDemoPopup and ISDemoPopup.instance
    if demo then demo:removeSelf();closed=closed+1 end
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
    local w=getWorld()
    print('[PersonalKnox][dev-scenario] world_check mode='..tostring(w and w:getGameMode())
        ..' name='..tostring(w and w:getWorld())..' expected='..tostring(config.world)
        ..' nonce='..tostring(config.nonce)..' match='..tostring(inWorld()))
    if not inWorld() then return end
    local current=read()
    if not current or current.nonce~=config.nonce or current.state~='loading' then return end
    setGameSpeed(1)
    local started=getTimestampMs();local ticks=0
    local visualStart,visualAt,visualDone=nil,0,false
    local npcReader=getFileReader('PersonalKnox-dev-npc.txt',false)
    local testNpc=npcReader and npcReader:readLine()==config.nonce
    local restoreNpc=testNpc and npcReader:readLine()=='own-restore'
    if npcReader then npcReader:close() end
    local restoredIds=nil
    local npcId=nil
    local npcReadyTicks=0
    local npcStarted=nil

    local nextUI=0
    local unpausedLogged=false
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
        if getGameSpeed and getGameSpeed()==0 then
            pcall(function()
                setGameSpeed(1)
                if getGameTime and getGameTime() then getGameTime():setMultiplier(1) end
            end)
            if not unpausedLogged then
                unpausedLogged=true
                print('[PersonalKnox][dev-scenario] run='..config.nonce..' auto_unpaused=true')
            end
        end
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
        if config.state~='loading' then Events.OnTickEvenPaused.Remove(poll);return end
        local player=getSpecificPlayer(0)
        if not player then return end
        if not inWorld() then Events.OnTickEvenPaused.Remove(poll);result('FAIL','world_changed');return end
        if player:isDead() then Events.OnTickEvenPaused.Remove(poll);result('FAIL','player_dead');return end
        ticks=ticks+1
        local nowMs=getTimestampMs()
        if not visualStart and nowMs-started>=6000 and PersonalKnoxAPI then
            local api=PersonalKnoxAPI.getInstance()
            local okCmd=pcall(function()
                api:ownCommandAll("FOLLOW", player:getX()+5, player:getY(), player:getZ())
            end)
            visualStart={}
            for i=0,api:ownCount()-1 do
                local id=api:ownId(i)
                local okBody,body=pcall(function() return api:ownBodyObject(id) end)
                if okBody and body then
                    visualStart[id]={x=body:getX(),y=body:getY()}
                end
            end
            visualAt=nowMs+5000
            print('[PersonalKnox][visual] follow_started command='..tostring(okCmd)
                ..' bodies='..tostring(api:ownCount()))
        elseif visualStart and not visualDone and nowMs>=visualAt then
            visualDone=true
            local api=PersonalKnoxAPI.getInstance()
            local moved=0
            for id,pos in pairs(visualStart) do
                local okBody,body=pcall(function() return api:ownBodyObject(id) end)
                if okBody and body then
                    local d=math.sqrt((body:getX()-pos.x)^2+(body:getY()-pos.y)^2)
                    if d>moved then moved=d end
                end
            end
            print('[PersonalKnox][visual] follow_moved='..string.format('%.2f',moved))
        end
        if testNpc then
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
                Events.OnTickEvenPaused.Remove(poll);result('FAIL','own_npc_spawn_failed');return
            end
            if npcReadyTicks<3 then return end
        end
        if getTimestampMs()-started<config.seconds*1000 then return end
        Events.OnTickEvenPaused.Remove(poll)
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
    Events.OnTickEvenPaused.Add(poll)
    print('[PersonalKnox][dev-scenario] run='..config.nonce..' world_started=true')
end)
