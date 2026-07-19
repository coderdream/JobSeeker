import re
import sys

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # 1. Replace imports
    content = content.replace("import AnalysisContent from '@/app/boss/analysis/AnalysisContent'", "import JobList from './JobList'")
    
    # 2. Replace button text
    content = content.replace("开始投递", "获取职位")
    
    # 3. Replace Tabs start
    tabs_start = """      <Tabs defaultValue="config" className="min-w-0">
        <TabsList className="grid w-full grid-cols-2">
          <TabsTrigger value="config">平台配置</TabsTrigger>
          <TabsTrigger value="analytics">投递分析</TabsTrigger>
        </TabsList>

        <TabsContent value="config" className="mt-5 min-w-0 space-y-4">"""
    content = content.replace(tabs_start, '      <div className="mt-5 min-w-0 space-y-4">')
    
    # 4. Replace Tabs end and Analysis Content
    tabs_end = """        </TabsContent>
        
        <TabsContent value="analytics" className="mt-5 min-w-0">
          <AnalysisContent />
        </TabsContent>
      </Tabs>"""
    
    replacement = """      </div>
      
      {/* Job List */}
      <JobList />"""
    
    # regex for the end of config tab
    content = re.sub(r'</TabsContent>\s*<TabsContent value="analytics" className="mt-5 min-w-0">\s*<AnalysisContent />\s*</TabsContent>\s*</Tabs>', replacement, content, flags=re.DOTALL)
    
    # 5. Compact configuration.
    # The user wants to group 搜索配置, 薪资与经验要求, 公司要求 into one.
    # Currently they are separate Cards.
    # Let's remove the card headers and boundaries between them, and put them in one grid.
    
    # Start of 搜索配置
    search_header = """          {/* 搜索配置 */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <BiSearch className="text-primary" />
                搜索配置
              </CardTitle>
              <CardDescription>设置职位搜索关键词和目标城市</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">"""
    
    new_search_header = """          {/* 综合配置 */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <BiSearch className="text-primary" />
                综合搜索与过滤配置
              </CardTitle>
              <CardDescription>配置搜索、薪资、经验、公司规模等参数</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">"""
    content = content.replace(search_header, new_search_header)
    
    # End of 搜索配置, start of 薪资
    salary_header = """              </div>
            </CardContent>
        </Card>

        {/* 薪资和经验 */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <BiMoney className="text-primary" />
              薪资与经验要求
            </CardTitle>
            <CardDescription>设置薪资待遇和工作经验要求</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">"""
    content = content.replace(salary_header, "")
    
    # End of 薪资, start of 公司
    company_header = """            </div>
          </CardContent>
        </Card>

        {/* 公司要求 */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <BiBuilding className="text-primary" />
              公司要求
            </CardTitle>
            <CardDescription>设置目标公司的规模和融资阶段</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">"""
    content = content.replace(company_header, "")
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)
        
process_file('front/app/boss/page.tsx')
