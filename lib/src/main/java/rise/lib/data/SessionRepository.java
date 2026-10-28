package rise.lib.data;

import com.mongodb.BasicDBObject;

import rise.lib.business.Session;
import rise.lib.config.RiseConfig;
import rise.lib.utils.Utils;
import rise.lib.utils.date.DateUtils;

public class SessionRepository extends MongoRepository {
	
	/**
	 * Minimum interval between two lastTouch updates, to avoid a db write on every API call
	 */
	private static final double s_dTouchIntervalMs = 60.0 * 1000.0;
	
	public SessionRepository() {
		m_sThisCollection = "sessions";
		m_oEntityClass = Session.class;
	}
	
	public Session getSession(String sToken) {
		
		if (Utils.isNullOrEmpty(sToken)) return null;
		
		BasicDBObject oCriteria = new BasicDBObject();
		oCriteria.append("token", sToken);

        return (Session) get(oCriteria);		
	}
	
	/**
	 * Get a session only if it is still valid. 
	 * Expired sessions are deleted, valid ones have their lastTouch refreshed (sliding expiration).
	 * @param sToken Session token
	 * @return the valid session or null
	 */
	public Session getValidSession(String sToken) {
		
		Session oSession = getSession(sToken);
		
		if (oSession == null) return null;
		
		if (isSessionExpired(oSession)) {
			deleteSession(sToken);
			return null;
		}
		
		touchSession(oSession);
		
		return oSession;
	}
	
	/**
	 * Check if a session is expired according to security.maxSessionAgeSeconds, 
	 * computed since the last activity of the session
	 * @param oSession Session to check
	 * @return true if expired
	 */
	public boolean isSessionExpired(Session oSession) {
		
		if (oSession == null) return true;
		
		Double oLastActivity = oSession.getLastTouch();
		if (oLastActivity == null) oLastActivity = oSession.getLoginDate();
		if (oLastActivity == null) return true;
		
		double dSecondsPassed = (DateUtils.getNowAsDouble() - oLastActivity) / 1000.0;
		
		return dSecondsPassed > RiseConfig.Current.security.maxSessionAgeSeconds;
	}
	
	/**
	 * Refresh the lastTouch of a session, at most once every s_dTouchIntervalMs
	 * @param oSession Session to touch
	 * @return true if the session is updated or does not need to be
	 */
	public boolean touchSession(Session oSession) {
		
		if (oSession == null || Utils.isNullOrEmpty(oSession.getToken())) return false;
		
		double dNow = DateUtils.getNowAsDouble();
		
		if (oSession.getLastTouch() != null && dNow - oSession.getLastTouch() < s_dTouchIntervalMs) return true;
		
		oSession.setLastTouch(dNow);
		
		BasicDBObject oCriteria = new BasicDBObject();
		oCriteria.append("token", oSession.getToken());
		
		return update(oCriteria, oSession);
	}
	
	/**
	 * Delete a session by token
	 * @param sToken Session token
	 * @return number of deleted sessions
	 */
	public int deleteSession(String sToken) {
		
		if (Utils.isNullOrEmpty(sToken)) return 0;
		
		BasicDBObject oCriteria = new BasicDBObject();
		oCriteria.append("token", sToken);
		
		return delete(oCriteria);
	}
	
	/**
	 * Delete all the sessions of a user
	 * @param sUserId User Id
	 * @return number of deleted sessions
	 */
	public int deleteSessionsByUserId(String sUserId) {
		
		if (Utils.isNullOrEmpty(sUserId)) return 0;
		
		BasicDBObject oCriteria = new BasicDBObject();
		oCriteria.append("userId", sUserId);
		
		return deleteMany(oCriteria);
	}
		
}
