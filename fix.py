import re
with open('front/app/boss/page.tsx', 'r', encoding='utf-8') as f:
    content = f.read()

pattern = r'</TabsContent>\s*<TabsContent value="analytics" className="mt-5 min-w-0">\s*<AnalysisContent />\s*</TabsContent>\s*</Tabs>'
replacement = """      </div>
      
      {/* Job List */}
      <JobList />"""

content = re.sub(pattern, replacement, content, flags=re.DOTALL)

with open('front/app/boss/page.tsx', 'w', encoding='utf-8') as f:
    f.write(content)
