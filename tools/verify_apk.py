#!/usr/bin/env python3
from pathlib import Path
from zipfile import ZipFile
apk=Path('app/build/outputs/apk/debug/app-debug.apk')
assert apk.exists(),f'Missing {apk}'
with ZipFile(apk) as z:
    names=set(z.namelist())
    required=['META-INF/xposed/java_init.list','META-INF/xposed/module.prop',
              'META-INF/xposed/scope.list','assets/spam_tr.txt','classes.dex']
    for name in required:
        assert name in names,f'Missing {name}'
    assert z.read('META-INF/xposed/java_init.list').strip()==b'dev.yellowpagetr.spam.HookEntry'
    props=z.read('META-INF/xposed/module.prop').decode()
    assert 'minApiVersion=102' in props and 'targetApiVersion=102' in props
    assert 'autoHotReload=false' in props
    assert z.read('META-INF/xposed/scope.list').decode().splitlines()==[
        'com.android.contacts','com.android.incallui','com.miui.yellowpage']
    assert 'assets/xposed_init' not in names and 'assets/xposed_scope' not in names
    assert not any('xposed-api-stubs.jar' in name or 'MIUIYellowPage.apk' in name for name in names)
print('PASS: API 102 metadata, scoped Java hook, offline list',apk.stat().st_size)
