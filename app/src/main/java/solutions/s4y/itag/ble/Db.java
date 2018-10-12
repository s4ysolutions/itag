package solutions.s4y.itag.ble;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import solutions.s4y.itag.BuildConfig;
import solutions.s4y.itag.ITagApplication;

public class Db {
    public interface DbListener {
        void onChange();
    }

    private final static List<DbListener> mDbListeners = new ArrayList<>(4);
    public static final List<Device> devices = new ArrayList<>(4);

    public static void addListener(DbListener listener){
        if (BuildConfig.DEBUG) {
            if (mDbListeners.contains(listener)) {
                ITagApplication.handleError(new Exception("Db.addListener duplcate"));
            }
        }
        mDbListeners.add(listener);
    }

    public static void removeListener(DbListener listener){
        if (BuildConfig.DEBUG) {
            if (!mDbListeners.contains(listener)) {
                ITagApplication.handleError(new Exception("Db.removeListener non existing"));
            }
        }
        mDbListeners.remove(listener);
    }

    public static void notifyChange(){
        for(DbListener listener:mDbListeners){
            listener.onChange();
        }
    }

    static private Device findByAddr(@NotNull final String addr) {
        for (Device d : devices) {
            if (d.addr.equals(addr)) return d;
        }
        return null;
    }

    static private File getSer(@NotNull final Context context) throws IOException {
        File file = new File(context.getFilesDir(), "db");
        //noinspection ResultOfMethodCallIgnored
        file.createNewFile();
        return file;
    }

    static public void load(@NotNull final Context context) {
        devices.clear();
        try (FileInputStream fis = new FileInputStream(getSer(context))) {
            ObjectInputStream ois = new ObjectInputStream(fis);
            Object read = ois.readObject();
            if (read instanceof List) {
                List dd = (List) read;
                for (Object d : dd) {
                    if (d instanceof Device) {
                        devices.add((Device) d);
                    }
                }
            }
            ois.close();
        } catch (FileNotFoundException e) {
            ITagApplication.handleError(e);
            e.printStackTrace();
        } catch (IOException e) {
            ITagApplication.handleError(e);
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            ITagApplication.handleError(e);
            e.printStackTrace();
        }
    }

    public static void save(@NotNull final Context context) {
        try (FileOutputStream fos = new FileOutputStream(getSer(context))) {
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(devices);
            oos.close();
        } catch (FileNotFoundException e) {
            ITagApplication.handleError(e);
            e.printStackTrace();
        } catch (IOException e) {
            ITagApplication.handleError(e);
            e.printStackTrace();
        }
    }

    static public void remember(@NotNull final Context context, @NotNull final BluetoothDevice device) {
        if (findByAddr(device.getAddress()) == null) {
            final Device d = new Device(device);
            devices.add(d);
            save(context);
            notifyChange();
        }
    }

    static public void forget(@NotNull final Context context, @NotNull final Device device) {
        Device existing = findByAddr(device.addr);
        if (existing != null) {
            devices.remove(existing);
            save(context);
            notifyChange();
        }
    }

    static public boolean has(@NotNull final BluetoothDevice device) {
        return findByAddr(device.getAddress()) != null;
    }

    static public boolean has(@NotNull final Device device) {
        return findByAddr(device.addr) != null;
    }

}
