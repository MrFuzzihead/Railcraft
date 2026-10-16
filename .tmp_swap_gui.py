import glob
import os
import subprocess

BASE = 'railcraft.gui.blast.furnace'
CLASSIC = BASE + '.classic'
GTNH = BASE + '.gtnh'

for path in sorted(glob.glob('src/main/resources/assets/railcraft/lang/*.lang')):
    base = os.path.basename(path)
    with open(path, 'r', encoding='utf-8', newline='') as fh:
        text = fh.read()
    if GTNH in text:
        print('skip (already done):', base)
        continue
    lines = text.split('\n')
    gtnh_val = None
    classic_val = None
    for line in lines:
        if line.startswith(BASE + '=') and not line.startswith(CLASSIC + '='):
            gtnh_val = line[len(BASE) + 1:]
        elif line.startswith(CLASSIC + '='):
            classic_val = line[len(CLASSIC) + 1:]
    if gtnh_val is None or classic_val is None:
        print('%-12s skipped (missing base/classic)' % base)
        continue
    out = []
    for line in lines:
        if line.startswith(BASE + '=') and not line.startswith(CLASSIC + '='):
            out.append(BASE + '=' + classic_val)
        elif line.startswith(CLASSIC + '='):
            out.append(GTNH + '=' + gtnh_val)
        else:
            out.append(line)
    with open(path, 'w', encoding='utf-8', newline='') as fh:
        fh.write('\n'.join(out))
    print('%-12s swapped (classic=%r gtnh=%r)' % (base, classic_val, gtnh_val))
