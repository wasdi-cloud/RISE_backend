package rise.lib.config;

import java.io.File;

import rise.lib.utils.Utils;

public class PathsConfig {
	
	public String riseTempFolder;
	
	/**
	 * Base root path that contains subfolders:
	 * 	.workspaces
	 * 	.metadata
	 * 	.styles
	 * 	.workflows
	 * 	.processors
	 *  .images
	 */
	public String downloadRootPath;
	
	public String scriptsPath = "/home/appwasdi/";
	
	
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
