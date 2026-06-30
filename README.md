# ForzaGaugeTelemetry

https://medium.com/@subin.bsuresh27/building-a-real-time-forza-dashboard-for-android-583eb1cded42

An Android app that turns your phone into a live digital instrument cluster for *Forza Horizon*. It connects to your PC or Xbox over Wi-Fi, reads the game's live telemetry stream at 60+ FPS, and updates the UI instantly using Jetpack Compose.

The app is built to be completely lightweight. It reuses a single memory buffer to read the incoming network data, which prevents memory allocation spikes and eliminates screen stutters while you are driving.

---

## Features

* **Lag-Free Networking:** Reuses one network buffer array on a background thread (`Dispatchers.IO`) so Android's garbage collector doesn't cause micro-stutters.
* **Raw Byte Parsing:** Uses Java's `ByteBuffer` configured to Little-Endian to correctly read the game engine's raw memory data.
* **Canvas-Based Graphics:** High-frequency UI elements like the RPM bar and tire temperature grids are drawn directly onto a hardware-accelerated Canvas, skipping heavy layout calculations.
* **Adaptive Layouts:** Automatically switches between a vertical portrait stack and a side-by-side landscape view meant for a desk or steering wheel mount.
* **Auto IP Display:** Automatically finds and displays your phone's current Wi-Fi IP address on screen so you know exactly what to type into the game settings.

---

## How to Set It Up

Your Android phone and your PC/Xbox **must** be connected to the exact same Wi-Fi network.

1. Open the app on your phone and look at the bottom overlay card to find your phone's current IP address.
2. Launch **Forza Horizon** and go to **Settings > HUD and Gameplay**.
3. Scroll down to the very bottom to find the **Data Out** options.
4. Set **Data Out** to **ON**.
5. Set **Data Out IP Address** to your phone's IP address (shown in the app).
6. Set **Data Out IP Port** to `20066`.

*Note: The game only starts sending data once you exit the menus and actually start driving your car in the open world or a race.*

---

## The Byte Map

Forza sends a raw **324-byte** packet on every single frame. This app plucks out the following positions to run the dashboard:

| Variable | Type | Byte Position | Description |
| :--- | :--- | :--- | :--- |
| `IsRaceOn` | 32-bit Int | `0` | 0 = Menus/Paused, 1 = Driving |
| `EngineMaxRpm` | 32-bit Float | `8` | The car's rev limit |
| `CurrentEngineRpm` | 32-bit Float | `16` | Live engine RPM |
| `Speed` | 32-bit Float | `256` | Meters per second (converted to KM/H in UI) |
| `TireTempFL` | 32-bit Float | `268` | Front Left tire temperature in Celsius |
| `Boost` | 32-bit Float | `284` | Turbo/Supercharger pressure in PSI |
| `Accel` | 8-bit Unsigned Int | `315` | Gas pedal input (0 to 255) |
| `Brake` | 8-bit Unsigned Int | `316` | Brake pedal input (0 to 255) |
| `Gear` | 8-bit Unsigned Int | `319` | Current gear (0=Reverse, 11=Neutral) |

---

## Permissions Required

The app needs internet permissions in the `AndroidManifest.xml` to listen to incoming UDP network packets:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
