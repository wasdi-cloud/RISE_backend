package rise.lib.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;

import rise.lib.config.RiseConfig;
import rise.lib.config.ShellExecItemConfig;
import rise.lib.utils.log.RiseLog;

public class RunTimeUtils {
	
	/**
	 * Execute a system task waiting the process to finish without collecting the logs
	 * It logs the command line executed on the launcher logs
	 *  
	 * @param asArgs List of strings that represents the command and the list of arguments
	 * @return True if the process is executed
	 */
	public static ShellExecReturn shellExec(List<String> asArgs) {
		return shellExec(asArgs,true);
	}
	
	/**
	 * Execute a system task without collecting the logs
	 * It logs the command line executed on the launcher logs
	 * 
	 * @param asArgs List of strings that represents the command and the list of arguments 
	 * @param bWait True if the method should wait the shell exec to finish, false otherwise
	 * @return 
	 */
	public static ShellExecReturn shellExec(List<String> asArgs, boolean bWait) {
		return shellExec(asArgs, bWait, false);
	}
	
	/**
	 * Execute a system task 
	 * It does not redirect the error streamn to std out
	 * It logs the command line executed on the launcher logs
	 * 
	 * @param asArgs
	 * @param bWait
	 * @param bReadOutput
	 * @return
	 */
	public static ShellExecReturn shellExec(List<String> asArgs, boolean bWait, boolean bReadOutput) {
		return shellExec(asArgs, bWait, bReadOutput, false);
	}
	
	/**
	 * Execute a system task
	 * It logs the command line executed on the launcher logs
	 * 
	 * @param asArgs
	 * @param bWait
	 * @param bReadOutput
	 * @param bRedirectError
	 * @return
	 */
	public static ShellExecReturn shellExec(List<String> asArgs, boolean bWait, boolean bReadOutput, boolean bRedirectError) {
		return shellExec(asArgs, bWait, bReadOutput, bRedirectError, false);
	}

	/**
	 * Execute a system task
	 * 
	 * @param asArgs List of arguments
	 * @param bWait True to wait the process to finish, false to not wait
	 * @param bReadOutput True to read the output of the process
	 * @param bRedirectError True to redirect also the error stream
	 * @param bLogCommandLine  True to log the command line false to jump
	 * @return True if the process is executed
	 */
	public static ShellExecReturn shellExec(List<String> asArgs, boolean bWait, boolean bReadOutput, boolean bRedirectError, boolean bLogCommandLine) {
		
		// Check if we need to log the command line or not
		if (bLogCommandLine) {
			logCommandLine(asArgs);
		}
		
		return localShellExec(asArgs, bWait, bReadOutput, bRedirectError, bLogCommandLine);

	}

	/**
	 * Execute a system task using the local (hosting) system
	 * 
	 * @param asArgs List of arguments
	 * @param bWait True to wait the process to finish, false to not wait
	 * @param bLogCommandLine  True to log the command line false to jump
	 * @return True if the process is executed
	 */
	private static ShellExecReturn localShellExec(List<String> asArgs, boolean bWait, boolean bReadOutput, boolean bRedirectError, boolean bLogCommandLine) {
		
		// Shell exec return object
		ShellExecReturn oReturn = new ShellExecReturn();
		
		// Set the asynch flag
		oReturn.setAsynchOperation(!bWait);
		
		try {
			// We need args!
			if (asArgs==null) {
				RiseLog.errorLog("RunTimeUtils.localShellExec: Args are null");
				return oReturn;
			}
			if (asArgs.size() == 0) {
				RiseLog.errorLog("RunTimeUtils.localShellExec: Args are empty");
				return oReturn;
			}
			
			// If this is asynch, we cannot collect logs
			if (bWait == false) { 
				if (bReadOutput || bRedirectError) {
					bReadOutput = false;
					bRedirectError = false;
					RiseLog.warnLog("RunTimeUtils.localShellExec: running with bWait = false, we cannot collect logs. Forcing ReadOutput and RedirectError to false (at least once was true!)");
				}
			}
			
			if (bRedirectError==true && bReadOutput==false) {
				bRedirectError = false;
				RiseLog.warnLog("RunTimeUtils.localShellExec: RedirectError makes no sense if we do not read output. Forcing to false");
			}
						
			// If we want to collect the logs, create the temp file
			File oLogFile = null;
			if (bReadOutput) oLogFile = createLogFile();
			
			// Get the shell exec item
			ShellExecItemConfig oShellExecItem = RiseConfig.Current.getShellExecItem(asArgs.get(0)); // TODO
			
			if (oShellExecItem != null) {
				// Check if we need to add a prefix to our command
				if (!Utils.isNullOrEmpty(oShellExecItem.addPrefixToCommand)) {
					RiseLog.debugLog("RunTimeUtils.localShellExec: adding prefix to the command line - " + oShellExecItem.addPrefixToCommand);
					asArgs.add(0, oShellExecItem.addPrefixToCommand);
				}
			}
			
			// Create the process builder
			ProcessBuilder oProcessBuilder = new ProcessBuilder(asArgs.toArray(new String[0]));
			
			if (bRedirectError) oProcessBuilder.redirectErrorStream(true);
			if (bReadOutput) oProcessBuilder.redirectOutput(oLogFile);
			
			Process oProcess = oProcessBuilder.start();
			
			if (bWait) {
				try {
					int iProcOuptut = oProcess.waitFor();
					oReturn.setOperationReturn(iProcOuptut);
					RiseLog.debugLog("RunTimeUtils.localShellExec CommandLine RETURNED: " + iProcOuptut);
				} 
				catch (InterruptedException oEx) {
					Thread.currentThread().interrupt();
					RiseLog.errorLog("RunTimeUtils.localShellExec: process was interrupted");
					return oReturn;
				}
				
				if (bReadOutput) {
					String sOutputFileContent = readLogFile(oLogFile);
					oReturn.setOperationLogs(sOutputFileContent);
					deleteLogFile(oLogFile);					
				}	
				
			}
			
			oReturn.setOperationOk(true);
			
			return oReturn;
		}
		catch (Exception oEx) {
			RiseLog.errorLog("RunTimeUtils.localShellExec exception: ", oEx);
			return oReturn;
		}		
	}
	

	/**
	 * Creates a temporary log file
	 * @return
	 */
	private static File createLogFile() {
		String sTmpFolder = RiseConfig.Current.paths.riseTempFolder;
		if (!sTmpFolder.endsWith("/")) sTmpFolder += "/";

		File oLogFile = new File(sTmpFolder + Utils.getRandomName() + ".log");

		return oLogFile;
	}
	
	/**
	 * Logs the command line
	 * @param asArgs
	 */
	protected static void logCommandLine(List<String> asArgs) {
		// Initialize the command line
		String sCommandLine = "";
		
		if (asArgs!=null) {
			for (String sArg : asArgs) {
				sCommandLine += sArg + " ";
			}			
		}
				
		// and log it
		RiseLog.debugLog("RunTimeUtils.logCommandLine CommandLine: " + sCommandLine);		
	}
	
	/**
	 * Reads a temporary log file
	 * @param oLogFile
	 * @return
	 */
	private static String readLogFile(File oLogFile) {
		try {
			return FileUtils.readFileToString(oLogFile, StandardCharsets.UTF_8);
		} catch (IOException oEx) {
			RiseLog.errorLog("RunTimeUtils.readLogFile exception: " + oEx.getMessage());
		}

		return null;
	}
	
	/**
	 * Deletes a temporary log file
	 * @param oLogFile
	 */
	private static void deleteLogFile(File oLogFile) {
		try {
			FileUtils.forceDelete(oLogFile);
		} catch (IOException e) {
			RiseLog.errorLog("RunTimeUtils.deleteLogFile exception: " + e.getMessage());
		}
	}

}
