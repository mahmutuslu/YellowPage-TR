#!/usr/bin/env python3
"""Static regression checks for the modern API 102 Xiaomi spam module."""
from pathlib import Path
import re
root=Path(__file__).resolve().parents[1]
main=root/'app/src/main'
numbers=(main/'assets/spam_tr.txt').read_text().splitlines()
assert len(numbers)==201 and len(set(numbers))==201
assert all(re.fullmatch(r'\+90[0-9]{10}',x) for x in numbers)
src=(main/'java/dev/yellowpagetr/spam')
db=(src/'SpamDb.java').read_text()
assert all('"'+n+'"' in db for n in numbers)
meta=main/'resources/META-INF/xposed'
assert (meta/'java_init.list').read_text().strip()=='dev.yellowpagetr.spam.HookEntry'
assert (meta/'scope.list').read_text().splitlines()==[
    'com.android.contacts','com.android.incallui','com.miui.yellowpage'
]
prop=dict(line.split('=',1) for line in (meta/'module.prop').read_text().splitlines() if '=' in line)
assert prop['minApiVersion']=='102' and prop['targetApiVersion']=='102'
assert prop['autoHotReload']=='false'
assert not (main/'assets/xposed_init').exists()
assert not (main/'assets/xposed_scope').exists()
manifest=(main/'AndroidManifest.xml').read_text()
assert 'xposedminversion' not in manifest and 'xposedmodule' not in manifest
for component in ['.BootSyncReceiver','.DailySpamSyncJob','.RiskCacheProvider']:
    assert component in manifest
hook=(src/'HookEntry.java').read_text()
assert 'extends XposedModule' in hook and 'onPackageReady(PackageReadyParam param)' in hook
assert 'chain.proceed()' in hook and '.setId(' in hook
assert 'hasOfflineSpam(' in hook and 'extraInfoSpamSet=true' in hook
assert 'installIncomingCallCard(param.getClassLoader())' in hook
assert 'com.miui.antispam' not in hook
for path in src.glob('*.java'):
    assert 'de.robv.android.xposed' not in path.read_text()
updater=(src/'DailyOfflineList.java').read_text()
assert 'raw.githubusercontent.com/symbuzzer/Turkish-Spam-Numbers/main/SpamBlocker.csv' in updater
assert 'MAX_BYTES=1024*1024' in updater
provider=(src/'RiskCacheProvider.java').read_text()
assert 'Binder.getCallingUid()' in provider and 'DailyOfflineList.contains' in provider
job=(src/'DailySpamSyncJob.java').read_text()
assert 'JobService' in job and '.setPeriodic(PERIOD,' in job
receiver=(src/'BootSyncReceiver.java').read_text()
assert 'Intent.ACTION_BOOT_COMPLETED' in receiver and 'Intent.ACTION_MY_PACKAGE_REPLACED' in receiver
ui=(src/'MainActivity.java').read_text()
assert 'DailySpamSyncJob.schedule(this)' in ui
assert 'API anahtarı, hesap veya' in ui
for forbidden in ['requestOpenSpamNative(', 'hasCachedOpenSpamRisk(', 'checkAndFetch']:
    assert forbidden not in hook
print('PASS: API 102 entry/scope, 201 TR numbers, on-device risk, keyless background updates')
