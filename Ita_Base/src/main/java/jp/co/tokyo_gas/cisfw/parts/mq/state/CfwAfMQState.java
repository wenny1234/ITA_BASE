package jp.co.tokyo_gas.cisfw.parts.mq.state;

import javax.enterprise.context.RequestScoped;

/**
 * IRIS非同期(MQ)伝文受信機能が受信した、IRIS MQ伝文の属性情報やMQ受信連携ユーザーIDを格納するクラスです。<br>
 * リクエストスコープで管理される。業務アプリケーションは、このクラスをCDIインジェクトして利用することにより、任意の処理でMQ伝文情報を取得することが可能となります<br>
 * （このクラスは、IRIS非同期(MQ)伝文機能ではシステム上必須のクラスですが、業務アプリケーションがこのクラスをCDIインジェクト利用することは必須ではありません）。
 * @author Tomomi Hiroi(IBM)
 * @version 1.0.0
 */
@RequestScoped
public class CfwAfMQState {	
	/** リクエストスコープで管理した、MQ連携で適用されるユーザーIDを格納する変数です。 */
	private String dukeId;
//	/** 維持管理ユーザーフラグを格納する変数です。 */
	private String adminUserFlag = "0";
	private String messageId;    //IRISメッセージのメッセージID(MQ MSGID)
	private String messageType;  //IRIS伝文のメッセージタイプ(IRIS定義)
	private Object messages;      //IRIS伝文本体の構造体（リスト）
	
	/**
	 * ユーザーIDを設定します。<br>
	 * @param dukeId ユーザーID
	 */
	public void setDukeId(String dukeId) {
		this.dukeId = dukeId;
	}
	
	/**
	 * ユーザーIDを取得します。<br>
	 * @return ユーザーID
	 */
	public String getDukeId() {
		return this.dukeId;
	}
	
	/**
	 * 維持管理者ユーザーフラグの状態を設定します。
	 * @param adminUserFlag 維持管理者ユーザーフラグ値(0または1)
	 */
	public void setAdmiUserFlag(String adminUserFlag) {
		this.adminUserFlag = adminUserFlag;
	}
	
	/**
	 * 維持管理者ユーザーフラグの状態を取得する。
	 * @return 維持管理者ユーザーフラグ値(0または1)
	 */
	public String getAdmiUserFlag() {
		return this.adminUserFlag;
	}
	
	/**
     * IRIS伝文のメッセージタイプを設定します。
	 * @param messageType IRIS伝文のメッセージタイプ
	 */
	public void setMessageType(String messageType) {
		this.messageType = messageType;
	}
	
	/**
     * IRIS伝文のメッセージタイプを取得します。
	 * @return MESSAGE_TYPE IRIS伝文のメッセージタイプ
	 */
	public String getMessageType()	{
		return this.messageType;
	}
	
	/**
	 * IIRIS伝文のメッセージIDを設定します。
	 * @param messageId IRISメッセージのメッセージID
	 */
	public void setMessageId(String messageId)	{
		this.messageId = messageId;
	}
	
	/**
     * IRIS伝文のメッセージIDを取得します。
	 * @return IRISメッセージのメッセージID
	 */
	public String getMessageId() {
		return this.messageId;
	}
	
	/**
     * IRIS伝文を取得します。
	 * @param messages IRIS伝文本体（List格納されたFwMQBaseMessage型)
	 */
	public void setMessages(Object messages) {
		this.messages = messages;
	}
	
	/**
     * IRIS伝文を設定します。
	 * @return  IRIS伝文本体（List格納されたFwMQBaseMessage型)
	 */
	public Object getMessages() {
		return this.messages;
	}

}