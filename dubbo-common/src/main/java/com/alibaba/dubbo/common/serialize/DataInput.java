package com.alibaba.dubbo.common.serialize;

import java.io.IOException;

public interface DataInput {

    /**
     * Read boolean.
     *
     * @return boolean.
     * @throws IOException IO Exception
     */
    boolean readBool() throws IOException;

    /**
     * Read byte.
     *
     * @return byte value.
     * @throws IOException IO Exception
     */
    byte readByte() throws IOException;

    /**
     * Read short integer.
     *
     * @return short.
     * @throws IOException IO Exception
     */
    short readShort() throws IOException;

    /**
     * Read integer.
     *
     * @return integer.
     * @throws IOException IO Exception
     */
    int readInt() throws IOException;

    /**
     * Read long.
     *
     * @return long.
     * @throws IOException IO Exception
     */
    long readLong() throws IOException;

    /**
     * Read float.
     *
     * @return float.
     * @throws IOException IO Exception
     */
    float readFloat() throws IOException;

    /**
     * Read double.
     *
     * @return double.
     * @throws IOException IO Exception
     */
    double readDouble() throws IOException;

    /**
     * Read UTF-8 string.
     *
     * @return string.
     * @throws IOException IO Exception
     */
    String readUTF() throws IOException;

    /**
     * Read byte array.
     *
     * @return byte array.
     * @throws IOException IO Exception
     */
    byte[] readBytes() throws IOException;
}