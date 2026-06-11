@echo off
cd /d D:\hongmengProjects\admin3
"D:\java\jdk21.0.11-win_x64\bin\java.exe" -jar "D:\hongmengProjects\admin3\admin3-server\target\admin3-server-0.0.1-SNAPSHOT.jar" --server.port=9099 -Dspring.datasource.url="jdbc:mysql://127.0.0.1:3306/admin3?characterEncoding=utf8" -Dspring.datasource.username=root -Dspring.datasource.password=123456 > "D:\hongmengProjects\admin3\logs\admin3-server.log" 2>&1
