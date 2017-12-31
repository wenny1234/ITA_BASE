/*
 * Copyright 2017 TOKYO GAS CO.,LTD. All Rights Reserved.
 */
package weld.util.sample;

import jp.co.tokyo_gas.cisfw.sql.CfwSQLUpdateCommonParam;

import java.io.Serializable;
import java.math.BigDecimal;
/**
 * データアクセス CXXDA11073 選択約款契約受付報告中フラグ更新用InputDaoDTOクラス。<br>
 * 
 * @author DAO自動生成ツール v1.04
 * @since 2017/09/05 19:28:56
 * @version データモデル v0.11.52 機能定義書 v第2期
 */
@SuppressWarnings("serial")
public class CXXDA11073InDaoDTO extends CfwSQLUpdateCommonParam implements Serializable {
    /**
     * 選択約款契約受付番号プロパティ
     */
    private BigDecimal sykyUtkNo;

    /**
     * 選択約款契約報告中フラグプロパティ
     */
    private String sykyHukcFlg;

    /**
     * 選択約款契約受付番号Getter
     * @return 選択約款契約受付番号
     */
    public BigDecimal getSykyUtkNo() {
        return sykyUtkNo;
    }

    /**
     * 選択約款契約受付番号Setter
     * @param sykyUtkNo 選択約款契約受付番号
     */
    public void setSykyUtkNo(BigDecimal sykyUtkNo) {
        this.sykyUtkNo = sykyUtkNo;
    }

    /**
     * 選択約款契約報告中フラグGetter
     * @return 選択約款契約報告中フラグ
     */
    public String getSykyHukcFlg() {
        return sykyHukcFlg;
    }

    /**
     * 選択約款契約報告中フラグSetter
     * @param sykyHukcFlg 選択約款契約報告中フラグ
     */
    public void setSykyHukcFlg(String sykyHukcFlg) {
        this.sykyHukcFlg = sykyHukcFlg;
    }

}
