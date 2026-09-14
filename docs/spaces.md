# Spaces In Nova

Spaces are optional gaming environments hosted by Polaris. Each Space keeps its
own sign-ins, installed games, and saves. Nova connects you to the Space assigned
to your paired device. Streaming Presets control picture and performance settings.

This flow is part of the Spaces preview. Creating, assigning, renaming, removing,
and restoring Spaces happens in the Polaris web interface under **Spaces**.
Polaris also guides the host owner through Docker setup. Nova does not require
Docker on your Android device.

## Open Your Space

1. Pair Nova with your Polaris host if it is not already paired.
2. In Polaris, assign a Space to that device under **Spaces**, then **Device Access**.
3. Open the host in Nova. When it exposes one assigned Space, Library shows
   **Your Space**, its name, and the host name.
4. Select **Open Space**. Nova checks the host and uses your saved stream settings.
   The current Steam Space opens Steam Big Picture. Sign in there the first time.
5. Choose your game inside Steam.

The Open Space action does not require a stop at ordinary game details. A required
host check, connection error, or settings review stays visible before launching.
The host still decides whether the device is allowed to start the Space.

Nova offers **Resume Space** only when the host reports a matching session owned
by this device. A matching session owned by another device is **In Use**. An
unrelated game on the host does not become this Space's Resume action. Opening
an available Space is still subject to the host's current capacity and access.

## Change Stream Settings

Select **Stream Settings** beside Open Space. The host's available choices include
**Resolution** and **Frame Rate**. These preferences are
saved on this Android device and applied on the next Space launch. Settings
currently follow the device's assigned Space; they are not separate saved presets
for each Steam account.

For 120 FPS, select **Frame Rate**, then **120 FPS** when offered. The display must
support it, and the host must accept the requested stream. Choosing 120 FPS is not
a guarantee that the game itself will render 120 frames per second.

Select **Done** to return without launching. Codec, bitrate, and other device-wide
preferences remain available through **System**, then Nova's streaming settings.

Spaces currently use the host runtime's fixed codec and quality contract. Nova's
general Streaming Presets remain separate from Space selection.

## Use A Controller

The primary action receives focus when you enter. Use the D-pad to move, **A** to
select, and **B** to return. **X** opens Stream Settings from the single-Space
Library. Returning from settings restores the previous action. Touch controls
perform the same actions.

## Ordinary Streaming And Shared Hosts

Hosts serving ordinary games retain the existing Library. Nova does not add a
required Spaces tab or a setup step for ordinary streaming. Current device
assignment exposes one Space; selecting from several permitted Spaces awaits a
host contract that provides those choices and their individual status.

Removing a Space in Polaris retains its Steam data and installed games. It removes
device access and does not reclaim disk space. Restore it and assign the device
again before returning to Nova.
