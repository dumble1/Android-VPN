package com.example.anonymous;

import android.content.Intent;
import android.net.VpnService;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;


import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;


/*
Created by 신승수 on 2016-06-27.
*/
public class Myvpn extends VpnService {
    private Thread mThread;
    private ParcelFileDescriptor mInterface;
    private Messenger mRemote;
    //a. Configure a builder for the interface.
    private  final String Tag = "VPNLog";
    Builder builder = new Builder();
    final DBManager dbManager = new DBManager(this, "Packet.db",null,1);

    @Override
    public IBinder onBind(Intent intent){
        return new Messenger(new RemoteHandler()).getBinder();
    }
    public void remoteSendMessage(String data){
        if(mRemote != null){
            Message msg =  new Message();
            msg.what = 1;
            msg.obj = data;
            try {
                mRemote.send(msg);
            }catch (RemoteException e){
                e.printStackTrace();
            }
        }
    }
    public class RemoteHandler extends Handler{
        @Override
        public void handleMessage(Message msg){
            switch (msg.what){
                case 0 :
                    mRemote = (Messenger) msg.obj;
                    Start();
                    break;
                default:
                    break;
            }
        }
    }
    // Services interface

  //  public int onStartCommand(final Intent intent, int flags, int startId) {
    public int Start(){
    // Start a new session by creating a new thread.
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    Log.d (Tag,"get packet");
                    dbManager.delete("delete from PACKET_LIST;");                   //clear the db
                    //a. Configure the TUN and get the interface.
                    mInterface = builder.setSession("AnonymousPC")
                            .addAddress("192.168.0.1", 24)
                            .addDnsServer("8.8.8.8")
                            .addRoute("0.0.0.0", 0).establish();
                    //b. Packets to be sent are queued in this input stream.
                    FileInputStream in = new FileInputStream(
                            mInterface.getFileDescriptor());
                    //b. Packets received need to be written to this output stream.
                    FileOutputStream out = new FileOutputStream(
                            mInterface.getFileDescriptor());
                    //c. The UDP channel can be used to pass/get ip package to/from server
                    DatagramChannel tunnel = DatagramChannel.open();
                    // Connect to the server, localhost is used for demonstration only.
                    tunnel.connect(new InetSocketAddress("147.46.216.84", 7979));
                    //d. Protect this socket, so package send by it will not be feedback to the vpn service.
                    protect(tunnel.socket());
                    //e. Use a loop to pass packets.

                    //Allocate the buffer for a single packet.
                    ByteBuffer packet = ByteBuffer.allocate(32767);
                    int count =0;
                    while (true) {
                        //Log.d(Tag,String.valueOf(count++));
                        //BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                        int length = in.read(packet.array());
                        if(length > 0) {
                            Log.d(Tag, "packet is longer than 0");
                            packet.limit(length);

                            Log.d(Tag, "copy to c");
                            byte[] c = packet.array();

                            String anspacket = new String();

                            anspacket = byteArrayToHex(c);          //change byte array to hex string

                           // Log.d(Tag, "answer " + anspacket);
                            remoteSendMessage(anspacket);
                            //anspacket = "insert into PACKET_LIST values(null, '" + anspacket + "' );";    // 패킷이 깨져서 DB에 안들어간다??
                            //dbManager.insert(anspacket);
                            //핸들러를 써보자.
                            tunnel.write(packet);
                            packet.clear();
                            length =0;
                        }
                        Thread.sleep(1000);                     //sleep 중에 interrupt 하면 exception이 생기는데 try catch하면 또 안되네..
                    }

                } catch (Exception e) {
                    // Catch any exception
                    e.printStackTrace();
                } finally {
                    try {
                        if (mInterface != null) {
                            mInterface.close();
                            mInterface = null;
                        }
                    } catch (Exception e) {

                    }
                }
            }

        }, "MyVpnRunnable");

    //start the service
        mThread.start();
        return START_STICKY;
    }

    public boolean onUnbind(Intent intent){
        Log.d("VPNLog","Endhgfjhk");
// TODO Auto-generated method stub
        if (mThread != null) {
            mThread.interrupt();
        }
        return super.onUnbind(intent);
    }
    @Override
    public void onDestroy() {
        Log.d("VPNLog","End");
// TODO Auto-generated method stub
        if (mThread != null) {
            mThread.interrupt();
        }
        super.onDestroy();

    }
    // hex to byte[]
    public byte[] hexToByteArray(String hex) {
        if (hex == null || hex.length() == 0) {
            return null;
        }
        byte[] ba = new byte[hex.length() / 2];
        for (int i = 0; i < ba.length; i++) {
            ba[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return ba;
    }

    // byte[] to hex
    public String byteArrayToHex(byte[] ba) {
        if (ba == null || ba.length == 0) {
            return null;
        }

        StringBuffer sb = new StringBuffer(ba.length * 2);
        String hexNumber;
        for (int x = 0; x < ba.length; x++) {
            hexNumber = "0" + Integer.toHexString(0xff & ba[x]);

            sb.append(hexNumber.substring(hexNumber.length() - 2));
        }
        return sb.toString();
    }

}