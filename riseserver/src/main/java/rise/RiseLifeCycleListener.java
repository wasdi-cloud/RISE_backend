package rise;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import rise.lib.utils.log.RiseLog;

@WebListener
public class RiseLifeCycleListener implements ServletContextListener {
	

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		RiseLog.debugLog("RiseLifeCycleListener.contextInitialized");
	}
	
	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		RiseLog.debugLog("Calling Rise Shut Down");
		Rise.shutDown();
	}
}
