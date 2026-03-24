Get-ChildItem -Filter *.java -Recurse | ForEach-Object { javac $_.FullName }; cd 00_Dashboard; java DashboardServer
