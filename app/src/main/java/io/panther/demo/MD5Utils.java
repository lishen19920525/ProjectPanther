/*
 * Copyright 2015 Glow Geniuses Studio
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.panther.demo;

import java.io.FileInputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;

/**
 * Created by Leonardo on 2015-12-15
 * MD5 utils
 */
public class MD5Utils {
    private static final char HEX_DIGITS[] = {'0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    private MD5Utils() {
    }

    public static String toHexString(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte aB : b) {
            sb.append(HEX_DIGITS[(aB & 0xf0) >>> 4]);
            sb.append(HEX_DIGITS[aB & 0x0f]);
        }
        return sb.toString();
    }


    public static String to32Lower(String str) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(str.getBytes());
            byte messageDigest[] = digest.digest();
            return toHexString(messageDigest);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }


    public static String to32Upper(String str) {
        try {
            return to32Lower(str).toUpperCase();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }


    public static String to16Lower(String str) {
        try {
            return to32Lower(str).substring(8, 24);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String to16Upper(String str) {
        try {
            return to32Lower(str).substring(8, 24).toUpperCase();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    public static String fileMD5(String inputFile) {
        int bufferSize = 256 * 1024;
        FileInputStream fileInputStream = null;
        DigestInputStream digestInputStream = null;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            fileInputStream = new FileInputStream(inputFile);
            digestInputStream = new DigestInputStream(fileInputStream, messageDigest);
            byte[] buffer = new byte[bufferSize];
            while (digestInputStream.read(buffer) > 0) ;
            messageDigest = digestInputStream.getMessageDigest();
            byte[] resultByteArray = messageDigest.digest();
            return toHexString(resultByteArray);
        } catch (Exception e) {
            return null;
        }
    }
}
