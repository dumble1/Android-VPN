package org.locationprivacy.vpntest;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.VpnService;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Arrays;


/**
 * Created by user on 2016-08-14.
 */
public class Vpn extends VpnService {
    private static final String TAG = "VpnServiceTest";
    private Thread mThread;
    private ParcelFileDescriptor mInterface;
    Builder builder = new Builder();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mInterface = builder.setSession("MyVPNService")
                            .addAddress("192.168.0.1", 24)
                            .addDnsServer("8.8.8.8")
                            .addRoute("0.0.0.0", 0).establish();
                    FileInputStream in = new FileInputStream(
                            mInterface.getFileDescriptor());
                    FileOutputStream out = new FileOutputStream(
                            mInterface.getFileDescriptor());

                    int length = 0;
                    int outLength = 0;

                    ByteBuffer packet = ByteBuffer.allocate(32767);

                    while (true) {
                        length = in.read(packet.array());
                        if (length > 0) {
                            packet.limit(length);
                            byte[] c = packet.array();
                            packet.clear();

                            Socket socket;
                            BufferedWriter networkWriter;
                            BufferedReader networkReader;

                            int ihl = (c[0] & 0xf) * 4;

                            String sourceIP = (int)(c[12] & 0xff) + "." + (int)(c[13] & 0xff) + "." +  (int)(c[14] & 0xff) + "." + (int)(c[15] & 0xff);
                            String destIP = (int)(c[16] & 0xff) + "." + (int)(c[17] & 0xff) + "." +  (int)(c[18] & 0xff) + "." + (int)(c[19] & 0xff);

                            int sPort = ((c[ihl] & 0xff) << 8) | (c[ihl+1] & 0xff);
                            int dPort = ((c[ihl+2] & 0xff) << 8) | (c[ihl+3] & 0xff);

                            long seqNum = (((c[ihl+4] &0xff) << 24) | ((c[ihl+5] &0xff) << 16) | ((c[ihl+6] &0xff) << 8) | (c[ihl+7] &0xff)) & 0xffffffff;
                            int offset = ((c[ihl+12] & 0xf0) >> 4) * 4;

                            int syn = (c[ihl+13] & 0x2) >> 1;
                            int ack = (c[ihl+13] & 0x10) >> 4;

                            String answer = Arrays.toString(c).substring(ihl + offset);

                            if (sourceIP.contains("147.46.215.152") || destIP.contains("147.46.215.152")) {
                                Log.d(TAG, "IHL: " + ihl);
                                Log.d(TAG, "offset: " + offset);
                                Log.d(TAG, "write: " + length);
                                Log.d(TAG, "source IP: " + sourceIP + ":" + sPort);
                                Log.d(TAG, "dest IP: " + destIP + ":" + dPort);
                                Log.d(TAG, "syn: " + syn);
                                Log.d(TAG, "ack: " + ack);
                                Log.d(TAG, "sequence number: " + seqNum);

                                //if (answer != null && answer.length() > 0)
                                //    Log.d(TAG, "answer: " + answer);

                                if (syn == 1 && ack == 0)
                                {
                                    Log.d(TAG, "TCP flags: " + c[ihl+13]);
                                    c[ihl+13] = (byte)(c[ihl+13] | 0x10);
                                    Log.d(TAG, "TCP flags changed: " + c[ihl+13]);

                                    byte tmp1 = c[12];
                                    byte tmp2 = c[13];
                                    byte tmp3 = c[14];
                                    byte tmp4 = c[15];
                                    c[12] = c[16];
                                    c[13] = c[17];
                                    c[14] = c[18];
                                    c[15] = c[19];
                                    c[16] = tmp1;
                                    c[17] = tmp2;
                                    c[18] = tmp3;
                                    c[19] = tmp4;
                                    sourceIP = (int)(c[12] & 0xff) + "." + (int)(c[13] & 0xff) + "." +  (int)(c[14] & 0xff) + "." + (int)(c[15] & 0xff);
                                    destIP = (int)(c[16] & 0xff) + "." + (int)(c[17] & 0xff) + "." +  (int)(c[18] & 0xff) + "." + (int)(c[19] & 0xff);

                                    tmp1 = c[ihl];
                                    tmp2 = c[ihl+1];
                                    c[ihl] = c[ihl+2];
                                    c[ihl+1] = c[ihl+3];
                                    c[ihl+2] = tmp1;
                                    c[ihl+3] = tmp2;

                                    sPort = ((c[ihl] & 0xff) << 8) | (c[ihl+1] & 0xff);
                                    dPort = ((c[ihl+2] & 0xff) << 8) | (c[ihl+3] & 0xff);

                                    long ackNum = seqNum +1;
                                    c[ihl+8] = (byte)((ackNum & 0xff000000) >> 24);
                                    c[ihl+9] = (byte)((ackNum & 0xff0000) >> 16);
                                    c[ihl+10] = (byte)((ackNum & 0xff00) >> 8);
                                    c[ihl+11] = (byte)(ackNum & 0xff);
                                    long testAck = ((c[ihl+8] &0xff) << 24) | ((c[ihl+9] &0xff) << 16) | ((c[ihl+10] &0xff) << 8) | (c[ihl+11] &0xff);

                                    long newSeqNum = (long)(Math.random() * Integer.MAX_VALUE) + 1;
                                    c[ihl+4] = (byte)((newSeqNum & 0xff000000) >> 24);
                                    c[ihl+5] = (byte)((newSeqNum & 0xff0000) >> 16);
                                    c[ihl+6] = (byte)((newSeqNum & 0xff00) >> 8);
                                    c[ihl+7] = (byte)(newSeqNum & 0xff);
                                    long testSeq = ((c[ihl+4] &0xff) << 24) | ((c[ihl+5] &0xff) << 16) | ((c[ihl+6] &0xff) << 8) | (c[ihl+7] &0xff);

                                    syn = (c[ihl+13] & 0x2) >> 1;
                                    ack = (c[ihl+13] & 0x10) >> 4;

                                    byte[] TCPd = new byte[length-ihl+12];                //TCP header + data
                                    byte[] IPheader =  new byte[ihl];

                                    System.arraycopy(c,ihl,TCPd,12,length-ihl);             //copy
                                    System.arraycopy(c,0,IPheader,0,ihl);

                                    IPheader[10]=(byte) 0;
                                    IPheader[11]=(byte)0;

                                    TCPd[16+12]=(byte)0;
                                    TCPd[17+12]=(byte)0;                  //make checksums to 0.

                                    System.arraycopy(c,12,TCPd,0,8);        //copy source and dest addr.

                                    TCPd[8]=(byte)0;     //reserved
                                    TCPd[9] = (byte)6;
                                    TCPd[10] = (byte) (((length-ihl)&0xff00)>>8);
                                    TCPd[11] = (byte) ((length-ihl)&0x00ff);

                                    int IPchecksum = makingChecksum(IPheader);
                                    int TCPchecksum =  makingChecksum(TCPd);

                                    c[10] = (byte)((IPchecksum & 0xff00)>>8);
                                    c[11]  = (byte)(IPchecksum & 0x00ff);

                                    c[ihl+16] = (byte)((TCPchecksum&0xff00)>>8);
                                    c[ihl+17] = (byte)(TCPchecksum&0x00ff);

                                    System.arraycopy(c,0,IPheader,0,ihl);
                                    System.arraycopy(c,ihl,TCPd,12,length-ihl);
                                    Log.d(TAG, "changed source IP: " + sourceIP + ":" + sPort);
                                    Log.d(TAG, "changed dest IP: " + destIP + ":" + dPort);
                                    Log.d(TAG, "ack number: " + ackNum);
                                    Log.d(TAG, "test ack: " + testAck);
                                    Log.d(TAG,"Old seqnum: "+ seqNum);
                                    Log.d(TAG, "new sequence number: " + newSeqNum);
                                    Log.d(TAG, "test seq: " + testSeq);
                                    Log.d(TAG, "syn changed: " + syn);
                                    Log.d(TAG, "ack changed: " + ack);
                                    Log.d(TAG, " IPcheck" + (makingChecksum(IPheader)&0xffff));
                                    Log.d(TAG, "TCP check" + (makingChecksum(TCPd)&0xffff));
                                    out.write(c, 0, length);
                                }

                                if (syn == 0 && ack == 1)
                                {
                                    Log.d(TAG, "TCP handshake complete!");
                                }
                                if(length-ihl-offset ==0){
                                    Log.d(TAG, "data is null");
                                }else {
                                    byte[] data = new byte[length - ihl - offset];
                                    System.arraycopy(c, (ihl + offset), data, 0, (length - ihl - offset));
                                    String SendingData =  new String(data);
                                    int startOfContent =SendingData.lastIndexOf("\r\n")+2;
                                    String content = SendingData.substring(startOfContent);

                                    SendingData = SendingData.replaceAll("\r"," r ");
                                    SendingData = SendingData.replaceAll("\n"," n ");
                                    Log.d(TAG,SendingData);
                                    Log.d(TAG,"data is "+content);

                                    JSONObject JSONmessage = new JSONObject(content.substring(1,content.length()-1));
                                    Log.d(TAG,"latitude : "+(String)JSONmessage.get("latitude") + " longitude : "+(String)JSONmessage.get("longitude") + " message - "+(String)JSONmessage.get("message"));

                                    socket = new Socket(sourceIP,sPort);
                                    networkWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                                    networkReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                                    networkWriter.write(SendingData);
                                }
                            }
                        }
                        Thread.sleep(100);
                    }
                } catch (Exception e) {
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

        mThread.start();
        return START_STICKY;
    }
    public int makingChecksum(byte[] header){
        int length = header.length;
        if(length ==0){
            return 1;
        }
        int answer=0;
        for(int i=0;i<length ;i+=2){
            if(i+1>=length) {               //odd
                answer = answer+(int) ((header[i]&0xff)<<8);
            }
            else {
                answer = answer+(int)(((header[i]&0xff) << 8)|(header[i+1]&0xff));
            }
        }
        answer=((answer&0xffff)+ (answer>>16));
        return ~answer;
    }
    @Override
    public void onDestroy() {
        if (mThread != null) {
            mThread.interrupt();
        }
        super.onDestroy();
    }

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

    public String hexToIp(String addr){                     //hex to IP addr form.
        String ip = "";
        for(int i =0;i<addr.length();i=i+2){
            ip = ip+Integer.valueOf(addr.substring(i,i+2),16)+".";
        }
        return ip;
    }
}
