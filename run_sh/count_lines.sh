
#!/bin/bash

FILE="/home/benjamin.reyes/BioDWH2/BioDWH2Fork/workspace/sources/UniProt/source/uniprot_trembl.xml.gz"

if [[ ! -f "$FILE" ]]; then
  echo "Error: file not found: $FILE"
  exit 1
fi

echo "Counting lines in: $FILE"
COUNT=$(zcat "$FILE" | wc -l)
echo "Line count: $COUNT"

primary secondary accessions 