package rise.lib.config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import rise.lib.data.MongoRepository;
import rise.lib.utils.log.RiseLog;

public class RiseConfig {
	
	public MongoConfig mongoMain;
	
	
	public static RiseConfig Current;
	
	
	/**
	 * Read the config from file
	 * @param sConfigFilePath json file path
	 * @return true if ok, false in case of problems
	 */
	public static boolean readConfig(String sConfigFilePath) {
		Stream<String> oLinesStream = null;
		boolean bRes = false;
		
        try {
        	
        	oLinesStream = Files.lines(Paths.get(sConfigFilePath), StandardCharsets.UTF_8);
			String sJson = oLinesStream.collect(Collectors.joining(System.lineSeparator()));
			Current = MongoRepository.s_oMapper.readValue(sJson,RiseConfig.class);
			//Current.paths.wasdiConfigFilePath = sConfigFilePath;
			bRes = true;
			
		} catch (Exception oEx) {
			RiseLog.errorLog("WasdiConfig.readConfig: exception ", oEx);
		} finally {
			if (oLinesStream != null) 
				oLinesStream.close();
		}
        
        return bRes;
	}	
}
