/*
* Copyright 2010 Srikanth Reddy Lingala  
* 
* Licensed under the Apache License, Version 2.0 (the "License"); 
* you may not use this file except in compliance with the License. 
* You may obtain a copy of the License at 
* 
* http://www.apache.org/licenses/LICENSE-2.0 
* 
* Unless required by applicable law or agreed to in writing, 
* software distributed under the License is distributed on an "AS IS" BASIS, 
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
* See the License for the specific language governing permissions and 
* limitations under the License. 
*/

package net.lingala.zip4j.util;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipModel;

public class Zip4jUtil {
	
	public static boolean isStringNotNullAndNotEmpty(String str) {
		if (str == null || str.trim().length() <= 0) {
			return false;
		}
		
		return true;
	}
	
	public static boolean checkOutputFolder(String path) {
		if (!isStringNotNullAndNotEmpty(path)) {
			throw new RuntimeException(new NullPointerException("output path is null"));
		}
		
		File file = new File(path);
		
		if (file.exists()) {
		
			if (!file.isDirectory()) {
				throw new RuntimeException("output folder is not valid");
			}
			
			if (!file.canWrite()) {
				throw new RuntimeException("no write access to output folder");
			}
		} else {
			try {
				file.mkdirs();
				if (!file.isDirectory()) {
					throw new RuntimeException("output folder is not valid");
				}
				
				if (!file.canWrite()) {
					throw new RuntimeException("no write access to destination folder");
				}
				
			} catch (Exception e) {
				throw new RuntimeException("Cannot create destination folder");
			}
		}
		
		return true;
	}
	
	public static boolean checkFileReadAccess(String path) throws RuntimeException {
		if (!isStringNotNullAndNotEmpty(path)) {
			throw new RuntimeException("path is null");
		}
		
		if (!checkFileExists(path)) {
			throw new RuntimeException("file does not exist: " + path);
		}
		
		try {
			File file = new File(path);
			return file.canRead();
		} catch (Exception e) {
			throw new RuntimeException("cannot read zip file");
		}
	}
	
	public static boolean checkFileWriteAccess(String path) {
		if (!isStringNotNullAndNotEmpty(path)) {
			throw new RuntimeException("path is null");
		}
		
		if (!checkFileExists(path)) {
			throw new RuntimeException("file does not exist: " + path);
		}
		
		try {
			File file = new File(path);
			return file.canWrite();
		} catch (Exception e) {
			throw new RuntimeException("cannot read zip file");
		}
	}
	
	public static boolean checkFileExists(String path) {
		if (!isStringNotNullAndNotEmpty(path)) {
			throw new RuntimeException("path is null");
		}
		
		File file = new File(path);
		return checkFileExists(file);
	}
	
	public static boolean checkFileExists(File file) {
		if (file == null) {
			throw new RuntimeException("cannot check if file exists: input file is null");
		}
		return file.exists();
	}
	
	public static boolean isWindows(){
		String os = System.getProperty("os.name").toLowerCase();
	    return (os.indexOf( "win" ) >= 0); 
	}
	
	public static void setFileReadOnly(File file) {
		if (file == null) {
			throw new RuntimeException("input file is null. cannot set read only file attribute");
		}
		
		if (file.exists()) {
			file.setReadOnly();
		}
	}
	
	public static long getLastModifiedFileTime(File file, TimeZone timeZone) {
		if (file == null) {
			throw new RuntimeException("input file is null, cannot read last modified file time");
		}
		
		if (!file.exists()) {
			throw new RuntimeException("input file does not exist, cannot read last modified file time");
		}
		
		return file.lastModified();
	}
	
	public static String getFileNameFromFilePath(File file) {
		if (file == null) {
			throw new RuntimeException("input file is null, cannot get file name");
		}
		
		if (file.isDirectory()) {
			return null;
		}
		
		return file.getName();
	}
	
	public static long getFileLengh(String file) {
		if (!isStringNotNullAndNotEmpty(file)) {
			throw new RuntimeException("invalid file name");
		}
		
		return getFileLengh(new File(file));
	}
	
	public static long getFileLengh(File file) {
		if (file == null) {
			throw new RuntimeException("input file is null, cannot calculate file length");
		}
		
		if (file.isDirectory()) {
			return -1;
		}
		
		return file.length();
	}
	
	/**
	 * Converts input time from Java to DOS format
	 * @param time
	 * @return time in DOS format 
	 */
	public static long javaToDosTime(long time) {
		
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(time);
		
		int year = cal.get(Calendar.YEAR);
		if (year < 1980) {
		    return (1 << 21) | (1 << 16);
		}
		return (year - 1980) << 25 | (cal.get(Calendar.MONTH) + 1) << 21 |
	               cal.get(Calendar.DATE) << 16 | cal.get(Calendar.HOUR_OF_DAY) << 11 | cal.get(Calendar.MINUTE) << 5 |
	               cal.get(Calendar.SECOND) >> 1;
	}
	
	/**
	 * Converts time in dos format to Java format
	 * @param dosTime
	 * @return time in java format
	 */
	public static long dosToJavaTme(int dosTime) {
		int sec = 2 * (dosTime & 0x1f);
	    int min = (dosTime >> 5) & 0x3f;
	    int hrs = (dosTime >> 11) & 0x1f;
	    int day = (dosTime >> 16) & 0x1f;
	    int mon = ((dosTime >> 21) & 0xf) - 1;
	    int year = ((dosTime >> 25) & 0x7f) + 1980;
	    
	    Calendar cal = Calendar.getInstance();
		cal.set(year, mon, day, hrs, min, sec);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTime().getTime();
	}
	
	
	public static String getZipFileNameWithoutExt(String zipFile) {
		if (!isStringNotNullAndNotEmpty(zipFile)) {
			throw new RuntimeException("zip file name is empty or null, cannot determine zip file name");
		}
		String tmpFileName = zipFile;
		if (zipFile.indexOf(System.getProperty("file.separator")) >= 0) {
			tmpFileName = zipFile.substring(zipFile.lastIndexOf(System.getProperty("file.separator")));
		}
		
		if (tmpFileName.indexOf(".") > 0) {
			tmpFileName = tmpFileName.substring(0, tmpFileName.lastIndexOf("."));
		}
		return tmpFileName;
	}
	
	public static byte[] convertCharset(String str) {
		try {
			byte[] converted = null;
			String charSet = detectCharSet(str);
			if (charSet.equals(InternalZipConstants.CHARSET_CP850)) {
				converted = str.getBytes(InternalZipConstants.CHARSET_CP850);
			} else if (charSet.equals(InternalZipConstants.CHARSET_UTF8)) {
				converted = str.getBytes(InternalZipConstants.CHARSET_UTF8);
			} else {
				converted = str.getBytes();
			}
			return converted;
		}
		catch (UnsupportedEncodingException err) {
			return str.getBytes();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Decodes file name based on encoding. If file name is UTF 8 encoded
	 * returns an UTF8 encoded string, else return Cp850 encoded String. If 
	 * appropriate charset is not supported, then returns a System default 
	 * charset encoded String
	 * @param data
	 * @param isUTF8
	 * @return String
	 */
	public static String decodeFileName(byte[] data, boolean isUTF8) {
		if (isUTF8) {
			try {
				return new String(data, InternalZipConstants.CHARSET_UTF8);
			} catch (UnsupportedEncodingException e) {
				return new String(data);
			}
		} else {
			return getCp850EncodedString(data);
		}
	}
	
	/**
	 * Returns a string in Cp850 encoding from the input bytes.
	 * If this encoding is not supported, then String with the default encoding is returned.
	 * @param data
	 * @return String
	 */
	public static String getCp850EncodedString(byte[] data) {
		try {
			String retString = new String(data, InternalZipConstants.CHARSET_CP850);
			return retString;
		} catch (UnsupportedEncodingException e) {
			return new String(data);
		}
	}
	
	/**
	 * Returns an absoulte path for the given file path 
	 * @param filePath
	 * @return String
	 */
	public static String getAbsoluteFilePath(String filePath) {
		if (!isStringNotNullAndNotEmpty(filePath)) {
			throw new RuntimeException("filePath is null or empty, cannot get absolute file path");
		}
		
		File file = new File(filePath);
		return file.getAbsolutePath();
	}
	
	/**
	 * Detects the encoding charset for the input string
	 * @param str
	 * @return String - charset for the String
	 * this method returns default System charset
	 */
	public static String detectCharSet(String str) {
		if (str == null) {
			throw new RuntimeException("input string is null, cannot detect charset");
		}
		
		try {
			byte[] byteString = str.getBytes(InternalZipConstants.CHARSET_CP850);
			String tempString = new String(byteString, InternalZipConstants.CHARSET_CP850);
			
			if (str.equals(tempString)) {
				return InternalZipConstants.CHARSET_CP850;
			}
			
			byteString = str.getBytes(InternalZipConstants.CHARSET_UTF8);
			tempString = new String(byteString, InternalZipConstants.CHARSET_UTF8);
			
			if (str.equals(tempString)) {
				return InternalZipConstants.CHARSET_UTF8;
			}
			
			return InternalZipConstants.CHARSET_DEFAULT;
		} catch (UnsupportedEncodingException e) {
			return InternalZipConstants.CHARSET_DEFAULT;
		} catch (Exception e) {
			return InternalZipConstants.CHARSET_DEFAULT;
		}
	}
	
	/**
	 * returns the length of the string by wrapping it in a byte buffer with
	 * the appropriate charset of the input string and returns the limit of the 
	 * byte buffer
	 * @param str
	 * @return length of the string
	 */
	public static int getEncodedStringLength(String str) {
		if (!isStringNotNullAndNotEmpty(str)) {
			throw new RuntimeException("input string is null, cannot calculate encoded String length");
		}
		
		String charset = detectCharSet(str);
		return getEncodedStringLength(str, charset);
	}
	
	/**
	 * returns the length of the string in the input encoding
	 * @param str
	 * @param charset
	 * @return int
	 */
	public static int getEncodedStringLength(String str, String charset) {
		if (!isStringNotNullAndNotEmpty(str)) {
			throw new RuntimeException("input string is null, cannot calculate encoded String length");
		}
		
		if (!isStringNotNullAndNotEmpty(charset)) {
			throw new RuntimeException("encoding is not defined, cannot calculate string length");
		}
		
		ByteBuffer byteBuffer = null;
		
		try {
			if (charset.equals(InternalZipConstants.CHARSET_CP850)) {
				byteBuffer = ByteBuffer.wrap(str.getBytes(InternalZipConstants.CHARSET_CP850));
			} else if (charset.equals(InternalZipConstants.CHARSET_UTF8)) {
				byteBuffer = ByteBuffer.wrap(str.getBytes(InternalZipConstants.CHARSET_UTF8));
			} else {
				byteBuffer = ByteBuffer.wrap(str.getBytes(charset));
			}
		} catch (UnsupportedEncodingException e) {
			byteBuffer = ByteBuffer.wrap(str.getBytes());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		return byteBuffer.limit();
	}
	
	/**
	 * Checks if the input charset is supported
	 * @param charset
	 * @return boolean
	 */
	public static boolean isSupportedCharset(String charset) {
		if (!isStringNotNullAndNotEmpty(charset)) {
			throw new RuntimeException("charset is null or empty, cannot check if it is supported");
		}
		
		try {
			new String("a".getBytes(), charset);
			return true;
		} catch (UnsupportedEncodingException e) {
			return false;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	
	public static String getRelativeFileName(String file, String rootFolderInZip, String rootFolderPath) {
		if (!Zip4jUtil.isStringNotNullAndNotEmpty(file)) {
			throw new RuntimeException("input file path/name is empty, cannot calculate relative file name");
		}
		
		String fileName = null;
		
		if (Zip4jUtil.isStringNotNullAndNotEmpty(rootFolderPath)) {
			
			File rootFolderFile = new File(rootFolderPath);
			
			String rootFolderFileRef = rootFolderFile.getPath();
			
			if (!rootFolderFileRef.endsWith(InternalZipConstants.FILE_SEPARATOR)) {
				rootFolderFileRef += InternalZipConstants.FILE_SEPARATOR;
			}
			
			String tmpFileName = file.substring(rootFolderFileRef.length());
			if (tmpFileName.startsWith(System.getProperty("file.separator"))) {
				tmpFileName = tmpFileName.substring(1);
			}
			
			File tmpFile = new File(file);
			
			if (tmpFile.isDirectory()) {
				tmpFileName = tmpFileName.replaceAll("\\\\", "/");
				tmpFileName += InternalZipConstants.ZIP_FILE_SEPARATOR;
			} else {
				String bkFileName = tmpFileName.substring(0, tmpFileName.lastIndexOf(tmpFile.getName()));
				bkFileName = bkFileName.replaceAll("\\\\", "/");
				tmpFileName = bkFileName + tmpFile.getName();
			}
			
			fileName = tmpFileName;
		} else {
			File relFile = new File(file);
			if (relFile.isDirectory()) {
				fileName = relFile.getName() + InternalZipConstants.ZIP_FILE_SEPARATOR;
			} else {
				fileName = Zip4jUtil.getFileNameFromFilePath(new File(file));
			}
		}
		
		if (Zip4jUtil.isStringNotNullAndNotEmpty(rootFolderInZip)) {
			fileName = rootFolderInZip + fileName;
		}
		
		if (!Zip4jUtil.isStringNotNullAndNotEmpty(fileName)) {
			throw new RuntimeException("Error determining file name");
		}
		
		return fileName;
	}
	
	public static long[] getAllHeaderSignatures() {
		long[] allSigs = new long[11];
		
		allSigs[0] = InternalZipConstants.LOCSIG;
		allSigs[1] = InternalZipConstants.EXTSIG;
		allSigs[2] = InternalZipConstants.CENSIG;
		allSigs[3] = InternalZipConstants.ENDSIG;
		allSigs[4] = InternalZipConstants.DIGSIG;
		allSigs[5] = InternalZipConstants.ARCEXTDATREC;
		allSigs[6] = InternalZipConstants.SPLITSIG;
		allSigs[7] = InternalZipConstants.ZIP64ENDCENDIRLOC;
		allSigs[8] = InternalZipConstants.ZIP64ENDCENDIRREC;
		allSigs[9] = InternalZipConstants.EXTRAFIELDZIP64LENGTH;
		allSigs[10] = InternalZipConstants.AESSIG;
		
		return allSigs;
	}
}
