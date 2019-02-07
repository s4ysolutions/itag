#!/usr/bin/env bash
echo $GS |sed 's/\\"/"/g' > "$APPCENTER_SOURCE_DIRECTORY/app/google-services.json"
