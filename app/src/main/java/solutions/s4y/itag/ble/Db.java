package solutions.s4y.itag.ble;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.annotations.NonNull;
import solutions.s4y.itag.ITagApplication;

public class Db {
    public static final List<Device> devices = new ArrayList<>(4);

    static private Device findByAddr(@NonNull final String addr) {
        for (Device d : devices) {
            if (d.addr.equals(addr)) return d;
        }
        return null;
    }

    static public Device find(@NonNull final BluetoothDevice device) {
        return findByAddr(device.getAddress());
    }

    static private File getSer(@NonNull final Context context) throws IOException {
        File file = new File(context.getFilesDir(), "db");
        //noinspection ResultOfMethodCallIgnored
        file.createNewFile();
        return file;
    }

    static public void load(@NonNull final Context context) {
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
            ITagApplication.errorNotifier.onNext(e);
            e.printStackTrace();
        } catch (IOException e) {
            ITagApplication.errorNotifier.onNext(e);
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            ITagApplication.errorNotifier.onNext(e);
            e.printStackTrace();
        }
    }

    private static void save(@NonNull final Context context) {
        try (FileOutputStream fos = new FileOutputStream(getSer(context))) {
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(devices);
            oos.close();
        } catch (FileNotFoundException e) {
            ITagApplication.errorNotifier.onNext(e);
            e.printStackTrace();
        } catch (IOException e) {
            ITagApplication.errorNotifier.onNext(e);
            e.printStackTrace();
        }
    }

    static public void remember(@NonNull final Context context, @NonNull final BluetoothDevice device) {
        if (findByAddr(device.getAddress()) == null) {
            devices.add(new Device(device));
            save(context);
        }
    }

    static public void forget(@NonNull final Context context, @NonNull final BluetoothDevice device) {
        Device existing = findByAddr(device.getAddress());
        if (existing != null) {
            devices.remove(existing);
            save(context);
        }
    }

    static public boolean has(@NonNull final BluetoothDevice device) {
        return findByAddr(device.getAddress()) != null;
    }
}
