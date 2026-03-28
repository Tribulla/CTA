-- remoteperipheral.lua API
-- Automatically loaded into CC:Tweaked ROM by the CTA mod

-- Wraps a remote peripheral over the CTA Wireless Connector
function wrap(channel, index)
    if type(channel) ~= "string" then
        error("bad argument #1 (expected string, got " .. type(channel) .. ")", 2)
    end

    index = index or 1

    -- Locate the wireless connector attached to this computer
    local connector = peripheral.find("wireless_connector")
    if not connector then
        return nil
    end

    -- Attempt to get the remote peripheral capability
    return connector.getRemotePeripheral(channel, index)
end

-- Returns a list of all available remote peripheral identifiers
function getNames()
    local connector = peripheral.find("wireless_connector")
    if not connector then
        return {}
    end

    local names = {}
    local channels = connector.getChannels()
    for _, ch in ipairs(channels) do
        local count = connector.getPeripheralCount(ch)
        for i = 1, count do
            if count == 1 then
                table.insert(names, ch)
            else
                table.insert(names, ch .. "_" .. i)
            end
        end
    end
    
    return names
end

-- Checks if a remote peripheral exists
function isPresent(channel, index)
    if type(channel) ~= "string" then
        error("bad argument #1 (expected string, got " .. type(channel) .. ")", 2)
    end
    
    index = index or 1
    
    local connector = peripheral.find("wireless_connector")
    if not connector then
        return false
    end
    
    local count = connector.getPeripheralCount(channel)
    return count >= index
end

-- Returns the block name of the remote peripheral
function getType(channel, index)
    if type(channel) ~= "string" then
        error("bad argument #1 (expected string, got " .. type(channel) .. ")", 2)
    end
    
    index = index or 1
    
    local connector = peripheral.find("wireless_connector")
    if not connector then
        return nil
    end
    
    local info = connector.getPeripheralInfo(channel, index)
    if info and info.block then
        return info.block
    end
    return nil
end
