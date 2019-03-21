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

package net.lingala.zip4j.core;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import net.lingala.zip4j.model.AESExtraDataRecord;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.LocalFileHeader;
import net.lingala.zip4j.model.Zip64EndCentralDirLocator;
import net.lingala.zip4j.model.Zip64EndCentralDirRecord;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.util.InternalZipConstants;
import net.lingala.zip4j.util.Raw;
import net.lingala.zip4j.util.Zip4jUtil;

public class HeaderWriter {
	
	private final int ZIP64_EXTRA_BUF = 50; 
	
	public int writeLocalFileHeader(ZipModel zipModel, LocalFileHeader localFileHeader, 
			OutputStream outputStream) {
		if (localFileHeader == null) {
			throw new RuntimeException("input parameters are null, cannot write local file header");
		}
		
		try {
			ArrayList byteArrayList = new ArrayList();
			
			byte[] shortByte = new byte[2];
			byte[] intByte = new byte[4];
			byte[] longByte = new byte[8];
			byte[] emptyLongByte = {0,0,0,0,0,0,0,0};
			
			Raw.writeIntLittleEndian(intByte, 0, localFileHeader.getSignature());
			copyByteArrayToArrayList(intByte, byteArrayList);
			Raw.writeShortLittleEndian(shortByte, 0, (short)localFileHeader.getVersionNeededToExtract());
			copyByteArrayToArrayList(shortByte, byteArrayList);
			//General Purpose bit flags
			copyByteArrayToArrayList(localFileHeader.getGeneralPurposeFlag(), byteArrayList);
			//Compression Method
			Raw.writeShortLittleEndian(shortByte, 0, (short)localFileHeader.getCompressionMethod());
			copyByteArrayToArrayList(shortByte, byteArrayList);
			//File modified time
			int dateTime = localFileHeader.getLastModFileTime();
			Raw.writeIntLittleEndian(intByte, 0, (int)dateTime);
			copyByteArrayToArrayList(intByte, byteArrayList);
			//Skip crc for now - this field will be updated after data is compressed
			Raw.writeIntLittleEndian(intByte, 0, (int)localFileHeader.getCrc32());
			copyByteArrayToArrayList(intByte, byteArrayList);
			boolean writingZip64Rec = false;
			
			//compressed & uncompressed size
			long uncompressedSize = localFileHeader.getUncompressedSize();
			if (uncompressedSize + ZIP64_EXTRA_BUF >= InternalZipConstants.ZIP_64_LIMIT) {
				Raw.writeLongLittleEndian(longByte, 0, InternalZipConstants.ZIP_64_LIMIT);
				System.arraycopy(longByte, 0, intByte, 0, 4);
				
				//Set the uncompressed size to ZipConstants.ZIP_64_LIMIT as 
				//these values will be stored in Zip64 extra record
				copyByteArrayToArrayList(intByte, byteArrayList);
				
				copyByteArrayToArrayList(intByte, byteArrayList);
				zipModel.setZip64Format(true);
				writingZip64Rec = true;
				localFileHeader.setWriteComprSizeInZip64ExtraRecord(true);
			} else {
				Raw.writeLongLittleEndian(longByte, 0, localFileHeader.getCompressedSize());
				System.arraycopy(longByte, 0, intByte, 0, 4);
				copyByteArrayToArrayList(intByte, byteArrayList);
				
				Raw.writeLongLittleEndian(longByte, 0, localFileHeader.getUncompressedSize());
				System.arraycopy(longByte, 0, intByte, 0, 4);
				//Raw.writeIntLittleEndian(intByte, 0, (int)localFileHeader.getUncompressedSize());
				copyByteArrayToArrayList(intByte, byteArrayList);
				
				localFileHeader.setWriteComprSizeInZip64ExtraRecord(false);
			}
			Raw.writeShortLittleEndian(shortByte, 0, (short)localFileHeader.getFileNameLength());
			copyByteArrayToArrayList(shortByte, byteArrayList);
			// extra field length
			int extraFieldLength = 0;
			if (writingZip64Rec) {
				extraFieldLength += 20;
			}
			if (localFileHeader.getAesExtraDataRecord() != null) {
				extraFieldLength += 11;
			}
			Raw.writeShortLittleEndian(shortByte, 0, (short)(extraFieldLength));
			copyByteArrayToArrayList(shortByte, byteArrayList);
			if (Zip4jUtil.isStringNotNullAndNotEmpty(zipModel.getFileNameCharset())) {
				byte[] fileNameBytes = localFileHeader.getFileName().getBytes(zipModel.getFileNameCharset());
				copyByteArrayToArrayList(fileNameBytes,	byteArrayList);
			} else {
				copyByteArrayToArrayList(Zip4jUtil.convertCharset(localFileHeader.getFileName()), byteArrayList);
			}
			
			//Zip64 should be the first extra data record that should be written
			//This is NOT according to any specification but if this is changed
			//then take care for compressed size 
			if (writingZip64Rec) {
				
				
				//Zip64 header
				Raw.writeShortLittleEndian(shortByte, 0, (short)InternalZipConstants.EXTRAFIELDZIP64LENGTH);
				copyByteArrayToArrayList(shortByte, byteArrayList);
				//Zip64 extra data record size
				//hardcoded it to 16 for local file header as we will just write
				//compressed and uncompressed file sizes
				Raw.writeShortLittleEndian(shortByte, 0, (short)16);
				copyByteArrayToArrayList(shortByte, byteArrayList);
				//uncompressed size
				Raw.writeLongLittleEndian(longByte, 0, localFileHeader.getUncompressedSize());
				copyByteArrayToArrayList(longByte, byteArrayList);
				//set compressed size to 0 for now
				copyByteArrayToArrayList(emptyLongByte, byteArrayList);
			}
			
			if (localFileHeader.getAesExtraDataRecord() != null) {
				AESExtraDataRecord aesExtraDataRecord = localFileHeader.getAesExtraDataRecord();
				
				Raw.writeShortLittleEndian(shortByte, 0, (short)aesExtraDataRecord.getSignature());
				copyByteArrayToArrayList(shortByte, byteArrayList);
				
				Raw.writeShortLittleEndian(shortByte, 0, (short)aesExtraDataRecord.getDataSize());
				copyByteArrayToArrayList(shortByte, byteArrayList);
				
				Raw.writeShortLittleEndian(shortByte, 0, (short)aesExtraDataRecord.getVersionNumber());
				copyByteArrayToArrayList(shortByte, byteArrayList);
				
				copyByteArrayToArrayList(aesExtraDataRecord.getVendorID().getBytes(), byteArrayList);
				
				byte[] aesStrengthBytes = new byte[1];
				aesStrengthBytes[0] = (byte)aesExtraDataRecord.getAesStrength();
				copyByteArrayToArrayList(aesStrengthBytes, byteArrayList);
				
				Raw.writeShortLittleEndian(shortByte, 0, (short)aesExtraDataRecord.getCompressionMethod());
				copyByteArrayToArrayList(shortByte, byteArrayList);
			}
			byte[] lhBytes = byteArrayListToByteArray(byteArrayList);
			outputStream.write(lhBytes);
			return lhBytes.length;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public int writeExtendedLocalHeader(LocalFileHeader localFileHeader, 
			OutputStream outputStream) throws IOException {
		if (localFileHeader == null || outputStream == null) {
			throw new RuntimeException("input parameters is null, cannot write extended local header");
		}
		
		ArrayList byteArrayList = new ArrayList();
		byte[] intByte = new byte[4];
		
		//Extended local file header signature
		Raw.writeIntLittleEndian(intByte, 0, (int)InternalZipConstants.EXTSIG);
		copyByteArrayToArrayList(intByte, byteArrayList);
		
		//CRC
		Raw.writeIntLittleEndian(intByte, 0, (int)localFileHeader.getCrc32());
		copyByteArrayToArrayList(intByte, byteArrayList);
		
		//compressed size
		long compressedSize = localFileHeader.getCompressedSize();
		if (compressedSize >= Integer.MAX_VALUE) {
			compressedSize = Integer.MAX_VALUE;
		}
		Raw.writeIntLittleEndian(intByte, 0, (int)compressedSize);
		copyByteArrayToArrayList(intByte, byteArrayList);
		
		//uncompressed size 
		long uncompressedSize = localFileHeader.getUncompressedSize();
		if (uncompressedSize >= Integer.MAX_VALUE) {
			uncompressedSize = Integer.MAX_VALUE;
		}
		Raw.writeIntLittleEndian(intByte, 0, (int)uncompressedSize);
		copyByteArrayToArrayList(intByte, byteArrayList);
		
		byte[] extLocHdrBytes = byteArrayListToByteArray(byteArrayList);
		outputStream.write(extLocHdrBytes);
		return extLocHdrBytes.length;
	}
	
	/**
	 * Processes zip header data and writes this data to the zip file 
	 * @param zipModel
	 * @param outputStream
	 */
	public void finalizeZipFile(ZipModel zipModel, 
			OutputStream outputStream) {
		if (zipModel == null || outputStream == null) {
			throw new RuntimeException("input parameters is null, cannot finalize zip file");
		}
		
		try {
			processHeaderData(zipModel, outputStream);
			
			long offsetCentralDir = zipModel.getEndCentralDirRecord().getOffsetOfStartOfCentralDir();
			
			List headerBytesList = new ArrayList();
			
			int sizeOfCentralDir = writeCentralDirectory(zipModel, outputStream, headerBytesList);
			
			if (zipModel.isZip64Format()) {
				if (zipModel.getZip64EndCentralDirRecord() == null) {
					zipModel.setZip64EndCentralDirRecord(new Zip64EndCentralDirRecord());
				}
				if (zipModel.getZip64EndCentralDirLocator() == null) {
					zipModel.setZip64EndCentralDirLocator(new Zip64EndCentralDirLocator());
				}
				
				zipModel.getZip64EndCentralDirLocator().setOffsetZip64EndOfCentralDirRec(offsetCentralDir + sizeOfCentralDir);
				zipModel.getZip64EndCentralDirLocator().setNoOfDiskStartOfZip64EndOfCentralDirRec(0);
				zipModel.getZip64EndCentralDirLocator().setTotNumberOfDiscs(1);
				
				writeZip64EndOfCentralDirectoryRecord(zipModel, outputStream, sizeOfCentralDir, offsetCentralDir, headerBytesList);
				
				writeZip64EndOfCentralDirectoryLocator(zipModel, outputStream, headerBytesList);
			}
			
			writeEndOfCentralDirectoryRecord(zipModel, outputStream, sizeOfCentralDir, offsetCentralDir, headerBytesList);
			
			writeZipHeaderBytes(zipModel, outputStream, byteArrayListToByteArray(headerBytesList));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Processes zip header data and writes this data to the zip file without any validations.
	 * This process is not intended to use for normal operations (adding, deleting, etc) of a zip file.
	 * This method is used when certain validations need to be skipped (ex: Merging split zip files, 
	 * adding comment to a zip file, etc) 
	 * @param zipModel
	 * @param outputStream
	 */
	public void finalizeZipFileWithoutValidations(ZipModel zipModel, OutputStream outputStream) {
		if (zipModel == null || outputStream == null) {
			throw new RuntimeException("input parameters is null, cannot finalize zip file without validations");
		}
		
		try {
		
			List headerBytesList = new ArrayList();
			
			long offsetCentralDir = zipModel.getEndCentralDirRecord().getOffsetOfStartOfCentralDir();
			
			int sizeOfCentralDir = writeCentralDirectory(zipModel, outputStream, headerBytesList);
			
			if (zipModel.isZip64Format()) {
				if (zipModel.getZip64EndCentralDirRecord() == null) {
					zipModel.setZip64EndCentralDirRecord(new Zip64EndCentralDirRecord());
				}
				if (zipModel.getZip64EndCentralDirLocator() == null) {
					zipModel.setZip64EndCentralDirLocator(new Zip64EndCentralDirLocator());
				}
				
				zipModel.getZip64EndCentralDirLocator().setOffsetZip64EndOfCentralDirRec(offsetCentralDir + sizeOfCentralDir);
				
				writeZip64EndOfCentralDirectoryRecord(zipModel, outputStream, sizeOfCentralDir, offsetCentralDir, headerBytesList);
				writeZip64EndOfCentralDirectoryLocator(zipModel, outputStream, headerBytesList);
			}
			
			writeEndOfCentralDirectoryRecord(zipModel, outputStream, sizeOfCentralDir, offsetCentralDir, headerBytesList);
			
			writeZipHeaderBytes(zipModel, outputStream, byteArrayListToByteArray(headerBytesList));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Writes the zip header data to the zip file
	 * @param outputStream
	 * @param buff
	 */
	private void writeZipHeaderBytes(ZipModel zipModel, OutputStream outputStream, byte[] buff) {
		if (buff == null) {
			throw new RuntimeException("invalid buff to write as zip headers");
		}
		
		try {
			outputStream.write(buff);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Fills the header data in the zip model
	 * @param zipModel
	 * @param outputStream
	 */
	private void processHeaderData(ZipModel zipModel, OutputStream outputStream) {
		int currSplitFileCounter = 0;
		
		if (zipModel.isZip64Format()) {
			if (zipModel.getZip64EndCentralDirRecord() == null) {
				zipModel.setZip64EndCentralDirRecord(new Zip64EndCentralDirRecord());
			}
			if (zipModel.getZip64EndCentralDirLocator() == null) {
				zipModel.setZip64EndCentralDirLocator(new Zip64EndCentralDirLocator());
			}
			
			zipModel.getZip64EndCentralDirLocator().setNoOfDiskStartOfZip64EndOfCentralDirRec(currSplitFileCounter);
			zipModel.getZip64EndCentralDirLocator().setTotNumberOfDiscs(currSplitFileCounter + 1);
		}
		zipModel.getEndCentralDirRecord().setNoOfThisDisk(currSplitFileCounter);
		zipModel.getEndCentralDirRecord().setNoOfThisDiskStartOfCentralDir(currSplitFileCounter);
	}
	
	/**
	 * Writes central directory header data to an array list
	 * @param zipModel
	 * @param outputStream
	 * @param headerBytesList
	 * @return size of central directory
	 */
	private int writeCentralDirectory(ZipModel zipModel, 
			OutputStream outputStream, List headerBytesList) {
		if (zipModel == null || outputStream == null) {
			throw new RuntimeException("input parameters is null, cannot write central directory");
		}
		
		if (zipModel.getCentralDirectory() == null || 
				zipModel.getCentralDirectory().getFileHeaders() == null || 
				zipModel.getCentralDirectory().getFileHeaders().size() <= 0) {
			return 0;
		}
		
		int sizeOfCentralDir = 0;
		for (int i = 0; i < zipModel.getCentralDirectory().getFileHeaders().size(); i++) {
			FileHeader fileHeader = (FileHeader)zipModel.getCentralDirectory().getFileHeaders().get(i);
			int sizeOfFileHeader = writeFileHeader(zipModel, fileHeader, outputStream, headerBytesList);
			sizeOfCentralDir += sizeOfFileHeader;
		}
		return sizeOfCentralDir;
	}
	
	private int writeFileHeader(ZipModel zipModel, FileHeader fileHeader, 
			OutputStream outputStream, List headerBytesList) {
		
		if (fileHeader == null || outputStream == null) {
			throw new RuntimeException("input parameters is null, cannot write local file header");
		}
		
		try {
			int sizeOfFileHeader = 0;
			
			byte[] shortByte = new byte[2];
			byte[] intByte = new byte[4];
			byte[] longByte = new byte[8];
			final byte[] emptyShortByte = {0,0};
			final byte[] emptyIntByte = {0,0,0,0};
			
			boolean writeZip64FileSize = false;
			boolean writeZip64OffsetLocalHeader = false;
			
			Raw.writeIntLittleEndian(intByte, 0, fileHeader.getSignature());
			copyByteArrayToArrayList(intByte, headerBytesList);
			sizeOfFileHeader += 4;
			
			Raw.writeShortLittleEndian(shortByte, 0, (short)fileHeader.getVersionMadeBy());
			copyByteArrayToArrayList(shortByte, headerBytesList);
			sizeOfFileHeader += 2; 
			
			Raw.writeShortLittleEndian(shortByte, 0, (short)fileHeader.getVersionNeededToExtract());
			copyByteArrayToArrayList(shortByte, headerBytesList);
			sizeOfFileHeader += 2;
			
			copyByteArrayToArrayList(fileHeader.getGeneralPurposeFlag(), headerBytesList);
			sizeOfFileHeader += 2;
			
			Raw.writeShortLittleEndian(shortByte, 0, (short)fileHeader.getCompressionMethod());
			copyByteArrayToArrayList(shortByte, headerBytesList);
			sizeOfFileHeader += 2;
			
			int dateTime = fileHeader.getLastModFileTime();
			Raw.writeIntLittleEndian(intByte, 0, dateTime);
			copyByteArrayToArrayList(intByte, headerBytesList);
			sizeOfFileHeader += 4;
			
			Raw.writeIntLittleEndian(intByte, 0, (int)(fileHeader.getCrc32()));
			copyByteArrayToArrayList(intByte, headerBytesList);
			sizeOfFileHeader += 4;
			
			if (fileHeader.getCompressedSize() >= InternalZipConstants.ZIP_64_LIMIT || 
					fileHeader.getUncompressedSize() + ZIP64_EXTRA_BUF >= InternalZipConstants.ZIP_64_LIMIT) {
				Raw.writeLongLittleEndian(longByte, 0, InternalZipConstants.ZIP_64_LIMIT);
				System.arraycopy(longByte, 0, intByte, 0, 4);
				
				copyByteArrayToArrayList(intByte, headerBytesList);
				sizeOfFileHeader += 4;
				
				copyByteArrayToArrayList(intByte, headerBytesList);
				sizeOfFileHeader += 4;
				
				writeZip64FileSize = true;
			} else {
				Raw.writeLongLittleEndian(longByte, 0, fileHeader.getCompressedSize());
				System.arraycopy(longByte, 0, intByte, 0, 4);
//				Raw.writeIntLittleEndian(intByte, 0, (int)fileHeader.getCompressedSize());
				copyByteArrayToArrayList(intByte, headerBytesList);
				sizeOfFileHeader += 4;
				
				Raw.writeLongLittleEndian(longByte, 0, fileHeader.getUncompressedSize());
				System.arraycopy(longByte, 0, intByte, 0, 4);
//				Raw.writeIntLittleEndian(intByte, 0, (int)fileHeader.getUncompressedSize());
				copyByteArrayToArrayList(intByte, headerBytesList);
				sizeOfFileHeader += 4;
			}
			
			Raw.writeShortLittleEndian(shortByte, 0, (short)fileHeader.getFileNameLength());
			copyByteArrayToArrayList(shortByte, headerBytesList);
			sizeOfFileHeader += 2;
			
			//Compute offset bytes before extra field is written for Zip64 compatibility
			//NOTE: this data is not written now, but written at a later point
			byte[] offsetLocalHeaderBytes = new byte[4];
			if (fileHeader.getOffsetLocalHeader() > InternalZipConstants.ZIP_64_LIMIT) {
				Raw.writeLongLittleEndian(longByte, 0, InternalZipConstants.ZIP_64_LIMIT);
				System.arraycopy(longByte, 0, offsetLocalHeaderBytes, 0, 4);
				writeZip64OffsetLocalHeader = true;
			} else {
				Raw.writeLongLittleEndian(longByte, 0, fileHeader.getOffsetLocalHeader());
				System.arraycopy(longByte, 0, offsetLocalHeaderBytes, 0, 4);
			}
			
			// extra field length
			int extraFieldLength = 0;
			if (writeZip64FileSize || writeZip64OffsetLocalHeader) {
				extraFieldLength += 4;
				if (writeZip64FileSize)
					extraFieldLength += 16;
				if (writeZip64OffsetLocalHeader)
					extraFieldLength += 8;
			}
			if (fileHeader.getAesExtraDataRecord() != null) {
				extraFieldLength += 11;
			}
			Raw.writeShortLittleEndian(shortByte, 0, (short)(extraFieldLength));
			copyByteArrayToArrayList(shortByte, headerBytesList);
			sizeOfFileHeader += 2;
			
			//Skip file comment length for now
			copyByteArrayToArrayList(emptyShortByte, headerBytesList);
			sizeOfFileHeader += 2;
			
			//Skip disk number start for now
			Raw.writeShortLittleEndian(shortByte, 0, (short)(fileHeader.getDiskNumberStart()));
			copyByteArrayToArrayList(shortByte, headerBytesList);
			sizeOfFileHeader += 2;
			
			//Skip internal file attributes for now
			copyByteArrayToArrayList(emptyShortByte, headerBytesList);
			sizeOfFileHeader += 2;
			
			//External file attributes
			if (fileHeader.getExternalFileAttr() != null) {
				copyByteArrayToArrayList(fileHeader.getExternalFileAttr(), headerBytesList);
			} else {
				copyByteArrayToArrayList(emptyIntByte, headerBytesList);
			}
			sizeOfFileHeader += 4;
			
			//offset local header
			//this data is computed above
			copyByteArrayToArrayList(offsetLocalHeaderBytes, headerBytesList);
			sizeOfFileHeader += 4;
			
			if (Zip4jUtil.isStringNotNullAndNotEmpty(zipModel.getFileNameCharset())) {
				byte[] fileNameBytes = fileHeader.getFileName().getBytes(zipModel.getFileNameCharset());
				copyByteArrayToArrayList(fileNameBytes,	headerBytesList);
				sizeOfFileHeader += fileNameBytes.length;
			} else {
				copyByteArrayToArrayList(Zip4jUtil.convertCharset(fileHeader.getFileName()), headerBytesList);
				sizeOfFileHeader += Zip4jUtil.getEncodedStringLength(fileHeader.getFileName());
			}
			
			if (writeZip64FileSize || writeZip64OffsetLocalHeader) {
				zipModel.setZip64Format(true);
				
				//Zip64 header
				Raw.writeShortLittleEndian(shortByte, 0, (short)InternalZipConstants.EXTRAFIELDZIP64LENGTH);
				copyByteArrayToArrayList(shortByte, headerBytesList);
				sizeOfFileHeader += 2;
				
				//Zip64 extra data record size
				int dataSize = 0;
				
				if (writeZip64FileSize) {
					dataSize += 16;
				}
				if (writeZip64OffsetLocalHeader) {
					dataSize += 8;
				}
				
				Raw.writeShortLittleEndian(shortByte, 0, (short)dataSize);
				copyByteArrayToArrayList(shortByte, headerBytesList);
				sizeOfFileHeader += 2;
				
				if (writeZip64FileSize) {
					Raw.writeLongLittleEndian(longByte, 0, fileHeader.getUncompressedSize());
					copyByteArrayToArrayList(longByte, headerBytesList);
					sizeOfFileHeader += 8;
					
					Raw.writeLongLittleEndian(longByte, 0, fileHeader.getCompressedSize());
					copyByteArrayToArrayList(longByte, headerBytesList);
					sizeOfFileHeader += 8;
				}
				
				if (writeZip64OffsetLocalHeader) {
					Raw.writeLongLittleEndian(longByte, 0, fileHeader.getOffsetLocalHeader());
					copyByteArrayToArrayList(longByte, headerBytesList);
					sizeOfFileHeader += 8;
				}
			}
			
			if (fileHeader.getAesExtraDataRecord() != null) {
				AESExtraDataRecord aesExtraDataRecord = fileHeader.getAesExtraDataRecord();
				
				Raw.writeShortLittleEndian(shortByte, 0, (short)aesExtraDataRecord.getSignature());
				copyByteArrayToArrayList(shortByte, headerBytesList);
				
				Raw.writeShortLittleEndian(shortByte, 0, (short)aesExtraDataRecord.getDataSize());
				copyByteArrayToArrayList(shortByte, headerBytesList);
				
				Raw.writeShortLittleEndian(shortByte, 0, (short)aesExtraDataRecord.getVersionNumber());
				copyByteArrayToArrayList(shortByte, headerBytesList);
				
				copyByteArrayToArrayList(aesExtraDataRecord.getVendorID().getBytes(), headerBytesList);
				
				byte[] aesStrengthBytes = new byte[1];
				aesStrengthBytes[0] = (byte)aesExtraDataRecord.getAesStrength();
				copyByteArrayToArrayList(aesStrengthBytes, headerBytesList);
				
				Raw.writeShortLittleEndian(shortByte, 0, (short)aesExtraDataRecord.getCompressionMethod());
				copyByteArrayToArrayList(shortByte, headerBytesList);
				
				sizeOfFileHeader += 11;
			}
			
//			outputStream.write(byteArrayListToByteArray(headerBytesList));
			
			return sizeOfFileHeader;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private void writeZip64EndOfCentralDirectoryRecord(ZipModel zipModel, 
			OutputStream outputStream, int sizeOfCentralDir, 
			long offsetCentralDir, List headerBytesList) {
		if (zipModel == null || outputStream == null) {
			throw new RuntimeException("zip model or output stream is null, cannot write zip64 end of central directory record");
		}
		
		try {
			
			byte[] shortByte = new byte[2];
			byte[] emptyShortByte = {0,0};
			byte[] intByte = new byte[4];
			byte[] longByte = new byte[8];
			
			//zip64 end of central dir signature
			Raw.writeIntLittleEndian(intByte, 0, (int)InternalZipConstants.ZIP64ENDCENDIRREC);
			copyByteArrayToArrayList(intByte, headerBytesList);
			
			//size of zip64 end of central directory record
			Raw.writeLongLittleEndian(longByte, 0, (long)44);
			copyByteArrayToArrayList(longByte, headerBytesList);
			
			//version made by
			//version needed to extract
			if (zipModel.getCentralDirectory() != null && 
					zipModel.getCentralDirectory().getFileHeaders() != null &&
					zipModel.getCentralDirectory().getFileHeaders().size() > 0) {
				Raw.writeShortLittleEndian(shortByte, 0, 
						(short)((FileHeader)zipModel.getCentralDirectory().getFileHeaders().get(0)).getVersionMadeBy());
				copyByteArrayToArrayList(shortByte, headerBytesList);
				
				Raw.writeShortLittleEndian(shortByte, 0, 
						(short)((FileHeader)zipModel.getCentralDirectory().getFileHeaders().get(0)).getVersionNeededToExtract());
				copyByteArrayToArrayList(shortByte, headerBytesList);
			} else {
				copyByteArrayToArrayList(emptyShortByte, headerBytesList);
				copyByteArrayToArrayList(emptyShortByte, headerBytesList);
			}
			
			//number of this disk
			Raw.writeIntLittleEndian(intByte, 0, zipModel.getEndCentralDirRecord().getNoOfThisDisk());
			copyByteArrayToArrayList(intByte, headerBytesList);
			
			//number of the disk with start of central directory
			Raw.writeIntLittleEndian(intByte, 0, zipModel.getEndCentralDirRecord().getNoOfThisDiskStartOfCentralDir());
			copyByteArrayToArrayList(intByte, headerBytesList);
			
			//total number of entries in the central directory on this disk
			int numEntries = 0;
			int numEntriesOnThisDisk = 0;
			if (zipModel.getCentralDirectory() == null || 
					zipModel.getCentralDirectory().getFileHeaders() == null) {
				throw new RuntimeException("invalid central directory/file headers, " +
						"cannot write end of central directory record");
			} else {
				numEntries = zipModel.getCentralDirectory().getFileHeaders().size();
				if (zipModel.isSplitArchive()) {
					countNumberOfFileHeaderEntriesOnDisk(zipModel.getCentralDirectory().getFileHeaders(), 
							zipModel.getEndCentralDirRecord().getNoOfThisDisk());
				} else {
					numEntriesOnThisDisk = numEntries;
				}
			}
			Raw.writeLongLittleEndian(longByte, 0, numEntriesOnThisDisk);
			copyByteArrayToArrayList(longByte, headerBytesList);
			
			//Total number of entries in central directory
			Raw.writeLongLittleEndian(longByte, 0, numEntries);
			copyByteArrayToArrayList(longByte, headerBytesList);
			
			//Size of central directory
			Raw.writeLongLittleEndian(longByte, 0, sizeOfCentralDir);
			copyByteArrayToArrayList(longByte, headerBytesList);
			
			//offset of start of central directory with respect to the starting disk number
			Raw.writeLongLittleEndian(longByte, 0, offsetCentralDir);
			copyByteArrayToArrayList(longByte, headerBytesList);
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private void writeZip64EndOfCentralDirectoryLocator(ZipModel zipModel, 
			OutputStream outputStream, List headerBytesList) {
		if (zipModel == null || outputStream == null) {
			throw new RuntimeException("zip model or output stream is null, cannot write zip64 end of central directory locator");
		}
		
		try {
			
			byte[] intByte = new byte[4];
			byte[] longByte = new byte[8];
			
			//zip64 end of central dir locator  signature
			Raw.writeIntLittleEndian(intByte, 0, (int)InternalZipConstants.ZIP64ENDCENDIRLOC);
			copyByteArrayToArrayList(intByte, headerBytesList);
			
			//number of the disk with the start of the zip64 end of central directory
			Raw.writeIntLittleEndian(intByte, 0, zipModel.getZip64EndCentralDirLocator().getNoOfDiskStartOfZip64EndOfCentralDirRec());
			copyByteArrayToArrayList(intByte, headerBytesList);
			
			//relative offset of the zip64 end of central directory record
			Raw.writeLongLittleEndian(longByte, 0, zipModel.getZip64EndCentralDirLocator().getOffsetZip64EndOfCentralDirRec());
			copyByteArrayToArrayList(longByte, headerBytesList);
			
			//total number of disks
			Raw.writeIntLittleEndian(intByte, 0, zipModel.getZip64EndCentralDirLocator().getTotNumberOfDiscs());
			copyByteArrayToArrayList(intByte, headerBytesList);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private void writeEndOfCentralDirectoryRecord(ZipModel zipModel, 
			OutputStream outputStream, 
			int sizeOfCentralDir, 
			long offsetCentralDir,
			List headrBytesList) {
		if (zipModel == null || outputStream == null) {
			throw new RuntimeException("zip model or output stream is null, cannot write end of central directory record");
		}
		
		try {
			
			byte[] shortByte = new byte[2];
			byte[] intByte = new byte[4];
			byte[] longByte = new byte[8];
			
			//End of central directory signature
			Raw.writeIntLittleEndian(intByte, 0, (int)zipModel.getEndCentralDirRecord().getSignature());
			copyByteArrayToArrayList(intByte, headrBytesList);
			
			//number of this disk
			Raw.writeShortLittleEndian(shortByte, 0, (short)(zipModel.getEndCentralDirRecord().getNoOfThisDisk()));
			copyByteArrayToArrayList(shortByte, headrBytesList);
			
			//number of the disk with start of central directory
			Raw.writeShortLittleEndian(shortByte, 0, (short)(zipModel.getEndCentralDirRecord().getNoOfThisDiskStartOfCentralDir()));
			copyByteArrayToArrayList(shortByte, headrBytesList);
			
			//Total number of entries in central directory on this disk
			int numEntries = 0;
			int numEntriesOnThisDisk = 0;
			if (zipModel.getCentralDirectory() == null || 
					zipModel.getCentralDirectory().getFileHeaders() == null) {
				throw new RuntimeException("invalid central directory/file headers, " +
						"cannot write end of central directory record");
			} else {
				numEntries = zipModel.getCentralDirectory().getFileHeaders().size();
				if (zipModel.isSplitArchive()) {
					numEntriesOnThisDisk = countNumberOfFileHeaderEntriesOnDisk(zipModel.getCentralDirectory().getFileHeaders(), 
							zipModel.getEndCentralDirRecord().getNoOfThisDisk());
				} else {
					numEntriesOnThisDisk = numEntries;
				}
			}
			Raw.writeShortLittleEndian(shortByte, 0, (short)numEntriesOnThisDisk);
			copyByteArrayToArrayList(shortByte, headrBytesList);
			
			//Total number of entries in central directory
			Raw.writeShortLittleEndian(shortByte, 0, (short)numEntries);
			copyByteArrayToArrayList(shortByte, headrBytesList);
			
			//Size of central directory
			Raw.writeIntLittleEndian(intByte, 0, sizeOfCentralDir);
			copyByteArrayToArrayList(intByte, headrBytesList);
			
			//Offset central directory
			if (offsetCentralDir > InternalZipConstants.ZIP_64_LIMIT) {
				Raw.writeLongLittleEndian(longByte, 0, InternalZipConstants.ZIP_64_LIMIT);
				System.arraycopy(longByte, 0, intByte, 0, 4);
				copyByteArrayToArrayList(intByte, headrBytesList);
			} else {
				Raw.writeLongLittleEndian(longByte, 0, offsetCentralDir);
				System.arraycopy(longByte, 0, intByte, 0, 4);
//				Raw.writeIntLittleEndian(intByte, 0, (int)offsetCentralDir);
				copyByteArrayToArrayList(intByte, headrBytesList);
			}
			
			//Zip File comment length
			int commentLength = 0;
			if (zipModel.getEndCentralDirRecord().getComment() != null) {
				commentLength = zipModel.getEndCentralDirRecord().getCommentLength();
			}
			Raw.writeShortLittleEndian(shortByte, 0, (short)commentLength);
			copyByteArrayToArrayList(shortByte, headrBytesList);
			
			//Comment
			if (commentLength > 0) {
				copyByteArrayToArrayList(zipModel.getEndCentralDirRecord().getCommentBytes(), headrBytesList);
			}
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private void copyByteArrayToArrayList(byte[] byteArray, List arrayList) {
		if (arrayList == null || byteArray == null) {
			throw new RuntimeException("one of the input parameters is null, cannot copy byte array to array list");
		}
		
		for (int i = 0; i < byteArray.length; i++) {
			arrayList.add(Byte.toString(byteArray[i]));
		}
	}
	
	private byte[] byteArrayListToByteArray(List arrayList) {
		if (arrayList == null) {
			throw new RuntimeException("input byte array list is null, cannot conver to byte array");
		}
		
		if (arrayList.size() <= 0) {
			return null;
		}
		
		byte[] retBytes = new byte[arrayList.size()];
		
		for (int i = 0; i < arrayList.size(); i++) {
			retBytes[i] = Byte.parseByte((String)arrayList.get(i));
		}
		
		return retBytes;
	}
	
	private int countNumberOfFileHeaderEntriesOnDisk(ArrayList fileHeaders, 
			int numOfDisk) {
		if (fileHeaders == null) {
			throw new RuntimeException("file headers are null, cannot calculate number of entries on this disk");
		}
		
		int noEntries = 0;
		for (int i = 0; i < fileHeaders.size(); i++) {
			FileHeader fileHeader = (FileHeader)fileHeaders.get(i);
			if (fileHeader.getDiskNumberStart() == numOfDisk) {
				noEntries++;
			}
		}
		return noEntries;
	}
	
}
