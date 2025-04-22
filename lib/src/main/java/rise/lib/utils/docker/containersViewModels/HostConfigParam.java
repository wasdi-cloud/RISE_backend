package rise.lib.utils.docker.containersViewModels;

import java.util.ArrayList;
import java.util.HashMap;

import rise.lib.utils.Utils;
import rise.lib.utils.log.RiseLog;


/**
 * Object Representation of the HostConfig sub payload to send to the Docker API 
 * to create a new container.
 * 
 * Given some "strange" features it is serialized in JSON by a custom object.
 * 
 * Refer to this documentation:
 * https://docs.docker.com/engine/api/v1.43/#tag/Container/operation/ContainerCreate
 */

public class HostConfigParam {

	public String NetworkMode =  "default";
	public boolean PublishAllPorts = false;
	public boolean Privileged = false;
	public boolean ReadonlyRootfs = false;
	public boolean AutoRemove = false;
	
	public boolean EnableGpu = false;
	
	public HashMap<String, String> PortBindings = new HashMap<String, String>();
	public ArrayList<String> Links = new ArrayList<>();
	public ArrayList<String> Binds = new ArrayList<>();
	public ArrayList<String> ExtraHosts = new ArrayList<>();
	public ArrayList<MountVolumeParam> Mounts = new ArrayList<>();
	public HashMap<String, Object> RestartPolicy = new HashMap<String, Object>();
	
	public String toJson() {
		String sReturn = "{\n";
		
		try {
			
			boolean bAdded = false;
			
			if (NetworkMode != null) {
				sReturn += "\"NetworkMode\": \"" + NetworkMode + "\"";
				bAdded = true;
			}			
			
			if (bAdded) sReturn += ",\n";
			
			sReturn += "\"PublishAllPorts\": " + PublishAllPorts + ",\n";
			sReturn += "\"Privileged\": " + Privileged + ",\n";
			sReturn += "\"ReadonlyRootfs\": " + ReadonlyRootfs + ",\n";
			sReturn += "\"AutoRemove\": " + AutoRemove + ",\n";
			
			sReturn += "\"PortBindings\": {";			
			if (PortBindings.size()>0) {
				for (String sKey : PortBindings.keySet()) {
					sReturn += "\"" +sKey +"\": [ { \"HostIp\":\"127.0.0.1\",\n \"HostPort\": \"" + PortBindings.get(sKey) + "\" } ],";
				}				
				
				sReturn = sReturn.substring(0, sReturn.length()-1);
			}
			sReturn += "\n},\n";
			
			
			if (Links.size()>0) {
				sReturn += "\"Links\": [ ";			
				for (int iLinks = 0; iLinks<Links.size(); iLinks++) {
					String sLink = Links.get(iLinks);
					sReturn += "\"" + sLink + "\"";
					if (iLinks<Links.size()-1) sReturn +=",\n";
				}
				sReturn += "],\n";				
			}
			else {
				sReturn += "\"Links\": null,\n";
			}
			
			if (Binds.size()>0) {
				sReturn += "\"Binds\": [ ";
				for (int iBinds = 0; iBinds<Binds.size(); iBinds++) {
					String sBind = Binds.get(iBinds);
					sReturn += "\"" + sBind.replace("\\", "\\\\") + "\"";
					if (iBinds<Binds.size()-1) sReturn +=",\n";
				}
				sReturn += "],\n";				
			}
			else {
				sReturn += "\"Binds\": null,\n";
			}
			
			sReturn += "\"ExtraHosts\": [ ";
			for (int iExtraHosts = 0; iExtraHosts<ExtraHosts.size(); iExtraHosts++) {
				String sHost = ExtraHosts.get(iExtraHosts);
				sReturn += "\"" + sHost + "\"";
				if (iExtraHosts<ExtraHosts.size()-1) sReturn +=",\n";
			}
			sReturn += "],\n";			
			
			sReturn += "\"Mounts\": [ ";
			for (int iMounts = 0; iMounts<Mounts.size(); iMounts++) {
				MountVolumeParam oMount = Mounts.get(iMounts);
				String sMount = oMount.toJson();
				
				if (!Utils.isNullOrEmpty(sMount)) {
					sReturn += sMount;
					if (iMounts<Mounts.size()-1) sReturn +=",\n";
				}
			}
			sReturn += "]";
			
			if (EnableGpu) {
				sReturn += ",\n\"DeviceRequests\": [\n";
				sReturn += "{\n";
				sReturn += "\"Driver\": \"nvidia\",\n\"Count\": -1,\n";
				sReturn += "\"Capabilities\": [\n";
				sReturn += "[";
				sReturn += "\"gpu\",\n";
				sReturn += "\"nvidia\",\n";
				sReturn += "\"compute\"\n";
				sReturn += "]\n";
				sReturn += "],\n";
				sReturn += "\"Options\":{}\n";
				sReturn += "}\n";
				sReturn += "]";
			}
			
			
			if (RestartPolicy.size()>0) {
				sReturn += ",\n\"RestartPolicy\": {";
				
				for (String sKey : RestartPolicy.keySet()) {
					
					if (RestartPolicy.get(sKey) instanceof String) {
						sReturn += "\"" +sKey +"\": \"" + RestartPolicy.get(sKey) + "\",";
					}
					else {
						sReturn += "\"" +sKey +"\": " + RestartPolicy.get(sKey) + ",";
					}
					
				}				
				
				sReturn = sReturn.substring(0, sReturn.length()-1);
				
				sReturn += "\n}\n";
			}
			else {
				sReturn += "\n";	
			}
			
		}
		catch (Exception oEx) {
			RiseLog.errorLog("ContainerHostConfigParam.toJson: Exception Converting ContainerHostConfigParam to JSON" ,  oEx);
			return null;
		}
		
		sReturn += "}\n";
		
		return sReturn;		
	}
	
	

}
