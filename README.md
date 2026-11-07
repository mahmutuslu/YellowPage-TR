# YellowPage TR v0.3 — Experimental LSPosed module
This project is a test harness for Xiaomi YellowPage on Xiaomi HyperOS. No Android Studio required.
## Scope
- 201 community-reported Turkish phone numbers stored on-device (GPL-3.0 upstream).
- Tries overriding existing Xiaomi `YellowPagePhone` and `QueryNumberResult` metadata for matching numbers.
- Does not patch the original priv-app APK, block calls, access Google Dialer data, make network requests, or request phone/contacts permissions.
- **The Xiaomi in-call overlay is NOT verified.** The correct class loader and runtime method path may differ across ROMs.
## Build
GitHub Actions > Build YellowPage TR LSPosed APK > run workflow; download Artifacts > YellowPage-TR-LSPosed-v0.3-debug-APK.
Android Studio is not needed.
## Device test
Install debug APK, enable it in LSPosed, and scope only the listed Xiaomi packages that actually exist on your phone. Reboot or restart target processes. App UI tests the local CSV only. LSPosed logs: `YellowPageTR/0.3`; `hooks=0` means no matched class in this process.
Please test using a number you control. Unknown names in an open spam list may be inaccurate. Do not depend on this experiment for emergency or business calls.
## License
Bundled community data is sourced from https://github.com/symbuzzer/Turkish-Spam-Numbers (GPL-3.0). See LICENSES/.

## v0.2 diagnostic test
When a YellowPage/Contacts/InCallUI process calls a hooked number-result method, LSPosed modules log a line with `call=<method> matched=true/false`; matching local-list numbers also log `override=<method> matched=true`. The first 100 invocations per process are traced, without the actual telephone number or its digits. Hooks are installed only if the class exists. For a safe test, search a listed number in the Xiaomi Phone/Kişiler interface **without placing any call**, then inspect `/data/adb/lspd/log/modules_*.log`. If no `call=` appears, that screen may use a different API path. This does not confirm that the incoming-call UI renders the spam label.

## v0.3 provider trace
- Observes `YellowPageProvider.query` and YellowPage/Antispam `ContentResolver.query` calls in scoped processes.
- Records only hook counts and whether a relevant query occurred; **does not record telephone numbers, URIs, or query arguments**.
- Check `YellowPageTR/0.3` in `/data/adb/lspd/log/modules_*.log` after using the original Xiaomi Phone or YellowPage search UI; no need to place a call.
- Results are diagnostics, **not** evidence the native incoming-call UI shows a spam warning.
