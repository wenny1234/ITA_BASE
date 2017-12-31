/*
 * Copyright 2017 TOKYO GAS CO.,LTD. All Rights Reserved.
 */
package weld.util.sample;


import java.io.Serializable;
/**
 * データアクセス CXXDA01644 サービス契約番号取得用OutputDaoDTOクラス。<br>
 * 
 * @author DAO自動生成ツール vfusever
 * @since 2017/02/17 16:31:53
 * @version データモデル v1AB 機能定義書 v第1期
 */
@SuppressWarnings("serial")
public class CXXDA01644OutDaoDTO implements Serializable {
    /**
     * PK使用契約/番号プロパティ
     */
    private String syokyNo;

    /**
     * 内容有効期限/年月日プロパティ
     */
    private String limitYmd;

    /**
     * PK使用契約/番号Getter
     * @return PK使用契約/番号
     */
    public String getSyokyNo() {
        return syokyNo;
    }

    /**
     * PK使用契約/番号Setter
     * @param syokyNo PK使用契約/番号
     */
    public void setSyokyNo(String syokyNo) {
        this.syokyNo = syokyNo;
    }

    /**
     * 内容有効期限/年月日Getter
     * @return 内容有効期限/年月日
     */
    public String getLimitYmd() {
        return limitYmd;
    }

    /**
     * 内容有効期限/年月日Setter
     * @param limitYmd 内容有効期限/年月日
     */
    public void setLimitYmd(String limitYmd) {
        this.limitYmd = limitYmd;
    }

}
