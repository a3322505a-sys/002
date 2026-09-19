"""Release APK UI check: no microphone permission, metronome remains usable."""
import subprocess, time, re, xml.etree.ElementTree as ET

def adb(*args):
    return subprocess.check_output(['adb',*args],text=True)
def dump():
    adb('shell','uiautomator','dump','/sdcard/window.xml')
    return ET.fromstring(adb('shell','cat','/sdcard/window.xml'))
def tap(suffix):
    for attempt in range(3):
        for n in dump().iter('node'):
            if n.attrib.get('resource-id','').endswith(suffix):
                x1,y1,x2,y2=map(int,re.findall(r'\d+',n.attrib['bounds']))
                adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));return
        time.sleep(.3)
    raise AssertionError('Missing control: '+suffix)
package=adb('shell','dumpsys','package','io.github.a3322505a.tunebeat')
assert 'android.permission.RECORD_AUDIO' not in package
assert not any(n.attrib.get('text') in ('调音','调音器') for n in dump().iter('node'))
tap('id/play_pause')
# Notification permission, if shown, is independent of microphone permission.
tree=dump()
if any(n.attrib.get('resource-id','').endswith('id/permission_allow_button') for n in tree.iter('node')):
    tap('id/permission_allow_button')
assert any(n.attrib.get('content-desc')=='暂停节拍' for n in dump().iter('node'))
tap('id/play_pause')
assert any(n.attrib.get('content-desc')=='开始节拍' for n in dump().iter('node'))
print('PASS: no microphone permission; metronome playback works')
