import re

with open('front/app/boss/page.tsx', 'r', encoding='utf-8') as f:
    content = f.read()

# I want to remove the Tabs tags.
# If they are already removed, maybe only </TabsContent> ... <TabsContent> are left.
pattern = r'</TabsContent>\s*<TabsContent value="analytics"[^>]*>\s*<AnalysisContent />\s*</TabsContent>\s*</Tabs>'
replacement = """      </div>
      
      {/* Job List */}
      <JobList />"""

content = re.sub(pattern, replacement, content, flags=re.DOTALL)

with open('front/app/boss/page.tsx', 'w', encoding='utf-8') as f:
    f.write(content)
