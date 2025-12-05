package rise.api;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.Response.Status;
import rise.Rise;
import rise.lib.business.Area;
import rise.lib.business.Layer;
import rise.lib.business.Map;
import rise.lib.business.Plugin;
import rise.lib.business.User;
import rise.lib.config.RiseConfig;
import rise.lib.data.AreaRepository;
import rise.lib.data.LayerRepository;
import rise.lib.data.MapRepository;
import rise.lib.data.PluginRepository;
import rise.lib.utils.JsonUtils;
import rise.lib.utils.PermissionsUtils;
import rise.lib.utils.RiseFileUtils;
import rise.lib.utils.RunTimeUtils;
import rise.lib.utils.ShellExecReturn;
import rise.lib.utils.Utils;
import rise.lib.utils.date.DateUtils;
import rise.lib.utils.log.RiseLog;
import rise.lib.viewmodels.LayerAnalyzerInputViewModel;
import rise.lib.viewmodels.LayerAnalyzerOutputViewModel;
import rise.lib.viewmodels.LayerMapViewModel;
import rise.lib.viewmodels.LayerViewModel;
import rise.lib.viewmodels.RiseViewModel;
import rise.stream.FileStreamingOutput;
import wasdi.jwasdilib.WasdiLib;

@Path("layer")
public class LayerResource {

	@GET
	@Path("find")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getLayer(@HeaderParam("x-session-token") String sSessionId, @QueryParam("map_id") String sMapId,
			@QueryParam("area_id") String sAreaId, @QueryParam("date") Long oDate) {
		try {
			// Check the session
			User oUser = Rise.getUserFromSession(sSessionId);

			if (oUser == null) {
				RiseLog.warnLog("LayerResource.getLayer: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			if (Utils.isNullOrEmpty(sAreaId)) {
				RiseLog.warnLog("LayerResource.getLayer: Area id null");
				return Response.status(Status.BAD_REQUEST).build();
			}

			// Check if we have this subscription
			AreaRepository oAreaRepository = new AreaRepository();
			Area oArea = (Area) oAreaRepository.get(sAreaId);

			if (oArea == null) {
				RiseLog.warnLog("LayerResource.getLayer: Area with this id " + sAreaId + " not found");
				return Response.status(Status.BAD_REQUEST).build();
			}

			if (!PermissionsUtils.canUserAccessArea(oArea, oUser)) {
				RiseLog.warnLog("LayerResource.getLayer: user cannot access area");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			if (Utils.isNullOrEmpty(sMapId)) {
				RiseLog.warnLog("LayerResource.getLayer: Map id null");
				return Response.status(Status.BAD_REQUEST).build();
			}

			MapRepository oMapRepository = new MapRepository();
			Map oMap = (Map) oMapRepository.get(sMapId);

			if (oMap == null) {
				RiseLog.warnLog("LayerResource.getLayer: Map with this id " + sMapId + " not found");
				return Response.status(Status.BAD_REQUEST).build();
			}

			if (oDate == null)
				oDate = 0L;

			double dDate = (double) oDate;

			if (dDate <= 0.0)
				dDate = DateUtils.getNowAsDouble();

			LayerRepository oLayerRepository = new LayerRepository();
			Layer oLayer = null;

			if (oMap.isDateFiltered()) {
				oLayer = oLayerRepository.getLayerByAreaMapTime(sAreaId, sMapId, (double) dDate/1000.0);
			}
			else {
				oLayer = oLayerRepository.getLayerByAreaMap(sAreaId, sMapId);
			}

			if (oLayer != null) {

				if (oMap.getMaxAgeDays()>=0 && oMap.isDateFiltered()) {
					long lReference = Double.valueOf(dDate).longValue();
					long lDistance = Math.abs(lReference - oLayer.getReferenceDate().longValue()*1000l);
					long lMaxAge = oMap.getMaxAgeDays()*24l*60l*60l*1000l;

					if (lDistance>lMaxAge) {
						RiseLog.debugLog("LayerResource.getLayer: found a layer but is too old, discard it");
						oLayer = null;
					}
				}

				LayerViewModel oLayerViewModel = (LayerViewModel) RiseViewModel.getFromEntity(LayerViewModel.class.getName(), oLayer);
				return Response.ok(oLayerViewModel).build();

			} 
			else {
				return Response.status(Status.NO_CONTENT).build();
			}
		} catch (Exception oEx) {
			RiseLog.errorLog("LayerResource.getLayer: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	@POST
	@Path("find_old")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getLayerPOST(@HeaderParam("x-session-token") String sSessionId, String sMapIds,
	        @QueryParam("area_id") String sAreaId, @QueryParam("date") Long oDate) {
	    try {
	        User oUser = Rise.getUserFromSession(sSessionId);
	        if (oUser == null) {
	            RiseLog.warnLog("LayerResource.getLayerPOST: invalid Session");
	            return Response.status(Status.UNAUTHORIZED).build();
	        }
	        if (Utils.isNullOrEmpty(sAreaId)) {
	            RiseLog.warnLog("LayerResource.getLayerPOST: Area id null");
	            return Response.status(Status.BAD_REQUEST).build();
	        }
	        AreaRepository oAreaRepository = new AreaRepository();
	        Area oArea = (Area) oAreaRepository.get(sAreaId);
	        if (oArea == null || !PermissionsUtils.canUserAccessArea(oArea, oUser)) {
	            RiseLog.warnLog("LayerResource.getLayerPOST: Area not found or user cannot access");
	            return Response.status(Status.UNAUTHORIZED).build();
	        }
	        if (Utils.isNullOrEmpty(sMapIds)) {
	            RiseLog.warnLog("LayerResource.getLayerPOST: Map ids null");
	            return Response.status(Status.BAD_REQUEST).build();
	        }
	        
	        // --- 2. INPUT CLEANING AND PROCESSING (Improved) ---
	        String sCleanMapIds = sMapIds.replaceAll("[\\n\\r\\t]", "");
	        List<String> asMapIds = Arrays.stream(sCleanMapIds.split(","))
	                .map(String::trim)
	                .filter(id -> !id.isEmpty())
	                .collect(Collectors.toList());

	        if (asMapIds.isEmpty()) {
	            RiseLog.warnLog("LayerResource.getLayerPOST: Map id list is empty");
	            return Response.status(Status.BAD_REQUEST).build();				
	        }
	        
	        // --- 3. DATE SETUP ---
	        if (oDate == null) oDate = 0L;
	        double dDate = (double) oDate;
	        if (dDate <= 0.0) dDate = DateUtils.getNowAsDouble();
	        
	        // Convert to seconds for repository use (matching original method signature)
	        final double dReferenceTimeSec = dDate / 1000.0;

	        // --- 4. OPTIMIZED BULK DATABASE ACCESS (Fast Operations) ---
	        
	     // --- 4. PARALLEL DATABASE ACCESS ---
	     // Start both tasks immediately. They run at the same time.

	     // Task A: Fetch Maps Config
	     CompletableFuture<java.util.Map<String, Map>> aoFutureMaps = CompletableFuture.supplyAsync(() -> {
	         MapRepository repo = new MapRepository();
	         return repo.getMapsByIds(asMapIds);
	     });

	     // Task B: Fetch Layer Dates
	     CompletableFuture<java.util.Map<String, Double>> aoFutureLayers = CompletableFuture.supplyAsync(() -> {
	         LayerRepository repo = new LayerRepository();
	         // Note: passing variables inside the lambda requires them to be 'effectively final'
	         return repo.getLatestLayerDates(sAreaId, asMapIds, dReferenceTimeSec);
	     });

	     // Wait for BOTH to finish (this takes only as long as the slowest one)
	     CompletableFuture.allOf(aoFutureMaps, aoFutureLayers).join();

	     // Get the results
	     java.util.Map<String, Map> aoMapsById = aoFutureMaps.get();
	     java.util.Map<String, Double> aoMapLatestDates = aoFutureLayers.get();

         // --- 5. LOGIC PROCESSING ---
            ArrayList<String> asAvailableMapsIds = new ArrayList<>();

            // We iterate over the maps returned by the DB (these are already the latest ones)
            for (java.util.Map.Entry<String, Double> oEntry : aoMapLatestDates.entrySet()) {
                String sFoundMapId = oEntry.getKey();
                Double dFoundDateSec = oEntry.getValue();
                
                // Get config
                Map oMap = aoMapsById.get(sFoundMapId);
                if (oMap == null) continue; // Should not happen

                // --- Max Age Check ---
                boolean bPassesMaxAgeCheck = true;
                if (oMap.getMaxAgeDays() >= 0 && oMap.isDateFiltered()) {
                    long lReferenceTimeMs = Double.valueOf(dDate).longValue();
                    long lLayerTimeMs = (long)(dFoundDateSec * 1000.0);
                    
                    long lDistance = Math.abs(lReferenceTimeMs - lLayerTimeMs);
                    long lMaxAgeMs = oMap.getMaxAgeDays() * 24L * 60L * 60L * 1000L;

                    if (lDistance > lMaxAgeMs) {
                         // Layer exists but is too old
                    	bPassesMaxAgeCheck = false;
                    }
                }

                if (bPassesMaxAgeCheck) {
                    asAvailableMapsIds.add(sFoundMapId);
                }
            }

            return Response.ok(asAvailableMapsIds).build();

	    } catch (Exception oEx) {
	        RiseLog.errorLog("LayerResource.getLayerPOST: " + oEx);
	        return Response.status(Status.INTERNAL_SERVER_ERROR).build();
	    }
	}
	
	@POST
	@Path("find")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getLayerPOST2(@HeaderParam("x-session-token") String sSessionId, String sMapIds, @QueryParam("area_id") String sAreaId, @QueryParam("date") Long oDate, @QueryParam("plugin_id") String sPluginId) {
		try {
			// Check the session
			User oUser = Rise.getUserFromSession(sSessionId);

			if (oUser == null) {
				RiseLog.warnLog("LayerResource.getLayerPOST: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			if (Utils.isNullOrEmpty(sAreaId)) {
				RiseLog.warnLog("LayerResource.getLayerPOST: Area id null");
				return Response.status(Status.BAD_REQUEST).build();
			}

			// Check if we have this subscription
			AreaRepository oAreaRepository = new AreaRepository();
			Area oArea = (Area) oAreaRepository.get(sAreaId);

			if (oArea == null) {
				RiseLog.warnLog("LayerResource.getLayerPOST: Area with this id " + sAreaId + " not found");
				return Response.status(Status.BAD_REQUEST).build();
			}

			if (!PermissionsUtils.canUserAccessArea(oArea, oUser)) {
				RiseLog.warnLog("LayerResource.getLayerPOST: user cannot access area");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			if (Utils.isNullOrEmpty(sMapIds) && Utils.isNullOrEmpty(sPluginId)) {
				RiseLog.warnLog("LayerResource.getLayerPOST: Map ids null");
				return Response.status(Status.BAD_REQUEST).build();
			}
			
			// Final list of Map Id to check			
			ArrayList<String> asMapIdsAsList = new ArrayList<String>();
			
			if (!Utils.isNullOrEmpty(sMapIds)) {
				// Get the Map Ids from the input string
				String [] asMapIds = sMapIds.split(",");	
				asMapIdsAsList = new ArrayList<String>(Arrays.asList(asMapIds));
			}
			
			if (!Utils.isNullOrEmpty(sPluginId))
			{
				// Get the Map Id from the plugin
	    		PluginRepository oPluginRepository = new PluginRepository();
	    		
				Plugin oPlugin= (Plugin) oPluginRepository.get(sPluginId);
				
				if(oPlugin!=null) {
					for (String sMapId : oPlugin.getMaps()) {
						if (!asMapIdsAsList.contains(sMapId)) {
							asMapIdsAsList.add(sMapId);
						}
					}
				}
			}
			
			// We need some maps...
			if (asMapIdsAsList.size()==0) {
				RiseLog.warnLog("LayerResource.getLayerPOST: impossible to get map Ids");
				return Response.status(Status.BAD_REQUEST).build();				
			}
						
			
			// Prepare the Repo
			MapRepository oMapRepository = new MapRepository();
			LayerRepository oLayerRepository = new LayerRepository();

			// Prepare the requested date
			if (oDate == null)
				oDate = 0L;

			double dDate = (double) oDate;

			if (dDate <= 0.0) {
				dDate = DateUtils.getNowAsDouble();	
			}
			
			Layer oLayer = null;
			ArrayList<LayerMapViewModel> aoLayerViewModels = new ArrayList<>();
			
			// Get all the map objects requested
			java.util.Map<String, Map> aoMapsDictionary = oMapRepository.getMapsByIds(asMapIdsAsList);
			
			// For each
			for (String sMapId : asMapIdsAsList) {
				
				Map oMap = (Map) aoMapsDictionary.get(sMapId);

				if (oMap == null) {
					RiseLog.warnLog("LayerResource.getLayerPOST: Map with this id " + sMapId + " not found");
					continue;
				}
				
				if (oMap.isHidden()) continue;
				
				if (oMap.isDateFiltered()) {
					// Get the layer filtered by time
					oLayer = oLayerRepository.getLayerByAreaMapTime(sAreaId, sMapId, (double) dDate/1000.0);
				}
				else {
					// Or as it is
					oLayer = oLayerRepository.getLayerByAreaMap(sAreaId, sMapId);
				}

				if (oLayer != null) {
					// Check if it is in the valid range
					if (oMap.getMaxAgeDays()>=0 && oMap.isDateFiltered()) {
						long lReference = Double.valueOf(dDate).longValue();
						long lDistance = Math.abs(lReference - oLayer.getReferenceDate().longValue()*1000l);
						long lMaxAge = oMap.getMaxAgeDays()*24l*60l*60l*1000l;

						if (lDistance>lMaxAge) {
							RiseLog.debugLog("LayerResource.getLayerPOST: found a layer but is too old, discard it");
							oLayer = null;
						}
					}
				}
				
				if (oLayer!=null) {
					// Get the view model
					LayerMapViewModel oLayerViewModel = (LayerMapViewModel) RiseViewModel.getFromEntity(LayerMapViewModel.class.getName(), oLayer);
					oLayerViewModel.name = oMap.getName();
					oLayerViewModel.icon = oMap.getIcon();
					oLayerViewModel.disabled = false;
					oLayerViewModel.description = oMap.getDescription();
					
					aoLayerViewModels.add(oLayerViewModel);
				} 
				else {
					LayerMapViewModel oLayerViewModel = new LayerMapViewModel();
					oLayerViewModel.name = oMap.getName();
					oLayerViewModel.icon = oMap.getIcon();
					oLayerViewModel.mapId = sMapId;
					oLayerViewModel.disabled = true;
					oLayerViewModel.description = oMap.getDescription();
					aoLayerViewModels.add(oLayerViewModel);					
				}
			}
			
			return Response.ok(aoLayerViewModels).build();

		} catch (Exception oEx) {
			RiseLog.errorLog("LayerResource.getLayerPOST: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}		

	@GET
	@Path("download_layer")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response downloadLayer(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("layer_id") String sLayerId, @QueryParam("format") String sFormat) {

		try {
			// Check the session
			User oUser = Rise.getUserFromSession(sSessionId);

			if (oUser == null) {
				RiseLog.warnLog("LayerResource.downloadLayer: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			if (Utils.isNullOrEmpty(sLayerId)) {
				RiseLog.warnLog("LayerResource.downloadLayer: layer id null");
				return Response.status(Status.BAD_REQUEST).build();
			}
			if (Utils.isNullOrEmpty(sFormat)) {
				RiseLog.warnLog("LayerResource.downloadLayer: format null");
				return Response.status(Status.BAD_REQUEST).build();
			}
			if (!sFormat.equals("shp") && !sFormat.equals("geotiff")) {
				RiseLog.warnLog("LayerResource.downloadLayer: invalid format ");
				return Response.status(Status.BAD_REQUEST).build();
			}

			// verify layer exist
			LayerRepository oLayerRepository = new LayerRepository();
			Layer oLayer = (Layer) oLayerRepository.get(sLayerId);
			if (oLayer == null) {
				RiseLog.warnLog("LayerResource.downloadLayer: layer null");
				return Response.status(Status.NOT_FOUND).build();
			}
			
			WasdiLib oWasdiLib = new WasdiLib();
			
			oWasdiLib.setUser(RiseConfig.Current.wasdiConfig.wasdiUser);
			oWasdiLib.setPassword(RiseConfig.Current.wasdiConfig.wasdiPassword);
			
			if (oWasdiLib.init()) {
				
				if (Utils.isNullOrEmpty(oLayer.getWorkspaceId())) {
					String sWorkspaceName = oLayer.getAreaId() + "|" + oLayer.getPluginId() + "|" + oLayer.getMapId();
					oWasdiLib.openWorkspace(sWorkspaceName);
				}
				else {
					oWasdiLib.openWorkspaceById(oLayer.getWorkspaceId());
				}
				
				String sLocalFilePath = oWasdiLib.getPath(oLayer.getId() + ".tif");
				
				if (!Utils.isNullOrEmpty(sLocalFilePath)) {
					
					// Ok send the file to the user
					File oFile = new File(sLocalFilePath);
					FileStreamingOutput oStream = new FileStreamingOutput(oFile);
					ResponseBuilder oResponseBuilder = Response.ok(oStream);
					String sFileName = oFile.getName();
					oResponseBuilder.header("Content-Disposition", "attachment; filename=" + sFileName);
					//oResponseBuilder.header("Content-Length", Long.toString(oFile.length()));
					oResponseBuilder.header("Access-Control-Expose-Headers", "Content-Disposition");

					return oResponseBuilder.build();
				} 
				else {
					return Response.status(Status.INTERNAL_SERVER_ERROR).build();
				}
			} 
			else {
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		} catch (Exception oEx) {
			RiseLog.errorLog("LayerResource.downloadLayer: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	

    @POST
    @Path("analyzer")
    @Produces(MediaType.APPLICATION_JSON)
    public Response layerAnalyzer(@HeaderParam("x-session-token") String sSessionId, LayerAnalyzerInputViewModel oInput) {
    	
    	try {
    		LayerAnalyzerOutputViewModel oOutput = new LayerAnalyzerOutputViewModel();
    		
        	List<String> asArgs = new ArrayList<String>();
        	
        	// Python executable
        	String sPythonPath = RiseConfig.Current.paths.pythonPath;
        	asArgs.add(sPythonPath);

        	// Our script
        	String sScriptsPath = RiseConfig.Current.paths.scriptsPath;
        	if (!sScriptsPath.endsWith("/")) sScriptsPath += "/";
        	asArgs.add(sScriptsPath+"layer_analyzer.py");
        	
        	// Operation
        	asArgs.add("analyze");
        	
        	String sTmpPath = RiseConfig.Current.paths.riseTempFolder;
        	if (!sTmpPath.endsWith("/")) sTmpPath += "/";
        	
        	String sInputFileName = Utils.getRandomName() + ".json";
        	String sOutputFileName = Utils.getRandomName() + ".json";
        	String sInputFullPath = sTmpPath + sInputFileName;
        	String sOutputFullPath = sTmpPath + sOutputFileName;
        	
        	oInput.outputPath = sTmpPath;
        	
        	String sInputJson = JsonUtils.stringify(oInput);
        	RiseFileUtils.writeFile(sInputJson, new File(sInputFullPath));
        	
        	// Input File
        	asArgs.add(sInputFullPath);
        	// Output File
        	asArgs.add(sOutputFullPath);
        	// Config
        	asArgs.add(RiseConfig.Current.paths.riseConfigPath);
        	
        	ShellExecReturn oReturn = RunTimeUtils.shellExec(asArgs, true, true,true,true);
        	
        	RiseLog.debugLog("LayerResource.layerAnalyzer: got ouput from python script:");
        	
        	RiseLog.debugLog(oReturn.getOperationLogs());
        	
        	File oOutputFile = new File(sOutputFullPath);
        	
        	if (oOutputFile.exists()) {
        		RiseLog.infoLog("LayerResource.layerAnalyzer: try to read Output " + sOutputFullPath);
        		JSONObject oJsonOutput = JsonUtils.loadJsonFromFile(sOutputFullPath);
        		
        		oOutput.areaPixelAffected = oJsonOutput.optString("areaPixelAffected");
        		oOutput.estimatedArea = oJsonOutput.optString("estimatedArea");
        		oOutput.percentAreaAffectedPixels = oJsonOutput.optString("percentAreaAffectedPixels");
        		oOutput.percentTotAreaAffectedPixels = oJsonOutput.optString("percentTotAreaAffectedPixels");
        		oOutput.totAreaPixels = oJsonOutput.optString("totAreaPixels");
        		JSONArray oHistogram = oJsonOutput.optJSONArray("histogram");
        		
        		if (oHistogram != null) {
        			List<Object> aoHistogramValues =oHistogram.toList();
        			for (Object oValue : aoHistogramValues) {
						oOutput.histogram.add(oValue.toString());
					}
        		}
        		else {
        			RiseLog.warnLog("LayerResource.layerAnalyzer: cannot find the histogram");
        		}
        	}
        	else {
        		// Problems reading the output
        		RiseLog.warnLog("LayerResource.layerAnalyzer: impossible to find the output file from the python script");
        	}        	
        	
//        	RiseFileUtils.deleteFile(sInputFullPath);
//        	RiseFileUtils.deleteFile(sOutputFullPath);
        	
        	return Response.ok(oOutput).build();    		
    	}
    	catch (Exception oEx) {
    		RiseLog.errorLog("LayerResource.layerAnalyzer: exception " + oEx.toString());
    		return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
    }

}
