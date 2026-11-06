#!/usr/bin/env python3
import pathlib,re
r=pathlib.Path(__file__).resolve().parents[1]
nums=(r/'app/src/main/assets/spam_tr.txt').read_text().splitlines()
assert len(nums)==201 and len(set(nums))==201
assert all(re.fullmatch(r'\+90[0-9]{10}',x) for x in nums)
source=(r/'app/src/main/java/dev/yellowpagetr/spam/SpamDb.java').read_text()
assert all('"'+x+'"' in source for x in nums)
print("PASS: 201 validated Turkish phone numbers")
