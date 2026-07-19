import re

with open('front/app/boss/page.tsx', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Add new imports (JobList, and whatever needed for logs, maybe just state)
content = content.replace("import AnalysisContent from '@/app/boss/analysis/AnalysisContent'", 
                          "import AnalysisContent from '@/app/boss/analysis/AnalysisContent'\nimport JobList from './JobList'")

# Add log state to BossPage
# Find `const [logoutResult...`
content = content.replace("const [logoutResult, setLogoutResult] = useState<{ success: boolean; message: string } | null>(null)",
                          "const [logoutResult, setLogoutResult] = useState<{ success: boolean; message: string } | null>(null)\n  const [logs, setLogs] = useState<string[]>([])")

# In the SSE listener for 'progress', add to logs
# console.log('[Boss task SSE] progress:', data)
progress_old = "console.log('[Boss task SSE] progress:', data)"
progress_new = """console.log('[Boss task SSE] progress:', data)
              setLogs(prev => [...prev, `[${new Date().toLocaleTimeString()}] ${data.message || JSON.stringify(data)}`])"""
content = content.replace(progress_old, progress_new)

# Default City to Wuhan
# const currentCityRaw = data.config?.cityCode || ''
content = content.replace("const currentCityRaw = data.config?.cityCode || ''", "const currentCityRaw = data.config?.cityCode || '101200100'")
# Ensure jobType defaults appropriately if needed, but only city was explicitly mentioned.

# Change 开始投递 to 获取职位
content = content.replace("开始投递", "获取职位")

# Replace TabsList
tabs_list_old = """        <TabsList className="grid w-full grid-cols-2">
          <TabsTrigger value="config">平台配置</TabsTrigger>
          <TabsTrigger value="analytics">投递分析</TabsTrigger>
        </TabsList>"""
tabs_list_new = """        <TabsList className="grid w-full grid-cols-4">
          <TabsTrigger value="config">平台配置</TabsTrigger>
          <TabsTrigger value="analytics">投递分析</TabsTrigger>
          <TabsTrigger value="blacklist">黑名单管理</TabsTrigger>
          <TabsTrigger value="logs">日志</TabsTrigger>
        </TabsList>"""
content = content.replace(tabs_list_old, tabs_list_new)

# Extract sections
def extract_card(header_title):
    pattern = r'(<Card>[\s]*<CardHeader>[\s]*<CardTitle[^>]*>.*?' + header_title + r'.*?</Card>)\s*(?=<!--|<Card>|<TabsContent|\{/\*|</div)'
    match = re.search(pattern, content, flags=re.DOTALL)
    return match.group(1) if match else None

platform_desc = extract_card('Boss直聘平台说明')
search_config = extract_card('搜索配置')
salary_config = extract_card('薪资与经验要求')
company_config = extract_card('公司要求')
blacklist_config = extract_card('黑名单管理')

# Replace these blocks with empty string temporarily
content = content.replace(platform_desc, '') if platform_desc else content
content = content.replace(search_config, '') if search_config else content
content = content.replace(salary_config, '') if salary_config else content
content = content.replace(company_config, '') if company_config else content
content = content.replace(blacklist_config, '') if blacklist_config else content

# Combine search, salary, company into one comprehensive card
combined_config = """          {/* 综合配置 */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <BiSearch className="text-primary" />
                综合搜索与过滤配置
              </CardTitle>
              <CardDescription>配置搜索、薪资、经验、公司规模等参数</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
"""
# Extract just the <div className="space-y-2"> elements from the three config cards
def extract_fields(card_html):
    if not card_html: return ""
    match = re.search(r'<CardContent>\s*<div[^>]*>(.*?)</div>\s*</CardContent>', card_html, flags=re.DOTALL)
    if match:
        return match.group(1)
    return ""

combined_config += extract_fields(search_config)
combined_config += extract_fields(salary_config)
combined_config += extract_fields(company_config)
combined_config += """              </div>
            </CardContent>
          </Card>
          
          <JobList />"""

# We need to reconstruct the TabsContent sections
tabs_content_config = f"""        <TabsContent value="config" className="mt-5 min-w-0 space-y-4">
{combined_config}
        </TabsContent>"""

tabs_content_analytics = """        <TabsContent value="analytics" className="mt-5 min-w-0">
          <AnalysisContent />
        </TabsContent>"""

tabs_content_blacklist = f"""        <TabsContent value="blacklist" className="mt-5 min-w-0 space-y-4">
{blacklist_config}
        </TabsContent>"""

tabs_content_logs = f"""        <TabsContent value="logs" className="mt-5 min-w-0 space-y-4">
{platform_desc}
          <Card>
            <CardHeader>
              <CardTitle>运行日志</CardTitle>
              <CardDescription>实时查看任务运行状态</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="h-[400px] bg-slate-900 text-green-400 p-4 rounded overflow-y-auto font-mono text-sm flex flex-col-reverse">
                <div>
                  {{logs.length === 0 ? "暂无日志" : logs.map((l, idx) => <div key={{idx}}>{{l}}</div>)}}
                </div>
              </div>
            </CardContent>
          </Card>
        </TabsContent>"""

# Replace the old config TabsContent
content = re.sub(r'<TabsContent value="config"[^>]*>.*?</TabsContent>', tabs_content_config, content, flags=re.DOTALL)

# Add blacklist and logs after analytics
# Find </TabsContent> \n </Tabs>
content = re.sub(r'(<TabsContent value="analytics"[^>]*>.*?</TabsContent>)', 
                 r'\1\n' + tabs_content_blacklist + '\n' + tabs_content_logs, 
                 content, flags=re.DOTALL)

with open('front/app/boss/page.tsx', 'w', encoding='utf-8') as f:
    f.write(content)
