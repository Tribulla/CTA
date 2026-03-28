# CC by Wire - Command & Control Integration

The **CC by Wire** system allows ComputerCraft (CC:Tweaked) computers to connect to remote peripherals in the world exactly as if they were physically wired, using the **Wireless Connector** tool.

## Overview

Unlike traditional wired connections that require physical cable placement, **CC by Wire** creates virtual links directly between a Computer and any number of remote Peripherals across named channels. It mirrors the exact same functionality and capabilities as normal `peripheral.wrap()`.

> [!NOTE]
> The CC by Wire API allows for **full active control**. Using the `getRemotePeripheral` function, you retrieve a direct proxy to the connected peripheral, enabling you to execute commands, read state, and interact with it identically to a local peripheral.

---

## The Wireless Connector Tool

The **Wireless Connector** is the primary tool for managing links.

### How to Link Blocks
1. **Select a Target**: Hold `Shift` and `Right-click` the peripheral you want to connect (e.g., a Missile Rack or Heat Detector).
2. **Select the Computer**: Hold `Shift` and `Right-click` the ComputerCraft computer you want to link it to.
3. **Set Channel**: A screen will appear. Enter a channel name (e.g., `silo_1`) and click connect.

### Managing Links
- **Check Connections**: Hover over the Wireless Connector in your inventory to see its current selection.
- **Clear Selection**: If you have a block selected, `Shift` + `Right-click` the air to clear it.
- **Remove Links**: `Shift` + `Right-click` a linked block to remove all its wireless connections.

---

## ComputerCraft API Reference

The easiest and most powerful way to interact with your CC by Wire network is the built-in `remoteperipheral` API. This API is automatically loaded onto all computers!

### Connecting to a Peripheral

```lua
-- If you only have one block on the channel:
local mySilo = remoteperipheral.wrap("silo_1")
mySilo.launch()

-- If you have multiple blocks on the same channel:
local siloTwo = remoteperipheral.wrap("silo_1", 2)
```

### Advanced Function Reference

If you prefer to manually access the Connector block (via `peripheral.wrap("top")`), the following methods are internally available:

#### `getChannels()`
Returns a list of all active channel names linked to this hub.
- **Returns**: `table` (list of strings)

#### `getPeripheralCount(channel)`
Returns the number of blocks linked to a specific channel.
- **Arguments**: `channel` (string)
- **Returns**: `number`

#### `getPeripheralInfo(channel, index)`
Returns basic information about a linked block.
- **Arguments**: `channel` (string), `index` (number, 1-based)
- **Returns**: `table`
  - `x`, `y`, `z`: Coordinates of the block.
  - `block`: The Registry Name of the block (e.g., `"cta:missile_rack"`).

#### `getPeripheralData(channel, index)`
Returns the **full NBT data** of the linked block as a Lua table. This allows you to inspect deep internal states like missile counts, temperatures, or fuse settings.
- **Arguments**: `channel` (string), `index` (number, 1-based)
- **Returns**: `table` (NBT structure)

#### `getRemotePeripheral(channel, index)`
Returns a dynamic proxy object for the connected peripheral. This allows for **full active control**, letting you call any method the remote block exposes as if it were directly connected to the local computer.
- **Arguments**: `channel` (string), `index` (number, 1-based)
- **Returns**: `table` (Peripheral Lua Object)

> [!TIP]
> Example usage: `local remote = connector.getRemotePeripheral("silo_1", 1); if remote then remote.launch() end`

#### `removeLink(channel, index)`
Removes a specific link from a channel.
- **Arguments**: `channel` (string), `index` (number)

#### `removeChannel(channel)`
Removes all links in a specific channel.
- **Arguments**: `channel` (string)

---

## Example Script

```lua
local connector = peripheral.find("wireless_connector")

if not connector then
    print("No wireless connector found!")
    return
end

local channels = connector.getChannels()
for _, channel in ipairs(channels) do
    local count = connector.getPeripheralCount(channel)
    print("Channel '" .. channel .. "' has " .. count .. " devices:")
    
    for i = 1, count do
        local info = connector.getPeripheralInfo(channel, i)
        print(" - [" .. i .. "] " .. info.block)
        
        -- Get the remote peripheral proxy!
        local remote = connector.getRemotePeripheral(channel, i)
        if remote then
             print("   Successfully connected to remote peripheral.")
             -- If it's a Missile Rack, we could do remote.launch()
             -- If it's a Heat Detector, we could do remote.getHeat()
        else
             print("   Device at target is not a ComputerCraft peripheral.")
        end
    end
end
```

---

## Bonus: Scope Binding
The Wireless Connector also handles **Scope-to-Cannon** binding:
1. `Shift` + `Right-click` a placed **Scope**.
2. `Shift` + `Right-click` a **Cannon Mount** to bind the scope to it.
