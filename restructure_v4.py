import re
import sys

def modify_file():
    with open('front/app/boss/page.tsx', 'r', encoding='utf-8') as f:
        content = f.read()

    # 1. Rename Tabs
    content = content.replace('<TabsTrigger value="config">平台配置</TabsTrigger>', '<TabsTrigger value="config">职位</TabsTrigger>')
    content = content.replace('<TabsTrigger value="blacklist">黑名单管理</TabsTrigger>', '<TabsTrigger value="blacklist">配置</TabsTrigger>')

    # 2. Extract Boss直聘平台说明 Card
    start_str = '{/* 平台说明 */}'
    start_idx = content.find(start_str)
    if start_idx == -1:
        start_str = '<CardTitle className="flex items-center gap-2">\n                <BiBriefcase className="text-primary" />\n                Boss直聘平台说明'
        start_idx = content.find(start_str)
        # find the <Card> before it
        card_start = content.rfind('<Card>', 0, start_idx)
        start_idx = card_start
        
    end_idx = content.find('</Card>', start_idx)
    if end_idx != -1:
        end_idx += len('</Card>')
        
        boss_card = content[start_idx:end_idx]
        
        # Remove from original location
        content = content[:start_idx] + content[end_idx:]
        
        # Insert into blacklist tab
        blacklist_str = '<TabsContent value="blacklist" className="mt-5 min-w-0 space-y-4">'
        blacklist_idx = content.find(blacklist_str)
        if blacklist_idx != -1:
            insert_idx = blacklist_idx + len(blacklist_str) + 1
            content = content[:insert_idx] + boss_card + '\n' + content[insert_idx:]

    with open('front/app/boss/page.tsx', 'w', encoding='utf-8') as f:
        f.write(content)

modify_file()
