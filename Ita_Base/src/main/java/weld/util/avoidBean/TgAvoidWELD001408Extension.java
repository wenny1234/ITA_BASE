package weld.util.avoidBean;

import jp.co.tokyo_gas.aion.tgfw.parts.db.sql.annotation.FwSQLMapper;

/**
 * CDI を利用した開発時、実装が揃わない状態で起動を試みると WELD-001408 エラーで起動できません。 これを回避するために、起動時に必要な
 * Bean の登録を自動で行う Extensionの
 *
 */
public class TgAvoidWELD001408Extension extends AvoidWELD001408Extension {
	public TgAvoidWELD001408Extension(){
		avoidTypes = new String[] { "jp.co.tokyo_gas.cirius" };
		excludeClazz = new Class<?>[] { FwSQLMapper.class };
	}
}
