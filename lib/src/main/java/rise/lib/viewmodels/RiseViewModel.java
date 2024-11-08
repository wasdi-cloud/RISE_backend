package rise.lib.viewmodels;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import rise.lib.business.RiseEntity;
import rise.lib.utils.Utils;
import rise.lib.utils.log.RiseLog;

/**
 * Parent of all the RISE View Models
 */
public class RiseViewModel {
	
	public RiseViewModel() {
		
	}
	
	/**
	 * Find the setter method starting from the name of the corresponding 
	 * getter method
	 * @param aoMethods
	 * @param sGetterName
	 * @return
	 */
	protected Method getSetterFromGetter(Method [] aoMethods, String sGetterName)  {
		try {
			
			for(int iMethods=0;iMethods<aoMethods.length;iMethods++){
			    String sMethodName = aoMethods[iMethods].getName();
			    
			    if (sMethodName.equals(sGetterName)) {
			    	String sSetterName = "";
			    	if (sGetterName.startsWith("is")) {
			    		sSetterName = sGetterName.replace("is", "set");	
			    	}
			    	else if (sGetterName.startsWith("get"))  {
			    		sSetterName = sGetterName.replace("set", "set");
			    	}
			    	
			    	if (!Utils.isNullOrEmpty(sSetterName)) {
			    		
			    		for(int iInnerMethods=0;iInnerMethods<aoMethods.length;iInnerMethods++){
			    			
			    			if (aoMethods[iInnerMethods].getName().equals(sSetterName)) {
			    				return aoMethods[iInnerMethods];
			    			}
			    		}
			    	}
			    }
			}
		}
		catch (Exception oEx) {
			RiseLog.errorLog("RiseViewModel.getSetterFromGetter: error " + oEx.toString());
		}
		
		return null;
	}
	
	/**
	 * Fills automatically the fields of the view models that fits with equivalent getter methods from the Entity
	 * @param sViewModelClass name (with classpath) of the View Model Class
	 * @param oEntity Instance of the entity to copy fields from
	 * @return An Instance of the View Model with the fields that match naming convention filled
	 */
	public static Object getFromEntity(String sViewModelClass, RiseEntity oEntity) {
		
		try {
			Object oViewModel = (Object) Class.forName(sViewModelClass).getDeclaredConstructor().newInstance();
			
			Method aoEntityMethods[]= oEntity.getClass().getDeclaredMethods();
			
			Field aoViewModelProperties[]= oViewModel.getClass().getDeclaredFields();
			
			for (Field oField : aoViewModelProperties) {
				
				String sFiledName = oField.getName();
				
				// Looks not needed until now at least
				//Class oFieldType = oField.getType();
				
				for(int iMethods=0;iMethods<aoEntityMethods.length;iMethods++){
				    String sMethodName = aoEntityMethods[iMethods].getName();
				    
				    if (sMethodName.startsWith("get") || sMethodName.startsWith("is")) {
				    	String sUpperCaseMethod = sMethodName.toUpperCase();
				    	
				    	//to avoid any error or conflicts like surname and name or first name and last name ..ect
				    	String sFieldGetterName= sMethodName.startsWith("get")?"GET"+sFiledName.toUpperCase():"IS"+sFiledName.toUpperCase();
				    	
				    	if (sUpperCaseMethod.equals(sFieldGetterName)) {
				    		try {
				    			oField.set(oViewModel, aoEntityMethods[iMethods].invoke(oEntity));
				    		}
				    		catch (Exception oInEx) {
				    			RiseLog.errorLog("RiseViewModel.getFromEntity: error setting value to view model " + oField.getName() + " , jump to next");
							}
				    		
				    		break;
				    		
				    	}
				    }
				}				
			}
			
			return oViewModel;
		} 
		catch (Exception oEx) {
			RiseLog.errorLog("RiseViewModel.getFromEntity: error " + oEx.toString());
		}
		
		return null;
	}
	
	/**
	 * Fills automatically the fields of the Entity that fits with equivalent fields in the View Model
	 * @param sEntityClass name (with class path) of the Entity Class
	 * @param oViewModel Instance of the entity to copy fields from
	 * @return An Instance of the Entity with the fields that match naming convention filled
	 */
	public static Object copyToEntity(String sEntityClass, Object oViewModel) {
		
		try {
			Object oEntity = (Object) Class.forName(sEntityClass).getDeclaredConstructor().newInstance();
			
			Method aoEntityMethods[]= oEntity.getClass().getDeclaredMethods();
			
			Field aoViewModelProperties[]= oViewModel.getClass().getDeclaredFields();
			
			for (Method oMethod : aoEntityMethods) {
				String sMethodName = oMethod.getName();
				
				if (sMethodName.startsWith("set")) {
					for (Field oField : aoViewModelProperties) {
						
						String sFiledName = oField.getName();
						
						String sMethodFieldName = sMethodName.substring(3);
						
						sMethodFieldName = sMethodFieldName.substring(0, 1).toLowerCase() + sMethodFieldName.substring(1);
						
						if (sMethodFieldName.equals(sFiledName)) {
							
							try {
								oMethod.invoke(oEntity, oField.get(oViewModel));	
							}
				    		catch (Exception oInEx) {
				    			RiseLog.errorLog("RiseViewModel.copyToEntity: error setting value to Entity " + oField.getName() + " , jump to next");
							}
							
							break;
						}
					}
				}
			}
			
			return oEntity;
		} 
		catch (Exception oEx) {
			RiseLog.errorLog("RiseViewModel.copyToEntity: error " + oEx.toString());
		}
		
		return null;
	}	

}
