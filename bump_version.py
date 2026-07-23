import os
import re
from datetime import datetime
import json

def bump_version():
    # Generate timestamp YYMMDD.hhmm
    now = datetime.now()
    timestamp = now.strftime("%y%m%d.%H%M")
    
    frontend_version = f"vf.{timestamp}"
    backend_version = f"vb.{timestamp}"
    
    print(f"Bumping version to Frontend: {frontend_version}, Backend: {backend_version}")
    
    # 1. Update pom.xml
    pom_path = "pom.xml"
    if os.path.exists(pom_path):
        with open(pom_path, 'r', encoding='utf-8') as f:
            pom_content = f.read()
        
        # Replace the first <version> inside <groupId>com.getjobs</groupId> block
        # We'll use a regex to replace <version>xxx</version> right after <artifactId>get_jobs</artifactId>
        pattern = r"(<artifactId>get_jobs</artifactId>\s*<version>)[^<]+(</version>)"
        if re.search(pattern, pom_content):
            pom_content = re.sub(pattern, rf"\g<1>{backend_version}\g<2>", pom_content)
            with open(pom_path, 'w', encoding='utf-8') as f:
                f.write(pom_content)
            print(f"Updated pom.xml with {backend_version}")
        else:
            print("Failed to find version tag in pom.xml")
    
    # 2. Update front/package.json
    package_path = os.path.join("front", "package.json")
    if os.path.exists(package_path):
        with open(package_path, 'r', encoding='utf-8') as f:
            package_data = json.load(f)
        
        package_data['version'] = frontend_version
        
        with open(package_path, 'w', encoding='utf-8') as f:
            json.dump(package_data, f, indent=2, ensure_ascii=False)
            f.write("\n")
        print(f"Updated front/package.json with {frontend_version}")
    else:
        print("Failed to find front/package.json")

if __name__ == "__main__":
    bump_version()
