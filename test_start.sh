#!/bin/bash
git pull origin main
if git diff --name-only ORIG_HEAD HEAD | grep -q "^version\.md$"; then
	echo "Hello it runs"
	git add start.log
	git commit -m "new logs"
	git push
fi