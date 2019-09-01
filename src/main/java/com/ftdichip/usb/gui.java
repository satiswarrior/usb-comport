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

        device.configureSerialPort(230400, LineDatabit.BITS_8, LineStopbit.STOP_BIT_1, LineParity.NONE, FlowControl.DISABLE_FLOW_CTRL);

        byte value1 = 0x55;
        byte value2 = 0x02;
        byte[] request = {value1, value2};

        sendrequest(value2);

        byte[] usbFrame = device.read();

        while (usbFrame.length > 0) {
            System.out.println("   READ " + usbFrame.length + " bytes: " + usbFrame);
            // System.out.println(new BigInteger(1, usbFrame).toString(16));
            System.out.println(bytesToHex(usbFrame));
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

    // method for sending command to COM-port
    public static void sendrequest(byte value2) throws UsbException {
        byte value1 = 0x55;
        byte[] request = {value1, value2};
//        System.out.println(request[0] + " " + request[1]);
        device.write(request);
    }

}
