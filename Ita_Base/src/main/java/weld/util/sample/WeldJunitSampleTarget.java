package weld.util.sample;

import java.math.BigDecimal;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import jp.co.tokyo_gas.aion.tgfw.parts.db.sql.annotation.FwSQLMapper;

@Dependent
public class WeldJunitSampleTarget {
	/** データアクセス CXXDA01644 サービス契約番号取得用Mapper. */
	@Inject
	@FwSQLMapper
	private CXXDA01644Mapper cXXDA01644Mapper;

	@Inject
	@FwSQLMapper
	private CXXDA14633Mapper cXXDA14633Mapper;
	
	@Inject
	@FwSQLMapper
	private CXXDA11073Mapper cXXDA11073Mapper;

	public CXXDA01644OutDaoDTO method(String opYmd) {
		System.out.println("hello!["+cXXDA01644Mapper+"]");
		if(cXXDA01644Mapper == null){
			System.err.println("CXXDA01644Mapperがインジェクトされていません");
			return null;
		}
		CXXDA01644InDaoDTO condition = new CXXDA01644InDaoDTO();
		condition.setOpYmd(opYmd);
		CXXDA01644OutDaoDTO ret = cXXDA01644Mapper.find(condition);
		System.out.println("hello!["+ret+"]");
		return ret;
	}
	
	public void method2(BigDecimal mtstNo,BigDecimal svkyNo) {
		System.out.println("hello!["+cXXDA11073Mapper+"]");
		if(cXXDA11073Mapper == null){
			System.err.println("cXXDA11073Mapperがインジェクトされていません");
			return;
		}
		CXXDA14633InDaoDTO cXXDA14633InDaoDTO = new CXXDA14633InDaoDTO();
		cXXDA14633InDaoDTO.setMtstNo(mtstNo.toString());
		cXXDA14633InDaoDTO.setSvkyNo(svkyNo.toString());
	    /** 関連終了 */
	    //C_END("04");
		cXXDA14633InDaoDTO.setRkkiKnrnJtkb("04");
		cXXDA14633InDaoDTO.setRkkiEndYmd("20171030");
		cXXDA14633Mapper.update(cXXDA14633InDaoDTO);
	}
	
	public void method3(BigDecimal sykyUtkNo) {
		System.out.println("hello!["+cXXDA11073Mapper+"]");
		if(cXXDA11073Mapper == null){
			System.err.println("cXXDA11073Mapperがインジェクトされていません");
			return;
		}
		CXXDA11073InDaoDTO cXXDA11073InDaoDTO = new CXXDA11073InDaoDTO();
		cXXDA11073InDaoDTO.setSykyUtkNo(sykyUtkNo);
		///** 報告中 */
	    //C_MRK("1"),
		cXXDA11073InDaoDTO.setSykyHukcFlg("1");
		cXXDA11073Mapper.update(cXXDA11073InDaoDTO);
	}
}
