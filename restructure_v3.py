import sys

def modify_file():
    with open('front/app/boss/page.tsx', 'r', encoding='utf-8') as f:
        content = f.read()

    # Rename Tab 1 from "平台配置" to "职位"
    content = content.replace('<TabsTrigger value="config">平台配置</TabsTrigger>', '<TabsTrigger value="config">职位</TabsTrigger>')
    
    # Rename Tab 3 from "黑名单管理" to "配置"
    content = content.replace('<TabsTrigger value="blacklist">黑名单管理</TabsTrigger>', '<TabsTrigger value="blacklist">配置</TabsTrigger>')
    
    # We need to move the Instruction card ("Boss直聘平台说明") from the "logs" tab to the "blacklist" (now "配置") tab.
    
    def extract_block(start_marker, end_marker):
        start = content.find(start_marker)
        if start == -1: return ""
        end = content.find(end_marker, start)
        if end == -1: return ""
        end += len(end_marker)
        return content[start:end]
        
    boss_card = extract_block("{/* Boss直聘平台说明 */}", "</Card>")
    
    if boss_card:
        # Remove it from its current location inside logs tab (or wherever it is)
        content = content.replace(boss_card, "")
        
        # We need to insert it at the top of the blacklist tab
        blacklist_tab_start = '<TabsContent value="blacklist" className="mt-5 min-w-0 space-y-4">'
        if blacklist_tab_start in content:
            content = content.replace(blacklist_tab_start, blacklist_tab_start + "\n" + boss_card)
        else:
            print("Could not find blacklist tab start!")

    with open('front/app/boss/page.tsx', 'w', encoding='utf-8') as f:
        f.write(content)

modify_file()
