package jp.co.tokyo_gas.cisfw.parts.svf;

import jp.co.tokyo_gas.aion.tgfw.parts.adapter.form.FwForm;
import jp.co.tokyo_gas.aion.tgfw.parts.adapter.form.FwFormManager;
import jp.co.tokyo_gas.aion.tgfw.parts.adapter.form.FwSvfException;
import jp.co.tokyo_gas.aion.tgfw.parts.adapter.form.impl.FwEmfParam;
import jp.co.tokyo_gas.aion.tgfw.parts.adapter.form.impl.FwPdfParam;
import jp.co.tokyo_gas.aion.tgfw.parts.config.FwPropertyManager;
import jp.co.tokyo_gas.cisfw.exception.CfwRuntimeException;
import jp.co.tokyo_gas.cisfw.file.CfwTemporaryFileUtil;
import jp.co.tokyo_gas.cisfw.init.CfwPropertyManager;
import jp.co.tokyo_gas.cisfw.init.CfwQualifier;
import jp.co.tokyo_gas.cisfw.logger.CfwLogger;
import jp.co.tokyo_gas.cisfw.utils.CfwLogHelper;
import jp.co.tokyo_gas.cisfw.utils.CfwStringValidator;
import jp.co.tokyo_gas.cisfw.file.CfwPermanentFileUtil;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

/**
 * 帳票連携（SVF印刷）のための各種機能を実装したクラスです。
 *   →ITa2用にFwFormManagerを無効化しました(2017/11/15)
 * 
 * @author IBM-yatohgo
 * @version 1.0.0
 */
@RequestScoped
public class CfwAfSvfUtils {

	// TODO ITa2用にFwFormManagerを無効化しました(2017/11/15)
//	@Inject
	private FwFormManager formManager;
	
	@Inject
	private CfwTemporaryFileUtil tempFileUtil;
		
	@Inject
	private CfwPermanentFileUtil permFileUtil;
	
	@Inject @CfwQualifier
	private CfwPropertyManager propmanager;
	
	private ByteArrayOutputStream fileStream = new ByteArrayOutputStream();
	
	/** 通信ログ用ロガー */
	@Inject
	private CfwLogger commLogger;
	
	@Inject
	private FwPropertyManager fwPropManager;
	
	/** ログ出力関連のユーティリティです。 **/
	@Inject
	private CfwLogHelper helper;
	
	/**
	 * 帳票PDF作成メソッドです、SVFアダプター経由でSVFに接続しPDFファイルを作成、層間ファイル保存した上でファイルIDを返します。
	 * @param reportId 帳票ＩＤを指定します。
	 * @param dto CfwAfSvfDTOに帳票個別のDTOデータを格納して渡します。
	 * @return PDFファイルを層間ファイルのファイルIDで返します。
	 * @see "CIRIUS-FW利用ガイド.帳票.帳票連携(SVF印刷)"
	 * 	 */
	public String createPdfFile(String reportId, CfwAfSvfDTO dto) {
		// ファイル作成の処理を実行し、ByteArrayOutputStreamにPDFデータを格納
		createFileCommon("PDF",reportId,dto);

		// 生成されたファイルを層間ファイルに保存し、ファイルＩＤをリターン
		return tempFileUtil.setFile(fileStream.toByteArray());
	}
	
	/**
	 * 帳票PDF作成メソッドです、SVFアダプター経由でSVFに接続しPDFファイルを作成、永続ファイル保存した上でファイルIDを返します。
	 * @param reportId 帳票ＩＤを指定します。
	 * @param dto CfwAfSvfDTOに帳票個別のDTOデータを格納して渡します。
	 * @param fileName ファイル名(拡張子なし)を渡します。
	 * @param extention 拡張子を渡します。
	 * @return PDFファイルを永続ファイルのファイルIDで返します。
	 * @see "CIRIUS-FW利用ガイド.帳票.帳票連携(SVF印刷)"
	 * 	 */
	public String createPermPdfFile(String reportId, CfwAfSvfDTO dto, String fileName, String extention) {
		// ファイル作成の処理を実行し、ByteArrayOutputStreamにPDFデータを格納
		createFileCommon("PDF",reportId,dto);

		// 生成されたファイルを永続ファイルに保存し、ファイルＩＤをリターン
		return permFileUtil.setFile(fileStream.toByteArray(),fileName,extention);
	}

	/**
	 * 帳票EMF作成メソッドです、SVFアダプター経由でSVFに接続しEMFファイルを作成、層間ファイル保存した上でファイルIDを返します。
	 * @param reportId 帳票ＩＤを指定します。
	 * @param dto CfwAfSvfDTOに帳票個別のDTOデータを格納して渡します。
	 * @return EMFファイルを層間ファイルのファイルIDで返します。
	 * @see "CIRIUS-FW利用ガイド.帳票.帳票連携(SVF印刷)"
	 * 	 */
	public String createEmfFile(String reportId, CfwAfSvfDTO dto) {
		// ファイル作成の処理を実行し、ByteArrayOutputStreamにEMFデータを格納
		createFileCommon("EMF",reportId,dto);

		// 生成されたファイルを層間ファイルに保存し、ファイルＩＤをリターン
		return tempFileUtil.setFile(fileStream.toByteArray());
	}

	/**
	 * 帳票ファイル(PDF/EMF)作成共通処理です、SVFアダプター経由でSVFに接続し、ファイルデータをByteArrayOutputStreamに格納します。
	 * @param mode "PDF"/"EMF"のいずれかを指定します。
	 * @param reportId 帳票ＩＤを指定します。
	 * @param dto CfwAfSvfDTOに帳票個別のDTOデータを格納して渡します。
	 * 	 */
	public void createFileCommon(String mode,String reportId, CfwAfSvfDTO dto) {
		
		// 入力データチェック
		if (reportId == null || reportId.equals("")) {
			throw new CfwRuntimeException("帳票IDが設定されていません。");
		}
		if (dto.getFormDtos() == null ) {
			throw new CfwRuntimeException("帳票データが設定されていません。");
		}
		
		// TODO ITa2用にFwFormManagerを無効化しました(2017/11/15)
//		FwForm svfAdapter = formManager.getForm();
		
		FwPdfParam pdfParam = new FwPdfParam();
		FwEmfParam emfParam = new FwEmfParam();
		
		if (mode.equals("PDF")) {
			// PDF生成パラメーターの設定（フォームファイル名以外。フォームファイル名はフォーム単位の処理を行う際にセット。）
			pdfParam = setPdfParam(reportId);
		} else {
			// EMF生成パラメーターの設定（フォームファイル名以外。フォームファイル名はフォーム単位の処理を行う際にセット。）
			emfParam = setEmfParam();
		}

		// reportCommon.propertiesよりスタブモードかどうかを判定し、スタブモードの場合のみログ出力する。
		Properties props = fwPropManager.getProperties("tgfw/reportCommon");
		if(!CfwStringValidator.isEmpty(props.getProperty("TestMode"))) {
			if ("true".toUpperCase().equals(props.getProperty("TestMode").toUpperCase())) {
				// 帳票連携（SVF印刷）へのパラメーター用DTOクラスの各プロパティの値をJSON形式で出力
				commLogger.debug(helper.toJSON(dto.getFormDtos()));
			}
		}

		// Formの数だけ、SVFアダプターへのForm値セットを繰り返す。
		for (int i = 0;i < dto.getFormDtos().size();i++) {
			
			// フォームファイル名の設定
			if (dto.getFormDtos().get(i).getFormFileNm() == null || dto.getFormDtos().get(i).getFormFileNm().equals("")) {
				throw new CfwRuntimeException("帳票ＩＤ：" + reportId + "のフォームファイル名が設定されていません。");
			}
			if (mode.equals("PDF")) {
				// フォームファイル名のセット
				pdfParam.setFormFileName(dto.getFormDtos().get(i).getFormFileNm());

				// TODO ITa2用にFwFormManagerを無効化しました(2017/11/15)
				// PDF生成パラメーターのセット
//				svfAdapter.setPdfParam(pdfParam);
			} else {
				// フォームファイル名のセット
				emfParam.setFormFileName(dto.getFormDtos().get(i).getFormFileNm());

				// TODO ITa2用にFwFormManagerを無効化しました(2017/11/15)
				// EMF生成パラメーターのセット
//				svfAdapter.setEmfParam(emfParam);
			}
			
			// TODO ITa2用にFwFormManagerを無効化しました(2017/11/15)
			// DTOデータのセット
//			svfAdapter.setDto(dto.getFormDtos().get(i).getFormObject());
			
			// TODO ITa2用にFwFormManagerを無効化しました(2017/11/15)
			// フォーム確定
//			svfAdapter.endForm();
		}
		
		// SVFアダプターでファイルを生成
		try {
			// TODO ITa2用にFwFormManagerを無効化しました(2017/11/15)
//			svfAdapter.print();
		} catch (FwSvfException e) {
			if (e.getStatus() == -505) {
				throw new CfwRuntimeException("フォームに定義していないフィールドをDTO上で指定しています。" + e.getMessage());
			} else {
				if (e.getCause() != null ) {
					String cause = e.getCause().toString();
					if (cause.indexOf("java.net.ConnectException") > 0) {
						throw new CfwRuntimeException("帳票基盤への接続エラーです。");
					} else if (cause.indexOf("java.io.EOFException") > 0) {
						throw new CfwRuntimeException("帳票基盤から切断されました。");
					} else if (cause.indexOf("java.net.SocketException") > 0) {
						throw new CfwRuntimeException("帳票基盤から切断されました。");
					} else if (cause.indexOf("java.net.UnknownHostException") > 0) {
						throw new CfwRuntimeException("接続先ホストが存在しません。");
					}
				} 
				// 上記ハンドリング処理に当てはまらない場合はその他エラーとしてリターンコード、メッセージを出力。
				throw new CfwRuntimeException(
						"SVFアダプターによる帳票生成でエラーが発生しました。リターンコード：" + e.getStatus() + " メッセージ：" + e.getMessage());
			}
		}
	}
	
	/**
	 * フォーム間で共通なPDF作成パラメーターの生成メソッドです、プロパティに設定があればその内容を、なければデフォルト値をセットしたものを返します。
	 * @param reportId 帳票ＩＤを指定します。
	 * @return PDF作成パラメーターDTOを返します。
	 * 	 */
	public FwPdfParam setPdfParam(String reportId) {
		FwPdfParam pdfParam = new FwPdfParam();

		// 出力ストリーム
		fileStream = new ByteArrayOutputStream();
		pdfParam.setOutputStream(fileStream);

		// 保存ファイル名
		pdfParam.setFormFileName("");
		
		// プロパティ設定値取得
		String propString = propmanager.getProperty("cisfwaf_svf",reportId);
		if (propString == null) {
			propString = ",,,,,,";
		}
		String[] parms = propString.split(",", 7); // プロパティで未指定値がある場合、""として扱う。
		
		// Flag許容値
		List<String> flgValue = new ArrayList<String>(Arrays.asList("true","True","false","False"));
		
		// 印刷禁止設定フラグ
		String noPrintFlg = parms[0];
		if (!noPrintFlg.equals("")) {
			if (!flgValue.contains(noPrintFlg)) {
				throw new CfwRuntimeException(
						"帳票ＩＤ：" + reportId + "の印刷禁止フラグの設定値が誤っています。設定可能値：T(t)rue/F(f)alse 設定値：" + noPrintFlg);
			}
		} else {
			noPrintFlg = "false";
		}
		pdfParam.setNoPrintFlg(Boolean.valueOf(noPrintFlg));
		
		// 編集禁止設定フラグ
		String noEditFlg = parms[1];
		if (!noEditFlg.equals("")) {
			if (!flgValue.contains(noEditFlg)) {
				throw new CfwRuntimeException(
						"帳票ＩＤ：" + reportId + "の編集禁止設定フラグの設定値が誤っています。設定可能値：T(t)rue/F(f)alse 設定値：" + noEditFlg);
			}
		} else {
			noEditFlg = "false";
		}
		pdfParam.setNoEditFlg(Boolean.valueOf(noEditFlg));
		
		// コピー禁止設定フラグ
		String noCopyFlg = parms[2];
		if (!noCopyFlg.equals("")) {
			if (!flgValue.contains(noCopyFlg)) {
				throw new CfwRuntimeException(
						"帳票ＩＤ：" + reportId + "のコピー禁止設定フラグの設定値が誤っています。設定可能値：T(t)rue/F(f)alse 設定値：" + noCopyFlg);
			}
		} else {
			noCopyFlg = "false";
		}
		pdfParam.setNoCopyFlg(Boolean.valueOf(noCopyFlg));
		
		// 注釈とフォームフィールドの追加禁止設定フラグ
		String noAddNotesFlg = parms[3];
		if (!noAddNotesFlg.equals("")) {
			if (!flgValue.contains(noAddNotesFlg)) {
				throw new CfwRuntimeException(
						"帳票ＩＤ：" + reportId + "の注釈とフォームフィールドの追加禁止設定フラグの設定値が誤っています。設定可能値：T(t)rue/F(f)alse 設定値："
						+ noAddNotesFlg);
			}
		} else {
			noAddNotesFlg = "false";
		}
		pdfParam.setNoAddNotesFlg(Boolean.valueOf(noAddNotesFlg));
		
		// ファイルオープン時のパスワード
		String openPassword = parms[4];
		pdfParam.setOpenPassword(openPassword);
		
		// セキュリティパスワード
		String securityPassword = parms[5];
		pdfParam.setSecurityPassword(securityPassword);

		// 透かし文字プロパティファイル名
		String waterMarkPropertyName = parms[6];
		pdfParam.setWaterMarkPropertyName(waterMarkPropertyName);
		
		// 相関チェック（各種フラグのいずれかがONの場合、セキュリティパスワードの設定が必要）
		if (noPrintFlg.equals("true") || noEditFlg.equals("true") || noCopyFlg.equals("true") || noAddNotesFlg.equals("true")) {
			if (securityPassword.equals("")) {
				throw new CfwRuntimeException("帳票ＩＤ：" + reportId + "のセキュリティパスワードが設定されていません。");
			}
		}
		
		return pdfParam;
	}

	/**
	 * フォーム間で共通なEMF作成パラメーターの生成メソッドです。
	 * @return EMF作成パラメーターDTOを返します。
	 * 	 */
	public FwEmfParam setEmfParam() {
		FwEmfParam emfParam = new FwEmfParam();
		
		// 出力ストリーム
		fileStream = new ByteArrayOutputStream();
		emfParam.setOutputStream(fileStream);

		// reportCommon.propertiesからsvfprinter名を取得
		Properties props = fwPropManager.getProperties("tgfw/reportCommon");
		String printer = props.getProperty("SvfPrinter");
		if (printer != null) {
			emfParam.setPrinter(printer);
		}
		
		return emfParam;
	}
}