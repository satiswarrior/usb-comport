/*
 * Copyright 2014-2016 Key Bridge LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ftdichip.usb;

import com.ftdichip.usb.enumerated.FlowControl;
import com.ftdichip.usb.enumerated.LineDatabit;
import com.ftdichip.usb.enumerated.LineParity;
import com.ftdichip.usb.enumerated.LineStopbit;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.ftdichip.usb.FTDIUtility.MODEM_STATUS_HEADER_LENGTH;

import javax.usb.*;

import com.ftdichip.usb.enumerated.*;
import java.util.Arrays;
import javax.usb.UsbDevice;
import javax.usb.UsbException;

/**
 * FTDI UART read/write utility.
 * <p>
 * This class library is tuned to communicate with {@code FT232R},
 * {@code FT2232} and {@code FT232B} chips from Future Technology Devices
 * International Ltd.
 * <p>
 * Developer note: There is an synchronous and asynchronous WRITE method but no
 * asynchronous READ method. This class wraps the USB I/O read transaction to
 * post-process returned USB packets and to strip FTDI-specific header
 * information before passing data to the reader. This strategy provides a clean
 * READ transaction but would require adding (yet another layer of) event
 * listener/notifier logic to support asynchronous reads. If you really need
 * ASYNC read then you should directly access the {@code UsbPipe}, which already
 * has a listener interface.
 *
 * @author Jesse Caulfield
 * @since v1.0.0 May 06, 2014
 */


public final class FTDI {

    /**
     * The USB Device to which this FTDI instance is attached.
     */
//    private static final String FTDeviceName0 = "MIOKARD-12";
//    private static final String FTDeviceName1 = "ECG";
//
//    public static void main(String[] args) {
//        try {
//            Connect();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//    public void Connect() throws IOException, Exception {
//        device = null;
//        try {
//            Collection<UsbDevice> Devices = FTDIUtility.findFTDIDevices();
//            for (UsbDevice Device : Devices) {
//                String DeviceName;
//                try {
//                    DeviceName = Device.getProductString();
//                    //.
//                    System.out.println((new SimpleDateFormat("dd/MM/yy HH:mm:ss")).format(new Date()) + ": Device - " + DeviceName);
//                } catch (Exception E) {
//                    //. Windows 10 workaround
//                    device = FTDI.getInstance(Device);
//                    //.
//                    break; //. >
//                }
//                //.
//                if (DeviceName.equals(FTDeviceName0) || DeviceName.equals(FTDeviceName1)) {
//                    device = FTDI.getInstance(Device);
//                    //.
//                    break; //. >
//                }
//            }
//        } catch (UsbClaimException UCE) {
//            throw UCE; //. =>
//        } catch (Exception E) {
//            throw new IOException("! error: " + E.getMessage()); //. =>
//        }
//        if (device == null) {
//            throw new IOException("! USB device is not found"); //. =>
//        }
//        //.
//        device.configureSerialPort(230400, LineDatabit.BITS_8, LineStopbit.STOP_BIT_1, LineParity.NONE, FlowControl.DISABLE_FLOW_CTRL);
//    }
//    public void Disconnect() {
//        if (device != null) {
//            try {
//                device.close();
//                //.
//                device = null;
//            } catch (Exception E) {
//                System.out.println((new SimpleDateFormat("dd/MM/yy HH:mm:ss")).format(new Date()) + ": closing device exception - ");
//                //.
//                E.printStackTrace();
//            }
//        }
//    }
//    private FTDI device = null;

//    private static final String FTDeviceName0 = "MIOKARD-12";
//    private static final String FTDeviceName1 = "ECG";
//    public static void main(String[] args) throws UsbException {
//        Collection<UsbDevice> Devices = FTDIUtility.findFTDIDevices();
//
//        for (UsbDevice Device : Devices) {
//            String DeviceName;
//            FTDI device;
//            try {
//                DeviceName = Device.getProductString();
//                System.out.println(Device);
//                System.out.println("Device - " + DeviceName);
//            } catch (Exception E) {
//                device = FTDI.getInstance(Device);
//                System.out.println("exception");
//                break;
//            }
//            //.
////            if (DeviceName.equals(FTDeviceName0) || DeviceName.equals(FTDeviceName1)) {
////                device = FTDI.getInstance(Device);
////                break;
////            }
//        }
//
//    }

    private final UsbDevice usbDevice;
    /**
     * The USB interface (within the IUsbDevice) through which this device
     * communicates. This is extracted from the IUsbDevice and stored here (at
     * the class level) for convenience.
     * <p>
     * IUsbInterface a synchronous wrapper through which this application sends
     * and receives messages with the device.
     */
    private UsbInterface usbInterface;
    /**
     * The USB Pipe used to READ data from the connected device.
     */
    private UsbPipe usbPipeRead;
    /**
     * The USB Pipe used to WRITE data from the connected device.
     */
    private UsbPipe usbPipeWrite;

    /**
     * Construct a new FTDI (read, write) instance.
     * <p>
     * The serial port is automatically configured for default operation at
     * 115200 bps, 8 data bits, no parity, 1 stop bit, no flow control.
     * <p>
     * A user can identify all attached FTDI UART chips on the USB with methods
     * in the {@link #FTDIUtil} class, then communicate with each desired device
     * using one or more instances of this FTDI class.
     *
     * @param usbDevice the specific UsbDevice instance to communicate with
     * @return a new instance
     * @throws UsbException if the USB device is not readable/writable by the
     * current user (permission error)
     */
    public static FTDI getInstance(UsbDevice usbDevice) throws UsbException {
        /**
         * Claim the USB device.
         */
        FTDI ftdi = new FTDI(usbDevice);
        /**
         * Set the serial line configuration: 115200 bps, 8, N, 1, no flow
         * control.
         */
        ftdi.configureSerialPort(FTDIUtility.DEFAULT_BAUD_RATE,
                LineDatabit.BITS_8,
                LineStopbit.STOP_BIT_1,
                LineParity.NONE,
                FlowControl.DISABLE_FLOW_CTRL);
        /**
         * Set the DTR and RTS lines.
         */
        FTDIUtility.setDTRRTS(usbDevice, false, true);
        return ftdi;
    }

    /**
     * Construct a new FTDI (read, write) instance.
     * <p>
     * Important: The serial port is NOT CONFIGURED when using this constructor.
     * You MUST configure the serial port prior to use via
     * {@link #configureSerialPort(int, com.ftdichip.usb.enumerated.ELineDatabits, com.ftdichip.usb.enumerated.ELineStopbits, com.ftdichip.usb.enumerated.ELineParity, com.ftdichip.usb.enumerated.EFlowControl)}.
     * <p>
     * In most circumstances a serial port configuration of 115200 bps, 8 data
     * bits, no parity, 1 stop bit, no flow control will work.
     *
     * @param usbDevice the specific UsbDevice instance to communicate with
     * @throws UsbException if the USB device is not readable/writable by the
     * current user (permission error)
     */
    public FTDI(UsbDevice usbDevice) throws UsbException {
        /**
         * Set the USB Device.
         */
        this.usbDevice = usbDevice;
        /**
         * USB Interfaces: When you want to communicate with an interface or
         * with endpoints of this interface then you have to claim it before
         * using it and you have to release it when you are finished.
         */
        UsbConfiguration configuration = usbDevice.getActiveUsbConfiguration();
        /**
         * Developer note: FTDI devices have only ONE IUsbInterface (Interface
         * #0). Therefore always get and use the first available IUsbInterface
         * from the list.
         * <p>
         * The returned interface setting will be the current active alternate
         * setting if this configuration (and thus the contained interface) is
         * active. If this configuration is not active, the returned interface
         * setting will be an implementation-dependent alternate setting.
         */
        usbInterface = (UsbInterface) configuration.getUsbInterfaces().iterator().next();
        //.
        usbInterface.claim((UsbInterface usbInterface1) -> false);
        /**
         * Scan the interface UsbEndPoint list to set the READ and WRITE USB
         * pipes.
         */
        List Endpoints = usbInterface.getUsbEndpoints();
        int Cnt = Endpoints.size();
        for (int I = 0; I < Cnt; I++) {
            UsbEndpoint EP = (UsbEndpoint) Endpoints.get(I);
            //.
            if (EEndpointDirection.HOST_TO_DEVICE.getByteCode() == EP.getDirection()) {
                usbPipeWrite = EP.getUsbPipe();
            } else {
                usbPipeRead = EP.getUsbPipe();
            }
        }
        /**
         * Add a shutdown hook to disconnect the USB interface and close the USB
         * port when shutting down. This will un-claim the device.
         */
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                try {
                    usbInterface.release();
                    Thread.sleep(250); // wait a quarter second for stuff to settle
                } catch (UsbException | UsbNotActiveException | UsbDisconnectedException | InterruptedException ex) {
                }
            }
        });
    }

    /**
     * Close the FTDI port and release this interface to it may be used by
     * another application.
     * <p>
     * This will only succeed if the interface has been properly claimed. If the
     * native release fails, this will fail. This should be done after the
     * interface is no longer being used. All pipes must be closed before this
     * can be released.
     */
    public void close() {
        try {
            usbInterface.release();
            Thread.sleep(250); // wait a quarter second for stuff to settle
        } catch (UsbException | UsbNotActiveException | UsbDisconnectedException | InterruptedException ex) {
        }
    }

    /**
     * Set the serial port configuration. This is a convenience method to send
     * multiple USB control messages to the FTDI device. It is a shortcut to the
     * same method in {@link FTDIUtility}.
     *
     * @param requestedBaudRate the requested baud rate (bits per second). e.g.
     * 115200.
     * @param bits Number of bits
     * @param stopbits Number of stop bits
     * @param parity LineParity mode
     * @param flowControl flow control to use.
     * @throws UsbException if the FTDI UART cannot be configured or control
     * messages cannot be sent (e.g. insufficient permissions)
     */
    public void configureSerialPort(int requestedBaudRate,
            LineDatabit bits,
            LineStopbit stopbits,
            LineParity parity,
            FlowControl flowControl) throws UsbException {
        FTDIUtility.setBaudRate(usbDevice, requestedBaudRate);
        FTDIUtility.setLineProperty(usbDevice, bits, stopbits, parity);
        FTDIUtility.setFlowControl(usbDevice, flowControl);
    }

    /**
     * Asynchronously write a byte[] array to the FTDI port input buffer.
     * <p>
     * The javax.usb default timeout value of (5000 ms) is used. i.e. This
     * method returns immediately but the system will try (for up to the timeout
     * value) to write data to the USB pipe.
     *
     * @param data A byte array containing the data to write to the device.
     * @exception UsbException If an error occurs.
     */
    public void writeAsync(byte[] data) throws UsbException {
        if (!usbPipeWrite.isOpen()) {
            usbPipeWrite.open();
        }
        UsbIrp usbIrp = usbPipeWrite.createUsbIrp();
        usbIrp.setData(data);
        usbPipeWrite.asyncSubmit(usbIrp);
    }

    /**
     * Synchronously write a byte[] array to the FTDI port input buffer.
     * <p>
     * Developer note: This method returns immediately upon completion, which is
     * when the data has been stuffed into the device IN buffer but not
     * necessarily when the device has read or acted upon the data.
     * <p>
     * Some (slower) USB devices require a little time to read and process
     * instructions from their input buffer. It is important to consider and
     * account for this potential delay during rapid-fire write/read
     * transactions (e.g. within a FOR/WHILE loop). Semaphore (lock objects) or
     * a Thread.sleep() delay of between 5 and 10 milliseconds are typically
     * sufficient.
     *
     * @param data A byte array containing the data to write to the device.
     * @return The number of bytes actually transferred to the device.
     * @exception UsbException If an error occurs.
     */
    public int write(byte[] data) throws UsbException {
        if (!usbPipeWrite.isOpen()) {
            usbPipeWrite.open();
        }
        return usbPipeWrite.syncSubmit(data);
    }

    /**
     * Read a USB frame: Synchronously read available data from the FTDI port
     * output buffer.
     * <p>
     * Developer note: This method automatically strips the FTDI modem status
     * header bytes and only returns actual device data.
     * <p>
     * The maximum length of the returned data byte array is the
     * {@code wMaxPacketSize} value provided by the USB EndPoint descriptor
     * (typically 64 bytes but up to 512 bytes for some newer, faster chips). If
     * there is no data on the device an empty array (length = 0) is returned.
     * <p>
     * The actual length of the data array returned is highly variable and
     * depends upon the query speed: e.g. slower HOST polling results in longer
     * data arrays as the device is able to stuff more data into its output
     * buffer between each request.
     *
     * @return a non-null, variable length byte array containing the actual data
     * produced by the device. The length of the byte array ranges between 0
     * (empty) and 64 bytes (typ), but may be up to 512 bytes.
     * @throws UsbException if the USB Port fails to read
     */
    public byte[] read() throws UsbException {
        if (!usbPipeRead.isOpen()) {
            usbPipeRead.open();
        }
        /**
         * Developer note: Always initialize and use a new byte array when
         * reading from a USB pipe!
         * <p>
         * The syncSubmit and presumably asyncSubmit methods do not clear the
         * input byte array - they merely write bytes into the provided array.
         * Reusing a previously populated byte array may produce JUNK data as
         * new bytes are written over the old bytes. If the new USB frame is
         * shorter than the previous then stale bytes will remain in the buffer
         * and not be overwritten or cleared.
         */
        byte[] usbFrame = new byte[usbPipeRead.getUsbEndpoint().getUsbEndpointDescriptor().wMaxPacketSize()];
        int bytesRead = usbPipeRead.syncSubmit(usbFrame);
        /**
         * Return an empty array if there is no data on the line. Otherwise
         * strip the MODEM_STATUS_HEADER and return only the device data.
         */
        return bytesRead == MODEM_STATUS_HEADER_LENGTH
                ? new byte[0]
                : Arrays.copyOfRange(usbFrame, MODEM_STATUS_HEADER_LENGTH, bytesRead);
    }

    @Override
    public String toString() {
        return "FTDI " + usbDevice;
    }
}
