package rise.api;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FilenameUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import rise.Rise;
import rise.lib.business.User;
import rise.lib.utils.PermissionsUtils;
import rise.lib.utils.RiseFileUtils;
import rise.lib.utils.Utils;
import rise.lib.utils.images.ImageFile;
import rise.lib.utils.images.ImageResourceUtils;
import rise.lib.utils.log.RiseLog;

/**
 * Images Resource.
 * Hosts the API to upload and show Images in RISE. 
 * Images can be associated to different entities (ie User, Processor, Organization..).
 * Each image must belong to a Collection. Collections of Images are declared in ImagesCollections
 * 
 * Given a collection, the image can also be set in a subfolder.
 * 
 * The API are:
 * 	.upload image
 * 	.get image
 * 	.delete Image
 * 	.There are 2 convenience methods for Processor Logo and Images (for retro-compatiability)
 * 
 * @author p.campanella
 *
 */
@Path("/images")
public class ImagesResource {
	
	/**
	 * Upload a generic image in WASDI
	 * @param oInputFileStream File Input Stream
	 * @param oFileMetaData Metadata of the file
	 * @param sSessionId Session Id 
	 * @param sCollection Collection where the image must be added. It is a subfolder of the base images path
	 * @param sImageName Name of the image to add
	 * @param obResize Optional flag to resize the image
	 * @param obThumbnail Optional flag to create a thumbnail of the image
	 * @return Http standard response
	 */
	@POST
	@Path("/upload")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadImage(@FormDataParam("image") InputStream oInputFileStream, @FormDataParam("image") FormDataContentDisposition oFileMetaData,
										@HeaderParam("x-session-token") String sSessionId, @QueryParam("collection") String sCollection, @QueryParam("folder") String sFolder, @QueryParam("name") String sImageName,
										@QueryParam("resize") Boolean obResize, @QueryParam("thumbnail") Boolean obThumbnail) {
		
		try {
			RiseLog.debugLog("ImagesResource.uploadImage( collection: " + sCollection + " sImageName: " + sImageName +")");
			
			User oUser = Rise.getUserFromSession(sSessionId);

			if (oUser==null) {
				RiseLog.warnLog("ImagesResource.uploadImage: invalid user or session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			if (!ImageResourceUtils.isValidCollection(sCollection)) {
				RiseLog.warnLog("ImagesResource.uploadImage: invalid collection");
				return Response.status(Status.BAD_REQUEST).build();			
			}
			
			if (Utils.isNullOrEmpty(sImageName)) {
				RiseLog.warnLog("ImagesResource.uploadImage: invalid image name");
				return Response.status(Status.BAD_REQUEST).build();			
			}			
			
			if (!PermissionsUtils.canUserWriteImage(oUser.getUserId(), sCollection, sFolder, sImageName)) {
				RiseLog.warnLog("ImagesResource.uploadImage: user cannot access image");
				return Response.status(Status.FORBIDDEN).build();				
			}
						
			if (Utils.isNullOrEmpty(sFolder)) {
				sFolder = "";			
			}
			
			//sanity check: is sImageName safe? It must be a file name, not a path
			if(sImageName.contains("/") || sImageName.contains("\\") || sCollection.contains("/") || sCollection.contains("\\") || sFolder.contains("/") || sFolder.contains("\\") ) {
				RiseLog.warnLog("ImagesResource.uploadImage: Image or Collection name looks like a path" );
				return Response.status(Status.BAD_REQUEST).build();
			}
			
			String sExt;
			String sFileName;
			
			//get filename and extension 
			if(oFileMetaData != null && Utils.isNullOrEmpty(oFileMetaData.getFileName()) == false){
				
				sFileName = oFileMetaData.getFileName();
				sExt = FilenameUtils.getExtension(sFileName);
				
				RiseLog.debugLog("ImagesResource.uploadImage: FileName " + sFileName + " Extension: " + sExt);
			} 
			else {
				RiseLog.warnLog("ImagesResource.uploadImage: File metadata not available");
				return Response.status(Status.BAD_REQUEST).build();
			}
			
			// Check if this is an accepted file extension
			if(ImageResourceUtils.isValidExtension(sExt) == false ){
				RiseLog.warnLog("ImagesResource.uploadImage: extension invalid");
				return Response.status(Status.BAD_REQUEST).build();
			}

			// Take path
			String sPath = ImageResourceUtils.getImagesSubPath(sCollection, sFolder);
			sPath += sImageName;
			
			RiseLog.debugLog("ImagesResource.uploadImage: sPath: " + sPath);
			
			String sExtensionOfSavedImage = ImageResourceUtils.getExtensionOfImageInFolder(RiseFileUtils.getFileNameWithoutLastExtension(sPath));
			
			//if there is a saved logo with a different extension remove it 
			if(!Utils.isNullOrEmpty(sExtensionOfSavedImage)) {
				
				RiseLog.debugLog("ImagesResource.uploadImage: cleaning old image");
				
			    File oOldImage = new File(sPath.replace(RiseFileUtils.getFileNameExtension(sPath), sExtensionOfSavedImage));
			    
			    if (oOldImage.exists()) {
			    	RiseLog.debugLog("ImagesResource.uploadImage: delete old image with same name and different extension");
			    }
			    
			    if (!oOldImage.delete()) {
			    	RiseLog.debugLog("ImagesResource.uploadImage: can't delete old image");
			   	}
			}
			else {
				RiseLog.debugLog("ImagesResource.uploadImage: no old logo present");
			}
		    	   
		    File oTouchFile = new File(sPath);
		    
		    try {
				if (!oTouchFile.createNewFile()) {
					RiseLog.debugLog("ImagesResource.uploadImage: can't create new file");
				}
				else {
					RiseLog.debugLog("ImagesResource.uploadImage: created new empty file in path");
				}
			} catch (IOException e) {
				RiseLog.errorLog("ImagesResource.uploadImage: " + e.toString());
			}	    
		    
		    ImageFile oOutputImage = new ImageFile(sPath);
		    RiseLog.debugLog("ImagesResource.uploadImage: created Image, saving");
		    boolean bIsSaved =  oOutputImage.saveImage(oInputFileStream);
		    
		    if(bIsSaved == false){
		    	RiseLog.warnLog("ImagesResource.uploadImage:  not saved!");
		    	return Response.status(Status.BAD_REQUEST).build();
		    }
		    else {
		    	RiseLog.debugLog("ImagesResource.uploadImage: Image saved");
		    }
		    
			double dBytes = (double) oOutputImage.length();
			double dKilobytes = (dBytes / 1024);
			double dMegabytes = (dKilobytes / 1024);
			
			if( dMegabytes > (double) ImageResourceUtils.s_iMAX_IMAGE_MB_SIZE){
				RiseLog.warnLog("ImagesResource.uploadImage: image too big, delete it");
				oOutputImage.delete();
		    	return Response.status(Status.BAD_REQUEST).build();
			}
			else {
				RiseLog.debugLog("ImagesResource.uploadImage: Dimension is ok proceed");
			}
		    
//		    if (obResize!=null) {
//		    	if (obResize) {
//		    		
//		    		RiseLog.debugLog("ImagesResource.uploadImage: start resizing");
//		    		
//		    		try {
//			    	    boolean bIsResized = oOutputImage.resizeImage(ImageResourceUtils.s_iLOGO_SIZE, ImageResourceUtils.s_iLOGO_SIZE);
//			    	    
//			    	    if(bIsResized == false){
//			    	    	RiseLog.debugLog("ImagesResource.uploadImage: error in resize");
//			    	    }	    				    			
//		    		}
//		    		catch (Throwable oEx) {
//		    			RiseLog.warnLog("ImagesResource.uploadImage: exception in resize " + oEx.toString());
//		    		}
//		    	}
//		    }
//		    
//		    RiseLog.debugLog("ImagesResource.uploadImage: check if we need to create a thumb");
//		    
//		    if (obThumbnail!=null) {
//		    	if (obThumbnail) {
//		    		RiseLog.debugLog("ImagesResource.uploadImage: create thumb");
//		    		try {
//		    			ImageResourceUtils.createThumbOfImage(sPath);
//		    		}
//		    		catch (Throwable oEx) {
//		    			RiseLog.warnLog("ImagesResource.uploadImage: exception in thumb " + oEx.toString());
//		    		}
//		    	}
//		    }
		    
		    RiseLog.debugLog("ImagesResource.uploadImage: ok, all done!");
		    
			return Response.status(Status.OK).build();
		}
		catch (Throwable oEx) {
			RiseLog.errorLog("ImagesResource.uploadImage: exception " + oEx.toString());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}	
	
	
	/**
	 * Gets an image as a Byte Stream
	 * @param sSessionId User Session Id
	 * @param sCollection Processor Id
	 * @return Logo byte stream
	 */
	@GET
	@Path("/get")
	public Response getImage(@HeaderParam("x-session-token") String sSessionId, @QueryParam("token") String sTokenSessionId, @QueryParam("collection") String sCollection, @QueryParam("folder") String sFolder, @QueryParam("name") String sImageName) {
		
		try {
			
			// Check session
			if( Utils.isNullOrEmpty(sSessionId) == false) {
				sTokenSessionId = sSessionId;
			}

			RiseLog.debugLog("ImagesResource.getImage( collection: " + sCollection + " sImageName: " + sImageName +")");
			
			User oUser = Rise.getUserFromSession(sTokenSessionId);

			if (oUser==null) {
				RiseLog.warnLog("ImagesResource.getImage: no valid user or session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			if (!ImageResourceUtils.isValidCollection(sCollection)) {
				RiseLog.warnLog("ImagesResource.getImage: invalid collection");
				return Response.status(Status.BAD_REQUEST).build();			
			}
			
			if(Utils.isNullOrEmpty(sImageName)) {
				RiseLog.warnLog("ImagesResource.getImage: Image name is null" );
				return Response.status(Status.BAD_REQUEST).build();
			}			
			
			if (!PermissionsUtils.canUserAccessImage(oUser.getUserId(), sCollection, sFolder, sImageName)) {
				RiseLog.warnLog("ImagesResource.getImage: invalid user or session");
				return Response.status(Status.FORBIDDEN).build();				
			}			
			
			if (sFolder==null) sFolder="";
			
			//sanity check: are the inputs safe? It must be a file name, not a path
			if(sImageName.contains("/") || sImageName.contains("\\") || sCollection.contains("/") || sCollection.contains("\\")|| sFolder.contains("/") || sFolder.contains("\\")) {
				RiseLog.warnLog("ImagesResource.getImage: Image or Collection name looks like a path" );
				return Response.status(Status.BAD_REQUEST).build();
			}			
					
			String sPathLogoFolder = ImageResourceUtils.getImagesSubPath(sCollection, sFolder);
			String sAbsolutePath = sPathLogoFolder + sImageName;
			
			RiseLog.debugLog("ImagesResource.getImage: sAbsolutePath " + sAbsolutePath);
			
			ImageFile oLogo = new ImageFile(sAbsolutePath);
			
			//Check the logo and extension
			if(oLogo.exists() == false){
				RiseLog.warnLog("ImagesResource.getImage: unable to find image in " + sAbsolutePath);
				return Response.status(Status.NO_CONTENT).build();
			}
			
			//prepare buffer and send the logo to the client 
			ByteArrayInputStream abImageLogo = oLogo.getByteArrayImage();
			
		    return Response.ok(abImageLogo).build();			
		}
		catch (Exception oEx) {
			RiseLog.errorLog("ImagesResource.getImage: exception " + oEx.toString());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}	

	/**
	 * Deletes one of the gallery images of a processor
	 * @param sSessionId User session Id
	 * @param sProcessorId Processor Id
	 * @param sImageName Image Name
	 * @return std http response
	 */
	@DELETE
	@Path("/delete")
	public Response deleteImage(@HeaderParam("x-session-token") String sSessionId, @QueryParam("collection") String sCollection, @QueryParam("folder") String sFolder, @QueryParam("name") String sImageName) {
		
		RiseLog.debugLog("ImagesResource.deleteImage( Collection: " + sCollection + ", Image Name: " + sImageName + " )");
		
		User oUser = Rise.getUserFromSession(sSessionId);

		if (oUser==null) {
			RiseLog.warnLog("ImagesResource.deleteImage: user or session invalid");
			return Response.status(Status.UNAUTHORIZED).build();
		}
						
		if(Utils.isNullOrEmpty(sImageName)) {
			RiseLog.warnLog("ImagesResource.deleteImage: Image name is null" );
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		if (!ImageResourceUtils.isValidCollection(sCollection)) {
			RiseLog.warnLog("ImagesResource.deleteImage: invalid collection");
			return Response.status(Status.BAD_REQUEST).build();			
		}
		
		if (!PermissionsUtils.canUserWriteImage(oUser.getUserId(), sCollection, sFolder, sImageName)) {
			RiseLog.warnLog("ImagesResource.deleteImage: invalid user or session");
			return Response.status(Status.FORBIDDEN).build();				
		}		
		
		if (sFolder==null) sFolder="";		

		//sanity check: is sImageName safe? It must be a file name, not a path
		if(sImageName.contains("/") || sImageName.contains("\\") || sCollection.contains("/") || sCollection.contains("\\")|| sFolder.contains("/") || sFolder.contains("\\")) {
			RiseLog.warnLog("ImagesResource.deleteImage: Image or Collection name looks like a path" );
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		String sPath = ImageResourceUtils.getImagesSubPath(sCollection, sFolder);
		String sFilePath = sPath+sImageName;
		
		File oFile = new File(sFilePath);
		
		if (!oFile.exists()) {
			RiseLog.warnLog("ImagesResource.deleteImage: file " + sImageName +" not found");
			return Response.status(Status.NOT_FOUND).build();			
		}
				
		//delete file
		try {
			if (!oFile.delete()) {
				RiseLog.warnLog("ImagesResource.deleteImage: error deleting file ");
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();				
			}
		} catch (Exception oE) {
			RiseLog.errorLog("ImagesResource.deleteImage: exception " + oE );
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		
		return Response.status(Status.OK).build();
	}	

	/**
	 * Gets an image as a Byte Stream
	 * @param sSessionId User Session Id
	 * @param sCollection Processor Id
	 * @return Logo byte stream
	 */
	@GET
	@Path("/exists")
	public Response existsImage(@HeaderParam("x-session-token") String sSessionId, @QueryParam("token") String sTokenSessionId, @QueryParam("collection") String sCollection, @QueryParam("folder") String sFolder, @QueryParam("name") String sImageName) {
		
		try {
			
			// Check session
			if( Utils.isNullOrEmpty(sSessionId) == false) {
				sTokenSessionId = sSessionId;
			}

			RiseLog.debugLog("ImagesResource.existsImage( collection: " + sCollection + " sImageName: " + sImageName +")");
			
			User oUser = Rise.getUserFromSession(sTokenSessionId);

			if (oUser==null) {
				RiseLog.warnLog("ImagesResource.existsImage: no valid user or session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			if (!ImageResourceUtils.isValidCollection(sCollection)) {
				RiseLog.warnLog("ImagesResource.existsImage: invalid collection");
				return Response.status(Status.BAD_REQUEST).build();			
			}
			
			if(Utils.isNullOrEmpty(sImageName)) {
				RiseLog.warnLog("ImagesResource.existsImage: Image name is null" );
				return Response.status(Status.BAD_REQUEST).build();
			}			
			
			if (!PermissionsUtils.canUserAccessImage(oUser.getUserId(), sCollection, sFolder, sImageName)) {
				RiseLog.warnLog("ImagesResource.existsImage: invalid user or session");
				return Response.status(Status.FORBIDDEN).build();				
			}			
			
			if (sFolder==null) sFolder="";
			
			//sanity check: are the inputs safe? It must be a file name, not a path
			if(sImageName.contains("/") || sImageName.contains("\\") || sCollection.contains("/") || sCollection.contains("\\")|| sFolder.contains("/") || sFolder.contains("\\")) {
				RiseLog.warnLog("ImagesResource.existsImage: Image or Collection name looks like a path" );
				return Response.status(Status.BAD_REQUEST).build();
			}			
					
			String sPathLogoFolder = ImageResourceUtils.getImagesSubPath(sCollection, sFolder);
			String sAbsolutePath = sPathLogoFolder + sImageName;
			
			RiseLog.debugLog("ImagesResource.existsImage: sAbsolutePath " + sAbsolutePath);
			
			File oImage = new File(sAbsolutePath);
			
			//Check the logo and extension
			if(oImage.exists() == false){
				RiseLog.debugLog("ImagesResource.existsImage: unable to find image in " + sAbsolutePath);
				return Response.status(Status.NOT_FOUND).build();
			}
			
		    return Response.ok().build();
		}
		catch (Exception oEx) {
			RiseLog.errorLog("ImagesResource.existsImage: exception " + oEx.toString());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}		
}
