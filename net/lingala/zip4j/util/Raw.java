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

public class Raw
{
	
	public static final void writeShortLittleEndian(byte[] array, int pos,
			short value) {
		array[pos +1] = (byte) (value >>> 8);
		array[pos ] = (byte) (value & 0xFF);

	}
	
	public static final void writeIntLittleEndian(byte[] array, int pos,int value) {
		array[pos+3] = (byte) (value >>>24); 
		array[pos+2] = (byte) (value >>>16);
		array[pos+1] = (byte) (value >>>8);
		array[pos] = (byte) (value &0xFF);
		
	}
	
	public static void writeLongLittleEndian(byte[] array, int pos, long value){
		array[pos+7] = (byte) (value >>>56); 
		array[pos+6] = (byte) (value >>>48);
		array[pos+5] = (byte) (value >>>40);
		array[pos+4] = (byte) (value >>>32);
		array[pos+3] = (byte) (value >>>24); 
		array[pos+2] = (byte) (value >>>16);
		array[pos+1] = (byte) (value >>>8);
		array[pos] = (byte) (value &0xFF);
	}
	
	public static byte bitArrayToByte(int[] bitArray) {
		if (bitArray == null) {
			throw new RuntimeException("bit array is null, cannot calculate byte from bits");
		}
		
		if (bitArray.length != 8) {
			throw new RuntimeException("invalid bit array length, cannot calculate byte");
		}
		
		if(!checkBits(bitArray)) {
			throw new RuntimeException("invalid bits provided, bits contain other values than 0 or 1");
		}
		
		int retNum = 0;
		for (int i = 0; i < bitArray.length; i++) {
			retNum += Math.pow(2, i) * bitArray[i];
		}
		
		return (byte)retNum;
	}
	
	private static boolean checkBits(int[] bitArray) {
		for (int i = 0; i < bitArray.length; i++) {
			if (bitArray[i] != 0 && bitArray[i] != 1) {
				return false;
			}
		}
		return true;
	}
	
	public static void prepareBuffAESIVBytes(byte[] buff, int nonce, int length) {
		buff[0] = (byte)nonce;
		buff[1] = (byte)(nonce >> 8);
		buff[2] = (byte)(nonce >> 16);
		buff[3] = (byte)(nonce >> 24);
		buff[4] = 0;
		buff[5] = 0;
		buff[6] = 0;
		buff[7] = 0;
		buff[8] = 0;
		buff[9] = 0;
		buff[10] = 0;
		buff[11] = 0;
		buff[12] = 0;
		buff[13] = 0;
		buff[14] = 0;
		buff[15] = 0;
	}
	
	/**
	 * Converts a char array to byte array 
	 * @param charArray
	 * @return byte array representation of the input char array
	 */
	public static byte[] convertCharArrayToByteArray(char[] charArray) {
		if (charArray == null) {
			throw new NullPointerException();
		}
		
		byte[] bytes = new byte[charArray.length];
		for(int i=0;i<charArray.length;i++) {
		   bytes[i] = (byte) charArray[i];
		}
		return bytes;
	}
}
