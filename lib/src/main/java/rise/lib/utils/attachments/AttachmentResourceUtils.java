package rise.lib.utils.attachments;

import java.io.File;

import org.apache.commons.io.FilenameUtils;

import com.google.common.io.Files;

import rise.lib.business.AttachmentsCollections;
import rise.lib.config.RiseConfig;
import rise.lib.utils.Utils;
import rise.lib.utils.log.RiseLog;

/**
 * Static Methods to handle images related to applications: 
 * can be logos or gallery images
 * @author p.campanella
 *
 */
public class AttachmentResourceUtils {
	
	/**
	 * List of extensions enabled to upload a user attachment
	 */
	public static String[] ENABLED_EXTENSIONS = {"jpg", "png", "svg", "doc", "pdf", "txt", "json", "xls"};
	
	/**
	 * Width of Thumbail images
	 */
	public static int s_iTHUMB_WIDTH = 44;
	/**
	 * Heigth of Thumbail images
	 */
	public static int s_iTHUMB_HEIGHT = 50;
	
	/**
	 * Max image size in Mb
	 */
	public static int s_iMAX_IMAGE_MB_SIZE = 20;
	/**
	 * Default base name of the processor logo
	 */
	public static String s_sDEFAULT_LOGO_PROCESSOR_NAME = "logo";
	/**
	 * Size of the logo in pixels
	 */
	public static Integer s_iLOGO_SIZE = 540;
	/**
	 * Pre-defined names of images in the processor gallery
	 */
	public static String[] s_asIMAGE_NAMES = { "1", "2", "3", "4", "5", "6" };	

	
	private AttachmentResourceUtils() { 
	}
	
	/**
	 * Base Path of the web application
	 */
	public static String s_sWebAppBasePath = "/var/lib/tomcat8/webapps/wasdiwebserver/";
	
	/**
	 * Check if the extension of an image is valid for WASDI
	 * @param sExt extension to check
	 * @param sValidExtensions List of accepted extensions
	 * @return true if valid
	 */
	public static boolean isValidExtension(String sExt){
		//Check if the extension is valid
		for (String sValidExtension : ENABLED_EXTENSIONS) {
			  if(sValidExtension.equals(sExt.toLowerCase()) ){
				  return true;
			  }
		}
		return false;
	}
	
	/**
	 * Safe create directory in path
	 * @param sPath new dir path
	 */
	public static void createDirectory(String sPath){
		File oDirectory = new File(sPath);
		//create directory
	    if (! oDirectory.exists()){
	    	oDirectory.mkdirs();
	    }
	} 
	
	/**
	 * Get the image file of the logo
	 * @param sPathAttachmentFolder
	 * @param asEnableExtension
	 * @return
	 */
	public static AttachmentFile getAttachmentInFolder(String sPathAttachmentFolder){
		AttachmentFile oAttachment = null;
		String sLogoExtension = getExtensionOfFileInFolder(sPathAttachmentFolder);
		
		RiseLog.debugLog("AttachmentResourceUtils.getAttachmentInFolder " + sLogoExtension);
		
		if(sLogoExtension.isEmpty() == false){
			String sPath = sPathAttachmentFolder;
			
			RiseLog.debugLog("AttachmentResourceUtils.getAttachmentInFolder: sPath "+ sPath);
			
			if (sPath.endsWith("."+sLogoExtension) == false) {
				sPath = sPathAttachmentFolder + "." + sLogoExtension ;
			}
			
			oAttachment = new AttachmentFile(sPath);
		}
		else {
			RiseLog.debugLog("AttachmentResourceUtils.getAttachmentInFolder: logo empty ");
		}
		return oAttachment;
		
	}
	
	
	/**
	 * Get the path of a subofolder of images in WASDI
	 * @return
	 */
	public static String getAttachmentSubPath(String sCollection, String sFolder) {
		String sPath = RiseConfig.Current.paths.attachmentsBasePath;
		sPath += sCollection;
		if (!sPath.endsWith("/")) sPath += "/";
		
		if (Utils.isNullOrEmpty(sFolder)==false) {
			sPath += sFolder;
			if (!sPath.endsWith("/")) sPath += "/";
		}
		
		AttachmentResourceUtils.createDirectory(sPath);
		
		return sPath;
	}
	
	
	
	/**
	 * Search the extension of a image
	 * @param sPathAttachmentFileWithoutExt
	 * @param asEnableExtension
	 * @return
	 */
	public static String getExtensionOfFileInFolder (String sPathAttachmentFileWithoutExt){
		File oFile = null;
		String sExtensionReturnValue = "";
		for (String sValidExtension : ENABLED_EXTENSIONS) {
			oFile = new File(sPathAttachmentFileWithoutExt + "." + sValidExtension );
		    if (oFile.exists()){
		    	sExtensionReturnValue = sValidExtension;
		    	break;
		    }

		}
		return sExtensionReturnValue;
	}
	
	/**
	 * Delete a file in a folder
	 * @param sPathFolder
	 * @param sDeleteFileName
	 */
	public static void deleteFileInFolder(String sPathFolder,String sDeleteFileName){
		File oFolder = new File(sPathFolder);
		File[] aoListOfFiles = oFolder.listFiles();
		for (File oImage : aoListOfFiles){ 
			String sName = oImage.getName();
			String sFileName = FilenameUtils.removeExtension(sName);	
			
			if(sDeleteFileName.equalsIgnoreCase(sFileName)){
				if (!oImage.delete()) {
					RiseLog.debugLog("Attachment resource Utils - File " + sFileName + " can't be deleted");
					}
				break;
			} 
			
		}
	}
	
	/**
	 * return a free name for the image (if it is available) 
	 * @param sPathFolder
	 * @return
	 */
	public static String getAvaibleProcessorImageFileName(String sPathFolder) {
		File oFolder = new File(sPathFolder);
		File[] aoListOfFiles = oFolder.listFiles();

		String sReturnValueName = "";
		boolean bIsAvaibleName = false; 
		for (String sAvaibleFileName : AttachmentResourceUtils.s_asIMAGE_NAMES){
			bIsAvaibleName = true;
			sReturnValueName = sAvaibleFileName;
			
			for (File oImage : aoListOfFiles){ 
				String sName = oImage.getName();
				String sFileName = FilenameUtils.removeExtension(sName);	
				
				if(sAvaibleFileName.equalsIgnoreCase(sFileName)){
					bIsAvaibleName = false;
					break;
				} 
				
			}
			
			if(bIsAvaibleName == true){
				break;
			}
			sReturnValueName = "";
		 }

		return sReturnValueName;
	}	
	
	/**
	 * Create a thumb of the image in sAbsoluteImageFilePath
	 * @param sAbsoluteImageFilePath path of the input image
	 * @return Path of the thumbail or null in case of problems
	 */
	public static String createThumbOfImage(String sAbsoluteImageFilePath) {
	    try {
	    	
	    	if (Utils.isNullOrEmpty(sAbsoluteImageFilePath)) {
	    		RiseLog.infoLog("AttachmentResourceUtils.createThumbOfImage: sAbsoluteImageFilePath null ");
	    		return null;
	    	}
	    	
	    	AttachmentFile oNewImage = new AttachmentFile(sAbsoluteImageFilePath);
	    	
	    	if (oNewImage.exists() == false) {
	    		RiseLog.infoLog("AttachmentResourceUtils.createThumbOfImage: oNewImage not found at " + sAbsoluteImageFilePath);
	    		return null;
	    	}
	    	
		    // Create the thumb:	    	
	    	RiseLog.debugLog("AttachmentResourceUtils.createThumbOfImage: creating thumb");
	    	
	    	String [] asSplit = sAbsoluteImageFilePath.split("\\.");
	    	
	    	String sThumbPath = asSplit[0] + "_thumb." + asSplit[1];	    	
	    	
	    	File oThumb = new File(sThumbPath);
	    	
	    	Files.copy(oNewImage, oThumb);
	    	
	    	RiseLog.debugLog("AttachmentResourceUtils.createThumbOfImage: thumb file created " + sThumbPath);
	    	
	    	AttachmentFile oImageThumb = new AttachmentFile(sThumbPath);
	    	
	    	if (!oImageThumb.resizeImage(AttachmentResourceUtils.s_iTHUMB_HEIGHT, AttachmentResourceUtils.s_iTHUMB_WIDTH)) {
	    		RiseLog.debugLog("AttachmentResourceUtils.createThumbOfImage: error resizing the thumb");
	    	}
	    	
	    	return sThumbPath;
	    	
	    }
	    catch (Exception oEx) {
	    	RiseLog.debugLog("AttachmentResourceUtils.createThumbOfImage:  error creating the thumb " + oEx.toString());
		}
	    
	    return null;
	}
	
	/**
	 * Get the http link to access an image
	 * @param sCollection Image Collection 
	 * @param sAttachment Image Name
	 * @return Url to get the image or empty string
	 */
	public static String getAttachmentLink(String sCollection, String sFolder, String sAttachment) {
		try {
			String sAddress = RiseConfig.Current.serverApiAddress;
			
			if (!sAddress.endsWith("/")) sAddress += "/";
			
			sAddress +="images/get?collection="+sCollection;
			if (!Utils.isNullOrEmpty(sFolder)) {
				sAddress += "&folder=" + sFolder;
			}
			
			sAddress += "&name="+sAttachment;
			
			return sAddress;
		}
	    catch (Exception oEx) {
	    	RiseLog.debugLog("AttachmentResourceUtils.getAttachmentLink:  error " + oEx.toString());
		}		
		
		return "";
	}
	
	/**
	 * Check if this is an accepted value for collection
	 * @param sCollection Collection to check
	 * @return True if it is allowed, False otherwise
	 */
	public static boolean isValidCollection(String sCollection) {
		
		if (Utils.isNullOrEmpty(sCollection)) {
			return false;
		}
		
		try {
			boolean bValidCollection = false;
			for (AttachmentsCollections oCollection : AttachmentsCollections.values()) {
				if (oCollection.getFolder().equals(sCollection)) {
					bValidCollection = true;
					break;
				}
			}
			return bValidCollection;			
		}
		catch (Exception oEx) {
			RiseLog.errorLog("AttachmentResourceUtils.isValidCollection: error " + oEx.toString());
		}
		
		return false;
	}
}
