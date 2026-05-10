# Google Play Foreground Service Declaration

**Service Type:** `specialUse`

**Core Functionality:**
This application provides dynamic home screen widgets that display real-time data from remote JSON sources. To ensure high-frequency updates (per-second precision) and maintain live countdown timers without being killed by the system, a persistent Foreground Service is required.

**Justification for `specialUse`:**
The application relies on a proven architecture previously approved and successfully implemented in our other application: `name.xoid.app1`. The service is essential for:
1. Maintaining per-second UI updates for live counters.
2. Managing network synchronization across multiple widget instances.
3. Ensuring reliable widget recovery immediately after device reboot.

**User Impact:**
Without this foreground service, the home screen widgets would freeze, lose real-time accuracy, and fail to update after the main application is closed, leading to a broken user experience.
