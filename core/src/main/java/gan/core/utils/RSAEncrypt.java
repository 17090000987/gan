package gan.core.utils;

import gan.core.file.FileHelper;
import gan.core.system.server.SystemServer;
import org.apache.tomcat.util.codec.binary.Base64;

import javax.crypto.Cipher;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;

public class RSAEncrypt {

	public static void generateKeyStore(String key) throws Exception {
		//生成公钥和私钥
		HashMap<String, String> keys = genKeyPair(null);
		//加密字符串
		String key_md5 = Encrypter.encryptByMD5(key);
		String publicKey = keys.get("public");
		String privateKey = keys.get("private");
		StringBuffer sb = new StringBuffer();
		sb.append("key:").append(key).append("\n");
		sb.append("key_md5:").append(key_md5).append("\n");
		sb.append("publicKey:").append(publicKey).append("\n");
		sb.append("privateKey:").append(privateKey).append("\n");
		String encrypted = encrypt(key_md5, publicKey);
		sb.append("encrypted:").append(encrypted).append("\n");
		String decrypted = decrypt(encrypted, privateKey);
		sb.append("decrypted:").append(decrypted).append("\n");

		FileHelper.save2File(sb.toString().getBytes("utf-8"),
				SystemServer.getRootPath("/gan.keystore"));
	}

	/** 
	 * 随机生成密钥对 
	 * @throws NoSuchAlgorithmException 
	 */  
	public static HashMap<String,String> genKeyPair(HashMap<String,String> keys) throws NoSuchAlgorithmException {
		// KeyPairGenerator类用于生成公钥和私钥对，基于RSA算法生成对象  
		KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");  
		// 初始化密钥对生成器，密钥大小为96-1024位  
		keyPairGen.initialize(1024,new SecureRandom());
		// 生成一个密钥对，保存在keyPair中  
		KeyPair keyPair = keyPairGen.generateKeyPair();  
		RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();   // 得到私钥  
		RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();  // 得到公钥  
		String publicKeyString = new String(Base64.encodeBase64(publicKey.getEncoded()));
		// 得到私钥字符串  
		String privateKeyString = new String(Base64.encodeBase64((privateKey.getEncoded())));

		if(keys==null){
			keys = new HashMap<>();
		}
		
		// 将公钥和私钥保存到Map
		keys.put("public",publicKeyString);  //0表示公钥
		keys.put("private",privateKeyString);  //1表示私钥
		return keys;
	}

	/** 
	 * RSA公钥加密 
	 *  
	 * @param str 
	 *            加密字符串
	 * @param publicKey 
	 *            公钥 
	 * @return 密文 
	 * @throws Exception 
	 *             加密过程中的异常信息 
	 */  
	public static String encrypt(String str, String publicKey ) throws Exception{
		//base64编码的公钥
		byte[] decoded = Base64.decodeBase64(publicKey);
		RSAPublicKey pubKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
		//RSA加密
		Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.ENCRYPT_MODE, pubKey);
		String outStr = Base64.encodeBase64String(cipher.doFinal(str.getBytes("UTF-8")));
		return outStr;
	}

	/** 
	 * RSA私钥解密
	 *  
	 * @param str 
	 *            加密字符串
	 * @param privateKey 
	 *            私钥 
	 * @return 铭文
	 * @throws Exception 
	 *             解密过程中的异常信息 
	 */  
	public static String decrypt(String str, String privateKey) throws Exception{
		//64位解码加密后的字符串
		byte[] inputByte = Base64.decodeBase64(str.getBytes("UTF-8"));
		//base64编码的私钥
		byte[] decoded = Base64.decodeBase64(privateKey);  
        RSAPrivateKey priKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));  
		//RSA解密
		Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.DECRYPT_MODE, priKey);
		String outStr = new String(cipher.doFinal(inputByte));
		return outStr;
	}
}