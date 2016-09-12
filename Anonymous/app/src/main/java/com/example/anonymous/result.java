package com.example.anonymous;

/**
 * Created by 신승수 on 2016-06-27.
 */
        import android.app.Activity;
        import android.app.Service;
        import android.content.ComponentName;
        import android.content.Context;
        import android.content.Intent;
        import android.content.ServiceConnection;
        import android.net.VpnService;
        import android.os.Handler;
        import android.os.IBinder;
        import android.os.Message;
        import android.os.Messenger;
        import android.os.RemoteException;
        import android.support.v7.app.ActionBarActivity;
        import android.os.Bundle;
        import android.support.v7.app.AppCompatActivity;
        import android.util.Log;
        import android.view.Menu;
        import android.view.MenuItem;
        import android.view.View;
        import android.widget.Button;
        import android.widget.TextView;
        import android.widget.Toast;


public class result extends AppCompatActivity {
    private TextView textr;
    private Messenger mRemote;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mRemote = new Messenger(service);

            if(mRemote != null){
                Message msg = new Message();
                msg.what =0;
                msg.obj = new Messenger(new RemoteHandler());
                try{
                    mRemote.send(msg);
                }catch (RemoteException e){
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d("VPNLog","Disconnecting");
            mRemote = null;
        }
    };
    boolean isBound = false;
    Intent intent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);  // layout xml 과 자바파일을 연결

        final DBManager dbManager = new DBManager(getApplicationContext(), "Packet.db",null,1);

        Button stop = (Button) findViewById(R.id.stop_button);

        textr = (TextView) findViewById(R.id.result);
        //String l =intent.getStringExtra("result");

        Toast.makeText(result.this, "Start Capturing.", Toast.LENGTH_SHORT).show();

        intent = VpnService.prepare(getApplicationContext());

        if (intent != null){                             //
            startActivityForResult(intent, 0);          //start service. A가 B를 호출했다가 B가 종료되면서 결과를 extra에 넘긴다.
        } else {
            onActivityResult(0, RESULT_OK, null);      //A를 끝내지 않고 B를 끝내고 A로 온다.
        }

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {            //stop button
                unbindService(mConnection);
                Log.d("Result","Stop packet capture");
            }
        });
    }//end  onCreate.

    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if(resultCode == RESULT_OK){
            String prefix = getPackageName();
            Intent intent = new Intent(this, Myvpn.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }
    private class RemoteHandler extends Handler{
        @Override
        public void handleMessage(Message msg){
            String packet = msg.obj.toString();                                        //packet.

            int sPortStart = Integer.valueOf(packet.substring(1,2),16)*4*2;           //where has the start of port number of the source
            int dPortStart = sPortStart+4;                                             // start of port number of the destination
            int offset = sPortStart+24;                                                 //where is the length of  TCP header.

            int TCPLength = Integer.valueOf(packet.substring(offset,offset+1),16)*4*2;  //length of the TCP header.
            int totalL = Integer.valueOf(packet.substring(4,8),16)*2;
            String answer = packet.substring(sPortStart+TCPLength,totalL);              //payload.

            if (answer == null || answer.length() == 0) {                               //no payload.
                answer = null;
            }
            else {
                StringBuilder sb = new StringBuilder();
                char[] ba = new char[answer.length() / 2];
                for (int i = 0; i < ba.length; i++) {
                    sb.append( (char)Integer.parseInt(answer.substring(2 * i, 2 * i + 2), 16));
                }
                answer = sb.toString();
            }
            String sPort = packet.substring(sPortStart,sPortStart+4);
            String dPort = packet.substring(dPortStart,dPortStart+4);

            sPort  = String.valueOf(Integer.valueOf(sPort,16));
            dPort  = String.valueOf(Integer.valueOf(dPort,16));

            String sourceIP  = packet.substring(24,32);
            String destIP = packet.substring(32,40);
            sourceIP = hexToIp(sourceIP);
            destIP = hexToIp(destIP);
            Log.d("VPNLog",packet);
            textr.setText(answer+"\nsource = "+sourceIP+":" +sPort+"\ndest = "+destIP+":"+dPort+"\n");
            if(destIP.contains("147.46.216.84")){
                Toast.makeText(result.this,"done",Toast.LENGTH_LONG).show();
            }
        }
    }
    public String hexToIp(String addr){                     //hex to IP addr form.
        String ip = "";
        for(int i =0;i<addr.length();i=i+2){
            ip = ip+Integer.valueOf(addr.substring(i,i+2),16)+".";
        }
        return ip;
    }
       // result.setText(l);
} // end MyTwo