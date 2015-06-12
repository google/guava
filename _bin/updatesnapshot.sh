#!/bin/bash

set -e -u

cd $(dirname $0)/..

source _bin/util.sh

_bin/updatejavadocs.sh
_bin/generatejdiffxml.sh
_bin/updatejdiff.sh $(latest_release) snapshot
