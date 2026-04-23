import os
import glob

dao_dir = r"c:\Users\91878\Desktop\hospital-management\src\main\java\com\shushant\hospital_management\dao"
for filepath in glob.glob(os.path.join(dao_dir, "*.java")):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    new_content = content.replace("e.printStackTrace();", 'throw new RuntimeException("Database error", e);')
    
    if content != new_content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"Updated {os.path.basename(filepath)}")
