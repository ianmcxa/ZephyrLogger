/*   
 * Copyright 2013 (C) Christian Orthmann
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

/*
 * Copyright (C) 2010 Pye Brook Company, Inc.
 *               http://www.pyebrook.com
 *               info@pyebrook.com
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
 *
 *
 * This software uses information from the document
 *
 *     'Bluetooth HXM API Guide 2010-07-22'
 *
 * which is Copyright (C) Zephyr Technology, and used with the permission
 * of the company. Information on Zephyr Technology products and how to 
 * obtain the Bluetooth HXM API Guide can be found on the Zephyr
 * Technology Corporation website at
 * 
 *      http://www.zephyr-technology.com
 * 
 *
 */

package org.mcxa.zephyrlogger.hxm;

import android.util.Log;

/*
 * HrmReading
 * 
 * This class holds the information corresponding to a single message from 
 * the Zephyr HxM Heart Rate Monitor
 * 
 * The constructor HrmReading(byte[]) will fill the member fields from the bytes presumably 
 * read from a connected Zephyr HxM Heart Rate Monitor.  Because Java does not support 
 * signed/unsigned variants of numbers, we sometimes put the fields extracted from the 
 * HxM message into fields larger than is necessary.
 * 
 *
 *  
 */
public class HrmReading {
    public final int STX = 0x02;
    public final int MSGID = 0x26;
    public final int DLC = 55;
    public final int ETX = 0x03;

    private static final String TAG = "HrmReading";

    public int serial;
    public byte stx;
    public byte msgId;
    public byte dlc;
    public int firmwareId;
    public int firmwareVersion;
    public int hardWareId;
    public int hardwareVersion;
    public int batteryIndicator;
    public int heartRate;
    public int heartBeatNumber;
    public long hbTime1;
    public long hbTime2;
    public long hbTime3;
    public long hbTime4;
    public long hbTime5;
    public long hbTime6;
    public long hbTime7;
    public long hbTime8;
    public long hbTime9;
    public long hbTime10;
    public long hbTime11;
    public long hbTime12;
    public long hbTime13;
    public long hbTime14;
    public long hbTime15;
    public long reserved1;
    public long reserved2;
    public long reserved3;
    public long distance;
    public long speed;
    public byte strides;
    public byte reserved4;
    public long reserved5;
    public byte crc;
    public byte etx;

    public HrmReading(byte[] buffer) {
        int bufferIndex = 0;

        Log.d(TAG, "HrmReading being built from byte buffer");

        try {
            stx = buffer[bufferIndex++];
            msgId = buffer[bufferIndex++];
            dlc = buffer[bufferIndex++];
            firmwareId = ((0x000000FF & buffer[bufferIndex++]) | (0x000000FF & buffer[bufferIndex++]) << 8);
            firmwareVersion = ((0x000000FF & buffer[bufferIndex++]) | (0x000000FF & buffer[bufferIndex++]) << 8);
            hardWareId = ((0x000000FF & buffer[bufferIndex++]) | (0x000000FF & buffer[bufferIndex++]) << 8);
            hardwareVersion = ((0x000000FF & buffer[bufferIndex++]) | (0x000000FF & buffer[bufferIndex++]) << 8);
            batteryIndicator = (0x000000FF & (buffer[bufferIndex++]));
            heartRate = (0x000000FF & (buffer[bufferIndex++]));
            heartBeatNumber = (0x000000FF & (buffer[bufferIndex++]));
            hbTime1 = ((0x000000FF & buffer[bufferIndex++]) | (0x000000FF & buffer[bufferIndex++]) << 8);
            hbTime2 = ((0x000000FF & buffer[bufferIndex++]) | (0x000000FF & buffer[bufferIndex++]) << 8);
            hbTime3 = ((0x000000FF & buffer[bufferIndex++]) | (0x000000FF & buffer[bufferIndex++]) << 8);
            hbTime4 = ((0x000000FF & buffer[bufferIndex++]) | (0x000000FF & buffer[bufferIndex++]) << 8);
            hbTime5 = ((0x000000FF & buffer[bufferIndex++]) | (0x000000FF & buffer[bufferIndex++]) << 8);
            hbTime6 = ((0x000000FF & buffer[bufferIndex++]) | (0x000000FF & buffer[bufferIndex++]) << 8);
            hbTime7 = ((0x000000FF & buffer[bufferIndex++]) | (0x000000FF & buffer[bufferIndex++]) << 8);
            hbTime8 = ((0x000000FF & buffer[bufferIndex++]) | (0x000000FF & buffer[bufferIndex++]) << 8);
            hbTime9 = ((0x000000FF & buffer[bufferIndex++]) | (0x000000FF & buffer[bufferIndex++]) << 8);
            hbTime10 = ((0x000000FF & buffer[bufferIndex++]) | (0x000000FF & buffer[bufferIndex++]) << 8);
            hbTime11 = ((0x000000FF & buffer[bufferIndex++]) | (0x000000FF & buffer[bufferIndex++]) << 8);
            hbTime12 = ((0x000000FF & buffer[bufferIndex++]) | (0x000000FF & buffer[bufferIndex++]) << 8);
            hbTime13 = ((0x000000FF & buffer[bufferIndex++]) | (0x000000FF & buffer[bufferIndex++]) << 8);
            hbTime14 = ((0x000000FF & buffer[bufferIndex++]) | (0x000000FF & buffer[bufferIndex++]) << 8);
            hbTime15 = ((0x000000FF & buffer[bufferIndex++]) | (0x000000FF & buffer[bufferIndex++]) << 8);
            reserved1 = ((0x000000FF & buffer[bufferIndex++]) | (0x000000FF & buffer[bufferIndex++]) << 8);
            reserved2 = ((0x000000FF & buffer[bufferIndex++]) | (0x000000FF & buffer[bufferIndex++]) << 8);
            reserved3 = ((0x000000FF & buffer[bufferIndex++]) | (0x000000FF & buffer[bufferIndex++]) << 8);
            distance = ((0x000000FF & buffer[bufferIndex++]) | (0x000000FF & buffer[bufferIndex++]) << 8);
            speed = ((0x000000FF & buffer[bufferIndex++]) | (0x000000FF & buffer[bufferIndex++]) << 8);
            strides = buffer[bufferIndex++];
            reserved4 = buffer[bufferIndex++];
            reserved5 = ((0x000000FF & buffer[bufferIndex++]) | (0x000000FF & buffer[bufferIndex++]) << 8);
            crc = buffer[bufferIndex++];
            etx = buffer[bufferIndex];
        } catch (Exception e) {
            /*
			 * An exception should only happen if the buffer is too short and we walk off the end of the bytes,
			 * because of the way we read the bytes from the device this should never happen, but just in case
			 * we'll catch the exception
			 */
            Log.d(TAG, "Failure building HrmReading from byte buffer, probably an incopmplete or corrupted buffer");
        }


        Log.d(TAG, "Building HrmReading from byte buffer complete, consumed " + bufferIndex + " bytes in the process");

		/*
		 * One simple check to see if we parsed the bytes properly is to check if the ETX 
		 * character was found where we expected it,  a more robust implementation would be
		 * to calculate the CRC from the message contents and compare it to the CRC from 
		 * the packet.  
		 */
        if (etx != ETX)
            Log.e(TAG, "...ETX mismatch!  The HxM message was not parsed properly");

		/*
		 * log the contents of the HrmReading, use logcat to watch the data as it arrives  
		 */
        dump();
    }

    /*
     * dump() sends the contents of the HrmReading object to the log, use 'logcat' to view
     */
    public void dump() {
        Log.d(TAG, "HrmReading Dump");
        Log.d(TAG, "...serial " + (serial));
        Log.d(TAG, "...stx " + (stx));
        Log.d(TAG, "...msgId " + (msgId));
        Log.d(TAG, "...dlc " + (dlc));
        Log.d(TAG, "...firmwareId " + (firmwareId));
        Log.d(TAG, "...sfirmwareVersiontx " + (firmwareVersion));
        Log.d(TAG, "...hardWareId " + (hardWareId));
        Log.d(TAG, "...hardwareVersion " + (hardwareVersion));
        Log.d(TAG, "...batteryIndicator " + (batteryIndicator));
        Log.d(TAG, "...heartRate " + (heartRate));
        Log.d(TAG, "...heartBeatNumber " + (heartBeatNumber));
        Log.d(TAG, "...shbTime1tx " + (hbTime1));
        Log.d(TAG, "...hbTime2 " + (hbTime2));
        Log.d(TAG, "...hbTime3 " + (hbTime3));
        Log.d(TAG, "...hbTime4 " + (hbTime4));
        Log.d(TAG, "...hbTime4 " + (hbTime5));
        Log.d(TAG, "...hbTime6 " + (hbTime6));
        Log.d(TAG, "...hbTime7 " + (hbTime7));
        Log.d(TAG, "...hbTime8 " + (hbTime8));
        Log.d(TAG, "...hbTime9 " + (hbTime9));
        Log.d(TAG, "...hbTime10 " + (hbTime10));
        Log.d(TAG, "...hbTime11 " + (hbTime11));
        Log.d(TAG, "...hbTime12 " + (hbTime12));
        Log.d(TAG, "...hbTime13 " + (hbTime13));
        Log.d(TAG, "...hbTime14 " + (hbTime14));
        Log.d(TAG, "...hbTime15 " + (hbTime15));
        Log.d(TAG, "...reserved1 " + (reserved1));
        Log.d(TAG, "...reserved2 " + (reserved2));
        Log.d(TAG, "...reserved3 " + (reserved3));
        Log.d(TAG, "...distance " + (distance));
        Log.d(TAG, "...speed " + (speed));
        Log.d(TAG, "...strides " + (strides));
        Log.d(TAG, "...reserved4 " + (reserved4));
        Log.d(TAG, "...reserved5 " + (reserved5));
        Log.d(TAG, "...crc " + (crc));
        Log.d(TAG, "...etx " + (etx));
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        Object[] data = new Object[]{stx, msgId, dlc, firmwareId, firmwareVersion, hardWareId,
                hardwareVersion, batteryIndicator, heartRate, heartBeatNumber, hbTime1, hbTime2,
                hbTime3, hbTime4, hbTime5, hbTime6, hbTime7, hbTime8, hbTime9, hbTime10, hbTime11,
                hbTime12, hbTime13, hbTime14, hbTime15, reserved1, reserved2, reserved3, distance,
                speed, strides, reserved4, reserved5, crc, etx, reserved5, crc, etx};
        for (int i = 0; i < data.length; i++) {
            builder.append(data[i]);
            if (i != data.length - 1)
                builder.append(",");
        }
        return builder.toString();
    }

}