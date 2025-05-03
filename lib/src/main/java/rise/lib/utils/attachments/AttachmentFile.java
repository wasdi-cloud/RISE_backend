package rise.lib.utils.attachments;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.imageio.ImageIO;

import org.apache.commons.io.FilenameUtils;

import rise.lib.utils.log.RiseLog;

public class AttachmentFile extends File {

	/**
	 * 
	 */
	private static final long serialVersionUID = -643189587567960950L;

	public AttachmentFile(String sPath) {
		super(sPath);

	}
	
	public boolean saveAttachment (InputStream oInputStream){
		return save(oInputStream);

	}
	
	public boolean save(InputStream oInputStream){
		
		int iRead = 0;
		byte[] ayBytes = new byte[1024];

		// Try with resources. Resource declaration auto closes object(even if IOExeption occours)
		try (OutputStream oOutStream = new FileOutputStream(this)){
			
			while ((iRead = oInputStream.read(ayBytes)) != -1) {
				oOutStream.write(ayBytes, 0, iRead);
			}
			
			oOutStream.flush();
			oOutStream.close();
		} 
		catch (FileNotFoundException e) {
			RiseLog.errorLog("AttachmentFile.save: error", e);
			return false;
		} 
		catch (IOException e) {
			RiseLog.errorLog("AttachmentFile.save: error", e);
			return false;
		}
		return true;
	}
	
	public String getNameWithouteExtension() {
		String sName = this.getName();
		return FilenameUtils.removeExtension(sName);				
	}
	
	public String getExtension(){
		String sName = this.getName();
		return FilenameUtils.getExtension(sName);	

	}

	public boolean resizeImage(int iHeight, int iWidth ){
		
		try{
			String sExt = FilenameUtils.getExtension(this.getAbsolutePath());
	        BufferedImage oImage = ImageIO.read(this);
	        
	        if (oImage.getWidth() > iWidth || oImage.getHeight() > iHeight) {
		        BufferedImage oResized = resize(oImage , iHeight, iWidth);
		        ImageIO.write(oResized, sExt.toLowerCase(), this);	        	
	        }
		} catch (FileNotFoundException e) {
			RiseLog.errorLog("AttachmentFile.resizeImage: error", e);
			return false;
		} catch (IOException e) {
			RiseLog.errorLog("AttachmentFile.resizeImage: error", e);
			return false;
		}
		return true;
	}
	
	public ByteArrayInputStream getByteArrayInputStream(){
		
		ByteArrayOutputStream oByteArrayOutputStream = new ByteArrayOutputStream();
		
		try {
			FileInputStream oFileInputStream = new FileInputStream(this);
			
			
            byte[] ayBuffer = new byte[1024];
            int iBytesRead;
            while ((iBytesRead = oFileInputStream.read(ayBuffer)) != -1) {
            	oByteArrayOutputStream.write(ayBuffer, 0, iBytesRead);
            }
            
            oFileInputStream.close();
            
		} 
		catch (IOException e) {
			RiseLog.errorLog("AttachmentFile.getByteArrayImage: error", e);
			return null;
		}
		
		return new ByteArrayInputStream(oByteArrayOutputStream.toByteArray());
	} 
	
    private static BufferedImage resize(BufferedImage img, int height, int width) {
        Image tmp = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        //BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Graphics2D g2d = resized.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();
        return resized;
    }


}
