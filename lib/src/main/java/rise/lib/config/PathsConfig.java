package rise.lib.config;

import java.io.File;

import rise.lib.utils.Utils;

public class PathsConfig {
	
	public String riseTempFolder;
	
	/**
	 * Base root path that contains subfolders:
	 */
	
	public String downloadRootPath="/data/wasdi/";
	
	public String scriptsPath = "/home/appwasdi/scripts";
	
	public String pythonPath = "python3";
	
	public String attachmentsBasePath = "/data/wasdi/rise-images/";
	
	/**
	 * My own path
	 */
	public String riseConfigPath = "";	
	
	
	public static String getRiseBasePath() {
		// Take path
		String sRiseBasePath = RiseConfig.Current.paths.downloadRootPath;

		if (Utils.isNullOrEmpty(sRiseBasePath)) {
			sRiseBasePath = "/data/wasdi/";
		}

		if (!sRiseBasePath.endsWith(File.separator)) {
			sRiseBasePath = sRiseBasePath + File.separator;
		}

		return sRiseBasePath;
	}
	
}
