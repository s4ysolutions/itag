package solutions.s4y.waytoday.errors;

import androidx.annotation.NonNull;

public class ErrorNotification {
    private static final int NO_RESOURCE_ID = -1;
    final Throwable th;
    final boolean toast;
    final int resourceID;
    private final String message;

    ErrorNotification(@NonNull Throwable th, boolean toast) {
        this.th = th;
        this.message = getMessage();
        this.toast = toast;
        this.resourceID = NO_RESOURCE_ID;
    }

    ErrorNotification(@NonNull Throwable th) {
        this(th, false);
    }

    ErrorNotification(int resourceID, boolean toast) {
        this.th = null;
        this.message = null;
        this.toast = toast;
        this.resourceID = resourceID;
    }

    ErrorNotification(String message, boolean toast) {
        this.th = null;
        this.message = message;
        this.toast = toast;
        this.resourceID = NO_RESOURCE_ID;
    }

    boolean hasResourceID() {
        return resourceID != NO_RESOURCE_ID;
    }

    String getMessage() {
        if (message != null) {
            return message;
        } else if (th != null) {
            Throwable real = this.th;
            while (real.getCause() != null && real.getCause().getMessage() != null) {
                real = real.getCause();
            }
            return real.getMessage();
        } else {
            return "";
        }
    }
}