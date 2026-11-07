# YellowPage TR v0.6 — Experimental LSPosed module
This project is a test harness for Xiaomi YellowPage on Xiaomi HyperOS. No Android Studio required.
## Scope
- 201 community-reported Turkish phone numbers stored on-device (GPL-3.0 upstream).
- Tries overriding existing Xiaomi `YellowPagePhone` and `QueryNumberResult` metadata for matching numbers.
- Does not patch the original priv-app APK, block calls, access Google Dialer data, make network requests, or request phone/contacts permissions.
- **The Xiaomi in-call overlay is NOT verified.** The correct class loader and runtime method path may differ across ROMs.
## Build
GitHub Actions > Build YellowPage TR LSPosed APK > run workflow; download Artifacts > YellowPage-TR-LSPosed-v0.6-debug-APK.
Android Studio is not needed.
## Device test
Install debug APK, enable it in LSPosed, and scope only the listed Xiaomi packages that actually exist on your phone. Reboot or restart target processes. App UI tests the local CSV only. LSPosed logs: `YellowPageTR/0.6`; `hooks=0` means no matched class in this process.
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

## v0.4 privacy-safe cursor trace
- The YellowPage ContentProvider is now observed **before and after** query execution.
- `inputMatch=true` means a local-list number was encountered **in one of the visible query arguments**; `false` does not prove there was no number, since an application may use indirect storage or different encoding.
- `providerResult cursor=true fields=[...]` reports a maximum of 20 sanitized column **names**, never data rows or phone numbers.
- No call blocking or network connection is added.
- v0.4 remains experimental: some Xiaomi processes may use a different path and incoming-call screen display is not established.

## v0.5 Xiaomi Dialer path traced from APK
The supplied `com.android.contacts` APK contains the route `TwelveKeyDialerFragment.E7` -> `DialerListDataSource.Q0` (T9 query). Separately, `YellowPagePhoneLoader.J` calls `YellowPageProxy.z`/`.t`, which call `YellowPageUtils.getPhoneInfo` and `getAntispamNumberCategory`. This version installs **diagnostic-only** hooks on these actual entry points, plus `YellowPageUtils.queryPhoneInfo`, to disambiguate dialer typing from YellowPage lookups. Check the logs for `entry=dialerFilter`, `entry=t9Query`, `entry=detailLookup`, `entry=phoneInfo`, `entry=spamCategory`, `entry=queryPhoneInfo`; match booleans only, no phone numbers. Hooking an entry point does not mean it is executed or that the caller will display a warning. No call blocking added.

## v0.6 visual dialer proof of concept
A successful v0.5 test showed `entry=t9Query inputMatch=true` in the supplied Xiaomi `com.android.contacts` process. v0.6 uses this exact path to display a rate-limited Android toast saying `Olası spam (TR)` when a dialer T9 query matches the local offline number list. It does **not** modify Xiaomi's real caller-ID/spam classification, incoming call window, provider data, or call blocking. No phone numbers are logged or sent over network. Look for `dialerNotice shown=true` when the toast is drawn, or `dialerNotice error=<type>` if it fails. One notice per 10 seconds maximum. If the toast cannot be displayed, the rest of the module continues as before.
