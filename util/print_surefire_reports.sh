#!/bin/bash

set -eu

# Ignore passing tests.
files=($(
  grep -e 'failures=.[^0]' -e 'errors=.[^0]' {android/,}*/target/surefire-reports/*.xml -l |
    sed -e 's/TEST-//; s/.xml$//'))

for file in "${files[@]}"; do
  # Dump file-output.txt and file.txt.
  # Use tail to include a filename header before each (and maybe truncate).
  tail -n 9999 "${file}"*.txt
done
