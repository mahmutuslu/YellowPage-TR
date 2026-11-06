# YellowPage TR v0.1 — Experimental LSPosed module
This project is a test harness for Xiaomi YellowPage on Xiaomi HyperOS. No Android Studio required.
## Scope
- 201 community-reported Turkish phone numbers stored on-device (GPL-3.0 upstream).
- Tries overriding existing Xiaomi `YellowPagePhone` and `QueryNumberResult` metadata for matching numbers.
- Does not patch the original priv-app APK, block calls, access Google Dialer data, make network requests, or request phone/contacts permissions.
- **The Xiaomi in-call overlay is NOT verified.** The correct class loader and runtime method path may differ across ROMs.
## Build
GitHub Actions > Build YellowPage TR LSPosed APK > run workflow; download Artifacts > YellowPage-TR-LSPosed-v0.1-debug-APK.
Android Studio is not needed.
## Device test
Install debug APK, enable it in LSPosed, and scope only the listed Xiaomi packages that actually exist on your phone. Reboot or restart target processes. App UI tests the local CSV only. LSPosed logs: `YellowPageTR/0.1`; `hooks=0` means no matched class in this process.
Please test using a number you control. Unknown names in an open spam list may be inaccurate. Do not depend on this experiment for emergency or business calls.
## License
Bundled community data is sourced from https://github.com/symbuzzer/Turkish-Spam-Numbers (GPL-3.0). See LICENSES/.
