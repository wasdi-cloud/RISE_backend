package rise.lib.config;


/**
 * Represent an external program or command that can be executed by RISE
 * In the standard installation each command is executed with a Shell Execute
 *
 */
public class ShellExecItemConfig {
	
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

		
}
