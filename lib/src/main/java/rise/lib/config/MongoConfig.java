package rise.lib.config;

/**
 * Mongo Database Configuration
 * @author p.campanella
 *
 */
public class MongoConfig {
	
	/**
	 * Server Address
	 */
	public String address;
	
	/**
	 * Server Port
	 */
	public int port;
	
	/**
	 * Database name
	 */
	public String dbName;
	
	/**
	 * Db user
	 */
	public String user;
	
	/**
	 * Db Password
	 */
	public String password;
	
	/**
	 * Replica Name
	 */
	public String replicaName;
	
	/**
	 * Flag for local mongo debug. Set it true to debug locally a mongo cluster with an ssh tunnel
	 */
	public boolean directConnection = false;
}
