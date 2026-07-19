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
        lines = content.split('\n')
        for i, line in enumerate(lines):
            field_match = re.search(r'@TableField\(\"(.*?)\"\)', line)
            if field_match:
                col_name = field_match.group(1)
                # Next line usually has the type
                type_line = lines[i+1].strip()
                if 'String' in type_line:
                    col_type = 'TEXT'
                elif 'Long' in type_line or 'Integer' in type_line or 'int ' in type_line or 'long ' in type_line or 'Boolean' in type_line or 'boolean ' in type_line:
                    col_type = 'INTEGER'
                elif 'LocalDateTime' in type_line or 'Date' in type_line:
                    col_type = 'TEXT'
                else:
                    col_type = 'TEXT'
                fields.append(f'    {col_name} {col_type}')
        
        id_field = 'id INTEGER PRIMARY KEY AUTOINCREMENT'
        
        table_def = f'CREATE TABLE IF NOT EXISTS {table_name} (\n    {id_field}'
        if fields:
            table_def += ',\n' + ',\n'.join(fields)
        table_def += '\n);\n'
        schema.append(table_def)

# Write to V1__init.sql
os.makedirs('src/main/resources/db/sqlite', exist_ok=True)
with open('src/main/resources/db/sqlite/V1__init.sql', 'w', encoding='utf-8') as f:
    f.write('\n'.join(schema))

print("Generated V1__init.sql in db/sqlite")
