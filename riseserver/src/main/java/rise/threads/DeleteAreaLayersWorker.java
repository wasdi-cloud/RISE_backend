package rise.threads;

import rise.lib.business.Layer;
import rise.lib.utils.GeoServerManager;
import rise.lib.utils.log.RiseLog;

import java.net.MalformedURLException;
import java.util.List;

/**
 * Thread that calls all the computing nodes to ask to delete a processor
 * @author jihed-Sh
 *
 */

public class DeleteAreaLayersWorker extends Thread {


    /**
     * List of RISE area layers to delete
     */
    List<Layer> m_aoLayers;


    String m_sAreaId;

    /**
     * Initializes the thread members' varialbes.
     * @param aoLayers List of Layers
     */
    public void init(List<Layer> aoLayers,String sAreaId) {
        m_aoLayers = aoLayers;
        m_sAreaId = sAreaId;
    }


    /**
     * Starts the thread: will try to call the
     * geoserver manager to delete the layers
     */

    @Override
    public void run() {
        RiseLog.debugLog("DeleteAreaLayersWorker.run : start layers delete from geoserver for area"+ m_sAreaId);
        try {
            GeoServerManager oGeoServerManager=new GeoServerManager();
            for (Layer oLayer : m_aoLayers) {
                oGeoServerManager.removeLayer(oLayer.getId());
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }


    }
}
