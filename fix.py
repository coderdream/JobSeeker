import sys
path = r'D:\04_GitHub\JobSeeker\src\main\java\com\wh\jobsbackend\worker\boss\Boss.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

target = '''                cmd.add("--cdp-port");
                cmd.add("9222");

                log.info("[BOSS-BREADCRUMB] launch args: keyword={}, city={}, pages=3, noDetail=true, allowDomFallback=false",'''

replacement = '''                cmd.add("--cdp-port");
                cmd.add("9222");
                cmd.add("--allow-dom-fallback");

                log.info("[BOSS-BREADCRUMB] launch args: keyword={}, city={}, pages=3, noDetail=true, allowDomFallback=true",'''

if target in content:
    content = content.replace(target, replacement)
    with open(path, 'w', encoding='utf-8', newline='') as f:
        f.write(content)
    print('Replaced successfully')
else:
    print('Target not found')
