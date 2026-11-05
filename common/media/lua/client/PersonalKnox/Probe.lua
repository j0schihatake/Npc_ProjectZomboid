-- Developer-only, one-shot diagnostics for a verified copy. Inert without an arm file.
local Probe = {}
local FILE = "PersonalKnox-probe.txt"
local MAGIC = "PersonalKnoxProbe1"

local function lines(path, count)
    if not getFileReader then return nil end
    local reader=getFileReader(path,false)
    if not reader then return nil end
    local result={}
    for i=1,count do
        local line=reader:readLine()
        if line==nil then break end
        result[i]=line
    end
    reader:close()
    return result
end

local function read()
    local data=lines(FILE,7)
    if not data or data[1]~=MAGIC then return nil end
    local world,nonce,seconds=data[2],data[3],tonumber(data[5])
    if not world or #world>100 or not world:match('^PersonalKnox%-test%-%w[%w%-_]*$') then return nil end
    if not nonce or #nonce~=32 or not nonce:match('^%x+$') then return nil end
    if not seconds or seconds<10 or seconds>300 or seconds~=math.floor(seconds) then return nil end
    if data[6]~='0' and data[6]~='1' then return nil end
    return {world=world,nonce=nonce,state=data[4],seconds=seconds,quit=data[6]=='1'}
end

local function sameCopy(config)
    -- getFileReader is rooted in Zomboid/Lua. The preparer writes this binding
    -- only after verifying a fresh save copy; never use ../ paths to read saves.
    local marker=lines('PersonalKnox-probes/'..config.world..'.txt',1)
    return marker and marker[1]==config.nonce
end

local function write(config,state)
    local writer=getFileWriter(FILE,true,false)
    assert(writer,'Cannot update probe arm file')
    writer:write(table.concat({MAGIC,config.world,config.nonce,state,tostring(config.seconds),config.quit and '1' or '0'},'\n')..'\n')
    writer:close()
    config.state=state
end

local function inCopy(config)
    local world=getWorld()
    return world and world:getGameMode()=='Sandbox' and world:getWorld()==config.world and sameCopy(config)
end

function Probe.install(deps)
    local config=read()
    if not config or (config.state~='armed' and config.state~='running') or not sameCopy(config) then return end
    local function log(message)
        print('[PersonalKnox][probe-harness] run='..config.nonce..' world='..config.world..' '..message)
    end
    if config.state=='armed' then
        Events.OnMainMenuEnter.Add(function()
            local current=read()
            if not current or current.nonce~=config.nonce or current.state~='armed' or not sameCopy(config) then return end
            local menu=MainScreen and MainScreen.instance
            if not menu or menu.inGame or menu.checkSavefileModal or menu.termsOfServiceDialog or menu.tutorialButton then
                write(config,'blocked');log('blocked=menu_not_ready');return
            end
            -- Consume before entering vanilla code: a Lua reset must not start another load.
            write(config,'running')
            log('loading=Sandbox/'..config.world)
            local ok,err=pcall(MainScreen.continueLatestSave,'Sandbox',config.world)
            if not ok then write(config,'blocked');log('load_error='..tostring(err)) end
        end)
    end
    Events.OnGameStart.Add(function()
        local current=read()
        if not current or current.nonce~=config.nonce or current.state~='running' or not inCopy(config) then return end
        config.state='running'
        local started=getTimestampMs()
        local nextRead,attempts,probed,apiOK=started+1000,0,false,false
        log('world_started=true scope=readonly_api')
        local poll
        local function finish(status)
            Events.OnPlayerUpdate.Remove(poll)
            if not inCopy(config) then return end
            local active=read()
            if not active or active.nonce~=config.nonce or active.state~='running' then return end
            write(config,'done')
            log('finished='..status..' apiOK='..tostring(apiOK)..' nutrition=unverified animation=unverified')
            if config.quit then
                -- Same public exit path as MainScreen, only for this token-bound copy.
                setGameSpeed(1);pauseSoundAndMusic();setShowPausedMessage(true)
                getCore():quitToDesktop()
            end
        end
        poll=function()
            local now=getTimestampMs()
            if now<nextRead then return end
            nextRead=now+5000
            if not inCopy(config) then Events.OnPlayerUpdate.Remove(poll);return end
            if now-started>=config.seconds*1000 then finish(apiOK and 'ok' or 'api_unverified');return end
            if probed then return end
            local player=getSpecificPlayer(0)
            if not player or player:isDead() then return end
            attempts=attempts+1
            local ok,err=pcall(function()
                local roster=deps.roster(player)
                for i=1,math.min(#roster,4) do
                    local npc=roster[i]
                    local body=npc:getWorldObject()
                    if body and not body:isDead() and deps.isMember(player,npc) then
                        local status=PersonalKnoxAPI.getInstance():npcTransferApi(npc)
                        probed,apiOK=true,status=='public_api_present'
                        log('npc_present=true javaNpcTransferApi='..tostring(status))
                        return
                    end
                end
            end)
            if not ok then log('probe_error='..tostring(err));finish('error')
            elseif not probed and attempts>=12 then finish('npc_missing') end
        end
        Events.OnPlayerUpdate.Add(poll)
    end)
end

return Probe
