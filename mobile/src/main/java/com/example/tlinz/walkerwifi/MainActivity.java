package com.example.tlinz.walkerwifi;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    Menu gOptionsMenu;
    boolean gbWiFiConnected = false;
    boolean gbTcpServerStarted = false;
    TCPclient gTcpClient = null;
    public static MainActivity instance = null;

    @Override
    public void onResume()
    {
        super.onResume();
        instance = this;
    }

    @Override
    public void onPause()
    {
        super.onPause();
        instance = null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.my_toolbar);

        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
    }

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
            // ============================
            // tcp server button pressed
            // ============================
            case R.id.action_startOrStopTCPServer:
                if (!gbTcpServerStarted) {
                    Toast toastTcpServer = Toast.makeText(getApplicationContext(), getString(R.string.user_startingTcpServer), Toast.LENGTH_SHORT);
                    toastTcpServer.show();

                    final TextView tv = findViewById(R.id.main_text);

                    if (tv == null) {
                        return false;
                    }

                    tv.setText("");

                    // ============================
                    // start TCP server
                    // ============================
                    gTcpClient = new TCPclient(getApplicationContext(), tv);
                    gTcpClient.StartServer();
                    // ============================

                    gbTcpServerStarted = true;
                    ToggleTcpServerMenuItemText(true);
                    return true;
                } else {
                    Toast toastTcpServer = Toast.makeText(getApplicationContext(), getString(R.string.user_stoppingTcpServer), Toast.LENGTH_SHORT);
                    toastTcpServer.show();

                    try {
                        // ============================
                        // destroy TCP server
                        // ============================
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
                    ToggleTcpServerMenuItemText(false);
                    return true;
                }

            // ============================
            // connect button pressed
            // ============================
            case R.id.action_connect:
                Toast toastConnect = Toast.makeText(getApplicationContext(), getString(R.string.user_connectingStarted), Toast.LENGTH_SHORT);
                toastConnect.show();

                final TextView tv = findViewById(R.id.main_text);

                if (tv == null){
                    return false;
                }

                WiFiConnectionHelper wiFiConnectionHelper = new WiFiConnectionHelper(tv, getApplicationContext());

                if (instance == null){
                    return false;
                }

                wiFiConnectionHelper.StartScanForSSID(instance);

                gbWiFiConnected = MakeToastByWiFiConnectionResult(wiFiConnectionHelper.ConnectToWiFi());
                boolean bTcpServerCanBeEnabled = gbWiFiConnected;

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
}



