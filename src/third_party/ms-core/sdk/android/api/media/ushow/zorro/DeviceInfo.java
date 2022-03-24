package media.ushow.zorro;

import android.hardware.Camera;
import android.os.Build;
import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Locale;

import org.webrtc.Logging;
import org.webrtc.CalledByNative;


public class DeviceInfo {
  private static final String TAG = "DeviceInfo";

  // @CalledByNative
  public static String getDeviceId() {
    String str1 = Build.MANUFACTURER + "/" + Build.MODEL + "/" + Build.PRODUCT + "/" + Build.DEVICE + "/" + Build.VERSION.SDK_INT + "/" + System.getProperty("os.version");
    if (str1 != null)
      str1 = str1.toLowerCase(Locale.ENGLISH); 
    String str2 = Build.ID;
    Pattern pattern = Pattern.compile(".*[A-Z][A-M][0-9]$");
    Matcher matcher = pattern.matcher(str2);
    if (Build.BRAND.toLowerCase(Locale.ENGLISH).equals("samsung") && Build.DEVICE.toLowerCase(Locale.ENGLISH).startsWith("cs02") && 
      !matcher.find() && Build.VERSION.SDK_INT == 19)
      str1 = "yeshen/simulator/" + Build.MODEL + "/" + Build.PRODUCT + "/" + Build.DEVICE + "/" + Build.VERSION.SDK_INT + "/" + System.getProperty("os.version"); 
    return str1;
  }
  
  // @CalledByNative
  public static String getDeviceInfo() {
    String str = Build.MANUFACTURER + "/" + Build.MODEL;
    if (str != null)
      str = str.toLowerCase(Locale.ENGLISH); 
    return str;
  }

  @CalledByNative
  public static int getSdkVersionCode() {
    return Build.VERSION.SDK_INT;
  }

  @CalledByNative
  public static String getManufacturer() {
    return Build.MANUFACTURER;
  }

  @CalledByNative
  public static String getDeviceModel() {
    return Build.MODEL;
  }
  
  // @CalledByNative
  public static String getSystemInfo() {
    return "Android/" + Build.VERSION.RELEASE;
  }
  
  private static final String[] H264_HW_BLACKLIST = new String[] {
      "SAMSUNG-SGH-I337", "Nexus 7", "Nexus 4", "P6-C00", "HM 2A", "XT105", "XT109", "XT1060"
  };
  
  // @CalledByNative
  public static boolean selectFrontCamera() {
    boolean bool = false;
    try {
      bool = (Camera.getNumberOfCameras() > 1) ? true : false;
    } catch (Exception exception) {
      Log.e(TAG, exception.toString());
    } 
    return bool;
  }
  
  // @CalledByNative
  public static int getNumberOfCameras() {
    try {
      return Camera.getNumberOfCameras();
    } catch (Exception exception) {
      Log.e(TAG, exception.toString());
      return 0;
    } 
  }
  
  // @CalledByNative
  public static int getRecommendedEncoderType() {
    List<String> list = Arrays.asList(H264_HW_BLACKLIST);
    if (list.contains(Build.MODEL)) {
      Logging.w(TAG, "Model: " + Build.MODEL + " has black listed H.264 encoder.");
      return 1;
    } 
    if (Build.VERSION.SDK_INT <= 18)
      return 1; 
    return 0;
  }
  
  @CalledByNative
  public static int getNumberOfCPUCores() {
    int b = 0;
    try {
      b = getCoresFromFileInfo("/sys/devices/system/cpu/possible");
      if (b == -1)
        b = getCoresFromFileInfo("/sys/devices/system/cpu/present"); 
      if (b == -1)
        b = getCoresFromCPUFileList(); 
    } catch (SecurityException securityException) {
      b = 0;
    } catch (NullPointerException nullPointerException) {
      b = 0;
    } 
    return b;
  }
  
  @CalledByNative
  public static String getCpuName() {
    BufferedReader br = null;
    String cpuName = "unknown";
    String model;
    try {
      br = new BufferedReader(new FileReader("/proc/cpuinfo"));
      String text;
      while ((text = br.readLine()) != null) {
        if (text.startsWith("Hardware")) {
          String[] array = text.split(":", 2);
          if (array != null && array.length > 1) {
            model = array[1];
            cpuName = model.trim();
          }
          break;
        }
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

    return cpuName; 
  }
  
  @CalledByNative
  public static String getCpuABI() {
    return Build.CPU_ABI;
  }
  
  private static int getCoresFromFileInfo(String file) {
    FileInputStream fileInputStream = null;
    BufferedReader bufferedReader = null;
    try {
      fileInputStream = new FileInputStream(file);
      bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
      String str = bufferedReader.readLine();
      if (fileInputStream != null)
        fileInputStream.close(); 
      return getCoresFromFileString(str);
    } catch (IOException iOException) {
      return -1;
    } finally {
      if (fileInputStream != null) {
        try {
          fileInputStream.close();
        } catch (IOException iOException) {
          Logging.e(TAG, "close file stream", iOException);
        }
      }
      if (bufferedReader != null) {
        try {
          bufferedReader.close();
        } catch (Exception e) {
        }
      }
    }
  }
  
  private static int getCoresFromFileString(String str) {
    if (str == null || !str.matches("0-[\\d]+$"))
      return -1; 
    return Integer.valueOf(str.substring(2)).intValue() + 1;
  }
  
  private static int getCoresFromCPUFileList() {
    return ((new File("/sys/devices/system/cpu")).listFiles(CPU_FILTER)).length;
  }
  
  private static final FileFilter CPU_FILTER = new FileFilter() {
    @Override
    public boolean accept(File param1File) {
      String str = param1File.getName();
      if (str.startsWith("cpu")) {
        for (int b = 3; b < str.length(); b++) {
          if (!Character.isDigit(str.charAt(b)))
            return false; 
        } 
        return true;
      } 
      return false;
    }
  };

  @CalledByNative
  public static int getCPUMaxFreqHz() {
    int maxFreq = -1;
    FileReader fr = null;
    BufferedReader br = null;
    try {
      fr = new FileReader("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq");
      br = new BufferedReader(fr);
      String text = br.readLine();
      maxFreq = Integer.parseInt(text.trim());
    } catch (Exception e) {
      maxFreq = -1;
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

    try {
     if (maxFreq == -1) {
        FileInputStream fileInputStream = new FileInputStream("/proc/cpuinfo");
        try {
          int j = parseFileForValue("cpu MHz", fileInputStream);
          j *= 1000000;
          if (j > maxFreq)
            maxFreq = j;
        } finally {
          if (fileInputStream != null)
            fileInputStream.close();
        }
      }
    } catch (IOException iOException) {
      maxFreq = -1;
    }

    return maxFreq;
  }
  
  private static int parseFileForValue(String value, FileInputStream stream) {
    byte[] arrayOfByte = new byte[1024];
    try {
      int i = stream.read(arrayOfByte);
      for (int b = 0; b < i; b++) {
        if (arrayOfByte[b] == 10 || b == 0) {
          if (arrayOfByte[b] == 10)
            b++; 
          for (int b1 = b; b1 < i; b1++) {
            int j = b1 - b;
            if (arrayOfByte[b1] != value.charAt(j))
              break; 
            if (j == value.length() - 1)
              return extractValue(arrayOfByte, b1); 
          } 
        } 
      } 
    } catch (IOException iOException) {
    
    } catch (NumberFormatException numberFormatException) {}
    return -1;
  }
  
  private static int extractValue(byte[] arr, int val) {
    while (val < arr.length && arr[val] != 10) {
      if (Character.isDigit(arr[val])) {
        int i = val;
        val++;
        while (val < arr.length && Character.isDigit(arr[val]))
          val++; 
        String str = new String(arr, 0, i, val - i);
        return Integer.parseInt(str);
      } 
      val++;
    } 
    return -1;
  }
}

