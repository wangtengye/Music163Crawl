package netCrawl;

import java.math.BigInteger;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Hex;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class EncryptTools {
	// AES����
	public static String encrypt(String text, String secKey) throws Exception {
		byte[] raw = secKey.getBytes();
		SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
		// "�㷨/ģʽ/���뷽ʽ"
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		// ʹ��CBCģʽ����Ҫһ������iv�������Ӽ����㷨��ǿ��
		IvParameterSpec iv = new IvParameterSpec("0102030405060708".getBytes());
		cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
		byte[] encrypted = cipher.doFinal(text.getBytes());
		return Base64.getEncoder().encodeToString(encrypted);
	}

	// �ַ����
	public static String zfill(String result, int n) {
		if (result.length() >= n) {
			result = result.substring(result.length() - n, result.length());
		} else {
			StringBuilder stringBuilder = new StringBuilder();
			for (int i = n; i > result.length(); i--) {
				stringBuilder.append("0");
			}
			stringBuilder.append(result);
			result = stringBuilder.toString();
		}
		return result;
	}

	public static int commentCount(String songId) throws Exception {
		// ˽Կ�����16λ�ַ������Լ��ɸģ�
		String secKey = "cd859f54539b24b7";
		String text = "{\"username\": \"\", \"rememberLogin\": \"true\", \"password\": \"\"}";
		String modulus = "00e0b509f6259df8642dbc35662901477df22677ec152b5ff68ace615bb7b725152b3ab17a876aea8a5aa76d2e417629ec4ee341f56135fccf695280104e0312ecbda92557c93870114af6c9d05c4f7f0c3685b7a46bee255932575cce10b424d813cfe4875d3e82047b97ddef52741d546b8e289dc6935b3ece0462db0a22b8e7";
		String nonce = "0CoJUm6Qyw8W8jud";
		String pubKey = "010001";
		// 2��AES���ܣ��õ�params
		String params = EncryptTools.encrypt(EncryptTools.encrypt(text, nonce), secKey);
		StringBuffer stringBuffer = new StringBuffer(secKey);
		// ����˽Կ
		secKey = stringBuffer.reverse().toString();
		String hex = Hex.encodeHexString(secKey.getBytes());
		BigInteger bigInteger1 = new BigInteger(hex, 16);
		BigInteger bigInteger2 = new BigInteger(pubKey, 16);
		BigInteger bigInteger3 = new BigInteger(modulus, 16);
		// RSA���ܼ���
		BigInteger bigInteger4 = bigInteger1.pow(bigInteger2.intValue()).remainder(bigInteger3);
		String encSecKey = Hex.encodeHexString(bigInteger4.toByteArray());
		// �ַ����
		encSecKey = EncryptTools.zfill(encSecKey, 256);
		// ���ۻ�ȡ
		Document document = Jsoup.connect("http://music.163.com/weapi/v1/resource/comments/R_SO_4_"+songId)
				.cookie("appver", "1.5.0.75771").header("Referer", "http://music.163.com/").data("params", params)
				.data("encSecKey", encSecKey).ignoreContentType(true).post();
		Pattern pattern=Pattern.compile(".*total\":(\\d+).*");
		Matcher matcher=pattern.matcher(document.text());
		matcher.matches();
		return Integer.parseInt(matcher.group(1));
	}

	public static void main(String[] args) throws Exception {
		System.out.println(commentCount("28819410"));
	}
}
