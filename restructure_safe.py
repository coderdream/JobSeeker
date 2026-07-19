import sys

def modify_file():
    with open('front/app/boss/page.tsx', 'r', encoding='utf-8') as f:
        content = f.read()
        
    # 1. Imports
    content = content.replace("import AnalysisContent from '@/app/boss/analysis/AnalysisContent'", 
                              "import AnalysisContent from '@/app/boss/analysis/AnalysisContent'\nimport JobList from './JobList'")
    
    # 2. State
    content = content.replace("const [logoutResult, setLogoutResult] = useState<{ success: boolean; message: string } | null>(null)",
                              "const [logoutResult, setLogoutResult] = useState<{ success: boolean; message: string } | null>(null)\n  const [logs, setLogs] = useState<string[]>([])")
    
    # 3. Logs
    progress_old = "console.log('[Boss task SSE] progress:', data)"
    progress_new = """console.log('[Boss task SSE] progress:', data)
              setLogs(prev => [...prev, `[${new Date().toLocaleTimeString()}] ${data.message || JSON.stringify(data)}`])"""
    content = content.replace(progress_old, progress_new)
    
    # 4. City Default
    content = content.replace("const currentCityRaw = data.config?.cityCode || ''", "const currentCityRaw = data.config?.cityCode || '101200100'")
    
    # 5. Buttons
    content = content.replace("开始投递", "获取职位")
    
    # 6. TabsList
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
    
    # Find the positions of the cards to move/modify
    def extract_block(start_marker, end_marker):
        start = content.find(start_marker)
        if start == -1: return ""
        end = content.find(end_marker, start)
        if end == -1: return ""
        end += len(end_marker)
        return content[start:end]
    
    boss_card = extract_block("{/* Boss直聘平台说明 */}", "</Card>")
    search_card = extract_block("{/* 搜索配置 */}", "</Card>")
    salary_card = extract_block("{/* 薪资和经验 */}", "</Card>")
    company_card = extract_block("{/* 公司要求 */}", "</Card>")
    blacklist_card = extract_block("{/* 黑名单管理 */}", "</Card>")
    
    # Remove them from their original locations
    if boss_card: content = content.replace(boss_card, "")
    if search_card: content = content.replace(search_card, "")
    if salary_card: content = content.replace(salary_card, "")
    if company_card: content = content.replace(company_card, "")
    if blacklist_card: content = content.replace(blacklist_card, "")
    
    # Now we build the combined config card
    def extract_inner_fields(card_str):
        if not card_str: return ""
        s = card_str.find('<div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">')
        if s == -1: return ""
        s += len('<div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">')
        e = card_str.rfind('</div>')
        if e == -1: return ""
        return card_str[s:e].strip()
        
    c_search = extract_inner_fields(search_card)
    c_salary = extract_inner_fields(salary_card)
    c_company = extract_inner_fields(company_card)
    
    combined_config = f"""          {{/* 综合配置 */}}
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
                {c_search}
                {c_salary}
                {c_company}
              </div>
            </CardContent>
          </Card>
          <JobList />"""
          
    # Place it at the beginning of `<TabsContent value="config"`
    tc_config_start = '<TabsContent value="config" className="mt-5 min-w-0 space-y-4">'
    content = content.replace(tc_config_start, tc_config_start + "\n" + combined_config)
    
    # Place blacklist and logs after analytics
    tc_analytics_end = """        <TabsContent value="analytics" className="mt-5 min-w-0 space-y-5">
          <AnalysisContent />
        </TabsContent>"""
        
    blacklist_tab = f"""        <TabsContent value="blacklist" className="mt-5 min-w-0 space-y-4">
          {blacklist_card}
        </TabsContent>"""
        
    logs_tab = f"""        <TabsContent value="logs" className="mt-5 min-w-0 space-y-4">
          {boss_card}
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
        
    content = content.replace(tc_analytics_end, tc_analytics_end + "\n" + blacklist_tab + "\n" + logs_tab)

    with open('front/app/boss/page.tsx', 'w', encoding='utf-8') as f:
        f.write(content)

modify_file()
