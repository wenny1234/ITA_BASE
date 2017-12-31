package weld.util;

import java.util.Map;

import javax.enterprise.inject.spi.CDI;

import jp.co.tokyo_gas.cisfw.constantmaster.CfwConstantMasterCache;
import jp.co.tokyo_gas.cisfw.constantmaster.CfwConstantMasterCacheManager;

/*
 * FW の初期化をサポートするユーティリティ
 */

public class CIRIUSFWHelper {

	static boolean initCfwConstantMasterCacheManager = true;
	
	static public void initCfwConstantMasterCacheManager() {
		CfwConstantMasterCache t = CDI.current().select(CfwConstantMasterCache.class).get();
		Map<?, ?> m = t.getConstantMasterMap();
		if (initCfwConstantMasterCacheManager && (m == null || m.size() == 0)) {
			CfwConstantMasterCacheManager mgr = CDI.current().select(CfwConstantMasterCacheManager.class).get();
			mgr.initialize();
		}
		initCfwConstantMasterCacheManager = false;
	}
	
}
