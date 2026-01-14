package s4y.itag.itag;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import s4y.itag.ITagApplication;

public class ITagFileStore {
    private static final String DB_FILE_NAME="dbv1";
    static final class ITagDevice implements Serializable {
        private static final long serialVersionUID = 1345673754421L;
        String addr;
        private TagColor color;
        private String name;
        boolean linked;
    }

    @NonNull
    static private File getDbFile(
            @NonNull final Context context) throws IOException {
        File file = new File(context.getFilesDir(), ITagFileStore.DB_FILE_NAME);
        //noinspection ResultOfMethodCallIgnored
        file.createNewFile();
        return file;
    }

    static private void loadFromFile(
            @NonNull final Context context,
            @NonNull final List<ITagInterface> devices) {
        devices.clear();

        File file;

        try {
            file = getDbFile(context);
        } catch (IOException e) {
            ITagApplication.handleError(e, true);
            e.printStackTrace();
            return;
        }

        long l=file.length();
        if (l==0)
            return;

        try (FileInputStream fis = new FileInputStream(file)) {
            ObjectInputStream ois = new ObjectInputStream(fis);
            Object read = ois.readObject();
            if (read instanceof List) {
                //noinspection rawtypes
                List dd = (List) read;
                for (Object d : dd) {
                    if (d instanceof ITagDevice) {
                        ITagDevice td = (ITagDevice) d;
                        ITagDefault tagDefault = new ITagDefault(td.addr, td.name, td.color, td.linked, null, null, null);
                        devices.add(tagDefault);
                    }
                }
            }
            ois.close();
        } catch (IOException | ClassNotFoundException e) {
            ITagApplication.handleError(e, true);
            e.printStackTrace();
        }
    }

    static public List<ITagInterface> load(@NonNull final Context context) {
        List<ITagInterface> devices = new ArrayList<>();
        loadFromFile(context, devices);
        return devices;
    }
}
