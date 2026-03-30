#!/bin/bash

# Configuration: Ports to clear
PORTS=(9000 12345 12346 12347 8082 8081 8888)

echo "Performing nuclear port cleanup..."
for port in "${PORTS[@]}"; do
    # Using lsof to find the PID. Handling both IPv4 and IPv6.
    pids=$(lsof -ti :$port)
    if [ ! -z "$pids" ]; then
        for pid in $pids; do
            printf "  > Closing process %s on port %s...\n" "$pid" "$port"
            kill -9 "$pid" 2>/dev/null
        done
    fi
done

echo "Waiting for ports to be released..."
sleep 2

# Clean old .class files
echo "Cleaning old class files..."
find . -name "*.class" -type f -delete

# Recompile everything
echo "Recompiling full project..."
# Find all .java files and compile them
find . -name "*.java" -exec javac -encoding UTF-8 {} +

# Launch Dashboard
echo "Starting Master Dashboard v7.1..."
cd 00_Dashboard
if [ -f "DashboardServer.class" ]; then
    java DashboardServer
else
    echo "❌ ERROR: Could not compile DashboardServer.java"
fi
