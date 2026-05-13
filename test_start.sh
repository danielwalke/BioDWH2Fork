#!/bin/bash
git pull origin main
if git diff --name-only ORIG_HEAD HEAD | grep -q "^version\.md$"; then
	echo "Hello it runs Version 4"
	git add start.log
	git commit -m "new logs"
	git push
	echo "Finished?"
fi