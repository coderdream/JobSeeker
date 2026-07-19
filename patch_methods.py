import os

def append_method(filepath, old_method_sig, new_method_str):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # We will just append the new method before the last '}'
    last_brace_idx = content.rfind('}')
    if last_brace_idx != -1:
        content = content[:last_brace_idx] + new_method_str + '\n' + content[last_brace_idx:]
        
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

append_method('src/main/java/com/wh/jobsbackend/application/service/Job51Service.java',
              'public Job51Config loadJob51Config()',
              '''
    public Job51Config loadJob51Config(Long userId) {
        return loadJob51Config();
    }
''')

append_method('src/main/java/com/wh/jobsbackend/application/service/ZhilianService.java',
              'public ZhilianConfig loadZhilianConfig()',
              '''
    public ZhilianConfig loadZhilianConfig(Long userId) {
        return loadZhilianConfig();
    }
''')

append_method('src/main/java/com/wh/jobsbackend/application/service/YupaoService.java',
              'public YupaoConfig loadYupaoConfig()',
              '''
    public YupaoConfig loadYupaoConfig(Long userId) {
        return loadYupaoConfig();
    }
''')
