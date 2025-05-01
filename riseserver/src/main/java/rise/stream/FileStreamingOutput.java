package rise.stream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.StreamingOutput;
import rise.lib.utils.log.RiseLog;


/**
 * @author c.nattero
 *
 */
public class FileStreamingOutput implements StreamingOutput {

	final File m_oFile;

	public FileStreamingOutput(File oFile){
		RiseLog.debugLog("FileStreamingOutput.FileStreamingOutput");
		if(null==oFile) {
			throw new NullPointerException("FileStreamingOutput.FileStreamingOutput: passed a null File");
		}
		m_oFile = oFile;
	}

	private static final int BUFFER_SIZE = 8192;
	
	public static long copyByteStream(InputStream oInputStream, OutputStream oOutputStream) throws IOException {
		
		if (oInputStream == null) {
			return 0l;
		}
		
		if (oOutputStream == null) {
			return 0l;
		}
		
		byte[] ayBuffer = new byte[BUFFER_SIZE];
		long lTotal = 0;
		while (true) {
			int iRead = oInputStream.read(ayBuffer);
		    if (iRead == -1) {
		    	break;
		    }
		    oOutputStream.write(ayBuffer, 0, iRead);
		    lTotal += iRead;
		}
		
		return lTotal;
	}
	  
	/* (non-Javadoc)
	 * @see javax.ws.rs.core.StreamingOutput#write(java.io.OutputStream)
	 */
	@Override
	public void write(OutputStream oOutputStream) throws IOException, WebApplicationException {
		try {
			RiseLog.debugLog("FileStreamingOutput.write");
			if(null == oOutputStream) {
				throw new NullPointerException("FileStreamingOutput.write: passed a null OutputStream");
			}
			try {
				try (InputStream oInputStream = new FileInputStream(m_oFile)) {
					long lCopiedBytes = 0;
					RiseLog.debugLog("FileStreamingOutput.write: using guava ByteStreams.copy to copy file");
					lCopiedBytes = copyByteStream(oInputStream,  oOutputStream);
					
					if( oOutputStream!=null ) {
						oOutputStream.flush();
					}
					
					RiseLog.debugLog("FileStreamingOutput.write: "+ m_oFile.getName()+": copied "+lCopiedBytes+" B out of " + m_oFile.length() );					
				}
				
			} 
			catch (Exception oE) {
				RiseLog.debugLog("FileStreamingOutput.write: " + oE);
			} finally {
				
				// Flush output
				if( oOutputStream!=null ) {
					try {
						oOutputStream.close();
						RiseLog.debugLog("FileStreamingOutput.write: OutputStream closed");						
					}
					catch (Exception oEx) {
						RiseLog.debugLog("FileStreamingOutput.write: OutputStream close exception: " + oEx.toString());
					}
				}
				
			}
		} catch (Exception oE) {
			RiseLog.debugLog("FileStreamingOutput.write: uncaught error: " + oE);
		}
	}
}
