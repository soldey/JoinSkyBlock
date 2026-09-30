# Join SkyBlock

A small Fabric client mod: **Singleplayer** in the main menu becomes **Join SkyBlock**. One click
connects to Hypixel and, once you are in the lobby, sends `/play skyblock` for you.

Supports **Minecraft 26.1.x and 26.2**, with a separate jar per version - take the one matching
your game. Needs Fabric Loader 0.19.3+, Fabric API and Fabric Language Kotlin; ModMenu is
optional.

`hypixel-mod-api` is **recommended**: with it the command goes out the moment Hypixel reports
that you are in a lobby. Without it the mod waits two seconds after joining.

## How it behaves

* The button takes Singleplayer's exact place and size, wherever the menu (or a mod like
  Essential) put it. Turn **Replace Singleplayer** off to get the vanilla button back.
* `/play skyblock` is sent **once**, and only after joining through the button. Joining Hypixel
  from the server list does nothing. If the connection drops before the lobby, nothing is sent.
* The server is taken from your server list, and added as "Hypixel" if it is not there yet. That
  entry is what remembers your answer to the server resource pack question, so it is only asked
  once.
* If SkyHanni's own **Auto Join Skyblock** is on, the button only connects and leaves the
  command to SkyHanni, so it is never sent twice.

### Back to SkyBlock after a kick (off by default)

Sometimes SkyBlock throws you out into a Hypixel lobby or limbo while you stay connected. With
this option on, the mod waits 1:10 and sends `/play skyblock` once. A chat line only you can see
says when it will happen; `/jsb cancel` stops it.

* It needs `hypixel-mod-api`, which is how the mod knows you were in SkyBlock and now are in a
  lobby. Without it the option does nothing.
* Leaving on purpose - `/lobby`, `/l`, `/play <game>`, `/limbo` and the like - is not a kick.
* Getting back to SkyBlock yourself, or being warped into a game, cancels the countdown.
* One attempt per kick, never repeated.

## Settings

Through ModMenu, or in `config/joinskyblock.json`:

| Key | Default | Meaning |
| --- | --- | --- |
| `serverAddress` | `mc.hypixel.net` | where the button connects to |
| `autoPlaySkyblock` | `true` | send `/play skyblock`; off means the button only connects (it then reads "Join Hypixel") |
| `showButton` | `true` | replace Singleplayer with the button |
| `rejoinSkyblock` | `false` | go back to SkyBlock after being kicked into a lobby |
| `rejoinDelaySeconds` | `70` | how long to wait in the lobby first, 5 to 600 |

## Hypixel rules

The mod connects to the server and sends one command you could type yourself - the same thing
SkyHanni's option does. Going back to SkyBlock after a kick is the same one command on a timer, and
it is off unless you turn it on; whether Hypixel is fine with it for the way you play is your call. There is no in-game automation, and there
never will be.

## Licence

Copyright (C) 2026 kmsold.

Join SkyBlock is free software: you can redistribute it and/or modify it under the terms of the
**GNU Lesser General Public License, version 3 or later**, as published by the Free Software
Foundation. See `LICENSE` for the LGPL text and `COPYING` for the GPL text it builds on. In
short: use it freely, but if you publish a modified version, publish its source too.

It is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

## Something broken?

Send `logs/latest.log` from your game folder to **soldey** on Discord - the file itself, not a
screenshot, and right after the problem happens: `latest.log` is overwritten every time the game
starts. Every step of the mod (connecting, the lobby, the command) is logged under `Join SkyBlock`.
