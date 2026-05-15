#!/bin/bash
git pull origin main
echo "Pulled" >> start.plog
df -h >> start.plog
echo "Finished!" >> start.plog
