#!/usr/bin/env python3
"""Checks for keyless, on-device Xiaomi YellowPage TR integration."""
from pathlib import Path
import re

r=Path(__file__).resolve().parents[1]
nums=(r/'app/src/main/assets/spam_tr.txt').read_text().splitlines()
assert len(nums)==201 and len(set(nums))==201
assert all(re.fullmatch(r'\+90[0-9]{10}',x) for x in nums)
spam_db=(r/'app/src/main/java/dev/yellowpagetr/spam/SpamDb.java').read_text()
assert all('"'+x+'"' in spam_db for x in nums)

manifest=(r/'app/src/main/AndroidManifest.xml').read_text()
scope=(r/'app/src/main/res/values/arrays.xml').read_text()
legacy=(r/'app/src/main/assets/xposed_scope').read_text().splitlines()
pkgs=('com.android.contacts','com.android.incallui','com.miui.yellowpage')
assert set(legacy)==set(pkgs)
assert all('<item>'+pkg+'</item>' in scope for pkg in pkgs)
assert 'android:name="xposedscope"' in manifest
assert 'android:resource="@array/yellowpage_recommended_scope"' in manifest
assert 'android.permission.RECEIVE_BOOT_COMPLETED' in manifest
assert '.BootSyncReceiver' in manifest
assert '.DailySpamSyncJob' in manifest
assert '.RiskCacheProvider' in manifest

code_root=r/'app/src/main/java/dev/yellowpagetr/spam'
updater=(code_root/'DailyOfflineList.java').read_text()
job=(code_root/'DailySpamSyncJob.java').read_text()
receiver=(code_root/'BootSyncReceiver.java').read_text()
provider=(code_root/'RiskCacheProvider.java').read_text()
hook=(code_root/'HookEntry.java').read_text()
ui=(code_root/'MainActivity.java').read_text()
assert 'raw.githubusercontent.com/symbuzzer/Turkish-Spam-Numbers/main/SpamBlocker.csv' in updater
assert 'MAX_BYTES=1024*1024' in updater
assert 'JobService' in job and '.setPeriodic(PERIOD,' in job
assert 'Intent.ACTION_BOOT_COMPLETED' in receiver
assert 'Intent.ACTION_MY_PACKAGE_REPLACED' in receiver
assert 'hasLocalRisk' in provider and 'DailyOfflineList.contains' in provider
assert 'Binder.getCallingUid()' in provider
assert 'hasOfflineSpam(' in hook and 'YellowPageTR/1.3' in hook
assert 'installIncomingCallCard(param.classLoader)' in hook
assert 'extraInfoSpamSet=true' in hook
assert 'DailySpamSyncJob.schedule(this)' in ui
assert 'API anahtarı, hesap veya' in ui

# An incoming call must never send numbers to online services.
assert 'requestOpenSpamNative(' not in hook
assert 'hasCachedOpenSpamRisk(' not in hook
assert 'checkAndFetch' not in provider
assert not (code_root/'OpenSpamClient.java').exists()
assert not (code_root/'OpenSpamAutoLookup.java').exists()
assert not (code_root/'OpenSpamSettings.java').exists()
assert not (code_root/'OpenSpamCache.java').exists()
print('PASS: 201 bundled TR numbers; Xiaomi native call UI; keyless automatic daily updates')
