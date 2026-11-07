#!/usr/bin/env python3
import pathlib,re
r=pathlib.Path(__file__).resolve().parents[1]
nums=(r/'app/src/main/assets/spam_tr.txt').read_text().splitlines()
assert len(nums)==201 and len(set(nums))==201
assert all(re.fullmatch(r'\+90[0-9]{10}',x) for x in nums)
source=(r/'app/src/main/java/dev/yellowpagetr/spam/SpamDb.java').read_text()
assert all('"'+x+'"' in source for x in nums)
print("PASS: 201 validated Turkish phone numbers")

# v1.0: validate the scope recommendation and OpenSpam privacy boundary.
scope=(r/'app/src/main/res/values/arrays.xml').read_text()
manifest=(r/'app/src/main/AndroidManifest.xml').read_text()
legacy=(r/'app/src/main/assets/xposed_scope').read_text().splitlines()
packages=('com.android.contacts','com.android.incallui','com.miui.yellowpage')
assert set(legacy)==set(packages)
assert 'android:name="xposedscope"' in manifest
assert 'android:resource="@array/yellowpage_recommended_scope"' in manifest
assert all('<item>'+pkg+'</item>' in scope for pkg in packages)
assert 'android.permission.INTERNET' in manifest
assert 'RiskCacheProvider' in manifest
client=(r/'app/src/main/java/dev/yellowpagetr/spam/OpenSpamClient.java').read_text()
main=(r/'app/src/main/java/dev/yellowpagetr/spam/MainActivity.java').read_text()
provider=(r/'app/src/main/java/dev/yellowpagetr/spam/RiskCacheProvider.java').read_text()
hook=(r/'app/src/main/java/dev/yellowpagetr/spam/HookEntry.java').read_text()
assert 'https://api.openspam.es/api/number/' in client
assert 'X-API-Key' in client
assert 'consent.isChecked()' in main
assert 'Binder.getCallingUid()' in provider
assert 'OpenSpamCache.hasRisk' in provider
assert 'hasCachedOpenSpamRisk' in hook
assert 'Olası spam (OpenSpam)' in hook
print('PASS: recommended LSPosed scope and explicit OpenSpam privacy boundary')
