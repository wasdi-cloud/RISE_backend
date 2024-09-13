package rise.lib;

import rise.lib.business.Organization;
import rise.lib.business.Session;
import rise.lib.business.User;
import rise.lib.data.SessionRepository;
import rise.lib.data.UserRepository;
import rise.lib.utils.Utils;
import rise.lib.utils.log.RiseLog;
import rise.lib.viewmodels.OrganizationViewModel;
import rise.lib.viewmodels.RiseViewModel;

public class RiseLib {
	
	
	public static void main(String[] args) {
		
		Organization oOrg = new Organization();
		oOrg.setName("WASDI");
		oOrg.setCity("Dudelange");
		oOrg.setCreationDate(123.0);
		oOrg.setId("id");
		oOrg.setNumber("1");
		oOrg.setPhone("+3935688");
		oOrg.setPostalCode("16100");
		oOrg.setStreet("Rue de Volmerange");
		oOrg.setType("Commercial");
		oOrg.setVat("VAT");
		
		OrganizationViewModel oVM = (OrganizationViewModel) OrganizationViewModel.getFromEntity(OrganizationViewModel.class.getName(), oOrg);
		
		RiseLog.debugLog(oVM.toString());
		
		Organization oTest = (Organization) RiseViewModel.copyToEntity(Organization.class.getName(), oVM);
		
		RiseLog.debugLog(oTest.toString());
	}
	
	public static User getUserFromSession(String sToken) {
		
		try {
			if (Utils.isNullOrEmpty(sToken)) return null;
			
			SessionRepository oSessionRepository = new SessionRepository();
			Session oSession = oSessionRepository.getSession(sToken);
			
			if (oSession == null) return null;
			
			UserRepository oUserRepo = new UserRepository();
			User oUser = oUserRepo.getUser(oSession.getUserId());
			
			if (oUser == null) return null;
			
			return oUser;
		}
		catch (Exception oEx) {
			RiseLog.errorLog("Rise.getUserFromSession: exception " + oEx.toString());
		}		
		
		return null;
	}
}
