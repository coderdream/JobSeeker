import sqlite3
import os
import re

db_path = 'db/getjobs.db'
migrations_dir = 'src/main/resources/db/migration'

def get_insert_statements():
    statements = []
    # Read files in order
    files = sorted([f for f in os.listdir(migrations_dir) if f.endswith('.sql')])
    for file in files:
        with open(os.path.join(migrations_dir, file), 'r', encoding='utf-8') as f:
            content = f.read()
            
            # Global replace for table names in older scripts
            if file <= 'V20260604_02__normalize_table_names.sql':
                # Replace exact words but avoid matching columns or arbitrary strings
                content = re.sub(r'\bcity\b', 'hub_city', content, flags=re.IGNORECASE)
                content = re.sub(r'\bcity_platform_code\b', 'hub_city_platform_code', content, flags=re.IGNORECASE)
                content = re.sub(r'\bplatform_option\b', 'hub_platform_option', content, flags=re.IGNORECASE)
                content = re.sub(r'\bplatform_option_type\b', 'hub_platform_option_type', content, flags=re.IGNORECASE)

            for stmt in content.split(';'):
                stmt = stmt.strip()
                if stmt.upper().startswith('INSERT INTO'):
                    statements.append(stmt)
    return statements

def main():
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    stmts = get_insert_statements()
    count = 0
    for stmt in stmts:
        try:
            cursor.execute(stmt)
            count += 1
        except Exception as e:
            print(f"Error executing: {stmt[:50]}... -> {e}")
    conn.commit()
    conn.close()
    print(f"Successfully executed {count} INSERT statements.")

if __name__ == '__main__':
    main()
