/*
 * Copyright 2017 TOKYO GAS CO.,LTD. All Rights Reserved.
 */
package weld.util.sample;

import jp.co.tokyo_gas.cisfw.sql.CfwSQLUpdateCommonParam;

import java.io.Serializable;
/**
 * データアクセス CXXDA14633 一般契約解約によるサービス契約・メーター設置場所関連を適用終了に更新する用InputDaoDTOクラス。<br>
 * 
 * @author DAO自動生成ツール v1.05
 * @since 2017/09/14 10:33:29
 * @version データモデル v0.11.52 機能定義書 v第2期
 */
@SuppressWarnings("serial")
public class CXXDA14633InDaoDTO extends CfwSQLUpdateCommonParam implements Serializable {
    /**
     * メーター設置場所番号プロパティ
     */
    private String mtstNo;

    /**
     * サービス契約番号プロパティ
     */
    private String svkyNo;

    /**
     * 料金計算関連状態区分プロパティ
     */
    private String rkkiKnrnJtkb;

    /**
     * 料金計算終了年月日プロパティ
     */
    private String rkkiEndYmd;

    /**
     * メーター設置場所番号Getter
     * @return メーター設置場所番号
     */
    public String getMtstNo() {
        return mtstNo;
    }

    /**
     * メーター設置場所番号Setter
     * @param mtstNo メーター設置場所番号
     */
    public void setMtstNo(String mtstNo) {
        this.mtstNo = mtstNo;
    }

    /**
     * サービス契約番号Getter
     * @return サービス契約番号
     */
    public String getSvkyNo() {
        return svkyNo;
    }

    /**
     * サービス契約番号Setter
     * @param svkyNo サービス契約番号
     */
    public void setSvkyNo(String svkyNo) {
        this.svkyNo = svkyNo;
    }

    /**
     * 料金計算関連状態区分Getter
     * @return 料金計算関連状態区分
     */
    public String getRkkiKnrnJtkb() {
        return rkkiKnrnJtkb;
    }

    /**
     * 料金計算関連状態区分Setter
     * @param rkkiKnrnJtkb 料金計算関連状態区分
     */
    public void setRkkiKnrnJtkb(String rkkiKnrnJtkb) {
        this.rkkiKnrnJtkb = rkkiKnrnJtkb;
    }

    /**
     * 料金計算終了年月日Getter
     * @return 料金計算終了年月日
     */
    public String getRkkiEndYmd() {
        return rkkiEndYmd;
    }

    /**
     * 料金計算終了年月日Setter
     * @param rkkiEndYmd 料金計算終了年月日
     */
    public void setRkkiEndYmd(String rkkiEndYmd) {
        this.rkkiEndYmd = rkkiEndYmd;
    }

}
