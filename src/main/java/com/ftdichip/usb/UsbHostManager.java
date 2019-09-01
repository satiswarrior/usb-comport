/*
 * Copyright (C) 1999 - 2001, International Business Machines
 * Corporation. All Rights Reserved. Provided and licensed under the terms and
 * conditions of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 *
 * Copyright (C) 2014 Key Bridge LLC. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.ftdichip.usb;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.usb.UsbDevice;
import javax.usb.UsbException;
import javax.usb.UsbHub;
import javax.usb.UsbServices;

/**
 * Default programmer entry point for the {@code javax.usb} class library.
 * <p>
 * This class implements the JSR80 defined USB class to (perhaps unnecessarily)
 * emphasize the differentiation in this implementation.
 * <p>
 * This class instantiates the platform-specific instance of the UsbServices
 * interface. From the UsbServices instance, the virtual root UsbHub is
 * available.
 * <p>
 * To get started: null {@code IUsbServices USB_SERVICES = USB.getUsbServices();
 * IUsbHub usbHub = USB_SERVICES.getRootUsbHub();
 * System.out.println("Number of ports: " + usbHub.getNumberOfPorts());}
 *
 * @author Dan Streetman
 * @author E. Michael Maximilien
 * @author Jesse Caulfield (complete rewrite)
 */
public final class UsbHostManager {

    /**
     * Get the UsbServices implementation.
     *
     * @return The UsbServices implementation instance.
     * @exception UsbException If there is an error creating the UsbSerivces
     * implementation.
     * @exception SecurityException If the caller does not have security access.
     */
    public static UsbServices getUsbServices() throws UsbException, SecurityException {
        synchronized (servicesLock) {
            if (null == usbServices) {
                usbServices = createUsbServices();
            }
        }

        return usbServices;
    }

    /**
     * Get the Properties loaded from the properties file.
     * <p>
     * If the properties have not yet been loaded, this loads them.
     *
     * @return An copy of the Properties.
     * @exception UsbException If an error occurs while loading the properties.
     * @exception SecurityException If the caller does not have security access.
     */
    public static Properties getProperties() throws UsbException, SecurityException {
        synchronized (propertiesLock) {
            if (!propertiesLoaded) {
                setupProperties();
            }
        }

        return (Properties) properties.clone();
    }

    /**
     * Create the UsbServices implementation instance.
     * <p>
     * This creates the UsbServices implementation instance based on the class
     * named in the properties file.
     *
     * @return The UsbServices implementation instance.
     * @exception UsbException If the UsbServices class could not be
     * instantiated.
     * @exception SecurityException If the caller does not have security access.
     */
    private static UsbServices createUsbServices() throws UsbException, SecurityException {
        String className = getProperties().getProperty(JAVAX_USB_USBSERVICES_PROPERTY);

        if (null == className) {
            throw new UsbException(USBSERVICES_PROPERTY_NOT_DEFINED());
        }

        try {
            return (UsbServices) Class.forName(className).newInstance();
        } catch (ClassNotFoundException cnfE) {
            throw new UsbException(USBSERVICES_CLASSNOTFOUNDEXCEPTION(className) + " : " + cnfE.getMessage());
        } catch (ExceptionInInitializerError eiiE) {
            throw new UsbException(USBSERVICES_EXCEPTIONININITIALIZERERROR(className) + " : " + eiiE.getMessage());
        } catch (InstantiationException iE) {
            throw new UsbException(USBSERVICES_INSTANTIATIONEXCEPTION(className) + " : " + iE.getMessage());
        } catch (IllegalAccessException iaE) {
            throw new UsbException(USBSERVICES_ILLEGALACCESSEXCEPTION(className) + " : " + iaE.getMessage());
        } catch (ClassCastException ccE) {
            throw new UsbException(USBSERVICES_CLASSCASTEXCEPTION(className) + " : " + ccE.getMessage());
        }
    }

    /**
     * Set up the Properties using the properties file.
     * <p>
     * This populates the Properties using the key-values listed in the
     * properties file.
     *
     * @exception UsbException If an error occurs.
     * @exception SecurityException If the caller does not have security access.
     */
    private static void setupProperties() throws UsbException, SecurityException {
        InputStream i = null;

        // First look in 'java.home'/lib
        String h = System.getProperty("java.home");
        String s = System.getProperty("file.separator");
        if (null != h && null != s) {
            try {
                i = new FileInputStream(h + s + "lib" + s + JAVAX_USB_PROPERTIES_FILE);
            } catch (FileNotFoundException fnfE) {
                /* no 'java.home'/lib properties file, ok */ }
        }

        // Now check the normal CLASSPATH
        if (null == i) {
            i = UsbHostManager.class.getClassLoader().getResourceAsStream(JAVAX_USB_PROPERTIES_FILE);
        }

        if (null == i) {
            throw new UsbException(PROPERTIES_FILE_NOT_FOUND);
        }

        try {
            properties.load(i);
        } catch (IOException ioE) {
            throw new UsbException(PROPERTIES_FILE_IOEXCEPTION_READING + " : " + ioE.getMessage());
        }

        propertiesLoaded = true;

        try {
            i.close();
        } catch (IOException ioE) {
//FIXME - handle this better than System.err
            System.err.println(PROPERTIES_FILE_IOEXCEPTION_CLOSING + " : " + ioE.getMessage());
        }
    }

    public static final String JAVAX_USB_PROPERTIES_FILE = "javax.usb.properties";
    public static final String JAVAX_USB_USBSERVICES_PROPERTY = "javax.usb.services";

    private static final String PROPERTIES_FILE_NOT_FOUND = "Properties file " + JAVAX_USB_PROPERTIES_FILE + " not found.";
    private static final String PROPERTIES_FILE_IOEXCEPTION_READING = "IOException while reading properties file " + JAVAX_USB_PROPERTIES_FILE;
    private static final String PROPERTIES_FILE_IOEXCEPTION_CLOSING = "IOException while closing properties file " + JAVAX_USB_PROPERTIES_FILE;

    private static final String USBSERVICES_PROPERTY_NOT_DEFINED() {
        return "The property " + JAVAX_USB_USBSERVICES_PROPERTY + " is not defined as the implementation class of UsbServices";
    }

    private static final String USBSERVICES_CLASSNOTFOUNDEXCEPTION(String c) {
        return "The UsbServices implementation class " + c + " was not found";
    }

    private static final String USBSERVICES_EXCEPTIONININITIALIZERERROR(String c) {
        return "an Exception occurred during initialization of the UsbServices Class " + c;
    }

    private static final String USBSERVICES_INSTANTIATIONEXCEPTION(String c) {
        return "An Exception occurred during instantiation of the UsbServices implementation " + c;
    }

    private static final String USBSERVICES_ILLEGALACCESSEXCEPTION(String c) {
        return "An IllegalAccessException occurred while creating the UsbServices implementation " + c;
    }

    private static final String USBSERVICES_CLASSCASTEXCEPTION(String c) {
        return "The class " + c + " does not implement UsbServices";
    }

    private static boolean propertiesLoaded = false;
    private static Properties properties = new Properties();
    private static Object propertiesLock = new Object();

    private static UsbServices usbServices = null;
    private static Object servicesLock = new Object();

  /**
   * Get the virtual IUsbHub to which all physical Host Controller IUsbHubs are
   * attached.
   * <p>
   * The USB root hub is a special hub at the top of the topology tree. The USB
   * 1.1 specification mentions root hubs in sec 5.2.3, where it states that
   * 'the host includes an embedded hub called the root hub'. The implication of
   * this seems to be that the (hardware) Host Controller device is the root
   * hub, since the Host Controller device 'emulates' a USB hub, and in systems
   * with only one physical Host Controller device, its emulated hub is in
   * effect the root hub. However when multiple Host Controller devices are
   * considered, there are two (2) options that were considered:
   * <ol>
   * <li>Have an array or list of the available topology trees, with each
   * physical Host Controller's emulated root hub as the root IUsbHub of that
   * particular topology tree. This configuration could be compared to the
   * MS-DOS/Windows decision to assign drive letters to different physical
   * drives (partitions).
   * </li>
   * <li>Have a 'virtual' root hub, which is completely virtual (not associated
   * with any physical device) and is created and managed solely by the
   * javax.usb implementation. This configuration could be compared to the UNIX
   * descision to put all physical drives on 'mount points' under a single
   * 'root' (/) directory filesystem.
   * </li>
   * </ol>
   * <p>
   * The first configuration results in having to maintain a list of different
   * and completely unconnected device topologies. This means a search for a
   * particular device must be performed on all the device topologies. Since a
   * IUsbHub already has a list of UsbDevices, and a IUsbHub <i>is</i> a
   * UsbDevice, introducing a new, different list is not a desirable action,
   * since it introduces extra unnecessary steps in performing actions, like
   * searching.
   * <p>
   * As an example, a recursive search for a certain device in the first
   * configuration involves getting the first root IUsbHub, getting all its
   * attached UsbDevices, and checking each device; any of those devices which
   * are IUsbHubs can be also searched recursively. Then, the entire operation
   * must be performed on the next root IUsbHub, and this is repeated for all
   * the root IUsbHubs in the array/list. In the second configuration, the
   * virtual root IUsbHub is recursively searched in a single operation.
   * <p>
   * The second configuration is what is used in this API. The implementation is
   * responsible for creating a single root IUsbHub which is completely virtual
   * (and available through the IUsbServices object). Every IUsbHub attached to
   * this virtual root IUsbHub corresponds to a physical Host Controller's
   * emulated hub. I.e., the first level of UsbDevices under the virtual root
   * IUsbHub are all IUsbHubs corresponding to a particular Host Controller on
   * the system. Note that since the root IUsbHub is a virtual hub, the number
   * of ports is not posible to specify; so all that is guaranteed is the number
   * of ports is at least equal to the number of IUsbHubs attached to the root
   * IUsbHub. The number of ports on the virtual root IUsbHub may change if
   * IUsbHubs are attached or detached (e.g., if a Host Controller is physically
   * hot-removed from the system or hot-plugged, or if its driver is dynamically
   * loaded, or for any other reason a top-level Host Controller's hub is
   * attached/detached). This API specification suggests that the number of
   * ports for the root IUsbHub equal the number of directly attached IUsbHubs.
   *
   * @return The virtual IUsbHub object.
   * @exception UsbException      If there is an error accessing javax.usb.
   * @exception SecurityException If current client not configured to access
   *                              javax.usb.
   */
  public static UsbHub getRootUsbHub() throws UsbException {
    return getUsbServices().getRootUsbHub();
  }

  /**
   * Get a List of all devices that match the specified vendor and product id.
   * <p>
   * Set the productID to capture all USB devices with the given vendor id.
   *
   * @param usbDevice The IUsbDevice to check. If null then a new recursive
   *                  search from the ROOT device will be initiated.
   * @param vendorId  The vendor ID to match.
   * @param productId (Optional) The product id to match. Set to MINUS ONE (-1)
   *                  to match all vendor IDs.
   * @return A non-null ArrayList instance containing any matching
   *         IUsbDevice(s).
   * @throws javax.usb3.exception.UsbException if the USB bus cannot be accessed
   *                                           (e.g. permission error)
   * @since 3.1
   */
  public static List<UsbDevice> getUsbDeviceList(short vendorId, short productId) throws UsbException {
    return getUsbDeviceList(getUsbServices().getRootUsbHub(), vendorId, productId);
  }

  /**
   * Get a List of all devices that match the specified vendor and product id.
   * <p>
   * Set the productID to capture all USB devices with the given vendor id.
   *
   * @param usbDevice The IUsbDevice to check. If null then a new recursive
   *                  search from the ROOT device will be initiated.
   * @param vendorId  The vendor ID to match.
   * @param productId (Optional) The product id to match. Set to MINUS ONE (-1)
   *                  to match all vendor IDs.
   * @return A non-null ArrayList instance containing any matching
   *         IUsbDevice(s).
   * @throws javax.usb3.exception.UsbException if the USB bus cannot be accessed
   *                                           (e.g. permission error)
   * @since 3.1
   */
  public static List<UsbDevice> getUsbDeviceList(UsbDevice usbDevice, short vendorId, short productId) throws UsbException {
    List<UsbDevice> iUsbDeviceList = new ArrayList<>();
    /**
     * If the usbDevice is null then get initialize the search at the virtual
     * ROOT hub.
     */
    if (usbDevice == null) {
      return getUsbDeviceList(getUsbServices().getRootUsbHub(), vendorId, productId);
    }
    /*
     * A device's descriptor is always available. All descriptor field names and
     * types match exactly what is in the USB specification.
     */
    if (vendorId == usbDevice.getUsbDeviceDescriptor().idVendor()
      && (productId == -1 || productId == usbDevice.getUsbDeviceDescriptor().idProduct())) {
      iUsbDeviceList.add(usbDevice);
    }
    /*
     * If the device is a HUB then recurse and scan the hub connected devices.
     * This is just normal recursion: Nothing special.
     */
    if (usbDevice.isUsbHub()) {
      for (Object usbDeviceTemp : ((UsbHub) usbDevice).getAttachedUsbDevices()) {
        iUsbDeviceList.addAll(getUsbDeviceList((UsbDevice)usbDeviceTemp, vendorId, productId));
      }
    }
    return iUsbDeviceList;
  }

  /**
   * Get a List of all devices that match the specified vendor and product id.
   * <p>
   * Set the productID to capture all USB devices with the given vendor id.
   *
   * @param usbDevice The IUsbDevice to check.
   * @param vendorId  The vendor ID to match.
   * @param productId (Optional) A non-null list of product IDs to match.
   *                  Provide an empty list to match all product IDs for the
   *                  given vendor ID.
   * @return A non-null ArrayList instance containing of any matching
   *         IUsbDevice(s).
   * @since 3.1
   */
  public static List<UsbDevice> getUsbDeviceList(short vendorId, List<Short> productId) throws UsbException {
    return getUsbDeviceList(getUsbServices().getRootUsbHub(), vendorId, productId);
  }

  /**
   * Get a List of all devices that match the specified vendor and product id.
   * <p>
   * Set the productID to capture all USB devices with the given vendor id.
   *
   * @param usbDevice The IUsbDevice to check.
   * @param vendorId  The vendor ID to match.
   * @param productId (Optional) A non-null list of product IDs to match.
   *                  Provide an empty list to match all product IDs for the
   *                  given vendor ID.
   * @return A non-null ArrayList instance containing of any matching
   *         IUsbDevice(s).
   * @since 3.1
   */
  public static List<UsbDevice> getUsbDeviceList(UsbDevice usbDevice, short vendorId, List<Short> productId) {
    List<UsbDevice> iUsbDeviceList = new ArrayList<>();
    /*
     * A device's descriptor is always available. All descriptor field names and
     * types match exactly what is in the USB specification.
     */
    if (vendorId == usbDevice.getUsbDeviceDescriptor().idVendor()
      && (productId.isEmpty() || productId.contains(usbDevice.getUsbDeviceDescriptor().idProduct()))) {
      iUsbDeviceList.add(usbDevice);
    }
    /*
     * If the device is a HUB then recurse and scan the hub connected devices.
     * This is just normal recursion: Nothing special.
     */
    if (usbDevice.isUsbHub()) {
      for (Object usbDeviceTemp : ((UsbHub) usbDevice).getAttachedUsbDevices()) {
        iUsbDeviceList.addAll(getUsbDeviceList((UsbDevice)usbDeviceTemp, vendorId, productId));
      }
    }
    return iUsbDeviceList;
  }

}
