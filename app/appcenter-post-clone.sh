#!/usr/bin/env bash
echo $GS |sed 's/\\"/"/g' > "$APPCENTER_SOURCE_DIRECTORY/app/google-services.json"

cat > "$APPCENTER_SOURCE_DIRECTORY/app/src/main/java/s4y/waytoday/wsse/Secret.java" <<EOT
package s4y.waytoday.wsse;

import androidx.annotation.NonNull;

public class Secret {
    @NonNull
    static String get(){
        return "$SECRET";
    };
}
EOT