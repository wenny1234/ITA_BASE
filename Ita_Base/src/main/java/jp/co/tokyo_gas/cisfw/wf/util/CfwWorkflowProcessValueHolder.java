package jp.co.tokyo_gas.cisfw.wf.util;

import javax.enterprise.context.RequestScoped;

/**
 * パラメータで渡された分岐判定値をリクエストスコープで保持するクラスです。<br/>
 * なお、未設定は0とします。従って、分岐判定値に0を使用することはできません。
 *
 * @author Takashi Takeuchi (TDC)
 * @version 1.0.0
 */
@RequestScoped
public class CfwWorkflowProcessValueHolder {

	/** 分岐判定値 */
	private int processValue;

	/**
	 * 分岐判定値を取得します。
	 * @return 分岐判定値
	 */
	public int getProcessValue() {
		return processValue;
	}

	/**
	 * 分岐判定値を設定します。
	 * @param processValue 分岐判定値
	 */
	public void setProcessValue(int processValue) {
		this.processValue = processValue;
	}
}
