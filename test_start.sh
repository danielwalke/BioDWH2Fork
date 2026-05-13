#!/bin/bash
git pull origin main
echo "Pulled" >> start.plog
if git diff --name-only ORIG_HEAD HEAD | grep -q "^version\.md$"; then
	echo "Hello it runs Version 6" >> start.plog
	echo "Finished!" >> start.plog
fi