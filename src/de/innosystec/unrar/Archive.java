/*
 * Copyright (c) 2007 innoSysTec (R) GmbH, Germany. All rights reserved.
 * Original author: Edmund Wagner
 * Creation date: 22.05.2007
 *
 * Source: $HeadURL$
 * Last changed: $LastChangedDate$
 *
 * the unrar licence applies to all junrar source and binary distributions
 * you are not allowed to use this source to re-create the RAR compression
 * algorithm
 *
 * Here some html entities which can be used for escaping javadoc tags:
 * "&":  "&#038;" or "&amp;"
 * "<":  "&#060;" or "&lt;"
 * ">":  "&#062;" or "&gt;"
 * "@":  "&#064;"
 */
package de.innosystec.unrar;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import de.innosystec.unrar.exception.RarException;
import de.innosystec.unrar.exception.RarException.RarExceptionType;
import de.innosystec.unrar.io.IReadOnlyAccess;
import de.innosystec.unrar.io.ReadOnlyAccessFile;
import de.innosystec.unrar.rarfile.AVHeader;
import de.innosystec.unrar.rarfile.BaseBlock;
import de.innosystec.unrar.rarfile.BlockHeader;
import de.innosystec.unrar.rarfile.CommentHeader;
import de.innosystec.unrar.rarfile.EAHeader;
import de.innosystec.unrar.rarfile.EndArcHeader;
import de.innosystec.unrar.rarfile.FileHeader;
import de.innosystec.unrar.rarfile.MacInfoHeader;
import de.innosystec.unrar.rarfile.MainHeader;
import de.innosystec.unrar.rarfile.MarkHeader;
import de.innosystec.unrar.rarfile.ProtectHeader;
import de.innosystec.unrar.rarfile.SignHeader;
import de.innosystec.unrar.rarfile.SubBlockHeader;
import de.innosystec.unrar.rarfile.UnixOwnersHeader;
import de.innosystec.unrar.rarfile.UnrarHeadertype;
import de.innosystec.unrar.unpack.ComprDataIO;
import de.innosystec.unrar.unpack.Unpack;

/**
 * The Main Rar Class; represents a rar Archive
 *
 * @author $LastChangedBy$
 * @version $LastChangedRevision$
 */
public class Archive implements Closeable {

    private static Logger logger = Logger.getLogger(Archive.class.getName());

    private IReadOnlyAccess file;

    private InputStream rof;

//    private final UnrarCallback unrarCallback;

    private final ComprDataIO dataIO;

    private final List<BaseBlock> headers = new ArrayList<BaseBlock>();

    private MarkHeader markHead = null;

    private MainHeader newMhd = null;

    private EndArcHeader endHeader = null;

    private Unpack unpack;

    /**
     * Archive data CRC.
     */
    private long arcDataCRC = 0xffffffff;

    private int currentHeaderIndex;

    private int sfxSize = 0;

    /**
     * Size of packed data in current file.
     */
    private long totalPackedSize = 0L;

    /**
     * Number of bytes of compressed data read from current file.
     */
    private long totalPackedRead = 0L;

    private boolean saltRead = false;

//	private String password;

    private boolean pass;

    private boolean stop = false;

    /**
     * create a new archive object using the given file
     *
     * @param file the file to extract
     * @throws RarException
     */
    public Archive(IReadOnlyAccess file, long length) throws RarException, IOException {
//		this.password = password;
        dataIO = new ComprDataIO(this);
//		dataIO.setTestMode(test);
        setFile(file, length);
//		this.unrarCallback = unrarCallback;
    }

    public IReadOnlyAccess getFile() {
        return file;
    }

    void setFile(IReadOnlyAccess file, long length) throws IOException {
        this.file = file;
        totalPackedSize = 0L;
        totalPackedRead = 0L;
        close();
        rof = new de.innosystec.unrar.io.ReadOnlyAccessInputStream(file, true, length);
        try {
//			readHeaders();
            this.pass = true;
        } catch (Exception e) {
            this.pass = false;
            // logger.log(Level.WARNING,
            // "exception in archive constructor maybe file is encrypted "
            // + "or currupt", e);
            // ignore exceptions to allow exraction of working files in
            // corrupt archive
        }
        // Calculate size of packed data
    }

    public void bytesReadRead(int count) {
        if (count > 0) {
            totalPackedRead += count;
//            if (unrarCallback != null) {
//                unrarCallback.volumeProgressChanged(totalPackedRead,
//                        totalPackedSize);
//            }
        }
    }

    public InputStream getRof() {
        return rof;
    }

    // unpVersion: 29
    public void extractFile(OutputStream os, int packSize, int unpackSize,
                            int unpVersion, int fileCRC) throws RarException {
//		if (!headers.contains(hd)) {
//			throw new RarException(RarExceptionType.headerNotInArchive);
//		}
        try {
            // TODO: 2019/9/17 john 源码中看到：first file is never solid。
            doExtractFile(os, false, packSize, unpackSize, unpVersion <= 0 ? 29 : unpVersion, false, fileCRC);
        } catch (Exception e) {
            if (e instanceof RarException) {
                throw (RarException) e;
            } else {
                throw new RarException(e);
            }
        }
    }

    // unpVersion: 29
    private void doExtractFile(OutputStream os, boolean isSolid, int packSize, int unpackSize,
                               int unpVersion, boolean isSplitAfter, int fileCRC)
            throws RarException, IOException {
        dataIO.init(os);
        dataIO.init(rof, isSplitAfter, packSize);
        dataIO.setUnpFileCRC(isOldFormat() ? 0 : 0xffFFffFF);
        if (unpack == null) {
            unpack = new Unpack(dataIO);
        }
        if (!isSolid) {
            unpack.init(null);
        }
        unpack.setDestSize(unpackSize);
        try {
            // TODO: 2019/9/17 第一个参数得修改
            unpack.doUnpack((byte) 0/*0x30*/, unpVersion, isSolid);
            // Verify file CRC
            long actualCRC = isSplitAfter ? ~dataIO.getPackedCRC()
                    : ~dataIO.getUnpFileCRC();
            int expectedCRC = fileCRC;
            if (actualCRC != expectedCRC) {
                System.out.println("actualCRC: " + actualCRC + ",  CRC: " + fileCRC);
//                throw new RarException(RarExceptionType.crcError);
                // System.out.println(hd.isEncrypted());
            }
            // System.out.println(hd.isEncrypted());
            // if (!hd.isSplitAfter()) {
            // // Verify file CRC
            // if(~dataIO.getUnpFileCRC() != hd.getFileCRC()){
            // throw new RarException(RarExceptionType.crcError);
            // }
            // }
        } catch (Exception e) {
            unpack.cleanUp();
            if (e instanceof RarException) {
                // throw new RarException((RarException)e);
                throw (RarException) e;
            } else {
                throw new RarException(e);
            }
        }
    }

    public boolean isOldFormat() {
        // TODO: 2019/9/17
        return false;
    }

    /**
     * Close the underlying compressed file.
     */
    public void close() throws IOException {
        if (rof != null) {
            rof.close();
            rof = null;
        }
        if (unpack != null) {
            unpack.cleanUp();
        }
    }
}
