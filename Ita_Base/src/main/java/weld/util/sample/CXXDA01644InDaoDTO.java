/*
 * Copyright 2017 TOKYO GAS CO.,LTD. All Rights Reserved.
 */
package weld.util.sample;

import jp.co.tokyo_gas.cisfw.sql.CfwSQLParamBase;

import java.io.Serializable;
/**
 * データアクセス CXXDA01644 サービス契約番号取得用InputDaoDTOクラス。<br>
 * 
 * @author DAO自動生成ツール vfusever
 * @since 2017/02/17 16:31:53
 * @version データモデル v1AB 機能定義書 v第1期
 */
@SuppressWarnings("serial")
public class CXXDA01644InDaoDTO extends CfwSQLParamBase implements Serializable {
    /**
     * 支払契約番号プロパティ
     */
    private String sihKiyNo;

    /**
     * ＯＰ日プロパティ
     */
    private String opYmd;

    /**
     * 支払契約番号Getter
     * @return 支払契約番号
     */
    public String getSihKiyNo() {
        return sihKiyNo;
    }

    /**
     * 支払契約番号Setter
     * @param sihKiyNo 支払契約番号
     */
    public void setSihKiyNo(String sihKiyNo) {
        this.sihKiyNo = sihKiyNo;
    }

    /**
     * ＯＰ日Getter
     * @return ＯＰ日
     */
    public String getOpYmd() {
        return opYmd;
    }

    /**
     * ＯＰ日Setter
     * @param opYmd ＯＰ日
     */
    public void setOpYmd(String opYmd) {
        this.opYmd = opYmd;
    }

}
