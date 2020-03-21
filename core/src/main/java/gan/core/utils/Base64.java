/**
 * Copyright (c) 2012 The Wiseserc. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package gan.core.utils;

/**
 * Base64 工具类
 * 
 * @author Michael
 */

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.io.IOException;

public class Base64 {

    public static String encode(byte[] data) {
        final BASE64Encoder encoder = new BASE64Encoder();
        return encoder.encode(data);
    }

    public static byte[] decode(String s) throws IOException {
        final BASE64Decoder decoder = new BASE64Decoder();
        return decoder.decodeBuffer(s);
    }
}
