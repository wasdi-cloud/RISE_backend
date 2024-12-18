package rise.lib.utils.mail;

import java.util.ArrayList;

import it.fadeout.mercurius.business.Message;
import it.fadeout.mercurius.client.MercuriusAPI;
import rise.lib.config.RiseConfig;
import rise.lib.data.MongoRepository;
import rise.lib.utils.Utils;
import rise.lib.utils.http.HttpCallResponse;
import rise.lib.utils.http.HttpUtils;
import rise.lib.utils.log.RiseLog;

public class MailUtils {
	
	/**
	 * Send an email from deafault sender to recipient with title and message
	 * @param sRecipient Recipient 
	 * @param sTitle Title
	 * @param sMessage Message
	 * @return true if sent false otherwise
	 */
	public static boolean sendEmail(String sRecipient, String sTitle, String sMessage) {
		return sendEmail(RiseConfig.Current.notifications.riseAdminMail, sRecipient, sTitle, sMessage);
	}
	
	/**
	 * Send an email from sender to recipient with title and message
	 * @param sSender Sender of the mail
	 * @param sRecipient Recipient 
	 * @param sTitle Title
	 * @param sMessage Message
	 * @return true if sent false otherwise
	 */
	public static boolean sendEmail(String sSender, String sRecipient, String sTitle, String sMessage) {
		return sendEmail(sSender, sRecipient, sTitle, sMessage, false);
	}
	
	/**
	 * Send an email from sender to recipient with title and message
	 * @param sSender Sender of the mail
	 * @param sRecipient Recipient 
	 * @param sTitle Title
	 * @param sMessage Message
	 * @param bAddAdminToRecipient Set true to add by default the WADSI admin to the recipient
	 * @return true if sent false otherwise
	 */
	public static boolean sendEmail(String sSender, String sRecipient, String sTitle, String sMessage, boolean bAddAdminToRecipient) {
		
		if (RiseConfig.Current.notifications.useMailJet) {
			return sendEmailMailJet(sSender, sRecipient, sTitle, sMessage, bAddAdminToRecipient);
		}
		else {
			return sendEmailMercurius(sSender, sRecipient, sTitle, sMessage, bAddAdminToRecipient);
		}
	}
	
	protected static boolean sendEmailMercurius(String sSender, String sRecipient, String sTitle, String sMessage, boolean bAddAdminToRecipient) {
		
		try {
			String sMercuriusAPIAddress = RiseConfig.Current.notifications.mercuriusAPIAddress;

			if(Utils.isNullOrEmpty(sMercuriusAPIAddress)) {
				RiseLog.debugLog("MailUtils.sendEmailMercurius: mercuriusAPIAddress is null");
				return false;
			} else {
				RiseLog.debugLog("MailUtils.sendEmailMercurius: send notification");

				MercuriusAPI oAPI = new MercuriusAPI(sMercuriusAPIAddress);			
				Message oMessage = new Message();

				oMessage.setTilte(sTitle);
				
				if (Utils.isNullOrEmpty(sSender)) {
					sSender = RiseConfig.Current.notifications.riseAdminMail;
					if (Utils.isNullOrEmpty(sSender)) {
						sSender = "info@wasdi.net";
					}
				}				
				
				oMessage.setSender(sSender);

				oMessage.setMessage(sMessage);

				Integer iPositiveSucceded = 0;
				
				String sWasdiAdminMail = sRecipient;

				if (!Utils.isNullOrEmpty(RiseConfig.Current.notifications.riseAdminMail) && bAddAdminToRecipient) {
					sWasdiAdminMail += ";" + RiseConfig.Current.notifications.riseAdminMail;
				}

				iPositiveSucceded = oAPI.sendMailDirect(sWasdiAdminMail, oMessage);
				

				if(iPositiveSucceded > 0 ) {
					RiseLog.debugLog("MailUtils.sendEmailMercurius: notification sent with result " + iPositiveSucceded);
					return true;
				}				
				else {
					RiseLog.debugLog("MailUtils.sendEmailMercurius: notification NOT sent with result " + iPositiveSucceded);
					return false;
				}				
			}
		} catch (Exception oEx) {
			RiseLog.errorLog("MailUtils.sendEmailMercurius: notification exception " + oEx.toString());
		}
		
		return false;
			
	}
	
	protected static boolean sendEmailMailJet(String sSender, String sRecipient, String sTitle, String sMessage, boolean bAddAdminToRecipient) {
		try {
			
			// Create the payload for Mail Jet
			MJPayload oPayload = new MJPayload();
			
			// The message
			MJMessage oMessage = new MJMessage();
			
			// The sender
			MJRecipient oFrom = new MJRecipient();
			oFrom.Email = sSender;
			oFrom.Name = sSender;
			
			// Assign sender to message
			oMessage.From = oFrom;
			
			// We may have more recipient
			ArrayList<String> asTo = new ArrayList<>();
			String [] asSplitted = sRecipient.split(";");
			
			// Get  list 
			for (String sSplit : asSplitted) {
				asTo.add(sSplit);
			}
			
			// Do we need to add admin?
			if (bAddAdminToRecipient) asTo.add(RiseConfig.Current.notifications.riseAdminMail);
			
			for (String sMail : asTo) {
				// Create the recipient
				MJRecipient oRecipient = new MJRecipient();
				oRecipient.Email = sMail;
				oRecipient.Name = sMail;
				// add to the message
				oMessage.To.add(oRecipient);
			}
			
			// Set title and body
			oMessage.Subject = sTitle;
			oMessage.HTMLPart = sMessage;
			
			// Add the message 
			oPayload.Messages.add(oMessage);
			
			// Serialize
			String sJSON = MongoRepository.s_oMapper.writeValueAsString(oPayload);
			
			RiseLog.debugLog("MailUtils.sendEmailMailJet: JSON for MailJet " + sJSON);
			
			// Basic Auth
			String sAuth = RiseConfig.Current.notifications.mailJetUser + ":" + RiseConfig.Current.notifications.mailJetPassword;
			
			// Call the service	
			HttpCallResponse oHttpResponse = HttpUtils.httpPost(RiseConfig.Current.notifications.mailJetSendAPI, sJSON, null, sAuth);
			
			if (oHttpResponse.getResponseCode()>=200 && oHttpResponse.getResponseCode()<=299) {
				RiseLog.infoLog("MailUtils.sendEmailMailJet: mail sent");
				return true;	
			}
			else {
				RiseLog.errorLog("MailUtils.sendEmailMailJet: mail NOT sent " + oHttpResponse.getResponseBody());
				return false;
			}
		} 
		catch (Exception oEx) {
			RiseLog.errorLog("MailUtils.sendEmailMailJet: notification exception " + oEx.toString());
		}
		
		return false;
	}
}
