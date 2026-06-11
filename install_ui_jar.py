import shutil, os
repo = os.path.join(os.environ["USERPROFILE"], ".m2", "repository", "tech", "wetech", "admin3", "admin3-ui", "0.0.1-SNAPSHOT")
src = r"D:\hongmengProjects\admin3\admin3-ui\target\admin3-ui-0.0.1-SNAPSHOT.jar"
dst = os.path.join(repo, "admin3-ui-0.0.1-SNAPSHOT.jar")

for f in os.listdir(repo):
    fp = os.path.join(repo, f)
    os.remove(fp)
    print(f"Removed: {f}")

shutil.copy2(src, dst)
print(f"Copied: {dst}")

pom_path = os.path.join(repo, "admin3-ui-0.0.1-SNAPSHOT.pom")
with open(pom_path, "w", encoding="utf-8") as f:
    f.write('<?xml version="1.0" encoding="UTF-8"?><project><modelVersion>4.0.0</modelVersion><groupId>tech.wetech.admin3</groupId><artifactId>admin3-ui</artifactId><version>0.0.1-SNAPSHOT</version><packaging>jar</packaging></project>')
print("OK - UI jar installed")
