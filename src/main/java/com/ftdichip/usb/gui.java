package com.ftdichip.usb;

import com.ftdichip.usb.enumerated.FlowControl;
import com.ftdichip.usb.enumerated.LineDatabit;
import com.ftdichip.usb.enumerated.LineParity;
import com.ftdichip.usb.enumerated.LineStopbit;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.usb.UsbClaimException;
import javax.usb.UsbDevice;
import javax.usb.UsbException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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

        final JFrame frame = new JFrame();
        frame.setTitle("MigLayoutSample");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        MigLayout layout = new MigLayout();
        frame.setLayout(layout);

//        Creating the MenuBar and adding components
        JMenuBar mb=new JMenuBar();
        JMenu Menu = new JMenu("About");
        JMenuItem SerialNO=new JMenuItem("Serial Number");
        JMenuItem Ver=new JMenuItem("Version");
        Menu.add(SerialNO); Menu.add(Ver);
        mb.add(Menu);
        frame.add(mb, "wrap");

//        Creating the first panel with labels and comboboxes
        JPanel panel1 = new JPanel();
        JLabel label1 = new JLabel("Electrode break monitoring range:");
        String[] ranges = { "Narrow (+/- 10,23 mV)", "Wide (+/- 20.46 mV)" };
        final JComboBox range = new JComboBox(ranges);

        JLabel label2 = new JLabel("Frequency:");
        label2.setBorder(new EmptyBorder(0, 37, 0, 30));
        String[] frequencies = { "500 Hz", "1000 Hz", "2000 Hz" };
        final JComboBox frequency = new JComboBox(frequencies);
        panel1.add(label1); panel1.add(range); panel1.add(label2); panel1.add(frequency);

        frame.add(panel1, "wrap");

//        Creating the second panel with labels and comboboxes
        JPanel panel2 = new JPanel();
        JLabel label3 = new JLabel("Isoline alignment speed:");
        JLabel label4 = new JLabel("Isoline output to zero:");
        label3.setBorder(new EmptyBorder(0, 0, 0, 55));
        String[] speeds = { "Normal", "High" };
        final JComboBox speed = new JComboBox(speeds);
        speed.setBorder(new EmptyBorder(0, 0, 0, 120));
        String[] checks = { "Off", "On" };
        final JComboBox tozero = new JComboBox(checks);
        panel2.add(label3); panel2.add(speed); panel2.add(label4); panel2.add(tozero); // Components Added using Flow Layout

        frame.add(panel2, "wrap");

//        Creating the panel with labels and buttons
        JLabel label5 = new JLabel("Anser field:");
        JLabel label6 = new JLabel("Log field:");
        JButton ModeInfo = new JButton("Mode Info");
        JButton Start = new JButton("Start");
        JButton Stop = new JButton("Stop");
        JButton Clear = new JButton("Clear");
        JButton Save = new JButton("Save");
        JPanel panel3 = new JPanel();
        label6.setBorder(new EmptyBorder(0, 120, 0, 0));

        panel3.add(label5); panel3.add(ModeInfo); panel3.add(label6); panel3.add(Start); panel3.add(Stop); panel3.add(Clear); panel3.add(Save);
        frame.add(panel3, "wrap");

//        Creating the panel with Text Areas
        JPanel panel4 = new JPanel();
        final JTextArea AnswerField = new JTextArea(10,25);
        AnswerField.setLineWrap(true);
        AnswerField.setRows(10);
        JScrollPane scrollPane1 = new JScrollPane(AnswerField);

        final JTextArea LogField = new JTextArea(10,28);
        LogField.setLineWrap(true);
        LogField.setRows(10);
        JScrollPane scrollPane2 = new JScrollPane(LogField);
        panel4.add(scrollPane1); panel4.add(scrollPane2);

        frame.add(panel4, "wrap");

        frame.pack();
        frame.setSize(650, 400);
        frame.setLocationRelativeTo(null);
        frame.setMinimumSize(new Dimension(650, 400));
        frame.setVisible(true);

        SerialNO.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                byte[] res = new byte[0];
                try {
                    sendRequest((byte) 0x07);
                    res = device.read();
                } catch (UsbException ex) {
                    ex.printStackTrace();
                }
                String str = StringWithSpaces(bytesToHex(res));
                JOptionPane.showMessageDialog(null, "Serial Number:\n" + str);
            }
        });

        Ver.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                byte[] res = new byte[0];
                try {
                    sendRequest((byte) 0x0F);
                    res = device.read();
                } catch (UsbException ex) {
                    ex.printStackTrace();
                }
                String str = StringWithSpaces(bytesToHex(res));
                JOptionPane.showMessageDialog(null, "Version:\n" + str);
            }
        });

//        Listener for range combobox
        range.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int item = range.getSelectedIndex();
                if (item == 0) {
                    try {
                        sendRequest((byte) 0x0D);   // narrow range
                    } catch (UsbException ex) {
                        ex.printStackTrace();
                    }
                }
                if (item == 1) {
                    try {
                        sendRequest((byte) 0x0E);   // wide range
                    } catch (UsbException ex) {
                        ex.printStackTrace();
                    }
                }
                AnswerField.append("Range changed to: " + range.getItemAt(item) + "\n");
            }
        });

        frequency.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int item = frequency.getSelectedIndex();
                if (item == 0) {
                    try {
                        sendRequest((byte) 0x0A);   // 500 Hz
                    } catch (UsbException ex) {
                        ex.printStackTrace();
                    }
                }
                if (item == 1) {
                    try {
                        sendRequest((byte) 0x0B);   // 1000 Hz
                    } catch (UsbException ex) {
                        ex.printStackTrace();
                    }
                }
                if (item == 2) {
                    try {
                        sendRequest((byte) 0x0C);   // 2000 Hz
                    } catch (UsbException ex) {
                        ex.printStackTrace();
                    }
                }
                AnswerField.append("Frequency changed to: " + frequency.getItemAt(item) + "\n");
            }
        });

        speed.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int item = speed.getSelectedIndex();
                if (item == 0) {
                    try {
                        sendRequest((byte) 0x06);   // normal speed
                    } catch (UsbException ex) {
                        ex.printStackTrace();
                    }
                }
                if (item == 1) {
                    try {
                        sendRequest((byte) 0x05);   // high speed
                    } catch (UsbException ex) {
                        ex.printStackTrace();
                    }
                }
                AnswerField.append("Speed changed to: " + speed.getItemAt(item) + "\n");
            }
        });

        tozero.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int item = tozero.getSelectedIndex();
                if (item == 0) {
                    try {
                        sendRequest((byte) 0x04);   // to zero is Off
                    } catch (UsbException ex) {
                        ex.printStackTrace();
                    }
                }
                if (item == 1) {
                    try {
                        sendRequest((byte) 0x03);   // to zero is On
                    } catch (UsbException ex) {
                        ex.printStackTrace();
                    }
                }
                AnswerField.append("Isoline output to zero is " + tozero.getItemAt(item) + "\n");
            }
        });

        ModeInfo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                byte[] res = new byte[0];
                try {
                    sendRequest((byte) 0x02);
                    res = device.read();
                } catch (UsbException ex) {
                    ex.printStackTrace();
                }

                String str = DecodeWorkMode(res);
                AnswerField.append(str);
            }
        });

        Start.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    sendRequest((byte) 0x09);
                } catch (UsbException ex) {
                    ex.printStackTrace();
                }
                byte[] usbFrame = new byte[0];
                try {
                    usbFrame = device.read();
                } catch (UsbException ex) {
                    ex.printStackTrace();
                }

                while (usbFrame.length > 0) {
                    String str = StringWithSpaces(bytesToHex(usbFrame));
                    System.out.println(str);
                    LogField.append(str);
//                    try {
//                        usbFrame = device.read();
//                    } catch (UsbException ex) {
//                        ex.printStackTrace();
//                    }
                }
            }
        });

        Stop.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    sendRequest((byte) 0x08);
                } catch (UsbException ex) {
                    ex.printStackTrace();
                }
            }
        });

        Clear.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                LogField.setText(null);
            }
        });

        Save.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveAs();
            }

            private void saveAs() {
                FileNameExtensionFilter extensionFilter = new FileNameExtensionFilter("Text File", "txt");
                final JFileChooser saveAsFileChooser = new JFileChooser();
                saveAsFileChooser.setApproveButtonText("Save");
                saveAsFileChooser.setFileFilter(extensionFilter);
                int actionDialog = saveAsFileChooser.showOpenDialog(frame);
                if (actionDialog != JFileChooser.APPROVE_OPTION) {
                    return;
                }

                File file = saveAsFileChooser.getSelectedFile();
                if (!file.getName().endsWith(".txt")) {
                    file = new File(file.getAbsolutePath() + ".txt");
                }

                BufferedWriter outFile = null;
                try {
                    outFile = new BufferedWriter(new FileWriter(file));

                    LogField.write(outFile);

                } catch (IOException ex) {
                    ex.printStackTrace();
                } finally {
                    if (outFile != null) {
                        try {
                            outFile.close();
                        } catch (IOException e) {}
                    }
                }
            }
        });

    }

    public static void Connect() throws IOException, Exception {
        device = null;
        try {
            Collection<UsbDevice> Devices = FTDIUtility.findFTDIDevices();
            for (UsbDevice Device : Devices) {
                String DeviceName;
                try {
                    DeviceName = Device.getProductString();
//                    System.out.println((new SimpleDateFormat("dd/MM/yy HH:mm:ss")).format(new Date()) + ": Device - " + DeviceName);
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

//        start program with sending empty command
        sendRequest((byte) 0x08);

//        byte[] usbFrame = device.read();

//        while (usbFrame.length > 0) {
//            System.out.println("   READ " + usbFrame.length + " bytes: " + usbFrame);
//            String byteFrame = bytesToHex(usbFrame);
//            System.out.println(byteFrame);
//            System.out.println(StringWithSpaces(byteFrame));
//            System.out.println(DecodeWorkMode(usbFrame));
//            usbFrame = device.read();
//        }
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
    public static void sendRequest(byte value2) throws UsbException {
        byte[] request = {(byte) 0x55, value2};
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
                "Electrode break monitoring: On\n";
    }

}
