/*
 * Copyright (c) 2007 innoSysTec (R) GmbH, Germany. All rights reserved.
 * Original author: Edmund Wagner
 * Creation date: 31.05.2007
 *
 * Source: $HeadURL$
 * Last changed: $LastChangedDate$
 *
 * the unrar licence applies to all junrar source and binary distributions
 * you are not allowed to use this source to re-create the RAR compression algorithm
 *
 * Here some html entities which can be used for escaping javadoc tags:
 * "&":  "&#038;" or "&amp;"
 * "<":  "&#060;" or "&lt;"
 * ">":  "&#062;" or "&gt;"
 * "@":  "&#064;"
 */
package de.innosystec.unrar.unpack;

import de.innosystec.unrar.Archive;
import de.innosystec.unrar.crc.RarCRC;
import de.innosystec.unrar.exception.RarException;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * DOCUMENT ME
 *
 * @author $LastChangedBy$
 * @version $LastChangedRevision$
 */
public class ComprDataIO {
    private final Archive archive;
    private long unpPackedSize;
    private boolean testMode;
    private boolean skipUnpCRC;
    private InputStream inputStream;
    private OutputStream outputStream;
    //	private FileHeader subHead;
    private boolean isSplitAfter;
    // cryptData Crypt;
    // cryptData Decrypt;
    private boolean packVolume;
    private boolean unpVolume;
    private boolean nextVolumeMissing;
    private long totalPackRead;
    private long unpArcSize;
    private long curPackRead, curPackWrite, curUnpRead, curUnpWrite;
    private long processedArcSize, totalArcSize;
    private long packFileCRC, unpFileCRC, packedCRC;
    private int encryption;
    private int decryption;
    private int lastPercent;
    private char currentCommand;

    public ComprDataIO(Archive arc) {
        this.archive = arc;
    }

    public void init(OutputStream outputStream) {
        this.outputStream = outputStream;
        unpPackedSize = 0;
        testMode = false;
        skipUnpCRC = false;
        packVolume = false;
        unpVolume = false;
        nextVolumeMissing = false;
        // command = null;
        encryption = 0;
        decryption = 0;
        totalPackRead = 0;
        curPackRead = curPackWrite = curUnpRead = curUnpWrite = 0;
        packFileCRC = unpFileCRC = packedCRC = 0xffffffff;
        lastPercent = -1;
//		subHead = null;

        currentCommand = 0;
        processedArcSize = totalArcSize = 0;
    }

    public void init(InputStream in, boolean isSplitAfter, int fullPackSize) throws IOException {
        long startPos = 0;//hd.getPositionInFile() + hd.getHeaderSize();
//        if(isEncrypted){
//        	startPos += hd.getPaddedSize()+8;
//        }
        unpPackedSize = fullPackSize; //hd.getFullPackSize();
        inputStream = in;
//		subHead = hd;
        this.isSplitAfter = isSplitAfter;
        curUnpRead = 0;
        curPackWrite = 0;
        packedCRC = 0xFFffFFff;
    }

    public int unpRead(byte[] addr, int offset, int count)
            throws IOException, RarException {
        int retCode = 0, totalRead = 0;
        while (count > 0) {
            int readSize = (count > unpPackedSize) ? (int) unpPackedSize : count;
            retCode = inputStream.read(addr, offset, readSize);
            if (retCode < 0) {
                throw new EOFException();
            }
            if (isSplitAfter) {
                packedCRC = RarCRC.checkCrc(
                        (int) packedCRC, addr, offset, retCode);
            }

            curUnpRead += retCode;
            totalRead += retCode;
            offset += retCode;
            count -= retCode;
            unpPackedSize -= retCode;
            archive.bytesReadRead(retCode);
            if (unpPackedSize == 0 && isSplitAfter) {
                /*if (!Volume.mergeArchive(archive, this)) {
                    nextVolumeMissing = true;
                    return -1;
                }*/
            } else {
                break;
            }
        }

        if (retCode != -1) {
            retCode = totalRead;
        }
        return retCode;
    }

    public void unpWrite(byte[] addr, int offset, int count)
            throws IOException {
        if (!testMode) {
            // DestFile->Write(Addr,Count);
            outputStream.write(addr, offset, count);
        }

        curUnpWrite += count;

        if (!skipUnpCRC) {
            if (archive.isOldFormat()) {
                unpFileCRC = RarCRC.checkOldCrc(
                        (short) unpFileCRC, addr, count);
            } else {
                unpFileCRC = RarCRC.checkCrc(
                        (int) unpFileCRC, addr, offset, count);
            }
        }
//            if (!skipArcCRC) {
//                archive.updateDataCRC(Addr, offset, ReadSize);
//            }
    }

    public void setPackedSizeToRead(long size) {
        unpPackedSize = size;
    }

    public void setTestMode(boolean mode) {
        testMode = mode;
    }

    public boolean isTestMode() {
        return testMode;
    }

    public void setSkipUnpCRC(boolean skip) {
        skipUnpCRC = skip;
    }

    public long getCurPackRead() {
        return curPackRead;
    }

    public void setCurPackRead(long curPackRead) {
        this.curPackRead = curPackRead;
    }

    public long getCurPackWrite() {
        return curPackWrite;
    }

    public void setCurPackWrite(long curPackWrite) {
        this.curPackWrite = curPackWrite;
    }

    public long getCurUnpRead() {
        return curUnpRead;
    }

    public void setCurUnpRead(long curUnpRead) {
        this.curUnpRead = curUnpRead;
    }

    public long getCurUnpWrite() {
        return curUnpWrite;
    }

    public void setCurUnpWrite(long curUnpWrite) {
        this.curUnpWrite = curUnpWrite;
    }

    public int getDecryption() {
        return decryption;
    }

    public void setDecryption(int decryption) {
        this.decryption = decryption;
    }

    public int getEncryption() {
        return encryption;
    }

    public void setEncryption(int encryption) {
        this.encryption = encryption;
    }

    public boolean isNextVolumeMissing() {
        return nextVolumeMissing;
    }

    public void setNextVolumeMissing(boolean nextVolumeMissing) {
        this.nextVolumeMissing = nextVolumeMissing;
    }

    public long getPackedCRC() {
        return packedCRC;
    }

    public void setPackedCRC(long packedCRC) {
        this.packedCRC = packedCRC;
    }

    public long getPackFileCRC() {
        return packFileCRC;
    }

    public void setPackFileCRC(long packFileCRC) {
        this.packFileCRC = packFileCRC;
    }

    public boolean isPackVolume() {
        return packVolume;
    }

    public void setPackVolume(boolean packVolume) {
        this.packVolume = packVolume;
    }

    public long getProcessedArcSize() {
        return processedArcSize;
    }

    public void setProcessedArcSize(long processedArcSize) {
        this.processedArcSize = processedArcSize;
    }

    public long getTotalArcSize() {
        return totalArcSize;
    }

    public void setTotalArcSize(long totalArcSize) {
        this.totalArcSize = totalArcSize;
    }

    public long getTotalPackRead() {
        return totalPackRead;
    }

    public void setTotalPackRead(long totalPackRead) {
        this.totalPackRead = totalPackRead;
    }

    public long getUnpArcSize() {
        return unpArcSize;
    }

    public void setUnpArcSize(long unpArcSize) {
        this.unpArcSize = unpArcSize;
    }

    public long getUnpFileCRC() {
        return unpFileCRC;
    }

    public void setUnpFileCRC(long unpFileCRC) {
        this.unpFileCRC = unpFileCRC;
    }

    public boolean isUnpVolume() {
        return unpVolume;
    }

    public void setUnpVolume(boolean unpVolume) {
        this.unpVolume = unpVolume;
    }
}
