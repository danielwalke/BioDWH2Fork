#!/bin/bash
git pull origin main
echo "Pulled" >> start.plog
if git diff --name-only HEAD~1 HEAD | grep -q "^version\.md$"; then
	echo "Hello it runs Version 7" >> start.plog
	echo "Finished!" >> start.plog
fi