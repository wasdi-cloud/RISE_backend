package rise.lib.config;

/**
 * Configuration of the notifications mail sent by WASDI to users.
 * @author p.campanella
 *
 */
public class NotificationsConfig {
	
	/**
	 * Address of the Mercurius service. 
	 * Mercurius is a CIMA service API to send e-mails.
	 */
	public String mercuriusAPIAddress;
	
	/**
	 * Declared WASDI admin mail
	 */
	public String riseAdminMail;
}
