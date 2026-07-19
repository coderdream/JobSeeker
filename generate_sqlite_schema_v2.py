import os
import re

entity_dir = 'src/main/java/com/wh/jobsbackend/application/entity'
schema = []

for file in sorted(os.listdir(entity_dir)):
    if file.endswith('Entity.java'):
        with open(os.path.join(entity_dir, file), 'r', encoding='utf-8') as f:
            content = f.read()
            
        table_match = re.search(r'@TableName\(\"(.*?)\"\)', content)
        if not table_match:
            continue
        table_name = table_match.group(1)
        
        fields = []
        
        # Regex to find private fields: private [Type] [name];
        field_pattern = re.compile(r'private\s+([A-Za-z0-9_<>]+)\s+([A-Za-z0-9_]+);')
        
        for type_str, field_name in field_pattern.findall(content):
            if field_name == 'id':
                continue
            
            # Convert camelCase to snake_case
            col_name = re.sub(r'(?<!^)(?=[A-Z])', '_', field_name).lower()
            
            # Map types
            if 'String' in type_str:
                col_type = 'TEXT'
            elif 'Long' in type_str or 'Integer' in type_str or 'int' in type_str or 'long' in type_str or 'Boolean' in type_str or 'boolean' in type_str:
                col_type = 'INTEGER'
            else:
                col_type = 'TEXT'
                
            fields.append(f'    {col_name} {col_type}')
        
        id_field = 'id INTEGER PRIMARY KEY AUTOINCREMENT'
        
        table_def = f'CREATE TABLE IF NOT EXISTS {table_name} (\n    {id_field}'
        if fields:
            table_def += ',\n' + ',\n'.join(fields)
        table_def += '\n);\n'
        schema.append(table_def)

# Add reference data inserts
ref_data = """
INSERT INTO hub_platform_option_type (platform, type, label, sort_order, enabled, created_at, updated_at) VALUES ('boss', 'city', 'city', 1000, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO hub_city_platform_code (city_id, platform, platform_city_code, platform_city_name, enabled, created_at, updated_at) VALUES (1, 'boss', '101200100', '武汉', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
"""
schema.append(ref_data)

with open('src/main/resources/db/sqlite/V1__init.sql', 'w', encoding='utf-8') as f:
    f.write('\n'.join(schema))

print("Schema generated")
