#!/bin/bash

# Returns the current git branch, if any; the HEAD commit's SHA1 otherwise.
function currentref {
  branch=$(git rev-parse --abbrev-ref HEAD)

  if [ $branch == "HEAD" ]; then
    echo $(git rev-parse HEAD)
  else
    echo $branch
  fi
}
