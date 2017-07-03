package com.alibaba.dubbo.common.serialize;

import java.io.IOException;

public interface DataOutput {

	/**
	 * Write boolean.
	 * 
	 * @param v value.
	 * @throws IOException IO Exception
	 */
	void writeBool(boolean v) throws IOException;

	/**
	 * Write byte.
	 * 
	 * @param v value.
	 * @throws IOException IO Exception
	 */
	void writeByte(byte v) throws IOException;

	/**
	 * Write short.
	 * 
	 * @param v value.
	 * @throws IOException IO Exception
	 */
	void writeShort(short v) throws IOException;

	/**
	 * Write integer.
	 * 
	 * @param v value.
	 * @throws IOException IO Exception
	 */
	void writeInt(int v) throws IOException;

	/**
	 * Write long.
	 * 
	 * @param v value.
	 * @throws IOException IO Exception
	 */
	void writeLong(long v) throws IOException;

	/**
	 * Write float.
	 * 
	 * @param v value.
	 * @throws IOException IO Exception
	 */
	void writeFloat(float v) throws IOException;

	/**
	 * Write double.
	 * 
	 * @param v value.
	 * @throws IOException IO Exception
	 */
	void writeDouble(double v) throws IOException;

	/**
	 * Write string.
	 * 
	 * @param v value.
	 * @throws IOException IO Exception
	 */
	void writeUTF(String v) throws IOException;

	/**
	 * Write byte array.
	 * 
	 * @param v value.
	 * @throws IOException IO Exception
	 */
	void writeBytes(byte[] v) throws IOException;

	/**
	 * Write byte array.
	 * 
	 * @param v value.
	 * @param off offset.
	 * @param len length.
	 * @throws IOException IO Exception
	 */
	void writeBytes(byte[] v, int off, int len) throws IOException;

	/**
	 * Flush buffer.
	 * 
	 * @throws IOException IO Exception
	 */
	void flushBuffer() throws IOException;
}