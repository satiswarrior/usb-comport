package com.ftdichip.usb;

import com.ftdichip.usb.enumerated.FlowControl;
import com.ftdichip.usb.enumerated.LineDatabit;
import com.ftdichip.usb.enumerated.LineParity;
import com.ftdichip.usb.enumerated.LineStopbit;

import javax.usb.UsbClaimException;
import javax.usb.UsbDevice;
import javax.usb.UsbException;
import java.io.IOException;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

public class gui {
    private static final String FTDeviceName0 = "MIOKARD-12";
    private static final String FTDeviceName1 = "ECG";
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    private static FTDI device = null;

    public static void main(String[] args) {
        try {
            Connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void Connect() throws IOException, Exception {
        device = null;
        try {
            Collection<UsbDevice> Devices = FTDIUtility.findFTDIDevices();
            for (UsbDevice Device : Devices) {
                String DeviceName;
                try {
                    DeviceName = Device.getProductString();
                    System.out.println((new SimpleDateFormat("dd/MM/yy HH:mm:ss")).format(new Date()) + ": Device - " + DeviceName);
                } catch (Exception E) {
                    device = FTDI.getInstance(Device);
                    break;
                }

                if (DeviceName.equals(FTDeviceName0) || DeviceName.equals(FTDeviceName1)) {
                    device = FTDI.getInstance(Device);
                    break;
                }
            }
        } catch (UsbClaimException UCE) {
            throw UCE;
        } catch (Exception E) {
            throw new IOException("! error: " + E.getMessage());
        }

        if (device == null) {
            throw new IOException("! USB device is not found");
        }

        device.configureSerialPort(230400,
                                    LineDatabit.BITS_8,
                                    LineStopbit.STOP_BIT_1,
                                    LineParity.NONE,
                                    FlowControl.DISABLE_FLOW_CTRL );

        byte value1 = 0x04;
        byte value2 = 0x02;
        byte[] request = {value1, value2};

        sendrequest(value2);
        String s = "1122";
        System.out.println(StringWithSpaces(s));

        byte[] usbFrame = device.read();

        while (usbFrame.length > 0) {
            System.out.println("   READ " + usbFrame.length + " bytes: " + usbFrame);
            System.out.println(usbFrame[0]);
            String byteFrame = bytesToHex(usbFrame);
            System.out.println(byteFrame);
            System.out.println(StringWithSpaces(byteFrame));
            System.out.println(DecodeWorkMode(usbFrame));
            usbFrame = device.read();
        }
    }

    public static void Disconnect() {
        if (device != null) {
            try {
                device.close();
                device = null;
            } catch (Exception E) {
                System.out.println((new SimpleDateFormat("dd/MM/yy HH:mm:ss")).format(new Date()) + ": closing device exception - ");
                E.printStackTrace();
            }
        }
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

//     method for sending command to COM-port
    public static void sendrequest(byte value2) throws UsbException {
        byte value1 = 0x55;
        byte[] request = {value1, value2};
//        System.out.println(request[0] + " " + request[1]);
        device.write(request);
    }

//    method to split string by bytes
    public static String StringWithSpaces(String str) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            if ((i > 0) && (i % 2 == 0)) {
                result.append(" ");
            }
            result.append(str.charAt(i));
        }
        return result.toString();
    }

//    method to decode work mode
    public static String DecodeWorkMode(byte[] arr) {
        String result[] = new String[6];
        byte HighByte = arr[2];
        byte LowByte = arr[1];
//    Range
        if (HighByte - 0x08 >= 0x00) {
            result[0] = "Wide (+/- 20.46 mV)\n";
            HighByte -= 0x08;
        } else {
            result[0] = "Narrow (+/- 10,23 mV)\n";
            HighByte -= 0x04;
        }
//    Speed
        if (HighByte - 0x02 == 0x00) {
            result[1] = "High\n";
        } else {
            result[1] = "Normal\n";
        }
//    Frequency
        if (LowByte - 0x80 >= 0x00) {
            result[2] = "2000Hz\n";
            LowByte -= 0x80;
        } else if (LowByte - 0x40 >= 0x00) {
            result[2] = "1000Hz\n";
            LowByte -= 0x40;
        } else {
            result[2] = "500Hz\n";
            LowByte -= 0x20;
        }
//    ADS model
        if (LowByte - 0x10 >= 0x00) {
            result[3] = "ADS1298 (24 bits)\n";
            LowByte -= 0x10;
        } else {
            result[3] = "ADS1198 (16 bits)\n";
            LowByte -= 0x08;
        }
//    Control mode
        if (LowByte - 0x04 >= 0x00) {
            result[4] = "On\n";
            LowByte -= 0x04;
        } else {
            result[4] = "Off\n";
        }
//    Electrode break monitoring
        if (LowByte - 0x02 == 0x01) {
            result[5] = "On\n";
        } else {
            result[5] = "Off\n";
        }

        return  "Miocard-12 works in mode:\n" +
                "Electrode break monitoring range: " + result[0] +
                "Isoline alignment speed: " + result[1] +
                "Frequency: " + result[2] +
                "ADS model: " + result[3] +
                "Control mode: " + result[4] +
                "Isoline output to zero: " + result[5] +
                "Electrode break monitoring: On";
    }

}
