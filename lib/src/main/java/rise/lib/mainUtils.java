package rise.lib;

import java.util.List;
import java.util.Scanner;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import rise.lib.business.Layer;
import rise.lib.config.RiseConfig;
import rise.lib.data.LayerRepository;
import rise.lib.data.MongoRepository;
import rise.lib.utils.GeoServerManager;
import rise.lib.utils.log.RiseLog;

public class mainUtils {
	
	private static Scanner s_oScanner;
	
	public static void areas() {
		
        try {

            System.out.println("Ok, what we do with areas?");

            System.out.println("\t1 - Clean area layers in geoserver");
            System.out.println("\tx - back");
            System.out.println("");

            String sInputString = s_oScanner.nextLine();

            if (sInputString.equals("x")) {
                return;
            }

            if (sInputString.equals("1")) {
            	
            	System.out.println("\tInsert the AREA ID:");
                String sAreaId = s_oScanner.nextLine();
                
                LayerRepository oLayerRepository = new LayerRepository();
                List<Layer> aoAreaLayers = oLayerRepository.getLayerByArea(sAreaId);
                
                if (aoAreaLayers != null) {
                	
                	GeoServerManager oGeoServerManager = new GeoServerManager();
                	
                	for (Layer oLayer : aoAreaLayers) {
                		oGeoServerManager.removeLayer(oLayer.getId());
					}
                }

                System.out.println("All area layers cleaned");
            }

        } catch (Exception oEx) {
            System.out.println("Areas Exception: " + oEx);
        }
		
	}

	public static void main(String[] args) {
		
		RiseLog.debugLog("----------- Welcome to RISE Remote Imaging Support for Emergencies 0.8.1 -----------");

		String sConfigFilePath = "/etc/rise/riseConfig.json";
				
        // create the parser
        CommandLineParser oParser = new DefaultParser();

        // create Options object
        Options oOptions = new Options();

        oOptions.addOption("c", "config", true, "RISE Configuration File Path");
        oOptions.addOption("hello", "hello", false, "Command to just check the hello mode");

        // parse the command line arguments
        CommandLine oLine;
		try {
			oLine = oParser.parse(oOptions, args);

	        if (oLine.hasOption("config")) {
	            // Get the Parameter File
	        	sConfigFilePath = oLine.getOptionValue("config");
	        }
	        
		} 
		catch (ParseException oEx) {
			oEx.printStackTrace();
			return;
		}
    			
		if (!RiseConfig.readConfig(sConfigFilePath)) {
			RiseLog.debugLog("ERROR IMPOSSIBLE TO READ CONFIG FILE IN " + sConfigFilePath);
		}
		else {
			RiseLog.debugLog("READ CONFIG FILE " + sConfigFilePath);
		}
		
		// Read MongoDb Configuration
		try {

            MongoRepository.readConfig();

            RiseLog.debugLog("-------Mongo db User " + MongoRepository.DB_USER);

		} catch (Throwable oEx) {
			RiseLog.errorLog("Read MongoDb Configuration exception " + oEx.toString());
		}
		
		
        if (oLine.hasOption("hello")) {
        	System.out.println("---- RISE db Utils HELLO MODE!!!----");
        	System.out.println("Bye bye");
        }
        else {
            boolean bExit = false;

            s_oScanner = new Scanner(System.in);

            while (!bExit) {
                System.out.println("---- RISE db Utils ----");
                System.out.println("Welcome, how can I help you?");

                System.out.println("\t1 - Area");
                System.out.println("\tx - Exit");
                System.out.println("");


                String sInputString = s_oScanner.nextLine();

                if (sInputString.equals("1")) {
                    areas();
                } 
                else if (sInputString.toLowerCase().equals("x")) {
                    bExit = true;
                } else {
                    System.out.println("Please select a valid option or x to exit");
                    System.out.println("");
                    System.out.println("");
                }
            }
            
            s_oScanner.close();
        }
	}
}
