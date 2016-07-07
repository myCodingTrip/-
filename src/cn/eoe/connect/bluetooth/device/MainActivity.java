package cn.eoe.connect.bluetooth.device;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnItemClickListener {

	private ListView lvDevices;
	private EditText etMessage;
	private BluetoothAdapter bluetoothAdapter;
	//存储蓝牙设备的地址
	private List<String> bluetoothDevices = new ArrayList<String>();
	private ArrayAdapter<String> arrayAdapter;
	//指定一个UUID
	private final UUID MY_UUID = UUID
			.fromString("db764ac8-4b08-7f25-aafe-59d03c27bae3");
//	private final UUID MY_UUID = UUID.randomUUID();
	private final String NAME = "Bluetooth_Socket";
	private BluetoothSocket clientSocket;
	private BluetoothDevice device;  
    private AcceptThread acceptThread; 
	private OutputStream os;
 
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.main);

		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		etMessage = (EditText) findViewById(R.id.edit_text);
		lvDevices = (ListView) findViewById(R.id.lvDevices);
		//显示已配对的蓝牙设备
		Set<BluetoothDevice> pairedDevices = bluetoothAdapter
				.getBondedDevices();

		if (pairedDevices.size() > 0) {
			for (BluetoothDevice device : pairedDevices) {
				bluetoothDevices.add(device.getName() + ":"
						+ device.getAddress() + "\n");

			}
		}
		//将所有设备显示在列表上
		arrayAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, android.R.id.text1,
				bluetoothDevices);

		lvDevices.setAdapter(arrayAdapter);
		lvDevices.setOnItemClickListener(this);
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(receiver, filter);

        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(receiver, filter);
        //启动服务
		acceptThread = new AcceptThread();
		acceptThread.start();
	}

	public void onClick_Search(View view) {
		setProgressBarIndeterminateVisibility(true);
		setTitle("正在扫描...");

		if (bluetoothAdapter.isDiscovering()) {
			bluetoothAdapter.cancelDiscovery();
		}
		bluetoothAdapter.startDiscovery();
	}

	private final BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {

				BluetoothDevice device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
					bluetoothDevices.add(device.getName() + ":"
							+ device.getAddress() + "\n");
					arrayAdapter.notifyDataSetChanged();
				}
			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED
					.equals(action)) {
				setProgressBarIndeterminateVisibility(false);
				setTitle("连接蓝牙设备");
			}
		}
	};
	//列表单击事件（客户端）
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		String s = arrayAdapter.getItem(position);
		//OutputStream os;
		//解析地址
		String address = s.substring(s.indexOf(":") + 1).trim();
		try {
			if (bluetoothAdapter.isDiscovering()) {
				bluetoothAdapter.cancelDiscovery();
			}
			try {
				if (device == null) {
					//获得远程设备
					device = bluetoothAdapter.getRemoteDevice(address);
				}
				if (clientSocket == null) {
					clientSocket = device
							.createRfcommSocketToServiceRecord(MY_UUID);
					//连接
					clientSocket.connect();
					os = clientSocket.getOutputStream();
				}
			} catch (Exception e) {
			}
			if (os != null) {				
				String DATA = etMessage.getText().toString();
				//生成3DES密钥
				byte[] tridesKey = TripleDESUtil.initKey();
				//System.out.println("发送tridesKey:"+printByte(tridesKey));
				//生成3DES密文
				byte[] tridesResult = TripleDESUtil.encrypt(DATA.getBytes(), tridesKey);
				//System.out.println("发送tridesResult:"+printByte(tridesResult));
				//整合发送
				byte[] b = new byte[24+tridesResult.length];//24+8
				int i;
				for(i=0; i<tridesKey.length; i++)	b[i] = tridesKey[i];
				for(int j=0; j<tridesResult.length; j++)	b[i+j] = tridesResult[j];
				//System.out.println("bs:"+printByte(b));
				os.write(b);
				Toast.makeText(this, "信息已发送",
						Toast.LENGTH_SHORT).show();
			}
		} catch (Exception e) {
		}
	}
	
	private Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			Toast.makeText(MainActivity.this, String.valueOf(msg.obj),
					Toast.LENGTH_LONG).show();
			super.handleMessage(msg);
		}
	};
	//用于接受客户端信息的线程
	private class AcceptThread extends Thread {
		private BluetoothServerSocket serverSocket;
		private BluetoothSocket socket;
		private InputStream is;
		//private OutputStream os;

		public AcceptThread() {
			try {
				serverSocket = bluetoothAdapter
						.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
			} catch (Exception e) {
			}
		}
		@Override
		public void run() {
			try
			{
				socket = serverSocket.accept();
				is = socket.getInputStream();
				//os = socket.getOutputStream();
				while(true)
				{
					//必须不断声明？
					Message msg = new Message();
					byte[] b = new byte[128];
					int count = is.read(b);
					//获取密钥
					byte[] tridesKey = new byte[24];
					//System.out.println("获取tridesKey:"+printByte(tridesKey));
					//获取密文
					byte[] tridesResult = new byte[count-24];
					//System.out.println("获取tridesResult:"+printByte(tridesResult));
					for(int i=0; i<24; i++)	  tridesKey[i] = b[i];
					for(int j=24; j<count; j++)	  tridesResult[j-24] = b[j];
					byte[] tridesPlain = TripleDESUtil.decrypt(tridesResult, tridesKey);
					msg.obj = new String(tridesPlain);
					System.out.println("解密后："+msg.obj);
					handler.sendMessage(msg);
				}
			}
			catch (Exception e) {
			}
		}
	}

}
