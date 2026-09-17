# Nova Flatpak (Deck and Linux desktops)

`com.papi_ux.Nova` runs on `org.kde.Platform` 6.10, the runtime Moonlight-Qt already installs on a Steam Deck. It carries the Deck shell and the bounded native CLI streaming path. The graphical Play flow still uses Moonlight-Qt as its own Flatpak.

Build a bundle from a checkout with its submodules initialised (moonlight-common-c is in-tree):

    flatpak install --user flathub org.kde.Platform//6.10 org.kde.Sdk//6.10
    flatpak-builder --user --force-clean --repo=build/flatpak-repo build/flatpak clients/deck/packaging/flatpak/com.papi_ux.Nova.json
    flatpak build-bundle build/flatpak-repo build/Nova.flatpak com.papi_ux.Nova

Install and run it (Desktop Mode on a Deck, or any desktop):

    flatpak install --user build/Nova.flatpak
    flatpak run com.papi_ux.Nova --print-live-state

Permissions, and why: network for Polaris; wayland and dri for the shell; `input` for the Deck's controls; read-only access to Moonlight's config, which is where the pairing lives; `org.freedesktop.Flatpak` so the sandbox can ask the host to run `flatpak run com.moonlight_stream.Moonlight`; the Steam userdata directories so `--register-steam-shortcut` can add Nova to Game Mode.

The bounded native CLI streaming path uses `xdg-run/pipewire-0` for direct
PipeWire audio output. This exposes the default PipeWire socket, not the whole
runtime directory. The current graphical Play flow still hands off to Moonlight;
this audio backend does not establish standalone Deck release readiness.

Register Nova with Steam from Desktop Mode, with Steam closed:

    flatpak run com.papi_ux.Nova --register-steam-shortcut

The shortcut runs `flatpak run com.papi_ux.Nova --live`; Steam shows it as "Nova" and Game Mode launches it like any other non-Steam game.
