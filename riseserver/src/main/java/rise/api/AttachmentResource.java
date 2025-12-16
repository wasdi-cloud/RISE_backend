package rise.api;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FilenameUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.json.JSONObject;

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
import rise.lib.utils.JsonUtils;
import rise.lib.utils.PermissionsUtils;
import rise.lib.utils.RiseFileUtils;
import rise.lib.utils.Utils;
import rise.lib.utils.attachments.AttachmentFile;
import rise.lib.utils.attachments.AttachmentResourceUtils;
import rise.lib.utils.log.RiseLog;
import rise.lib.viewmodels.AttachmentListViewModel;

/**
 * Attach Resource.
 * Hosts the API to upload and show attachments. 
 * Attachment can be associated to different entities (ie User, Processor, Organization..).
 * Each attachment must belong to a Collection. Collections of Attachments are declared in AttachmentCollections
 * 
 * Given a collection, the image can also be set in a subfolder.
 * 
 * The API are:
 * 	.upload attachment
 * 	.get attachment
 * 	.delete attachment
 * 	.get attachment list of folder
 * 
 * @author p.campanella
 *
 */
@Path("/attachment")
public class AttachmentResource {
	
	/**
	 * Upload a generic image in WASDI
	 * @param oInputFileStream File Input Stream
	 * @param oFileMetaData Metadata of the file
	 * @param sSessionId Session Id 
	 * @param sCollection Collection where the file must be added. It is a subfolder of the base  path
	 * @param sAttachmentName Name of the file to add
	 * @param obResize Optional flag to resize the file if it is an image
	 * @param obThumbnail Optional flag to create a thumbnail if it is an the image
	 * @param ofLat Optional Lat Coordinate to associate to the attachment
	 * @param ofLng Optional Lng Coordinate to associate to the attachment
	 * @return Http standard response
	 */
	@POST
	@Path("/upload")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response upload(@FormDataParam("file") InputStream oInputFileStream, @FormDataParam("file") FormDataContentDisposition oFileMetaData,
										@HeaderParam("x-session-token") String sSessionId, @QueryParam("collection") String sCollection, @QueryParam("folder") String sFolder, @QueryParam("name") String sAttachmentName,
										@QueryParam("notsamename") Boolean obNotSameName, @QueryParam("lat") Float ofLat, @QueryParam("lng") Float ofLng) {
		
		try {
			RiseLog.debugLog("AttachResource.upload( collection: " + sCollection + " sAttachmentName: " + sAttachmentName +")");
			
			User oUser = Rise.getUserFromSession(sSessionId);

			if (oUser==null) {
				RiseLog.warnLog("AttachResource.upload: invalid user or session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			if (!AttachmentResourceUtils.isValidCollection(sCollection)) {
				RiseLog.warnLog("AttachResource.upload: invalid collection");
				return Response.status(Status.BAD_REQUEST).build();			
			}
			
			if (Utils.isNullOrEmpty(sAttachmentName)) {
				RiseLog.warnLog("AttachResource.upload: invalid attachment name");
				return Response.status(Status.BAD_REQUEST).build();			
			}			
			
			if (!PermissionsUtils.canUserWriteAttachment(oUser.getUserId(), sCollection, sFolder, sAttachmentName)) {
				RiseLog.warnLog("AttachResource.upload: user cannot access attachment");
				return Response.status(Status.FORBIDDEN).build();				
			}
						
			if (Utils.isNullOrEmpty(sFolder)) {
				sFolder = "";			
			}
			
			if (obNotSameName==null) {
				obNotSameName = false;
			}
			
			//sanity check: is sAttachmentName safe? It must be a file name, not a path
			if(sAttachmentName.contains("/") || sAttachmentName.contains("\\") || sCollection.contains("/") || sCollection.contains("\\") || sFolder.contains("/") || sFolder.contains("\\") ) {
				RiseLog.warnLog("AttachResource.upload: Attachment or Collection name looks like a path" );
				return Response.status(Status.BAD_REQUEST).build();
			}
			
			String sExt;
			String sFileName;
			
			//get filename and extension 
			if(oFileMetaData != null && Utils.isNullOrEmpty(oFileMetaData.getFileName()) == false){
				
				sFileName = oFileMetaData.getFileName();
				sExt = FilenameUtils.getExtension(sFileName);
				
				RiseLog.debugLog("AttachResource.upload: FileName " + sFileName + " Extension: " + sExt);
			} 
			else {
				RiseLog.warnLog("AttachResource.upload: File metadata not available");
				return Response.status(Status.BAD_REQUEST).build();
			}
			
			// Check if this is an accepted file extension
			if(AttachmentResourceUtils.isValidExtension(sExt) == false ){
				RiseLog.warnLog("AttachResource.upload: extension invalid");
				return Response.status(Status.BAD_REQUEST).build();
			}

			// Take path
			String sPath = AttachmentResourceUtils.getAttachmentSubPath(sCollection, sFolder);
			sPath += sAttachmentName;
			
			RiseLog.debugLog("AttachResource.upload: sPath: " + sPath);
			
			if (obNotSameName) {
				// Search a file with the same name but different extension in our folder
				String sExtensionOfSavedImage = AttachmentResourceUtils.getExtensionOfFileInFolder(RiseFileUtils.getFileNameWithoutLastExtension(sPath));
				
				//if there is a saved attachment with a different extension remove it 
				if(!Utils.isNullOrEmpty(sExtensionOfSavedImage)) {
					
					RiseLog.debugLog("AttachResource.upload: cleaning old image");
					
				    File oOldImage = new File(sPath.replace(RiseFileUtils.getFileNameExtension(sPath), sExtensionOfSavedImage));
				    
				    if (oOldImage.exists()) {
				    	RiseLog.debugLog("AttachResource.upload: delete old image with same name and different extension");
				    }
				    
				    if (!oOldImage.delete()) {
				    	RiseLog.debugLog("AttachResource.upload: can't delete old image");
				   	}
				}
				else {
					RiseLog.debugLog("AttachResource.upload: no old logo present");
				}				
			}
			
		    	   
		    File oTouchFile = new File(sPath);
		    
		    try {
				if (!oTouchFile.createNewFile()) {
					RiseLog.debugLog("AttachResource.upload: can't create new file");
				}
				else {
					RiseLog.debugLog("AttachResource.upload: created new empty file in path");
				}
			} catch (IOException e) {
				RiseLog.errorLog("AttachResource.upload: " + e.toString());
			}	    
		    
		    AttachmentFile oOutputImage = new AttachmentFile(sPath);
		    RiseLog.debugLog("AttachResource.upload: created File, saving");
		    boolean bIsSaved =  oOutputImage.saveAttachment(oInputFileStream);
		    
		    if(bIsSaved == false){
		    	RiseLog.warnLog("AttachResource.upload:  not saved!");
		    	return Response.status(Status.BAD_REQUEST).build();
		    }
		    else {
		    	RiseLog.debugLog("AttachResource.upload: Image saved");
		    }
		    
			double dBytes = (double) oOutputImage.length();
			double dKilobytes = (dBytes / 1024);
			double dMegabytes = (dKilobytes / 1024);
			
			if( dMegabytes > (double) AttachmentResourceUtils.s_iMAX_IMAGE_MB_SIZE){
				RiseLog.warnLog("AttachResource.upload: file too big, delete it");
				oOutputImage.delete();
		    	return Response.status(Status.BAD_REQUEST).build();
			}
			else {
				RiseLog.debugLog("AttachResource.upload: Dimension is ok proceed");
			}
			
			// If coordinates are provided
			if (ofLat != null && ofLng != null) {
				RiseLog.debugLog("AttachResource.upload: lat lng coordinates available, save in associated json");
				
				// We try to save a json file associate to the attachment
				File oJsonFile = new File(sPath.replace(RiseFileUtils.getFileNameExtension(sPath), ".json"));
				if (oJsonFile.createNewFile()) {
					String sJson = "{\n\t\"lat\": " + ofLat.toString() + ",\n\t\"lng\": " + ofLng.toString() + " }";
					RiseFileUtils.writeFile(sJson, oJsonFile);
				}
				else {
					RiseLog.errorLog("AttachResource.upload: error creating json file associated to the " + sFileName + " attachment");
				}
			}
		    
		    RiseLog.debugLog("AttachResource.upload: ok, all done!");
		    
			return Response.status(Status.OK).build();
		}
		catch (Throwable oEx) {
			RiseLog.errorLog("AttachResource.upload: exception " + oEx.toString());
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
	public Response get(@HeaderParam("x-session-token") String sSessionId, @QueryParam("token") String sTokenSessionId, @QueryParam("collection") String sCollection, @QueryParam("folder") String sFolder, @QueryParam("name") String sAttachmentName) {
		
		try {
			
			// Check session
			if( Utils.isNullOrEmpty(sSessionId) == false) {
				sTokenSessionId = sSessionId;
			}

			RiseLog.debugLog("AttachResource.get( collection: " + sCollection + " sAttachmentName: " + sAttachmentName +")");
			
			User oUser = Rise.getUserFromSession(sTokenSessionId);

			if (oUser==null) {
				RiseLog.warnLog("AttachResource.get: no valid user or session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			if (!AttachmentResourceUtils.isValidCollection(sCollection)) {
				RiseLog.warnLog("AttachResource.get: invalid collection");
				return Response.status(Status.BAD_REQUEST).build();			
			}
			
			if(Utils.isNullOrEmpty(sAttachmentName)) {
				RiseLog.warnLog("AttachResource.get: Attachment name is null" );
				return Response.status(Status.BAD_REQUEST).build();
			}			
			
			if (!PermissionsUtils.canUserAccessAttachment(oUser.getUserId(), sCollection, sFolder, sAttachmentName)) {
				RiseLog.warnLog("AttachResource.get: user cannot access the collection and folder");
				return Response.status(Status.FORBIDDEN).build();				
			}			
			
			if (sFolder==null) sFolder="";
			
			//sanity check: are the inputs safe? It must be a file name, not a path
			if(sAttachmentName.contains("/") || sAttachmentName.contains("\\") || sCollection.contains("/") || sCollection.contains("\\")|| sFolder.contains("/") || sFolder.contains("\\")) {
				RiseLog.warnLog("AttachResource.get: Attachment or Collection name looks like a path" );
				return Response.status(Status.BAD_REQUEST).build();
			}			
					
			String sPathAttachmentFolder = AttachmentResourceUtils.getAttachmentSubPath(sCollection, sFolder);
			String sAbsolutePath = sPathAttachmentFolder + sAttachmentName;
			
			RiseLog.debugLog("AttachResource.get: sAbsolutePath " + sAbsolutePath);
			
			AttachmentFile oAttachment = new AttachmentFile(sAbsolutePath);
			
			//Check the attachment and extension
			if(oAttachment.exists() == false){
				RiseLog.warnLog("AttachResource.get: unable to find image in " + sAbsolutePath);
				return Response.status(Status.NO_CONTENT).build();
			}
			
			//prepare buffer and send the attachment to the client 
			ByteArrayInputStream abAttachment = oAttachment.getByteArrayInputStream();
			
		    return Response.ok(abAttachment).build();			
		}
		catch (Exception oEx) {
			RiseLog.errorLog("AttachResource.get: exception " + oEx.toString());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}	

	/**
	 * Deletes one attachment
	 * @param sSessionId User session Id
	 * @param sProcessorId Processor Id
	 * @param sAttachmentName Image Name
	 * @return std http response
	 */
	@DELETE
	@Path("/delete")
	public Response delete(@HeaderParam("x-session-token") String sSessionId, @QueryParam("collection") String sCollection, @QueryParam("folder") String sFolder, @QueryParam("name") String sAttachmentName) {
		
		RiseLog.debugLog("AttachResource.delete( Collection: " + sCollection + ", Attachment Name: " + sAttachmentName + " )");
		
		User oUser = Rise.getUserFromSession(sSessionId);

		if (oUser==null) {
			RiseLog.warnLog("AttachResource.delete: user or session invalid");
			return Response.status(Status.UNAUTHORIZED).build();
		}
						
		if(Utils.isNullOrEmpty(sAttachmentName)) {
			RiseLog.warnLog("AttachResource.delete: Attachment name is null" );
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		if (!AttachmentResourceUtils.isValidCollection(sCollection)) {
			RiseLog.warnLog("AttachResource.delete: invalid collection");
			return Response.status(Status.BAD_REQUEST).build();			
		}
		
		if (!PermissionsUtils.canUserWriteAttachment(oUser.getUserId(), sCollection, sFolder, sAttachmentName)) {
			RiseLog.warnLog("AttachResource.delete: user cannot access the collection and folder");
			return Response.status(Status.FORBIDDEN).build();				
		}		
		
		if (sFolder==null) sFolder="";		

		//sanity check: is sImageName safe? It must be a file name, not a path
		if(sAttachmentName.contains("/") || sAttachmentName.contains("\\") || sCollection.contains("/") || sCollection.contains("\\")|| sFolder.contains("/") || sFolder.contains("\\")) {
			RiseLog.warnLog("AttachResource.delete: Attachment or Collection name looks like a path" );
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		String sPath = AttachmentResourceUtils.getAttachmentSubPath(sCollection, sFolder);
		String sFilePath = sPath+sAttachmentName;
		
		File oFile = new File(sFilePath);
		
		if (!oFile.exists()) {
			RiseLog.warnLog("AttachResource.delete: file " + sAttachmentName +" not found");
			return Response.status(Status.NOT_FOUND).build();			
		}
				
		//delete file
		try {
			if (!oFile.delete()) {
				RiseLog.warnLog("AttachResource.delete: error deleting file ");
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();				
			}
			
			String sJsonFile = sFilePath.replace(RiseFileUtils.getFileNameExtension(sFilePath), ".json");
			File oJsonFile = new File(sJsonFile);
			
			if (oJsonFile.exists()) {
				RiseLog.debugLog("AttachResource.delete: deleting associated json file");
				
				if (!oJsonFile.delete()) {
					RiseLog.warnLog("AttachResource.delete: error deleting the json associated file ");
				}
			}
			
		} catch (Exception oE) {
			RiseLog.errorLog("AttachResource.delete: exception " + oE );
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		
		return Response.status(Status.OK).build();
	}	

	/**
	 * Check if an attachment exists
	 * @param sSessionId User Session Id
	 * @param sCollection Processor Id
	 * @return Logo byte stream
	 */
	@GET
	@Path("/exists")
	public Response exists(@HeaderParam("x-session-token") String sSessionId, @QueryParam("collection") String sCollection, @QueryParam("folder") String sFolder, @QueryParam("name") String sAttachmentName) {
		
		try {

			RiseLog.debugLog("AttachResource.exists( collection: " + sCollection + " sAttachmentName: " + sAttachmentName + " sFoler: " + sFolder+ ")");
			
			User oUser = Rise.getUserFromSession(sSessionId);

			if (oUser==null) {
				RiseLog.warnLog("AttachResource.exists: no valid user or session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			if (!AttachmentResourceUtils.isValidCollection(sCollection)) {
				RiseLog.warnLog("AttachResource.exists: invalid collection");
				return Response.status(Status.BAD_REQUEST).build();			
			}
			
			if(Utils.isNullOrEmpty(sAttachmentName)) {
				RiseLog.warnLog("AttachResource.exists: Attachment name is null" );
				return Response.status(Status.BAD_REQUEST).build();
			}			
			
			if (!PermissionsUtils.canUserAccessAttachment(oUser.getUserId(), sCollection, sFolder, sAttachmentName)) {
				RiseLog.warnLog("AttachResource.exists: user cannot access attachment");
				return Response.status(Status.FORBIDDEN).build();				
			}			
			
			if (sFolder==null) sFolder="";
			
			//sanity check: are the inputs safe? It must be a file name, not a path
			if(sAttachmentName.contains("/") || sAttachmentName.contains("\\") || sCollection.contains("/") || sCollection.contains("\\")|| sFolder.contains("/") || sFolder.contains("\\")) {
				RiseLog.warnLog("AttachResource.exists: Attachment or Collection name looks like a path" );
				return Response.status(Status.BAD_REQUEST).build();
			}			
					
			String sPathLogoFolder = AttachmentResourceUtils.getAttachmentSubPath(sCollection, sFolder);
			String sAbsolutePath = sPathLogoFolder + sAttachmentName;
			
			RiseLog.debugLog("AttachResource.exists: sAbsolutePath " + sAbsolutePath);
			
			File oImage = new File(sAbsolutePath);
			
			//Check the logo and extension
			if(oImage.exists() == false){
				RiseLog.debugLog("AttachResource.exists: unable to find image in " + sAbsolutePath);
				return Response.status(Status.NOT_FOUND).build();
			}
			
		    return Response.ok().build();
		}
		catch (Exception oEx) {
			RiseLog.errorLog("AttachResource.exists: exception " + oEx.toString());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	/**
	 * Gets a list of attachments available in a collection
	 * @param sSessionId User Session Id
	 * @param sCollection Processor Id
	 * @return Logo byte stream
	 */
	@GET
	@Path("/list")
	public Response list(@HeaderParam("x-session-token") String sSessionId, @QueryParam("collection") String sCollection, @QueryParam("folder") String sFolder) {
		
		try {
			
			RiseLog.debugLog("AttachResource.list( collection: " + sCollection + " folder: " + sFolder +")");
			
			User oUser = Rise.getUserFromSession(sSessionId);

			if (oUser==null) {
				RiseLog.warnLog("AttachResource.list: no valid user or session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			if (!AttachmentResourceUtils.isValidCollection(sCollection)) {
				RiseLog.warnLog("AttachResource.list: invalid collection");
				return Response.status(Status.BAD_REQUEST).build();			
			}
			
			
			if (!PermissionsUtils.canUserAccessAttachment(oUser.getUserId(), sCollection, sFolder, "")) {
				RiseLog.warnLog("AttachResource.list: user cannot access the collection and folder");
				return Response.status(Status.FORBIDDEN).build();				
			}			
			
			if (sFolder==null) sFolder="";
			
			//sanity check: are the inputs safe? It must be a file name, not a path
			if(sCollection.contains("/") || sCollection.contains("\\")|| sFolder.contains("/") || sFolder.contains("\\")) {
				RiseLog.warnLog("AttachResource.list: Folder or Collection name looks like a path" );
				return Response.status(Status.BAD_REQUEST).build();
			}			
					
			String sPathAttachmentFolder = AttachmentResourceUtils.getAttachmentSubPath(sCollection, sFolder);
			
			AttachmentFile oFolder = new AttachmentFile(sPathAttachmentFolder);
			
			//Check the attachment and extension
			if(oFolder.exists() == false){
				RiseLog.warnLog("AttachResource.list: unable to find image in " + sPathAttachmentFolder);
				return Response.status(Status.NO_CONTENT).build();
			}
			
			File [] asFiles = oFolder.listFiles();
			
			AttachmentListViewModel oAttachmentList = new AttachmentListViewModel();
			oAttachmentList.collection=sCollection;
			oAttachmentList.folder=sFolder;
			
			for (File oFile : asFiles) {
				oAttachmentList.files.add(oFile.getName());
				
				String sFileFullPath = oFile.getPath();
				String sJsonFullPath = sFileFullPath.replace(RiseFileUtils.getFileNameExtension(sFileFullPath), ".json");
				
				try {
					File oJsonFile = new File(sJsonFullPath);
					if (oJsonFile.exists()) {
						JSONObject oCoordinates = JsonUtils.loadJsonFromFile(sJsonFullPath);
						if (oCoordinates != null) {
							float fLat = oCoordinates.getFloat("lat");
							float fLng = oCoordinates.getFloat("lng");
							
							oAttachmentList.lats.add(fLat);
							oAttachmentList.lngs.add(fLng);							
						}
					}
					else {
						oAttachmentList.lats.add(-9999.0f);
						oAttachmentList.lngs.add(-9999.0f);
					}
				}
				catch (Exception oEx) {
					RiseLog.warnLog("Exception trying to read the attached json file of "+ oFile.getName() + " We try to recover " + oEx.toString());
					if (oAttachmentList.files.size()>oAttachmentList.lats.size()) oAttachmentList.lats.add(-9999.0f);
					if (oAttachmentList.files.size()>oAttachmentList.lngs.size()) oAttachmentList.lngs.add(-9999.0f);
				}
				
			}
						
		    return Response.ok(oAttachmentList).build();			
		}
		catch (Exception oEx) {
			RiseLog.errorLog("AttachResource.list: exception " + oEx.toString());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}		
}
