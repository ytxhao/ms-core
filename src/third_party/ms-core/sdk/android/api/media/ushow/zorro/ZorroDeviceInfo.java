package media.ushow.zorro;

import java.lang.String;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import java.io.FileFilter;
import java.util.regex.Pattern;

import android.os.Build;
import android.content.Context;

import org.webrtc.Logging;
import org.webrtc.CalledByNative;

public class ZorroDeviceInfo {
  private static final String TAG = "ZorroDeviceInfo";

  private static String cpuModel;
  private static String cpuName;
  private static int cpuCores;
  private static int cpuMaxFreq;

  public static class DeviceInfo {
    public int androidVersionCodes;
    public String model;
    public String cpu;
    public int cpuMaxFreq;
    public int cpuCores;

    public DeviceInfo(int androidVersionCodes, String model, String cpu, int cpuMaxFreq, int cpuCores) {
      this.androidVersionCodes = androidVersionCodes;
      this.model = model;
      this.cpu = cpu;
      this.cpuMaxFreq = cpuMaxFreq;
      this.cpuCores = cpuCores;
    }

    @CalledByNative("DeviceInfo")
    int getAndroidVersionCodes() {
      return androidVersionCodes;
    }

    @CalledByNative("DeviceInfo")
    String getModel() {
      return model;
    }

    @CalledByNative("DeviceInfo")
    String getCpu() {
      return cpu;
    }

    @CalledByNative("DeviceInfo")
    int getCpuMaxFreq() {
      return cpuMaxFreq;
    }

    @CalledByNative("DeviceInfo")
    int getCpuCores() {
      return cpuCores;
    }
  }

  private static void updateCpuInfo() {
    if (cpuModel != null && cpuName != null) {
      Logging.e(TAG, "haha 1");
      return;
    }
    Logging.e(TAG, "waw waw 1");

    BufferedReader br = null;
    String model = "unknown";
    String name = "unknown";
    try {
      br = new BufferedReader(new FileReader("/proc/cpuinfo"));
      String text;
      int idx = 0;
      while ((text = br.readLine()) != null) {
        if (idx == 0) {
          String[] array = text.split(":\\s+", 2);
          if (array != null && array.length > 1) {
            name = array[1];
            cpuModel = name.trim();
          }
        } else if (text.startsWith("Hardware")) {
          String[] array = text.split(":", 2);
          if (array != null && array.length > 1) {
            model = array[1];
            cpuName = model.trim();
          }
        }
        idx++;
      }
    } catch (Exception e) {

    } finally {
      if (br != null) {
        try {
          br.close();
        } catch (Exception e) {
        }
      }
    }
  }

  private static String getCpuName() {
    if (cpuName != null) {
      return cpuName;
    }
    return "unknown";
  }

  private static String getCpuModel() {
    if (cpuModel != null) {
      return cpuModel;
    }
    return "unknown";
  }

  private static int getCpuMaxFreq() {
    if (cpuMaxFreq != 0) {
    Logging.e(TAG, "haha 2");
      return cpuMaxFreq;
    }

    FileReader fr = null;
    BufferedReader br = null;
    try {
      fr = new FileReader("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq");
      br = new BufferedReader(fr);
      String text = br.readLine();
      cpuMaxFreq = Integer.parseInt(text.trim());
    } catch (Exception e) {
      cpuMaxFreq = -1;
    } finally {
      if (fr != null) {
        try {
          fr.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }

      if (br != null) {
        try {
          br.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    return cpuMaxFreq;
  }

  private static int getCpuCores() {
    if (cpuCores != 0) {
    Logging.e(TAG, "haha 3");
      return cpuCores;
    }
    // Private Class to display only CPU devices in the directory listing
    class CpuFilter implements FileFilter {
      @Override
      public boolean accept(File pathname) {
        // Check if filename is "cpu", followed by a single digit number
        if (Pattern.matches("cpu[0-9]", pathname.getName())) {
          return true;
        }
        return false;
      }
    }

    try {
      // Get directory containing CPU info
      File dir = new File("/sys/devices/system/cpu/");
      // Filter to only list the devices we care about
      File[] files = dir.listFiles(new CpuFilter());
      // Return the number of cores (virtual CPU devices)
      cpuCores = files.length;
    } catch (Exception e) {
      // Default to return -1 means exception
      cpuCores = -1;
    }

    return cpuCores;
  }

  public static DeviceInfo getDeviceInfo() {
    updateCpuInfo();
    return new DeviceInfo(Build.VERSION.SDK_INT, Build.MODEL, getCpuName(), getCpuMaxFreq(), getCpuCores());
  }
  
}

