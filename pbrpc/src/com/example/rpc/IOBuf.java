package com.example.rpc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class IOBuf {
	final private static int DEFAULT_SIZE = 2048;
	
	private byte[] buffer;
	private int length;
	private int offset;
	private int capacity;

	public IOBuf(int max_size) {
		buffer = new byte[max_size];
		length = 0;
		offset = 0;
		capacity = max_size;
	}
	
	public IOBuf() {
		this(DEFAULT_SIZE);
	}
	
	public IOBuf(byte[] buffer, int offset, int length) {
		this.buffer = buffer;
		this.offset = offset;
		this.length = length;
		this.capacity = buffer.length;
	}
	
	public int length() {
		return length;
	}
	
	public int offset() {
		return offset;
	}
	
	/*just return the origin buffer, without length and offset informations*/
	public byte[] array() {
		return buffer;
	}
	
	/* fetch n bytes, and then these bytes is never used again*/
	public ByteBuffer fetch(int n) {
		byte[] buf = new byte[n];
        System.arraycopy(buffer, offset, buf, 0, n);
        length -= n;
        offset += n;
		return ByteBuffer.wrap(buf, 0, n);
	}
	
	/* return a new iobuf with the same array and different length/offset */
	public IOBuf cutn(int n) {
		IOBuf n_iobuf = new IOBuf(buffer, offset, n);
		offset += n;
		length -= n;
		return n_iobuf;
	}

	
	
	
	
	
	
    /**
     * Write something into IOBuf, it's not guaranteed that the bytes written
     * can be seen in the base {@link ThreadIOBuf} until {@link Output#flush} or
     * {@link Output#close} is called
     */
    public static final class Output extends OutputStream {
    	private IOBuf base;

    	public Output(IOBuf iobuf) {
    		this.base = iobuf;
    	}
    	
		@Override
		public void write(int b) throws IOException {
			base.buffer[base.length++] = (byte) (b & 0xFF);
		}
    	
		@Override
        public final void write(byte[] b) {
			write(b, 0, b.length);
		}
		
		@Override
        public final void write(byte[] b, int off, int len) {
			if (b == null) {
                throw new NullPointerException();
            }
            if (off < 0 || len < 0 || off + len > b.length || off + len < 0) {
                throw new IndexOutOfBoundsException();
            }
            if (base.offset + base.length + len > base.capacity) {
            	throw new IndexOutOfBoundsException();
            }
            System.arraycopy(b, off, base.buffer, base.offset + base.length, len);
            base.length += len;
		}
		
		@Override
        public void flush() {
			// nothing to do, directly write into base-buffer
        }
		
		@Override
        public void close() {
            flush();
        }
    }


    /**
     * read from the iobuf
     */    
    public static final class Input extends InputStream {
        private IOBuf base;
        private int curPos;  // Start from 0
        private int byteCount;
        
        public Input(IOBuf base) {
            this.base = base;
            this.curPos = 0;
            this.byteCount = 0;

        }

		@Override
		public int read() throws IOException {
			// TODO Auto-generated method stub
			return 0;
		}
		
		@Override
        public int read(byte[] b) {
            return read(b, 0, b.length);
        }
        @Override
        public int read(byte[] b, int off, int len) {
        	return len;
        }
        
        @Override
        public int available() {
            return base != null ? base.length - byteCount : 0;
        }
        
        @Override
        public void close() {
            base = null;
        }
        
        @Override
        public long skip(long n) {
            if (base == null) {
                return 0;
            }
            return n;
        }
    }






}
