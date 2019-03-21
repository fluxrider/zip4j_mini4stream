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

package net.lingala.zip4j;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import net.lingala.zip4j.util.HeaderWriter;
import net.lingala.zip4j.crypto.AESEncrypter;
import net.lingala.zip4j.crypto.IEncrypter;
import net.lingala.zip4j.model.AESExtraDataRecord;
import net.lingala.zip4j.model.CentralDirectory;
import net.lingala.zip4j.model.EndCentralDirRecord;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.LocalFileHeader;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.ZipParameters;
import net.lingala.zip4j.util.InternalZipConstants;
import net.lingala.zip4j.util.Raw;
import net.lingala.zip4j.util.Zip4jConstants;
import net.lingala.zip4j.util.Zip4jUtil;

public class ZipOutputStream extends OutputStream {
	
  private OutputStream outputStream;
  private FileHeader fileHeader;
  private LocalFileHeader localFileHeader;
	private IEncrypter encrypter;
	private ZipParameters zipParameters;
	private ZipModel zipModel;
	private long totalBytesWritten;
	private CRC32 crc;
	private long bytesWrittenForThisFile;
	private byte[] pendingBuffer;
	private int pendingBufferLength;
	private long totalBytesRead;
	private byte[] buff;
	private Deflater deflater;
	private boolean firstBytesRead;

	public ZipOutputStream(OutputStream outputStream, ZipParameters zipParameters) {
		this(outputStream, null, zipParameters);
	}

	public ZipOutputStream(OutputStream out, ZipModel model, ZipParameters params) {
		this.outputStream = out;
		initZipModel(model);
		crc = new CRC32();
		this.totalBytesWritten = 0;
		this.bytesWrittenForThisFile = 0;
		this.pendingBuffer = new byte[InternalZipConstants.AES_BLOCK_SIZE];
		this.pendingBufferLength = 0;
		this.totalBytesRead = 0;

		deflater = new Deflater();
		buff = new byte[InternalZipConstants.BUFF_SIZE];
		firstBytesRead = false;
		putNextEntry(params);
	}
	
	private void putNextEntry(ZipParameters zipParameters) {
		try {
			this.zipParameters = (ZipParameters)zipParameters.clone();
			
			if (!Zip4jUtil.isStringNotNullAndNotEmpty(this.zipParameters.getFileNameInZip())) {
				throw new RuntimeException("file name is empty for external stream");
			}
			
			createFileHeader();
			createLocalFileHeader();
			
			if (zipModel.isSplitArchive()) {
				if (zipModel.getCentralDirectory() == null || 
						zipModel.getCentralDirectory().getFileHeaders() == null || 
						zipModel.getCentralDirectory().getFileHeaders().size() == 0) {
					byte[] intByte = new byte[4];
					Raw.writeIntLittleEndian(intByte, 0, (int)InternalZipConstants.SPLITSIG);
					outputStream.write(intByte);
					totalBytesWritten += 4;
				}
			}
			
			if (totalBytesWritten == 4) {
				fileHeader.setOffsetLocalHeader(4);
			} else {
				fileHeader.setOffsetLocalHeader(totalBytesWritten);
			}
			
			HeaderWriter headerWriter = new HeaderWriter();
			totalBytesWritten += headerWriter.writeLocalFileHeader(zipModel, localFileHeader, outputStream);
			
			if (this.zipParameters.isEncryptFiles()) {
				initEncrypter();
				if (encrypter != null) {
					byte[] saltBytes = ((AESEncrypter)encrypter).getSaltBytes();
					byte[] passwordVerifier = ((AESEncrypter)encrypter).getDerivedPasswordVerifier();
					outputStream.write(saltBytes);
					outputStream.write(passwordVerifier);
					totalBytesWritten += saltBytes.length + passwordVerifier.length;
					bytesWrittenForThisFile += saltBytes.length + passwordVerifier.length;
				}
			} 
			
			crc.reset();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		if (zipParameters.getCompressionMethod() == Zip4jConstants.COMP_DEFLATE) {
			deflater.reset();
			if ((zipParameters.getCompressionLevel() < 0 || zipParameters
					.getCompressionLevel() > 9)
					&& zipParameters.getCompressionLevel() != -1) {
				throw new RuntimeException("invalid compression level for deflater, range is [0,9].");
			}
			deflater.setLevel(zipParameters.getCompressionLevel());
		}
	}
	
	private void initEncrypter() {
		if (!zipParameters.isEncryptFiles()) {
			encrypter = null;
			return;
		}
		
		encrypter = new AESEncrypter(zipParameters.getPassword(), zipParameters.getAesKeyStrength());
	}
	
	private void initZipModel(ZipModel zipModel) {
		if (zipModel == null) {
			this.zipModel = new ZipModel();
		} else {
			this.zipModel = zipModel;
		}
		
		if (this.zipModel.getEndCentralDirRecord() == null)
			this.zipModel.setEndCentralDirRecord(new EndCentralDirRecord());
		
		if (this.zipModel.getCentralDirectory() == null)
			this.zipModel.setCentralDirectory(new CentralDirectory());
		
		if (this.zipModel.getCentralDirectory().getFileHeaders() == null)
			this.zipModel.getCentralDirectory().setFileHeaders(new ArrayList<FileHeader>());
		
		this.zipModel.getEndCentralDirRecord().setSignature(InternalZipConstants.ENDSIG);
	}
	
	public void write(int bval) throws IOException {
	    byte[] b = new byte[1];
	    b[0] = (byte) bval;
	    write(b, 0, 1);
	}
	
	public void write(byte[] b) throws IOException {
		if (b.length == 0) return;
		
		write(b, 0, b.length);
	}

	public void write(byte[] b, int off, int len) throws IOException {
		crc.update(b, off, len);
		updateTotalBytesRead(len);
		if (zipParameters.getCompressionMethod() != Zip4jConstants.COMP_DEFLATE) {
			_write(b, off, len);
		} else {
			deflater.setInput(b, off, len);
			while (!deflater.needsInput()) {
				deflate();
			}
		}
	}

	public void _write(byte [] b, int off, int len) throws IOException {
			if (len == 0) return;
			
			if (zipParameters.isEncryptFiles()) {
				if (pendingBufferLength != 0) {
					if (len >= (InternalZipConstants.AES_BLOCK_SIZE - pendingBufferLength)) {
						System.arraycopy(b, off, pendingBuffer, pendingBufferLength,
										(InternalZipConstants.AES_BLOCK_SIZE - pendingBufferLength));
						encryptAndWrite(pendingBuffer, 0, pendingBuffer.length);
						off = (InternalZipConstants.AES_BLOCK_SIZE - pendingBufferLength);
						len = len - off;
						pendingBufferLength = 0;
					} else {
						System.arraycopy(b, off, pendingBuffer, pendingBufferLength,
								len);
						pendingBufferLength += len;
						return;
					}
				}
				if (len != 0 && len % 16 != 0) {
					System.arraycopy(b, (len + off) - (len % 16), pendingBuffer, 0, len % 16);
					pendingBufferLength = len % 16;
					len = len - pendingBufferLength; 
				}
			}
			if (len != 0)
				encryptAndWrite(b, off, len);
	}

	private void encryptAndWrite(byte[] b, int off, int len) throws IOException {
		if (encrypter != null) {
			encrypter.encryptData(b, off, len);
		}
		outputStream.write(b, off, len);
		totalBytesWritten += len;
		bytesWrittenForThisFile += len;
	}
	
	private void closeEntry() throws IOException {
		if (zipParameters.getCompressionMethod() == Zip4jConstants.COMP_DEFLATE) {
			if (!deflater.finished()) {
				deflater.finish();
				while (!deflater.finished()) {
					deflate();
				}
			}
			firstBytesRead = false;
		}

		if (this.pendingBufferLength != 0) {
			encryptAndWrite(pendingBuffer, 0, pendingBufferLength);
			pendingBufferLength = 0;
		}
		
		if (this.zipParameters.isEncryptFiles()) {
			if (encrypter instanceof AESEncrypter) {
				outputStream.write(((AESEncrypter)encrypter).getFinalMac());
				bytesWrittenForThisFile += 10;
				totalBytesWritten += 10;
			} else {
				throw new RuntimeException("invalid encrypter for AES encrypted file");
			}
		}
		fileHeader.setCompressedSize(bytesWrittenForThisFile);
		localFileHeader.setCompressedSize(bytesWrittenForThisFile);
		
		fileHeader.setUncompressedSize(totalBytesRead);
		if (localFileHeader.getUncompressedSize() != totalBytesRead) {
			localFileHeader.setUncompressedSize(totalBytesRead);
		}
		
		if (zipParameters.isEncryptFiles()) {
			fileHeader.setCrc32(0);
			localFileHeader.setCrc32(0);
		} else {
  		long crc32 = crc.getValue();
			fileHeader.setCrc32(crc32);
			localFileHeader.setCrc32(crc32);
		}
		
		zipModel.getCentralDirectory().getFileHeaders().add(fileHeader);
		
		totalBytesWritten += HeaderWriter.writeExtendedLocalHeader(localFileHeader, outputStream);
		
		crc.reset();
		bytesWrittenForThisFile = 0;
		encrypter = null;
		totalBytesRead = 0;
	}
	
	private void finish() {
		zipModel.getEndCentralDirRecord().setOffsetOfStartOfCentralDir(totalBytesWritten);
		
		HeaderWriter headerWriter = new HeaderWriter();
		headerWriter.finalizeZipFile(zipModel, outputStream);
	}
	
	public void close() throws IOException {
		closeEntry();
		finish();
		if (outputStream != null)
			outputStream.close();
	}
	
	private void createFileHeader() {
		this.fileHeader = new FileHeader();
		fileHeader.setSignature((int)InternalZipConstants.CENSIG);
		fileHeader.setVersionMadeBy(20);
		fileHeader.setVersionNeededToExtract(20);
		if (zipParameters.isEncryptFiles()) {
			fileHeader.setCompressionMethod(Zip4jConstants.ENC_METHOD_AES);
			fileHeader.setAesExtraDataRecord(generateAESExtraDataRecord(zipParameters));
		} else {
			fileHeader.setCompressionMethod(zipParameters.getCompressionMethod());
		}
		if (zipParameters.isEncryptFiles()) {
			fileHeader.setEncrypted(true);
			fileHeader.setEncryptionMethod(zipParameters.getEncryptionMethod());
		}
		String fileName = null;
		fileHeader.setLastModFileTime((int) Zip4jUtil.javaToDosTime(System.currentTimeMillis()));
		if (!Zip4jUtil.isStringNotNullAndNotEmpty(zipParameters.getFileNameInZip())) {
			throw new RuntimeException("fileNameInZip is null or empty");
		}
		fileName = zipParameters.getFileNameInZip();
		
		if (!Zip4jUtil.isStringNotNullAndNotEmpty(fileName)) {
			throw new RuntimeException("fileName is null or empty. unable to create file header");
		}
		
		fileHeader.setFileName(fileName);
		
		if (Zip4jUtil.isStringNotNullAndNotEmpty(zipModel.getFileNameCharset())) {
			fileHeader.setFileNameLength(Zip4jUtil.getEncodedStringLength(fileName, 
					zipModel.getFileNameCharset()));
		} else {
			fileHeader.setFileNameLength(Zip4jUtil.getEncodedStringLength(fileName));
		}
		
		fileHeader.setDiskNumberStart(0);
		
		byte[] shortByte = new byte[2]; 
		shortByte[0] = Raw.bitArrayToByte(generateGeneralPurposeBitArray(
				fileHeader.isEncrypted(), zipParameters.getCompressionMethod()));
		boolean isFileNameCharsetSet = Zip4jUtil.isStringNotNullAndNotEmpty(zipModel.getFileNameCharset());
	    if ((isFileNameCharsetSet &&
	            zipModel.getFileNameCharset().equalsIgnoreCase(InternalZipConstants.CHARSET_UTF8)) ||
	        (!isFileNameCharsetSet &&
	            Zip4jUtil.detectCharSet(fileHeader.getFileName()).equals(InternalZipConstants.CHARSET_UTF8))) {
	        shortByte[1] = 8;
	    } else {
	        shortByte[1] = 0;
	    }
		fileHeader.setGeneralPurposeFlag(shortByte);
	}
	
	private void createLocalFileHeader() {
		if (fileHeader == null) {
			throw new RuntimeException("file header is null, cannot create local file header");
		}
		this.localFileHeader = new LocalFileHeader();
		localFileHeader.setSignature((int)InternalZipConstants.LOCSIG);
		localFileHeader.setVersionNeededToExtract(fileHeader.getVersionNeededToExtract());
		localFileHeader.setCompressionMethod(fileHeader.getCompressionMethod());
		localFileHeader.setLastModFileTime(fileHeader.getLastModFileTime());
		localFileHeader.setUncompressedSize(fileHeader.getUncompressedSize());
		localFileHeader.setFileNameLength(fileHeader.getFileNameLength());
		localFileHeader.setFileName(fileHeader.getFileName());
		localFileHeader.setEncrypted(fileHeader.isEncrypted());
		localFileHeader.setEncryptionMethod(fileHeader.getEncryptionMethod());
		localFileHeader.setAesExtraDataRecord(fileHeader.getAesExtraDataRecord());
		localFileHeader.setCrc32(fileHeader.getCrc32());
		localFileHeader.setCompressedSize(fileHeader.getCompressedSize());
		localFileHeader.setGeneralPurposeFlag(fileHeader.getGeneralPurposeFlag().clone());
	}

	private static int[] generateGeneralPurposeBitArray(boolean isEncrpyted, int compressionMethod) {
		
		int[] generalPurposeBits = new int[8];
		if (isEncrpyted) {
			generalPurposeBits[0] = 1;
		} else {
			generalPurposeBits[0] = 0;
		}
		
		if (compressionMethod == Zip4jConstants.COMP_DEFLATE) {
			// Have to set flags for deflate
		} else {
			generalPurposeBits[1] = 0;
			generalPurposeBits[2] = 0;
		}

		generalPurposeBits[3] = 1;
		
		return generalPurposeBits;
	}
	
	private static AESExtraDataRecord generateAESExtraDataRecord(ZipParameters parameters) {
		
		if (parameters == null) {
			throw new RuntimeException("parameters null, cannot generate AES Extra Data record");
		}
		
		AESExtraDataRecord aesDataRecord = new AESExtraDataRecord();
		aesDataRecord.setSignature(InternalZipConstants.AESSIG);
		aesDataRecord.setDataSize(7);
		aesDataRecord.setVendorID("AE");
		// Always set the version number to 2 as we do not store CRC for any AES encrypted files
		// only MAC is stored and as per the specification, if version number is 2, then MAC is read
		// and CRC is ignored
		aesDataRecord.setVersionNumber(2); 
		aesDataRecord.setAesStrength(Zip4jConstants.AES_STRENGTH_256);
		aesDataRecord.setCompressionMethod(parameters.getCompressionMethod());
		
		return aesDataRecord;
	}
	
	public void decrementCompressedFileSize(int value) {
		if (value <= 0) return;
		
		if (value <= this.bytesWrittenForThisFile) {
			this.bytesWrittenForThisFile -= value;
		}
	}
	
	private void updateTotalBytesRead(int toUpdate) {
		if (toUpdate > 0) {
			totalBytesRead += toUpdate;
		}
	}

	private void deflate () throws IOException {
		int len = deflater.deflate(buff, 0, buff.length);
		if (len > 0) {
			if (deflater.finished()) {
				if (len == 4) return;
				if (len < 4) {
					decrementCompressedFileSize(4 - len);
					return;
				}
				len -= 4;
			}
			if (!firstBytesRead) {
				_write(buff, 2, len - 2);
				firstBytesRead = true;
			} else {
				_write(buff, 0, len);
			}
		}
	}

}
