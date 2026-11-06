#!/usr/bin/env python3
from pathlib import Path
from zipfile import ZipFile
p=Path('app/build/outputs/apk/debug/app-debug.apk')
assert p.exists(),f'Missing {p}'
with ZipFile(p) as z:
  names=set(z.namelist())
  for f in ['assets/xposed_init','assets/spam_tr.txt','classes.dex']:
    assert f in names,f'Missing {f}'
  assert b'dev.yellowpagetr.spam.HookEntry' in z.read('assets/xposed_init')
  assert not any('xposed-api-stubs.jar' in n or 'MIUIYellowPage.apk' in n for n in names)
print('PASS: APK includes hook and local database',p.stat().st_size)
