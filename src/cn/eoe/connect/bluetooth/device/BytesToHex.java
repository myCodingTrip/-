package cn.eoe.connect.bluetooth.device;

public class BytesToHex {
	
	public static String fromBytesToHex(byte[] resultBytes) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < resultBytes.length; i++) {
			if (Integer.toHexString(0xFF & resultBytes[i]).length() == 1) {
				builder.append("0").append(
						Integer.toHexString(0xFF & resultBytes[i]));
			} else {
				builder.append(Integer.toHexString(0xFF & resultBytes[i]));
			}
		}
		return builder.toString();
	}

}
