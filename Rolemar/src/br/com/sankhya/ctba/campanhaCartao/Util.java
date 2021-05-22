package br.com.sankhya.ctba.campanhaCartao;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import br.com.sankhya.modelcore.util.MGECoreParameter;

public class Util {
    public static String getRepositorio() throws Exception {
        String dir;
        
        dir = (String) MGECoreParameter.getParameter("FREPBASEFOLDER");
        if (dir == null) {
            dir = ".sw_file_repository";
        }else {
            dir = dir.replace("\n", "");    
        }
        
        
        if(dir.charAt(dir.length() - 1) != '/') {
            dir = dir + "/";
        }
        return dir;
    }
	
	public static String completeToLeft(String value, char c, int size) {
		if(value == null)
			value = "";
		if(value.length() > size){
			value =  value.substring(0,size);
		}
		String result = value;
		while (result.length() < size) {
			result = c + result;
		}
		return result;
	}
	
	public static String completeToRight(String value, char c, int size) {
		if(value == null)
			value = "";
		//size = size - value.length();
		if(value.length() > size){
			value =  value.substring(0,size);
		}
		StringBuffer sb = new StringBuffer(value);
        for (int i=sb.length() ; i < size ; i++){
            sb.append(c);
        }
        return sb.toString();
	}
	
	public static String geraMD5(String frase) throws NoSuchAlgorithmException {

		MessageDigest md = MessageDigest.getInstance("MD5");
		md.update(frase.getBytes());
		byte[] hashMd5 = md.digest();

		StringBuilder s = new StringBuilder();
		for (int i = 0; i < hashMd5.length; i++) {
			int parteAlta = ((hashMd5[i] >> 4) & 0xf) << 4;
			int parteBaixa = hashMd5[i] & 0xf;
			if (parteAlta == 0)
				s.append('0');
			s.append(Integer.toHexString(parteAlta | parteBaixa));
		}

		return s.toString();
	}
	
	public static void createDir(File file) throws Exception {
		if (!file.exists()) {
		    file.mkdirs();
		}
	}
	
	public static void createFile(File file) throws Exception {
		if (!file.exists()) {
		    file.createNewFile();
		}
	}

}
