package rise.lib.config;

import java.util.ArrayList;

/**
 * Represent an external program or command that can be executed by RISE
 * In the standard installation each command is executed with a Shell Execute
 */

public class ShellExecItemConfig {
	
	/**
	 * Name of the docker images to use instead of the command
	 */
	public String dockerImage;
	
	/**
	 * Version of the docker image to use
	 */
	public String containerVersion;
	
	/**
	 * True by default, all the parts of the command line are passed as docker args
	 * If false, the first element of the shell execute will NOT be passed to the docker command line
	 */
	public boolean includeFirstCommand=true;
	
	/**
	 * If true, WASDI will remove the path (if present) from the first command in the arg list
	 */
	public boolean removePathFromFirstArg = false;
	
	/**
	 * Here we can add a prefix that will be added as arg[0] of our shell execute
	 */
	public String addPrefixToCommand = "";

	/**
	 * False by default. If it is set to true, the command is executed locally also if WASDI 
	 * is configured to be dockerized.
	 */
	public boolean forceLocal = false;
	
	/**
	 * List of strings that are additional moung points for this specific docker
	 */
	public ArrayList<String> additionalMountPoints = new ArrayList<>();
	
	/**
	 * Flag to enable the config of system/docker Config (User and Group Name/Id, network..) using the specific values
	 * set for this docker 
	 */
	public boolean overrideDockerConfig = false;
	
	/**
	 * System name of the wasdi user
	 */
	public String systemUserName = "appwasdi";
	
	/**
	 * Id of the system user
	 */
	public Integer systemUserId = 2042;
	
	/**
	 * System name of the wasdi group
	 */
	public String systemGroupName = "appwasdi";
	
	/**
	 * Id of the system group
	 */
	public Integer systemGroupId = 2042;
	
	/**
	 * Docker network mode
	 */
	public String dockerNetworkMode = "net-wasdi";
}
