package com.example.tlinz.walkerwifi;

import android.content.Context;
import android.graphics.Color;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final TextView tv = findViewById(R.id.main_text);

        if (tv != null) {
            tv.setText(R.string.user_helloThisIsQuagga);
        }

        Toolbar myToolbar = findViewById(R.id.my_toolbar);

        if (myToolbar != null) {
            setSupportActionBar(myToolbar);
        }
    }

    Menu gOptionsMenu;
    boolean gbWiFiConnected = false;
    boolean gbTcpServerStarted = false;
    TCPclient gTcpClient = null;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.option_menu, menu);
        gOptionsMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_startTCPServer:
                if (!gbTcpServerStarted) {
                    Toast toastTcpServer = Toast.makeText(getApplicationContext(), getString(R.string.user_startingTcpServer), Toast.LENGTH_SHORT);
                    toastTcpServer.show();

                    final TextView tv = findViewById(R.id.main_text);

                    if (tv == null) {
                        return false;
                    }

                    tv.setText("");

                    // ============================
                    gTcpClient = new TCPclient(tv);

                    try {
                        gTcpClient.StartServer();
                    } catch (IOException e) {
                        Log.d("Sascha", e.getMessage());
                    }

                    gbTcpServerStarted = true;

                    ToggleTcpServerMenuItemText(gbTcpServerStarted);
                    return true;
                } else {
                    Toast toastTcpServer = Toast.makeText(getApplicationContext(), getString(R.string.user_stoppingTcpServer), Toast.LENGTH_SHORT);
                    toastTcpServer.show();

                    try {
                        //gTcpClient.myNetThread.stop();
                        gTcpClient.myNetThread = null;

                        if (gTcpClient.in != null) {
                            gTcpClient.in.close();
                        }

                        if (gTcpClient.socket != null) {
                            gTcpClient.socket.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }

                    gbTcpServerStarted = false;

                    ToggleTcpServerMenuItemText(gbTcpServerStarted);
                    return true;
                }

            case R.id.action_connect:
                Toast toastConnect = Toast.makeText(getApplicationContext(), getString(R.string.user_connectingStarted), Toast.LENGTH_SHORT);
                toastConnect.show();

                WiFiConnectResult wiFiConnectResult = ConnectToWiFi();
                boolean bTcpServerCanBeEnabled = MakeToastByWiFiConnectionResult(wiFiConnectResult);

                if (bTcpServerCanBeEnabled){
                    SetTcpServerMenuItemEnabled(true);
                    SetConnectionMenuItemEnabled(false);
                    final TextView connectionText = findViewById(R.id.connectionText);

                    if (connectionText == null) {
                        return false;
                    }

                    connectionText.setBackgroundColor(Color.GREEN);
                    connectionText.setText(R.string.user_connectionEstablished);
                    return true;
                } else {
                    Toast toastConnectError = Toast.makeText(getApplicationContext(), getString(R.string.user_connectionError), Toast.LENGTH_SHORT);
                    toastConnectError.show();
                    SetTcpServerMenuItemEnabled(false);
                    SetConnectionMenuItemEnabled(true);

                    final TextView connectionText = findViewById(R.id.connectionText);

                    if (connectionText == null) {
                        return false;
                    }

                    connectionText.setBackgroundColor(Color.RED);
                    connectionText.setText(R.string.user_connectionError);
                    return false;
                }

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    public enum WiFiConnectResult {
        WIFI_ALREADY_CONNECTED,
        WIFI_CONNECTED,
        WIFI_CONFIGURED_ONLY,
        WIFI_ENABLING_ERROR,
        WIFI_ERROR
    }


    public void SetConnectionMenuItemEnabled(boolean bEnabled){
        gOptionsMenu.getItem(1).setEnabled(bEnabled);
    }

    public void SetTcpServerMenuItemEnabled(boolean bEnabled){
        gOptionsMenu.getItem(0).setEnabled(bEnabled);
    }

    public void ToggleTcpServerMenuItemText(boolean bTcpServerIsOn){
        if (bTcpServerIsOn){
            gOptionsMenu.getItem(0).setTitle(R.string.action_stopTCPServer);
        } else {
            gOptionsMenu.getItem(0).setTitle(R.string.action_startTCPServer);
        }

    }

    public boolean MakeToastByWiFiConnectionResult(WiFiConnectResult wiFiConnectResult){
        String toastText;
        boolean bTcpServerCanBeEnabled = false;

        switch (wiFiConnectResult){
            case WIFI_ENABLING_ERROR:
                toastText = getString(R.string.user_WiFiCanNotBeEnabled);
                break;
            case WIFI_ALREADY_CONNECTED:
                toastText = getString(R.string.user_alreadyConnected);
                bTcpServerCanBeEnabled = true;
                break;
            case WIFI_CONNECTED:
                toastText = "connected :-)";
                bTcpServerCanBeEnabled = true;
                break;
            case WIFI_CONFIGURED_ONLY:
                toastText = "WiFi configuration added to known networks.  Please check connection and try again";
                break;
            case WIFI_ERROR:
            default:
                toastText = "WiFi connection error. Please check settings and try again";
                break;
        }

        Toast wifiConnectionToast = Toast.makeText(getApplicationContext(), toastText, Toast.LENGTH_LONG);
        wifiConnectionToast.show();

        return bTcpServerCanBeEnabled;
    }

    public WiFiConnectResult ConnectToWiFi(){
        final String SSID = getString(R.string.user_WiFiSSID);
        final TextView tv = findViewById(R.id.main_text);
        WifiManager wifiManager = (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (tv == null || wifiManager == null) {
            return WiFiConnectResult.WIFI_ERROR;
        }

        try {
            // create new wifi config for the wifi of the device
            WifiConfiguration conf = new WifiConfiguration();
            conf.SSID = "\"" + SSID + "\"";
            conf.preSharedKey = "\"" + getString(R.string.user_WiFiPassphrase) + "\"";
            conf.status = WifiConfiguration.Status.ENABLED;
            conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);

            tv.append(getString(R.string.user_TryingToConnect) + "\n");
            Log.d("connecting", conf.SSID + " " + conf.preSharedKey);

            // check if wifi is enabled, enable it if necessary
            if (!wifiManager.isWifiEnabled()){
                tv.append(getString(R.string.user_WiFiDiasabled));
                if (!wifiManager.setWifiEnabled(true)) {
                    tv.append(getString(R.string.user_WiFiCanNotBeEnabled) + "\n");
                    return WiFiConnectResult.WIFI_ENABLING_ERROR;
                } else {
                    tv.append(getString(R.string.user_WiFiHasBeenEnabled) + "\n");
                }
            } else {
                try {
                    // check if already connected
                    WifiInfo connectionInfo = wifiManager.getConnectionInfo();

                    if ((connectionInfo != null) && connectionInfo.getSSID().equals("\"" + SSID + "\"") && connectionInfo.getIpAddress() != 0){
                        tv.append("SSID: " + connectionInfo.getSSID() + "\n");
                        tv.append("IP: " + connectionInfo.getIpAddress() + "\n");
                        tv.append(getString(R.string.user_alreadyConnected) + "\n");
                        gbWiFiConnected = true;
                        return WiFiConnectResult.WIFI_ALREADY_CONNECTED;
                    } else {
                        tv.append("not connected yet\n");
                        tv.append("actual SSID: " + connectionInfo.getSSID() + "\n");
                    }
                } catch (Exception e){

                }
            }

            // add network to known networks and reconnect
            wifiManager.addNetwork(conf);

            Log.d("after adding network", conf.SSID + " " + conf.preSharedKey);

            boolean bFound = false;
            List<WifiConfiguration> list2 = wifiManager.getConfiguredNetworks();
            for( WifiConfiguration i : list2 ) {
                if(i.SSID != null && i.SSID.equals("\"" + SSID + "\"")) {
                    wifiManager.disconnect();
                    wifiManager.enableNetwork(i.networkId, true);
                    wifiManager.reconnect();
                    Log.d("re connecting", i.SSID + " " + conf.preSharedKey);
                    return WiFiConnectResult.WIFI_CONFIGURED_ONLY;
                }
            }

            WifiInfo connectionInfo = wifiManager.getConnectionInfo();

            if ((connectionInfo != null) && connectionInfo.getSSID().equals("\"" + SSID + "\"") && connectionInfo.getIpAddress() != 0){
                tv.append("SSID: " + connectionInfo.getSSID() + "\n");
                tv.append("IP: " + connectionInfo.getIpAddress() + "\n");
                tv.append(getString(R.string.user_alreadyConnected) + "\n");
                gbWiFiConnected = true;
                return WiFiConnectResult.WIFI_CONNECTED;
            }

            return WiFiConnectResult.WIFI_ERROR;

        } catch (Exception ex) {
            System.out.println(Arrays.toString(ex.getStackTrace()));
            return WiFiConnectResult.WIFI_ERROR;
        }
    }
}


class TCPclient extends AppCompatActivity {
    final String hostname = "192.168.4.1";
    final int port = 80;
    Thread myNetThread;
    TextView myOutput;
    //    PrintWriter out;
    BufferedReader in;
    Socket socket;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (myOutput != null) {
                myOutput.append(msg.getData().getString("msg"));
            }
        }

    };

    public void mkmsg(String str) {
        //handler junk, because thread can't update screen!
        Message msg = new Message();
        Bundle b = new Bundle();
        b.putString("msg", str);
        msg.setData(b);
        handler.sendMessage(msg);
    }

    TCPclient(TextView output){
        myOutput = output;
    }

    public void StartServer() throws IOException {
        InetAddress serverAddr = InetAddress.getByName(hostname);
        socket = new Socket(serverAddr, port);
        Log.d("Sascha", "TCP Server started");

        TcpNetwork tcpNetwork = new TcpNetwork();
        Thread tcpNetworkThread = new Thread(tcpNetwork);
        tcpNetworkThread.start();
    }

    public class TcpNetwork implements Runnable {
        public PrintWriter out;
        public BufferedReader in;

        public void run() {
            try {
                while (true) {
                    //                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    try {
        /*
                        String message = "Hello from Client android emulator";
                        write a message to the server
                        mkmsg("Attempting to send message ...\n");
                        out.println(message);
                        mkmsg("Message sent...\n");
        */

                        String str = in.readLine();
                        if (!str.isEmpty()) {
                            mkmsg(str + "\n");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                mkmsg(e.getMessage());
            }
        }
    }
}
