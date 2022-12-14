/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ua.zp.uvs.usbaccessory;

//import com.android.accessorydisplay.common.Logger;
//import com.android.accessorydisplay.common.Transport;

import android.hardware.usb.UsbAccessory;
import android.os.ParcelFileDescriptor;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Sends or receives messages over a file descriptor associated with a {@link UsbAccessory}.
 */
public class UsbAccessoryStreamTransport {
    private ParcelFileDescriptor mFd;
    private FileInputStream mInputStream;
    private FileOutputStream mOutputStream;

    public UsbAccessoryStreamTransport(ParcelFileDescriptor fd) {

        mFd = fd;
        mInputStream = new FileInputStream(fd.getFileDescriptor());
        mOutputStream = new FileOutputStream(fd.getFileDescriptor());
    }


    protected void ioClose() {
        try {
            mFd.close();
        } catch (IOException ex) {
        }
        mFd = null;
        mInputStream = null;
        mOutputStream = null;
    }


    protected int ioRead(byte[] buffer, int offset, int count) throws IOException {
        if (mInputStream == null) {
            throw new IOException("Stream was closed.");
        }
        return mInputStream.read(buffer, offset, count);
    }


    protected void ioWrite(byte[] buffer, int offset, int count) throws IOException {
        if (mOutputStream == null) {
            throw new IOException("Stream was closed.");
        }
        mOutputStream.write(buffer, offset, count);
    }
}
