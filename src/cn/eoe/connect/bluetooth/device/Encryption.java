package cn.eoe.connect.bluetooth.device;

public class Encryption {
	public static byte[] tridesKey;
	public static String encryptions(String info) throws Exception{
		String headerString = "FA010100";
		String length = Integer.toHexString(info.length());
		String tailerString = "0A0D";
			tridesKey = TripleDESUtil.initKey();
			byte[] tridesResult = TripleDESUtil.encrypt(info.getBytes(), tridesKey);
			String finalInfo = headerString+length+tridesResult+tailerString;
			return finalInfo;

	}
}