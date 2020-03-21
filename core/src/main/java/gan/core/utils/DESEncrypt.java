package gan.core.utils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import java.security.SecureRandom;

public class DESEncrypt{

    public static String encrypt(String data,String password){  //对string进行BASE64Encoder转换
        byte[] bt = encryptByKey(data.getBytes(),password);
        String strs = new String(Base64.encode(bt));
        return strs;
    }

    public static String decryptor(String data,String password)throws Exception{  //对string进行BASE64Encoder转换
        byte[] bt = decrypt(Base64.decode(data),password);
        String strs=new String(bt);
        return strs;
    }

    public static byte[] encryptByKey(byte[]datasource,String key){
        try{
        SecureRandom random=new SecureRandom();
        DESKeySpec desKey=new DESKeySpec(key.getBytes());
        //创建一个密匙工厂，然后用它把DESKeySpec转换成
        SecretKeyFactory keyFactory=SecretKeyFactory.getInstance("DES");
        SecretKey securekey=keyFactory.generateSecret(desKey);
        //Cipher对象实际完成加密操作
        Cipher cipher=Cipher.getInstance("DES");
        //用密匙初始化Cipher对象
        cipher.init(Cipher.ENCRYPT_MODE,securekey,random);
        //现在，获取数据并加密
        //正式执行加密操作
        return cipher.doFinal(datasource);
        }catch(Throwable e){
        e.printStackTrace();
        }
        return null;
    }

    private static byte[] decrypt(byte[]src,String key)throws Exception{
            // DES算法要求有一个可信任的随机数源
            SecureRandom random=new SecureRandom();
            // 创建一个DESKeySpec对象
            DESKeySpec desKey=new DESKeySpec(key.getBytes());
            // 创建一个密匙工厂
            SecretKeyFactory keyFactory=SecretKeyFactory.getInstance("DES");
            // 将DESKeySpec对象转换成SecretKey对象
            SecretKey securekey=keyFactory.generateSecret(desKey);
            // Cipher对象实际完成解密操作
            Cipher cipher=Cipher.getInstance("DES");
            // 用密匙初始化Cipher对象
            cipher.init(Cipher.DECRYPT_MODE,securekey,random);
            // 真正开始解密操作
            return cipher.doFinal(src);
    }

}