#!/bin/bash

# Script to launch multiple ticket clients
# Usage: ./launch_clients.sh <number_of_clients>

NUM_CLIENTS=5
if [ $# -gt 0 ]; then
    NUM_CLIENTS=$1
fi

echo "\033[36mLaunching $NUM_CLIENTS clients...\033[0m"

# Get current directory
DIR=$(pwd)

for i in $(seq 1 $NUM_CLIENTS); do
    # On macOS, use AppleScript to open a new terminal window and run the command
    osascript -e "tell application \"Terminal\" to do script \"cd '$DIR' && java ClienteTickets 'Client-$i'\""
done

echo "\033[32mDone!\033[0m"
