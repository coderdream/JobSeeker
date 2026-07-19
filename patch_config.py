import os
import re

def patch_file(filepath, replacements):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    for old, new in replacements:
        content = content.replace(old, new)
        
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

# 1. Patch BossService
patch_file('src/main/java/com/wh/jobsbackend/application/service/BossService.java', [
    ("""public BossConfig loadBossConfig() {
        Long userId = currentUserService.requireUserId();""", 
     """public BossConfig loadBossConfig() {
        return loadBossConfig(currentUserService.requireUserId());
    }

    public BossConfig loadBossConfig(Long userId) {""")
])

# 2. Patch Job51Service
patch_file('src/main/java/com/wh/jobsbackend/application/service/Job51Service.java', [
    ("""public Job51Config loadJob51Config() {
        Long userId = currentUserService.requireUserId();""",
     """public Job51Config loadJob51Config() {
        return loadJob51Config(currentUserService.requireUserId());
    }

    public Job51Config loadJob51Config(Long userId) {""")
])

# 3. Patch ZhilianService
patch_file('src/main/java/com/wh/jobsbackend/application/service/ZhilianService.java', [
    ("""public ZhilianConfig loadZhilianConfig() {
        Long userId = currentUserService.requireUserId();""",
     """public ZhilianConfig loadZhilianConfig() {
        return loadZhilianConfig(currentUserService.requireUserId());
    }

    public ZhilianConfig loadZhilianConfig(Long userId) {""")
])

# 4. Patch YupaoService
patch_file('src/main/java/com/wh/jobsbackend/application/service/YupaoService.java', [
    ("""public YupaoConfig loadYupaoConfig() {
        Long userId = currentUserService.requireUserId();""",
     """public YupaoConfig loadYupaoConfig() {
        return loadYupaoConfig(currentUserService.requireUserId());
    }

    public YupaoConfig loadYupaoConfig(Long userId) {""")
])

# 5. Patch ConfigService
patch_file('src/main/java/com/wh/jobsbackend/application/service/ConfigService.java', [
    ("""public BossConfig getBossConfig() {
        return bossService.loadBossConfig();
    }""",
     """public BossConfig getBossConfig() {
        return bossService.loadBossConfig();
    }
    
    public BossConfig getBossConfig(Long userId) {
        return bossService.loadBossConfig(userId);
    }"""),
    
    ("""public ZhilianConfig getZhilianConfig() {
        return zhilianService.loadZhilianConfig();
    }""",
     """public ZhilianConfig getZhilianConfig() {
        return zhilianService.loadZhilianConfig();
    }
    
    public ZhilianConfig getZhilianConfig(Long userId) {
        return zhilianService.loadZhilianConfig(userId);
    }"""),
    
    ("""public Job51Config getJob51Config() {
        return job51Service.loadJob51Config();
    }""",
     """public Job51Config getJob51Config() {
        return job51Service.loadJob51Config();
    }
    
    public Job51Config getJob51Config(Long userId) {
        return job51Service.loadJob51Config(userId);
    }"""),
    
    ("""public YupaoConfig getYupaoConfig() {
        return yupaoService.loadYupaoConfig();
    }""",
     """public YupaoConfig getYupaoConfig() {
        return yupaoService.loadYupaoConfig();
    }
    
    public YupaoConfig getYupaoConfig(Long userId) {
        return yupaoService.loadYupaoConfig(userId);
    }""")
])

# 6. Patch JobServices to pass userId
patch_file('src/main/java/com/wh/jobsbackend/worker/service/BossJobService.java', [
    ("BossConfig config = configService.getBossConfig();", "BossConfig config = configService.getBossConfig(userId);")
])

patch_file('src/main/java/com/wh/jobsbackend/worker/service/Job51JobService.java', [
    ("Job51Config config = configService.getJob51Config();", "Job51Config config = configService.getJob51Config(userId);")
])

patch_file('src/main/java/com/wh/jobsbackend/worker/service/ZhilianJobService.java', [
    ("ZhilianConfig config = configService.getZhilianConfig();", "ZhilianConfig config = configService.getZhilianConfig(userId);")
])
