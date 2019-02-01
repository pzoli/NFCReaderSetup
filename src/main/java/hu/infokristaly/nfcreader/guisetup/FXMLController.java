package hu.infokristaly.nfcreader.guisetup;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.glass.events.KeyEvent;
import java.awt.AWTException;
import java.awt.Robot;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import jssc.SerialPortList;

/**
 * @see https://stackoverflow.com/questions/336714/reading-serial-port-in-java
 * @author pzoli
 */
public class FXMLController implements Initializable, jssc.SerialPortEventListener {

    private SerialPort serialPort;
    private java.awt.Robot robot;

    @FXML
    private ComboBox serialPortList;
    @FXML
    private Button sendButton;
    @FXML
    private Button connectButton;
    @FXML
    private CheckBox chxUseDHCP;
    @FXML
    private TextField edReaderIP;
    @FXML
    private TextField edSubnetMask;
    @FXML
    private TextField edGateway;
    @FXML
    private TextField edDNS;
    @FXML
    private TextField edServerIp;
    @FXML
    private TextField edRequest;

    private byte[] parseIp(String value) throws IllegalArgumentException {
        String[] readerIP = value.split("\\.");
        byte idx = 0;
        byte result[] = {0, 0, 0, 0};
        for (String ipPart : readerIP) {
            result[idx++] = Short.valueOf(ipPart).byteValue();
        }
        if (idx != 4) {
            throw new IllegalArgumentException("Not valid IPv4 address");
        }
        return result;
    }

    @FXML
    private void handleSendButtonAction(ActionEvent event) {
        try {
            System.out.println("Write config");
            ReaderConfig conf = new ReaderConfig();
            conf.ip = parseIp(edReaderIP.getText());
            conf.subnet = parseIp(edSubnetMask.getText());
            conf.gateway = parseIp(edGateway.getText());
            conf.dnsserver = parseIp(edDNS.getText());
            byte reqArray[] = edRequest.getText().getBytes("852");
            conf.requestlen = Integer.valueOf(reqArray.length).byteValue();
            serialPort.writeBytes(new byte[]{'c', 'o', 'n', 'f', 'i', 'g', 'u', 'r', 'e', 0x0});
            serialPort.writeBytes(new byte[]{(byte)0xDE, (byte)0xAD, (byte)0xBE, (byte)0xEF, (byte)0xFE, (byte)0xED});
            serialPort.writeByte(chxUseDHCP.isSelected() ? (byte) 1 : (byte) 0);
            serialPort.writeBytes(conf.serverip);
            serialPort.writeBytes(conf.ip);
            serialPort.writeBytes(conf.subnet);
            serialPort.writeBytes(conf.gateway);
            serialPort.writeBytes(conf.dnsserver);
            serialPort.writeBytes(reqArray);
            
            serialPort.writeBytes(reqArray);
        } catch (SerialPortException ex) {
            Logger.getLogger(FXMLController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(FXMLController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static int maxLength = 256;

    @FXML
    private void onEdRequestChange(ObservableValue<? extends String> observable,
            String oldValue, String newValue) {
        if (newValue.length() > maxLength) {
            edRequest.setText(oldValue);
        }
    }

    @FXML
    private void handleConnectButtonAction(ActionEvent event) {
        if (serialPortList.getValue() != null) {
            if (serialPort == null) {
                serialPort = new SerialPort(serialPortList.getValue().toString());
            }
            if (!serialPort.isOpened()) {
                try {
                    //Open port
                    serialPort.openPort();
                    //We expose the settings. You can also use this line - serialPort.setParams(9600, 8, 1, 0);
                    serialPort.setParams(SerialPort.BAUDRATE_115200,
                            SerialPort.DATABITS_8,
                            SerialPort.STOPBITS_1,
                            SerialPort.PARITY_NONE);
                    int mask = SerialPort.MASK_RXCHAR;
                    serialPort.setEventsMask(mask);
                    serialPort.addEventListener(this);
                    setState();
                } catch (SerialPortException ex) {
                    Logger.getLogger(FXMLController.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                try {
                    serialPort.closePort();
                    setState();
                } catch (SerialPortException ex) {
                    Logger.getLogger(FXMLController.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private ReaderConfig getSetupFromString(String conf) {
        Gson gson = new GsonBuilder().create();
        ReaderConfig config = gson.fromJson(conf, ReaderConfig.class);
        return config;
    }
    public void serialEvent(SerialPortEvent event) {
        if (event.isRXCHAR()) {
            int count = event.getEventValue();
            if (count > 0) {
                try {
                    byte buffer[] = serialPort.readBytes(count);
                    String input = new String(buffer);
                    System.out.print(input);
                } catch (SerialPortException ex) {
                    System.out.println(ex);
                }
            }
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            robot = new Robot();
            String[] portNames = SerialPortList.getPortNames();
            for (int i = 0; i < portNames.length; i++) {
                serialPortList.getItems().add(new String(portNames[i]));
            }

            edRequest.textProperty().addListener(new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> observable,
                        String oldValue, String newValue) {
                    if(newValue.length() > maxLength)
                    edRequest.setText(oldValue);
                }
            });

        } catch (AWTException ex) {
            Logger.getLogger(FXMLController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        closeSerialPort();
        super.finalize();
    }

    void closeSerialPort() {
        if ((serialPort != null) && serialPort.isOpened()) {
            try {
                serialPort.closePort();
            } catch (SerialPortException ex) {
                Logger.getLogger(FXMLController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    public void setState() {
        boolean b = serialPort.isOpened();
        connectButton.setText(b ? "Disconnect" : "Connect");
        chxUseDHCP.setDisable(!b);
        serialPortList.setDisable(b);
        edDNS.setDisable(!b);
        edReaderIP.setDisable(!b);
        edGateway.setDisable(!b);
        edSubnetMask.setDisable(!b);
        edServerIp.setDisable(!b);
        edRequest.setDisable(!b);
        sendButton.setDisable(!b);
    }

}
