package GoogleOTP;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base32;

public class GoogleOTP {

	public static void main(String[] args) {
		String m_userName, m_hostName;
		Scanner sc = new Scanner(System.in);
		System.out.print("Input User Name : ");
		m_userName = sc.nextLine();
		System.out.print("Input Host Name : ");
		m_hostName = sc.nextLine();
		GoogleOTP m_otp = new GoogleOTP();
		System.out.println("OTP generated. Read QR Code below to register new key.");
		String otpkey = initCode(m_otp, m_userName, m_hostName);

		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		boolean check = false;
		System.out.print("Input OTP : ");
		do {
			String newCode = sc.nextLine();
			check = m_otp.checkCode(newCode, otpkey);
			if (check) {
				System.out.println("OTP Correct. Terminating Program..");
			} else {
				System.out.print("OTP Wrong, Please enter OTP again : ");
			}
		} while (!check);
		return;
	}

	private static String initCode(GoogleOTP otp, String userName, String hostName) {
		HashMap<String, String> map = otp.generate(userName, hostName);
		String otpkey = map.get("encodedKey");
		String url = map.get("url");
		System.out.println(url);
		return otpkey;
	}

	public HashMap<String, String> generate(String userName, String hostName) {
		HashMap<String, String> map = new HashMap<String, String>();
		byte[] buffer = new byte[5 + 5 * 5];
		new Random().nextBytes(buffer);
		Base32 codec = new Base32();
		byte[] secretKey = Arrays.copyOf(buffer, 10);
		byte[] bEncodedKey = codec.encode(secretKey);

		String encodedKey = new String(bEncodedKey);
		String url = getQRBarcodeURL(userName, hostName, encodedKey);
		// Google OTP 앱에 userName@hostName 으로 저장됨
		// key를 입력하거나 생성된 QR코드를 바코드 스캔하여 등록

		map.put("encodedKey", encodedKey);
		map.put("url", url);
		return map;
	}

	public boolean checkCode(String userCode, String otpkey) {
		long otpnum = Integer.parseInt(userCode); // Google OTP 앱에 표시되는 6자리 숫자
		long wave = new Date().getTime() / 30000; // Google OTP의 주기는 30초
		boolean result = false;
		try {
			Base32 codec = new Base32();
			byte[] decodedKey = codec.decode(otpkey);
			int window = 3;
			for (int i = -window; i <= window; ++i) {
				long hash = verify_code(decodedKey, wave + i);
				if (hash == otpnum)
					result = true;
			}
		} catch (InvalidKeyException | NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return result;
	}

	private static int verify_code(byte[] key, long t) throws NoSuchAlgorithmException, InvalidKeyException {
		byte[] data = new byte[8];
		long value = t;
		for (int i = 8; i-- > 0; value >>>= 8) {
			data[i] = (byte) value;
		}

		SecretKeySpec signKey = new SecretKeySpec(key, "HmacSHA1");
		Mac mac = Mac.getInstance("HmacSHA1");
		mac.init(signKey);
		byte[] hash = mac.doFinal(data);

		int offset = hash[20 - 1] & 0xF;

		// We're using a long because Java hasn't got unsigned int.
		long truncatedHash = 0;
		for (int i = 0; i < 4; ++i) {
			truncatedHash <<= 8;
			// We are dealing with signed bytes:
			// we just keep the first byte.
			truncatedHash |= (hash[offset + i] & 0xFF);
		}

		truncatedHash &= 0x7FFFFFFF;
		truncatedHash %= 1000000;

		return (int) truncatedHash;
	}

	public static String getQRBarcodeURL(String user, String host, String secret) {
		// QR코드 주소 생성
		String format2 = "http://chart.apis.google.com/chart?cht=qr&chs=200x200&chl=otpauth://totp/%s@%s%%3Fsecret%%3D%s&chld=H|0";
		return String.format(format2, user, host, secret);
	}
}