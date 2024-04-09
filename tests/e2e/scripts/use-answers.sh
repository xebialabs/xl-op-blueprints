#!/bin/bash

TMPFILE="$BUILD_REL_PATH/$1"
sed -e "s#\${NAMESPACE}#${NAMESPACE}#g" \
    ${ANSWERS_REL_PATH}/$1 > $TMPFILE
echo $TMPFILE
