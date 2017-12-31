/*
 * Copyright 2017 TOKYO GAS CO.,LTD. All Rights Reserved.
 */
package weld.util.sample;

import jp.co.tokyo_gas.cisfw.sql.CfwSQLBaseMapper;

/**
 * データアクセス CXXDA01644 サービス契約番号取得用Mapperインターフェース。<br>
 * 
 * @author DAO自動生成ツール vfusever
 * @since 2017/02/17 16:31:53
 * @version データモデル v1AB 機能定義書 v第1期
 */
public interface CXXDA01644Mapper extends CfwSQLBaseMapper<CXXDA01644OutDaoDTO> {
    /**
     * ガス使用契約テーブルから支払契約番号（2x）をキーに、現在または最後に紐づいていたサービス契約番号（4x）を取得する（１件）。<br>
     * 
     * @param condition InputDaoDTO
     * @return OutputDaoDTO 結果無しの場合はnull
     */
    public CXXDA01644OutDaoDTO find(CXXDA01644InDaoDTO condition);
}
