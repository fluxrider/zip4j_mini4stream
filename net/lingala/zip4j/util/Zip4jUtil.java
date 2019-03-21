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

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Calendar;

public class Zip4jUtil {
	
	public static boolean isStringNotNullAndNotEmpty(String str) {
		if (str == null || str.trim().length() <= 0) {
			return false;
		}
		
		return true;
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
	
}
