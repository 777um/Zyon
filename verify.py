from pathlib import Path
import re
root=Path(__file__).parent
sources=list(root.rglob('*.kt'))
assert len(sources)>=3
for source in sources:
    s=source.read_text()
    assert s.count('{') == s.count('}'), source
    assert s.count('(') == s.count(')'), source
renderer=(root/'app/src/main/java/com/zyon/prototype/PreviewRenderer.kt').read_text()
for token in ['onDraw(canvas: Canvas)', 'drawRect(', 'drawCircle(', 'drawLine(', 'espRange', 'health', 'skeleton']:
    assert token in renderer, token
for hp in [0, .3, .43, .65, .84, 1]:
    assert 0 <= (200 - 100*hp) <= 200
for fov in [20,110,300]:
    assert 0 < 400*fov/650 < 200
print('PASS: structural checks, required renderer elements, geometry invariants')
print('NOTE: not a Kotlin compilation, Android runtime test, or performance benchmark')
