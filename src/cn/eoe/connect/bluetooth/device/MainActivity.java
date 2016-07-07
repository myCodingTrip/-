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
	//�洢�����豸�ĵ�ַ
	private List<String> bluetoothDevices = new ArrayList<String>();
	private ArrayAdapter<String> arrayAdapter;
	//ָ��һ��UUID
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
		//��ʾ����Ե������豸
		Set<BluetoothDevice> pairedDevices = bluetoothAdapter
				.getBondedDevices();

		if (pairedDevices.size() > 0) {
			for (BluetoothDevice device : pairedDevices) {
				bluetoothDevices.add(device.getName() + ":"
						+ device.getAddress() + "\n");

			}
		}
		//�������豸��ʾ���б���
		arrayAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, android.R.id.text1,
				bluetoothDevices);

		lvDevices.setAdapter(arrayAdapter);
		lvDevices.setOnItemClickListener(this);
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(receiver, filter);

        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(receiver, filter);
        //��������
		acceptThread = new AcceptThread();
		acceptThread.start();
	}

	public void onClick_Search(View view) {
		setProgressBarIndeterminateVisibility(true);
		setTitle("����ɨ��...");

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
				setTitle("���������豸");
			}
		}
	};
	//�б����¼����ͻ��ˣ�
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		String s = arrayAdapter.getItem(position);
		//OutputStream os;
		//������ַ
		String address = s.substring(s.indexOf(":") + 1).trim();
		try {
			if (bluetoothAdapter.isDiscovering()) {
				bluetoothAdapter.cancelDiscovery();
			}
			try {
				if (device == null) {
					//���Զ���豸
					device = bluetoothAdapter.getRemoteDevice(address);
				}
				if (clientSocket == null) {
					clientSocket = device
							.createRfcommSocketToServiceRecord(MY_UUID);
					//����
					clientSocket.connect();
					os = clientSocket.getOutputStream();
				}
			} catch (Exception e) {
			}
			if (os != null) {				
				String DATA = etMessage.getText().toString();
				//����3DES��Կ
				byte[] tridesKey = TripleDESUtil.initKey();
				//System.out.println("����tridesKey:"+printByte(tridesKey));
				//����3DES����
				byte[] tridesResult = TripleDESUtil.encrypt(DATA.getBytes(), tridesKey);
				//System.out.println("����tridesResult:"+printByte(tridesResult));
				//���Ϸ���
				byte[] b = new byte[24+tridesResult.length];//24+8
				int i;
				for(i=0; i<tridesKey.length; i++)	b[i] = tridesKey[i];
				for(int j=0; j<tridesResult.length; j++)	b[i+j] = tridesResult[j];
				//System.out.println("bs:"+printByte(b));
				os.write(b);
				Toast.makeText(this, "��Ϣ�ѷ���",
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
	//���ڽ��ܿͻ�����Ϣ���߳�
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
					//���벻��������
					Message msg = new Message();
					byte[] b = new byte[128];
					int count = is.read(b);
					//��ȡ��Կ
					byte[] tridesKey = new byte[24];
					//System.out.println("��ȡtridesKey:"+printByte(tridesKey));
					//��ȡ����
					byte[] tridesResult = new byte[count-24];
					//System.out.println("��ȡtridesResult:"+printByte(tridesResult));
					for(int i=0; i<24; i++)	  tridesKey[i] = b[i];
					for(int j=24; j<count; j++)	  tridesResult[j-24] = b[j];
					byte[] tridesPlain = TripleDESUtil.decrypt(tridesResult, tridesKey);
					msg.obj = new String(tridesPlain);
					System.out.println("���ܺ�"+msg.obj);
					handler.sendMessage(msg);
				}
			}
			catch (Exception e) {
			}
		}
	}

}
