package solutions.s4y.waytoday.wsse;

import android.util.Base64;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.Date;

import solutions.s4y.waytoday.errors.ErrorsObservable;

public class Wsse {
    static private final Charset UTF8_CHARSET = Charset.forName("UTF-8");

    static private String sha1(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA");
            md.update(text.getBytes(UTF8_CHARSET));
            byte[] digest = md.digest();
            return Base64.encodeToString(digest, Base64.DEFAULT);
        } catch (Exception e) {
            ErrorsObservable.notify(e, true);
            return "";
        }
    }

    private static String digest(String password, String nonce, String created) {
        String text = nonce + created + password;
        return sha1(text);
    }

    public static String getToken() {
        String nonce = String.valueOf(Math.random());
        String created = new Date().toString();
        return "Username=\"solutions.s4y.itag\"," +
                "PasswordDigest=\"" + digest(Secret.get(), nonce, created) + "\"," +
                "nonce=\"" + nonce + "\"," +
                "Created=\"" + created + "\"";
    }
}
