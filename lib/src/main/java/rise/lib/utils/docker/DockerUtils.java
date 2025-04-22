package rise.lib.utils.docker;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import rise.lib.config.DockerRegistryConfig;
import rise.lib.config.PathsConfig;
import rise.lib.config.RiseConfig;
import rise.lib.utils.Utils;
import rise.lib.utils.docker.containersViewModels.AuthToken;
import rise.lib.utils.docker.containersViewModels.ContainerInfo;
import rise.lib.utils.docker.containersViewModels.CreateContainerResponse;
import rise.lib.utils.docker.containersViewModels.CreateParams;
import rise.lib.utils.docker.containersViewModels.LoginInfo;
import rise.lib.utils.docker.containersViewModels.constants.ContainerStates;
import rise.lib.utils.http.HttpCallResponse;
import rise.lib.utils.http.HttpUtils;
import rise.lib.utils.log.RiseLog;

import rise.lib.utils.JsonUtils;


public class DockerUtils {


    /**
     * Folder of the processor
     */
    // protected String m_sProcessorFolder;

    /**
     * User that run the docker
     */
    protected String m_sRiseSystemUserName = "appwasdi";
    
    /**
     * User that run the docker
     */
    protected String m_sRiseSystemGroupName = "appwasdi";
    
    /**
     * User that run the docker
     */
    protected int m_iRiseSystemUserId = 2042;
    
    /**
     * User that run the docker
     */
    protected int m_iRiseSystemGroupId = 2042;

    /**
     * Network mode
     */
    protected String m_sDockerNetworkMode = "net-wasdi";
    
    /**
     * Docker registry in use
     */
    protected String m_sDockerRegistry = "";
    
    
    /**
     * Basic Constructor. Set all the members to default value.
     */
    public DockerUtils() {
    	m_sDockerRegistry = "";
    	m_sRiseSystemUserName = RiseConfig.Current.systemUserName;
    	m_sRiseSystemGroupName = RiseConfig.Current.systemGroupName;
    	m_iRiseSystemUserId = RiseConfig.Current.systemUserId;
    	m_iRiseSystemGroupId = RiseConfig.Current.systemGroupId;
    	if (RiseConfig.Current != null) {
    		m_sDockerNetworkMode = RiseConfig.Current.dockers.dockerNetworkMode;
    	}
    }

	/**
	 * Get the name of user to pass to docker. Empty string to avoid.
	 * @return
	 */
	public String getRiseSystemUserName() {
		return m_sRiseSystemUserName;
	}
	
	/**
	 * Set the name of user to pass to docker. Empty string to avoid.
	 * @param sRiseSystemUserName
	 */
	public void setRiseSystemUserName(String sRiseSystemUserName) {
		this.m_sRiseSystemUserName = sRiseSystemUserName;
	}    

	/**
	 * get the rise system group name
	 * @return
	 */
	public String getRiseSystemGroupName() {
		return m_sRiseSystemGroupName;
	}

	/**
	 * set the rise system group name
	 * @param sWasdiSystemGroupName
	 */
	public void setRiseSystemGroupName(String sRiseSystemGroupName) {
		this.m_sRiseSystemGroupName = sRiseSystemGroupName;
	}

	/**
	 * Get the rise system user id
	 * @return
	 */
	public int getRiseSystemUserId() {
		return m_iRiseSystemUserId;
	}
	
	/**
	 * Set the rise system user id
	 * @param iRiseSystemUserId
	 */
	public void setRiseSystemUserId(int iRiseSystemUserId) {
		this.m_iRiseSystemUserId = iRiseSystemUserId;
	}

	/**
	 * Get the rise system group id
	 * @return
	 */
	public int geRiseSystemGroupId() {
		return m_iRiseSystemGroupId;
	}

	/**
	 * Get the rise system group id
	 * @param iWasdiSystemGroupId
	 */
	public void setRiseSystemGroupId(int iRiseSystemGroupId) {
		this.m_iRiseSystemGroupId = iRiseSystemGroupId;
	}    
	
	/**
	 * Get the network mode
	 * @return
	 */
	public String getDockerNetworkMode() {
		return m_sDockerNetworkMode;
	}

	/**
	 * Set the network node
	 * @param sDockerNetworkMode
	 */
	public void setDockerNetworkMode(String sDockerNetworkMode) {
		this.m_sDockerNetworkMode = sDockerNetworkMode;
	} 
	
	/**
	 * Address of the Docker Register in use. 
	 * @return id of the registry
	 */
	public String getDockerRegistry() {
		return m_sDockerRegistry;
	}

	/**
	 * Get the Address of the docker registry in use. "" to refer local registry
	 * @param sDockerRegistry
	 */
	public void setDockerRegistry(String sDockerRegistry) {
		this.m_sDockerRegistry = sDockerRegistry;
	}
	
	
    /**
     * Log in a Docker Registry
     * @param oDockerRegistryConfig
     * @return
     */
    public String loginInRegistry(DockerRegistryConfig oDockerRegistryConfig) {
    	return loginInRegistry(oDockerRegistryConfig.address, oDockerRegistryConfig.user, oDockerRegistryConfig.password);
    }
	
	/**
     * Log in docker on a specific Repository Server
     * @param sServer Server Address
     * @param sUser Server User
     * @param sPassword Server Password
     * @param sFolder 
     * @return The encoded auth header for the registry or an empty string
     */
    public String loginInRegistry(String sServer, String sUser, String sPassword) {
    	
    	try {
    		// Get the API Address
    		String sUrl = RiseConfig.Current.dockers.internalDockerAPIAddress;
    		if (!sUrl.endsWith("/")) sUrl += "/";
    		
    		// Auth end-point
    		sUrl += "auth";
    		
    		// Prepare the login info
    		LoginInfo oLoginInfo = new LoginInfo();
    		oLoginInfo.username = sUser;
    		oLoginInfo.password = sPassword;
    		oLoginInfo.serveraddress = sServer;
    		
    		if (!oLoginInfo.serveraddress.startsWith("https://")) oLoginInfo.serveraddress = "https://" + oLoginInfo.serveraddress; 
    		
    		// Convert in string
    		String sLoginInfo = JsonUtils.stringify(oLoginInfo);
    		
    		// Make the post
    		HttpCallResponse oResponse = HttpUtils.httpPost(sUrl, sLoginInfo);
    		
    		if (oResponse.getResponseCode() == 200) {
    			// Here we should had our Token
    			String sAuthToken = oResponse.getResponseBody();
    			AuthToken oToken = JsonUtils.s_oMapper.readValue(sAuthToken,AuthToken.class);
    			
    			if (Utils.isNullOrEmpty(oToken.IdentityToken)) {
    				String sEncodedAuth = Base64.getEncoder().encodeToString(sLoginInfo.getBytes(StandardCharsets.UTF_8));
    				return sEncodedAuth;
    			}
    			else {
    				
    				String sTokenJson = "\"identitytoken\":\""+oToken.IdentityToken+"\"}";
    				String sEncodedAuth = Base64.getEncoder().encodeToString(sTokenJson.getBytes(StandardCharsets.UTF_8));
    				return sEncodedAuth;
    			}
    		}
    		else {
    			// There is some problem
    			RiseLog.errorLog("DockerUtils.loginInRegistry: auth api for register " + sServer + " returned " + oResponse.getResponseCode() + " and we are without token!");
    			return "";
    		}
    	} catch (Exception oEx) {
    		RiseLog.errorLog("DockerRiseLog.loginInRegistry: " + oEx.toString());
            return "";
        }
    }

    /**
     * Create and Run a container
     * @param sImageName Name of the image
     * @param sImageVersion Version
     * @param asArg Args to be passed as CMD parameter
     * @return The Id of the container if created, empty string in case of problems
     */
    public String run(String sImageName, String sImageVersion, List<String> asArg, boolean bAlwaysRecreateContainer,  ArrayList<String> asAdditionalMountPoints, boolean bAutoRemove) {

        try {
        	
        	// We need the name of the image
        	if (Utils.isNullOrEmpty(sImageName)) {
        		RiseLog.warnLog("DockerUtils.run: image is null");
        		return "";
        	}        	
        	
            // Attach the version, if available
        	if (!Utils.isNullOrEmpty(sImageVersion))  sImageName += ":" + sImageVersion;
        	
        	// And the registry
            if (!Utils.isNullOrEmpty(m_sDockerRegistry)) {
            	sImageName = m_sDockerRegistry + "/" + sImageName;
            }
            
            RiseLog.debugLog("DockerUtils.run: try to start a container from image " + sImageName);
            
            // Authentication Token for the registry (if needed)
            String sToken = "";
            // Name of the container
            String sContainerName = "";
            // Id of the container
            String sContainerId = "";            
            
            // Check if we need to login in the registry
            if (!Utils.isNullOrEmpty(m_sDockerRegistry)) {
            	
            	RiseLog.debugLog("DockerUtils.run: login in the register");
            	
            	List<DockerRegistryConfig> aoRegisters = RiseConfig.Current.dockers.getRegisters();
            	
				// For each register: ordered by priority
				for (int iRegisters=0; iRegisters<aoRegisters.size(); iRegisters++) {
					
					DockerRegistryConfig oDockerRegistryConfig = aoRegisters.get(iRegisters);
					
					if (!m_sDockerRegistry.equals(oDockerRegistryConfig.address)) continue;
					
					RiseLog.debugLog("DockerUtils.run: logging in to " + oDockerRegistryConfig.id);
					
					// Try to login
					sToken = loginInRegistry(oDockerRegistryConfig);
					
					if (Utils.isNullOrEmpty(sToken)) {
						RiseLog.errorLog("DockerUtils.run: error in the login this will be a problem!!");
					}
					
					break;
				}
            }
        	
            // Search first of all if the container is already here
        	ContainerInfo oContainerInfo = null;
        	
        	// Shall we try to re-use a contanier?
        	if (!bAlwaysRecreateContainer) {
        		// Yes! Maybe I'll find it?
        		oContainerInfo = getContainerInfoByImageName(sImageName, sImageVersion); 
        	}
        	
        	if (oContainerInfo == null) {
        		
        		// No we do not have it
        		RiseLog.debugLog("DockerUtils.run: the container is not available");
        		
        		// Since we are creating the Container, we need to set up our name
        		sContainerName = sImageName.replace(":", "_") + "_" + Utils.getRandomName();
        		RiseLog.debugLog("DockerUtils.run: try to create a container named " + sContainerName);
        		
        		if (sContainerName.length()>62) {
        			sContainerName = sContainerName.substring(0, 62);
        			RiseLog.debugLog("DockerUtils.run: container name too long, cut it to " + sContainerName);
        		}
        		
        		// Create the container
            	try {
            		// API URL
            		String sUrl = RiseConfig.Current.dockers.internalDockerAPIAddress;
            		if (!sUrl.endsWith("/")) sUrl += "/";
            		sUrl += "containers/create?name=" + sContainerName;
            		
            		// Create the Payload to send to create the container
            		CreateParams oContainerCreateParams = new CreateParams();
            		
            		// Set the user
            		oContainerCreateParams.User = m_sRiseSystemUserName+":"+m_sRiseSystemGroupName;
            		
            		// Set the image
            		oContainerCreateParams.Image = sImageName;            		
            		
            		// Mount the /data/wasdi/ folder
            		oContainerCreateParams.HostConfig.Binds.add(PathsConfig.getRiseBasePath()+":"+"/data/wasdi");
            		
            		// Set the network mode
            		oContainerCreateParams.HostConfig.NetworkMode = m_sDockerNetworkMode;
            		
            		if (asAdditionalMountPoints!=null) {
            			for (String sMountPoint : asAdditionalMountPoints) {
            				oContainerCreateParams.HostConfig.Binds.add(sMountPoint);
						}
            		}
            		            		
            		if (bAutoRemove) {
            			oContainerCreateParams.HostConfig.AutoRemove = true;
            		}
            		else {
                		oContainerCreateParams.HostConfig.RestartPolicy.put("Name", "no");
                		oContainerCreateParams.HostConfig.RestartPolicy.put("MaximumRetryCount", 0);            			
            		}
            		
            		if (asArg!=null) oContainerCreateParams.Cmd.addAll(asArg);
            		
                    // Extra hosts mapping, useful for some instances when the server host can't be resolved
                    // The symptoms of such problem is that the POST call from the Docker container timeouts
                    if (RiseConfig.Current.dockers.extraHosts != null) {
                    	if (RiseConfig.Current.dockers.extraHosts.size()>0) {
                    		RiseLog.debugLog("DockerUtils.start adding configured extra host mapping to the run arguments");
                        	for (int iExtraHost = 0; iExtraHost<RiseConfig.Current.dockers.extraHosts.size(); iExtraHost ++) {
                        		String sExtraHost = RiseConfig.Current.dockers.extraHosts.get(iExtraHost);
                        		oContainerCreateParams.HostConfig.ExtraHosts.add(sExtraHost);
                        	}
                    	}
                    }            		   
            		
                    // Convert the payload in JSON (NOTE: is hand-made !!)
            		String sContainerCreateParams = oContainerCreateParams.toJson();
            		
            		if (Utils.isNullOrEmpty(sContainerCreateParams)) {
            			RiseLog.errorLog("DockerUtils.run: impossible to get the payload to create the container. We cannot proceed :(");
            			return "";
            		}
            		
            		if (RiseConfig.Current.dockers.logDockerAPICallsPayload) {
                		RiseLog.debugLog("DOCKER JSON PAYLOAD DUMP");
                		RiseLog.debugLog(sContainerCreateParams);
                		RiseLog.debugLog("------------------");            			
            		}
            		
            		// We need to set the json Content-Type
            		HashMap<String, String> asHeaders = new HashMap<>();
            		asHeaders.put("Content-Type", "application/json");
            		
            		// Is a post!
            		HttpCallResponse oResponse = HttpUtils.httpPost(sUrl, sContainerCreateParams, asHeaders);
            		
            		if (oResponse.getResponseCode()<200||oResponse.getResponseCode()>299) {
            			// Here is not good
            			RiseLog.errorLog("DockerUtils.run: impossible to create the container. We cannot proceed :(. ERROR = " + oResponse.getResponseBody());
            			return "";
            		}
            		
            		RiseLog.debugLog("DockerUtils.run: Container created. Message = " + oResponse.getResponseBody());
            		
            		String sCreateResult = oResponse.getResponseBody();
            		CreateContainerResponse oCreateContainerResponse = JsonUtils.s_oMapper.readValue(sCreateResult,CreateContainerResponse.class);
            		
            		if (oCreateContainerResponse != null) {
            			RiseLog.debugLog("DockerUtils.run: got the response, ContainerId = " + oCreateContainerResponse.Id);
            			sContainerId = oCreateContainerResponse.Id;
            		}
            		else {
            			RiseLog.warnLog("DockerUtils.run: impossible to get or transalte the api return, so not container id available");
            		}            		
            	}
            	catch (Exception oEx) {
            		RiseLog.errorLog("DockerUtils.run exception creating the container: " + oEx.toString());
                    return "";
                }        		
        	}
        	else {        		
        		// If the container exists, we can take the ID
        		RiseLog.debugLog("DockerUtils.run: Container already found, we will use the id");
        		sContainerId = oContainerInfo.Id;
        	}
            
            RiseLog.debugLog("DockerUtils.run: Starting Container Named " + sContainerName + " Id " + sContainerId);
            
            // Prepare the url to start it
    		String sUrl = RiseConfig.Current.dockers.internalDockerAPIAddress;
    		if (!sUrl.endsWith("/")) sUrl += "/";
    		sUrl +="containers/" + sContainerId + "/start";
    		
    		// Make the call
    		HttpCallResponse oResponse = HttpUtils.httpPost(sUrl, "");
    		
    		if (oResponse.getResponseCode() == 204) {
    			// Started Well
    			RiseLog.debugLog("DockerUtils.run: Container " + sContainerName + " started");
    			return sContainerId;
    		}
    		else if (oResponse.getResponseCode() == 304) {
    			// ALready Started (but so why we did not detected this before?!?)
    			RiseLog.debugLog("DockerUtils.run: Container " + sContainerName + " wasd already started");
    			return sContainerId;
    		}
    		else {
    			// Error!
    			RiseLog.errorLog("DockerUtils.run: Impossible to start Container " + sContainerName);
    			RiseLog.errorLog("DockerUtils.run: Message Received " + oResponse.getResponseBody());
    			return "";
    		}
            
        } catch (Exception oEx) {
        	RiseLog.errorLog("DockerUtils.run error creating the container: " + oEx.toString());
            return "";
        }
    }    
    

    /**
     * Check if a container is started
     * @param sProcessorName
     * @param sVersion
     * @return
     */
    @SuppressWarnings("unchecked")
	public ContainerInfo getContainerInfoByImageName(String sProcessorName, String sVersion) {
    	
    	try {
    		List<Object> aoOutputJsonMap = getContainersInfo(true);
            
            String sMyImage = "wasdi/" + sProcessorName + ":" + sVersion;
            
            if (!Utils.isNullOrEmpty(m_sDockerRegistry))   {
            	sMyImage = m_sDockerRegistry + "/" + sMyImage;
            }
            
            for (Object oContainer : aoOutputJsonMap) {
				try {
					LinkedHashMap<String, Object> oContainerMap = (LinkedHashMap<String, Object>) oContainer;
					String sImageName = (String) oContainerMap.get("Image");
					
					if (sImageName.endsWith(sMyImage)) {
						RiseLog.debugLog("DockerUtils.getContainerInfoByImageName: found my container " + sMyImage + " Docker Image = " +sImageName);
						
						return convertContainerMapToContainerInfo(oContainerMap);
					}
					
				}
		    	catch (Exception oEx) {
		    		RiseLog.errorLog("DockerUtils.getContainerInfoByImageName: error parsing a container json entity " + oEx.toString());
		        }
			}
    		
    		return null;
    	}
    	catch (Exception oEx) {
    		RiseLog.errorLog("DockerUtils.getContainerInfoByImageName: error ", oEx);
            return null;
        }
    }
    
    
    /**
     * Get the list of running containers from docker
     * @return
     */
    protected List<Object> getContainersInfo() {
    	return getContainersInfo(false);
    }
    
    /**
     * Get the list of containers from docker
     * @return List of objects as returned by Docker API
     */
    protected List<Object> getContainersInfo(boolean bAll) {
    	try {
    		String sUrl = RiseConfig.Current.dockers.internalDockerAPIAddress;
    		if (!sUrl.endsWith("/")) sUrl += "/";
    		sUrl += "containers/json";
    		
    		if (bAll) {
    			sUrl += "?all=true";
    		}
    		
    		HttpCallResponse oResponse = HttpUtils.httpGet(sUrl);
    		
    		if (oResponse.getResponseCode()<200||oResponse.getResponseCode()>299) {
    			return null;
    		}
    		
    		List<Object> aoOutputJsonMap = null;

            try {
                ObjectMapper oMapper = new ObjectMapper();
                aoOutputJsonMap = oMapper.readValue(oResponse.getResponseBody(), new TypeReference<List<Object>>(){});
            } catch (Exception oEx) {
                RiseLog.errorLog("DockerUtils.getContainerInfoByProcessor: exception converting API result " + oEx);
                return null;
            }
            
            return aoOutputJsonMap;
    	}
    	catch (Exception oEx) {
            RiseLog.errorLog("DockerUtils.getContainerInfoByProcessor: exception converting API result " + oEx);
            return null;
        }    	
    }
    
    
    /**
     * Converts a String,Object dictionary, return of the docker api for containers info
     * into a ContainerInfo object
     * @param oContainerMap Map object as returned by docker api
     * @return ContainerInfo entity
     */
    @SuppressWarnings("unchecked")
	protected ContainerInfo convertContainerMapToContainerInfo(LinkedHashMap<String, Object> oContainerMap) {
		ContainerInfo oContainerInfo = new ContainerInfo();
		
		try {
			oContainerInfo.Id = (String) oContainerMap.get("Id");
			oContainerInfo.Image = (String) oContainerMap.get("Image");
			oContainerInfo.ImageId = (String) oContainerMap.get("ImageId");
			oContainerInfo.State = (String) oContainerMap.get("State");
			oContainerInfo.Status = (String) oContainerMap.get("Status");
			oContainerInfo.Names = (List<String>) oContainerMap.get("Names");			
		}
    	catch (Exception oEx) {
    		RiseLog.errorLog("DockerUtils.convertContainerMapToContainerInfo: ", oEx);
        }
		
		return oContainerInfo;
    	
    }
    
    
    /**
     * Check if a container is started with a specific timeout
     * @param sContainerId Container Id
     * @return true when done. False in case of errors
     */
    public boolean waitForContainerToFinish(String sContainerId) {
    	return waitForContainerToFinish(sContainerId, -1);
    }
    
    /**
     * Check if a container is started with a specific timeout
     * @param sContainerId  Container Id
     * @param iMsTimeout Max Milliseconds to wait. -1 for infinite
     * @return true when done. False in case of errors
     */
    public boolean waitForContainerToFinish(String sContainerId, int iMsTimeout) {
    	
    	try {
    		
    		boolean bFinished = false;
    		
    		int iLoopCount = 0;
    		int iLogMaxCount = RiseConfig.Current.dockers.numberOfPollStatusPollingCycleForLog;
    		
    		if (iLogMaxCount<=1) iLogMaxCount = 1;
    		
    		while (!bFinished) {
    			
        		ContainerInfo oContainer = getContainerInfoByContainerId(sContainerId);
        		
        		if (oContainer == null) {
        			RiseLog.debugLog("DockerUtils.waitForContainerToFinish: container not found, so for sure not started");
        			return false;
        		}
        		
        		if (oContainer.State.equals(ContainerStates.EXITED) || oContainer.State.equals(ContainerStates.DEAD)) return true;
        		else {
        			
        			if (iLoopCount%iLogMaxCount == 0) {
        				RiseLog.debugLog("DockerUtils.waitForContainerToFinish: still waiting");
        			}
        			
        			try {
        				Thread.sleep(RiseConfig.Current.dockers.millisBetweenStatusPolling);
        			}
        			catch (InterruptedException oEx) {
						Thread.currentThread().interrupt();
						RiseLog.errorLog("DockerUtils.waitForContainerToFinish. Current thread was interrupted", oEx);
					}
        		}
        		
        		iLoopCount++;
    		}
    		
    		return true;
    	}
    	catch (Exception oEx) {
    		RiseLog.errorLog("DockerUtils.waitForContainerToFinish: " + oEx.toString());
            return false;
        }
    } 
    
    
    /**
     * Get the info of a container starting from the name
     * @param sContainerId Name of the container
     * @return ContainerInfo or null
     */
    @SuppressWarnings("unchecked")
	public ContainerInfo getContainerInfoByContainerId(String sContainerId) {
    	
    	try {
    		//RiseLog.debugLog("DockerUtils.getContainerInfoByContainerId: Searching for container id: " + sContainerId );
    		
    		List<Object> aoOutputJsonMap = getContainersInfo(true);
    		
            for (Object oContainer : aoOutputJsonMap) {
				try {
					LinkedHashMap<String, Object> oContainerMap = (LinkedHashMap<String, Object>) oContainer;
					
					String sId = (String) oContainerMap.get("Id");
					
					if (Utils.isNullOrEmpty(sId)) continue;
					
					if (sId.equals(sContainerId)) {
						//RiseLog.debugLog("DockerUtils.getContainerInfoByContainerId: found my container " + sContainerId );						
						return convertContainerMapToContainerInfo(oContainerMap);
					}					
					
				}
		    	catch (Exception oEx) {
		    		RiseLog.errorLog("DockerUtils.getContainerInfoByContainerId: error parsing a container json entity " + oEx.toString());
		        }
			}
    		
    		return null;
    	}
    	catch (Exception oEx) {
    		RiseLog.errorLog("DockerUtils.getContainerInfoByContainerId: " + oEx.toString());
            return null;
        }
    }
    
    
    /**
     * Get the logs of a container from the Id
     * @return List of objects as returned by Docker API
     */
    public String getContainerLogsByContainerId(String sContainerId) {
    	try {
    		String sUrl =  RiseConfig.Current.dockers.internalDockerAPIAddress;
    		if (!sUrl.endsWith("/")) sUrl += "/";
    		sUrl += "containers/"+sContainerId+"/logs?stdout=true&stderr=true";
    		
    		HttpCallResponse oResponse = HttpUtils.httpGet(sUrl);
    		
    		if (oResponse.getResponseCode()<200||oResponse.getResponseCode()>299) {
    			return "";
    		}
    		else {
    			String sResponseBody = oResponse.getResponseBody();
    			sResponseBody = sResponseBody.replaceAll("[\\x00-\\x09\\x11\\x12\\x14-\\x1F\\x7F]", "");
    			sResponseBody = sResponseBody.replaceAll("[^\\p{ASCII}]", "");
    			
    			return sResponseBody;
    		}    		
    	}
    	catch (Exception oEx) {
            RiseLog.errorLog("DockerUtils.getContainerLogsByContainerName: exception converting API result " + oEx);
            return "";
        }    	
    }

    
    /**
     * Remove a container
     * @param sId Container Id
     * @return true if was removed
     */
    public boolean removeContainer(String sId) {
    	return removeContainer(sId, false);
    }
    
    /**
     * Remove a container
     * @param sId Container Id
     * @param bForce True to force to kill running containers 
     * @return true if was removed
     */
    public boolean removeContainer(String sId, boolean bForce) {
       	try {
    		String sUrl = RiseConfig.Current.dockers.internalDockerAPIAddress;
                        
            if (!Utils.isNullOrEmpty(sId)) {
            	
            	RiseLog.debugLog("DockerUtils.removeContainer: search id " + sId);
            	
        		if (!sUrl.endsWith("/")) sUrl += "/";
        		sUrl += "containers/" + sId;
        		
        		if (bForce) {
        			sUrl+="?force=true";
        		}
        		
        		HttpCallResponse oResponse = HttpUtils.httpDelete(sUrl);
        		
        		if (oResponse.getResponseCode()<200||oResponse.getResponseCode()>299) {
        			return false;
        		}
        		else {
        			return true;
        		}
            }
            else {
            	RiseLog.debugLog("DockerUtils.removeContainer: sId is null or empty, nothing to do");
            }
    		
    		return false;
    	}
    	catch (Exception oEx) {
    		RiseLog.errorLog("DockerUtils.removeContainer: " + oEx.toString());
            return false;
        }    	
    }
    
    
}
