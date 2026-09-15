# Spaces In Nova

Spaces are optional gaming environments hosted by Polaris. Each Space keeps its
own sign-ins, installed games, and saves. Nova connects you to a Space allowed for your paired device. Streaming Presets control picture and performance settings.

This flow is part of the Spaces preview. Creating, assigning, renaming, removing,
and restoring Spaces happens in the Polaris web interface under **Spaces**.
Polaris also guides the host owner through Docker setup. Nova does not require
Docker on your Android device.

## Open Your Space

1. Pair Nova with your Polaris host if it is not already paired.
2. In Polaris, assign a Space to that device under **Spaces**, then **Default Space**.
3. Open the host in Nova. **Playing In** identifies the selected Space in the
   library toolbar, beside **Change Space**, **Options**, and **System**.
4. Choose **Steam Big Picture**, then **Open Steam Big Picture**. Sign into Steam
   and install a game the first time you use this Space.
5. Return to Nova and refresh the library. Select the installed game's poster,
   then **Play**. The library banner follows the focused game.

A Space name identifies your gaming environment; it does not confirm which
Steam account is signed in. Check or switch that account inside Steam Big Picture.
Nova checks access and saved stream settings before launching. Required host
checks, connection errors and settings reviews stay visible before launch.

Nova offers **Resume** only for the matching game and Space session owned by this
device. **In Use** means another device is using that Space. Availability remains
subject to the host's current capacity and access rules.

Older hosts that expose one Space without a game library retain the direct
**Your Space → Open Space** screen. This opens that host's configured launcher;
the current Steam runtime uses Steam Big Picture.

## Choose Another Space

1. In Polaris, choose a device's **Default Space**.
2. On any additional Space card, expand **Device Access** and allow that device.
   Wait for confirmation. Changing access requires all Space streams to be stopped.
3. Open the host in Nova and select **Change Space**. Only your permitted Spaces appear.
4. Pick a Space to browse its games. Select a game or Steam Big Picture when it
   is **Ready To Play**.

Choosing a Space does not launch it. Polaris remembers your choice for this device
across app and host restarts. Your device must finish its current stream and cleanup
before switching. Other devices can keep playing while you choose.

**In Use** means another device is using that Space. Choose another or wait for it
to become ready. Nova checks status while this screen is open and checks again
before opening. **Starting** and **Stopping** keep Open Space unavailable until the
host finishes. If status cannot be verified, check the connection and try again.
Older hosts without the chooser API retain their existing single-Space flow.

## Change Stream Settings

Select a game, then open **Play Setup**. The host's available choices include
**Resolution** and **Frame Rate**. **Change Space** chooses a permitted environment
where that title is installed. These stream preferences are
saved on this Android device and applied on the next Space launch. Settings
currently follow the device's assigned Space; they are not separate saved presets
for each Steam account.

Select **Frame Rate**, then the desired rate when offered. Nova offers up to
**240 FPS** on supported displays; a 120 Hz handheld offers rates through
**120 FPS**. The host must also accept the requested stream. A selected rate is a
target, and does not establish sustained game rendering or actual presentation.
Check gameplay, audio and frame pacing with all intended Spaces running.

If Polaris offers a different resolution or frame rate from your explicit choice,
Nova shows the host settings and waits before opening the Space. Choose **Auto**
for **Frame Rate** and **Device Settings** for **Resolution** to follow the host,
or update the device’s display settings in Polaris and retry.

Return to the library without launching when you have finished adjusting settings.
On the older direct Space screen, use **Stream Settings**, then **Done**.
Codec, bitrate, and other device-wide preferences remain available through
**System**, then Nova's streaming settings.

Spaces currently use the host runtime's fixed codec and quality contract. Nova's
general Streaming Presets remain separate from Space selection.

## Use A Controller

Use the D-pad to move through game posters and the toolbar, **A** to select, and
**B** to return. The artwork and selected-game banner follow focus. The toolbar
keeps Space selection and library controls in one row; on narrow screens or with
enlarged text, controller focus scrolls it to keep each action reachable. Touch
controls perform the same actions.

On the older single-Space screen, **X** opens Stream Settings. Returning from
settings restores the previous action.

## Ordinary Streaming And Shared Hosts

Hosts serving ordinary games retain the existing Library. Nova does not add a
required Spaces tab or a setup step for ordinary streaming. With one allowed Space,
its library opens directly. With several, **Change Space** stays in the toolbar.

**Remove Space** in Polaris retains its Steam data and installed games. It removes
device access and does not reclaim disk space. Restore it and assign the device
again before returning to Nova. Restorable Spaces are listed under **Archived Spaces**.

## Check Sound And Stuttering

For a comparison, ask the host owner to offer 1080p at 60 FPS for your device.
In **Play Setup** (or **Stream Settings** on an older host), choose **Auto** for **Frame Rate** and **Device Settings**
for **Resolution**. Begin with one Space, then check again with the other
players connected.

If audio crackles or pauses, note the time, game, Space, stream settings, and
whether the client uses Wi-Fi or Ethernet. Note whether picture or controls
paused too. Low average latency and zero reported video packet loss do not rule
out brief audio delivery pauses.

Save before reconnecting: the current runtime ends its game session when the
stream disconnects. **Leave Space** explains this before you confirm. It keeps
the Space's installed games, saved data and Steam sign-in. A dropped connection
can also end the session; Resume does not promise to recover a disconnected game.

When possible, repeat with the same client over Ethernet or another access
point, keeping the game and stream settings unchanged. Ask the host owner to
compare with downloads, builds, and updates paused. Change one thing at a time.
The [Polaris troubleshooting steps](https://github.com/papi-ux/polaris/blob/master/docs/spaces.md#check-sound-and-stuttering)
explain what to retain for Doctor & Support. Audio reliability is still being
validated for the preview.

## Current Validation

Two configured Spaces completed a 15 minute test at 1080p and 60 FPS, with
Control Ultimate Edition on a wired Shield and PEAK on a Wi-Fi RP6. The listener
reported clean Shield audio; the RP6 continued to record audio underruns.
Reopening the RP6 Space used its retained Steam home while the Shield kept
streaming. Reopening starts a new game session after disconnect.

The [September 15 acceptance report](https://github.com/papi-ux/polaris/blob/master/docs/research/container-multiseat-acceptance-20260915.md)
records the tested builds and limits. It does not establish sustained 120 or
240 FPS gameplay, a complete first installation from published artifacts, or
reliable audio on every client.
