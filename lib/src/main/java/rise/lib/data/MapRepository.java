package rise.lib.data;

import rise.lib.business.Map;

public class MapRepository extends MongoRepository {
	
	public MapRepository() {
		m_sThisCollection = "maps";
		m_oEntityClass = Map.class;
	}
}
