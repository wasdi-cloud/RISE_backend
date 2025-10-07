package rise.threads;

import rise.lib.config.RiseConfig;
import rise.lib.utils.log.RiseLog;
import wasdi.jwasdilib.WasdiLib;

import java.util.List;
import java.util.Map;

public class DeleteAreaWorkspacesWorker extends Thread{



    String m_sAreaId;

    /**
     * Initializes the thread members' variables.
     */
    public void init(String sAreaId) {
        m_sAreaId = sAreaId;

    }


    /**
     * Starts the thread: will try to call the
     * delete Workspaces from wasdi
     */

    @Override
    public void run() {
        RiseLog.debugLog("DeleteAreaWorkspaces.run : start deleting workspaces from wasdi for area"+ m_sAreaId);
        // Clean all the workspaces related to that area
        WasdiLib oWasdiLib = new WasdiLib();
        oWasdiLib.setUser(RiseConfig.Current.wasdiConfig.wasdiUser);
        oWasdiLib.setPassword(RiseConfig.Current.wasdiConfig.wasdiPassword);
        if (oWasdiLib.init()) {
            List<Map<String, Object>> aoWorkspaces = oWasdiLib.getWorkspaces();
            for (Map<String, Object> oWorkspace : aoWorkspaces) {
                Object oName = oWorkspace.get("workspaceName");
                Object oId = oWorkspace.get("workspaceId");

                if (oName instanceof String && oId instanceof String) {
                    String sName = (String) oName;
                    String sId = (String) oId;

                    if (sName.startsWith(m_sAreaId)) {
                        // Found a workspace that starts with the area ID
                        // You can delete or process it here
                        RiseLog.infoLog("AreaResource.deleteArea: Deleting WASDI Workspace " + sName + "with the Id : " + sId);

                        // Example: delete the workspace
                        oWasdiLib.deleteWorkspace(sId);
                    }
                }
            }
        }



    }
}
