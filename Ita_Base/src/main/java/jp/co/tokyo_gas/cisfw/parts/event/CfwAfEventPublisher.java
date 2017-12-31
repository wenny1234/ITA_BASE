package jp.co.tokyo_gas.cisfw.parts.event;

import jp.co.tokyo_gas.aion.tgfw.parts.config.FwConfig;
import jp.co.tokyo_gas.aion.tgfw.parts.tracking.FwTrackingIdManager;
import jp.co.tokyo_gas.cisfw.converter.CfwJSONConverter;
import jp.co.tokyo_gas.cisfw.event.EventGenerator;
import jp.co.tokyo_gas.cisfw.event.annotation.ProductionMode;
import jp.co.tokyo_gas.cisfw.exception.CfwApplicationException;
import jp.co.tokyo_gas.cisfw.exception.CfwMassEventMessageException;
import jp.co.tokyo_gas.cisfw.exception.CfwRuntimeException;
import jp.co.tokyo_gas.cisfw.init.CfwPropertyManager;
import jp.co.tokyo_gas.cisfw.init.CfwQualifier;
import jp.co.tokyo_gas.cisfw.logger.CfwLogger;
import jp.co.tokyo_gas.cisfw.parts.mq.state.CfwAfMQState;
import jp.co.tokyo_gas.cisfw.utils.CfwLogHelper;
import jp.co.tokyo_gas.cisfw.utils.CfwStringConverter;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.dgc.VMID;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;


/**
 * 逐次イベント発行（業務アプリ実装向け）機能です<br>
 * この機能は、CIRIUS-FW(CAFE)により提供済みの逐次イベント発行部品（EventGenerator）の機能をラップし、業務アプリケーションが共通的に行う前処理を含めた処理を追加実装しています.<br>
 * CIRIUS-FW(CAFE)の提供する逐次イベント発行本来の機能をほぼ継承しており、CAFE本来のイベント発行機能を大きく改編・置換はしておりません<br>
 *<br>
 *【CIRIUS-FW（CAFE）の機能(本部品が継承している機能です)】<br>
 * ① Javaオブジェクトに格納されている逐次伝文情報を、XMLメッセージ形式へ変換する<br>
 * ② イベントIDを指定した、逐次伝文の送信(MQによる Publish)<br>
 *【本部品での拡張機能】<br>
 * ③ 部品のクラス名をCAFE独自の命名規則から、CIRIUS-FWの提供クラスの命名部品に改名する（業務アプリケーションの開発者は、EventGeneratorではなく、
 * CfwAfEventPublisherを組み込むことで、他のフレームワークとの名称の統一性を高める）<br>
 * ④ イベント発行時に戻される不測のシステム例外値を、CfwRuntimeExceptionに変換しthrowする<br>
 * ⑤ イベント発行のためのMQ接続情報を外部プロパティから取得可能にする<br>
 * ⑥ 逐次イベントメッセージ(DTO)に、追跡IDを自動設定する<br>
 * ⑦ 逐次イベントメッセージ(DTO)に、発行要求元ユーザーIDを自動設定する<br>
 * ⑧ 逐次イベント発行を要求したユーザーの、維持管理者権限（維持管理者フラグがtrue）を確認し、維持管理者に該当する場合、業務アプリケーション例外を戻し、処理を停止する<br>
 * @author Tomomi Hiroi(IBM)
 * @version 1.0.0
 */
@Dependent
public class CfwAfEventPublisher implements Serializable {
	private static final long serialVersionUID = 1L;
	public static String USERID 	  = "setUserId"; 
	public static String TRACKINGID = "setTrackingId"; 
	public static String BASEFUNCID = "setStartFuncId"; 
	  
	// 維持管理者時エラーメッセージＩＤ
	private static final String adminErrMsgId = "CXXM91008E";
	  /** 逐次イベント発行部品機能で用いられるプロパティファイル名です. */
	protected static String EVENT_GENERATOR_PROPERTY = "event/event";
	@Inject @CfwQualifier  		/** プロパティ情報を読み取るためのクラス */
	private CfwPropertyManager 		propertyMmanager;
	@Inject @ProductionMode	  	/** CAFEイベント発行部品		      */
	private EventGenerator 			eventGenerator;
	@Inject	  					/** 追跡ID管理クラス			      */
	private FwTrackingIdManager 		manager;
	@Inject 					/** MQ連携状態(IRIS非同期(MQ)連携）を管理クラス     */
	private CfwAfMQState				mqstate;
	@Inject 					/**	MQ関連機能ログ（例外ログ）を出力するクラス */
	private CfwLogger 				logger;
	/** プロパティファイル初回読込を確認するフラグ(false=初回読込要 / true=読込済み(再読込み不要））*/
	protected static boolean 		propetyLoaded;
	/** TopicConnectionFactory名を入れえる場合の名称を格納する変数	      */
	private static String  			topicConnectionFactoryName ;
	/** JNDI接頭子を入れえる場合の名称を格納する変数					      */
	private static String  			jndiPrefix ;
	/** スタブモードを識別するフラグ （文字ストリングで受信）					      */
	private String					stub;			//文字ストリングでの状態
	/** スタブモードを識別するフラグ （boolean値）							    */
	private static boolean  		stubmode;		//booleanでの状態
	/** ユーザーＩＤ										   */
	private String userId;
	/** 維持管理者フラグ								   */
	private String adminUserFlg;
	/** 所属組織企業区分						           */
	private String compKbn;
	/** 所属組織企業コード						           */
	private String compCd;
	/** 所属組織事業所コード						           */
	private String officeCd;
	/** 所属組織課コード						           */
	private String departmentCd;
	/** 所属組織職場コード 						           */
	private String workplaceCd;
	/** 所属組織ＮＷ箇所コード					           */
	private String nwPlaceCd;
	/** 利用可能業務権限コード					           */
	private List<String> allowedAuthorityCd; 
	/** 処理ＩＤ										   */
	private String startFunctionId = " ";
	/** ログ出力関連のユーティリティです。 **/
	@Inject
	private CfwLogHelper helper;
	
	@Inject
	private FwConfig config;
	
	private String createTrackingIDEventID;

	/** フロントエンドティアのCfwSessionクラスのクラスパスです。 */
	protected static String CLASS_CFWSESSION  = "jp.co.tokyo_gas.cisfw.web.CfwSession";
	/** 業務基幹コンポーネントティアのCfwSOAPMessageHolderのクラスパスです。 */
	protected static String CLASS_CFWBIZ     = "jp.co.tokyo_gas.cisfw.utils.CfwSOAPMessageHolder";
	/** システム統合/フロントエンドティアのCfwCommunicationLogInfoHolderクラスのクラスパスです。 */
	protected static String CLASS_CFWCOMHOLDER = "jp.co.tokyo_gas.cisfw.ws.CfwCommunicationLogInfoHolder";
	  
	  /**
	   * 逐次イベントの発行メソッドです<br>
	   * 逐次イベントIDと逐次イベントDTOから、逐次処理基盤に向けてXMLイベント伝文を作成し、送信を行います. <br>
	   * このメソッドでは、XMLイベント伝文への変換に前に伝文内に追跡ID、ユーザーIDを自動付与し、逐次イベント伝文を作成後、イベントを発行します<br>
	   * ユーザーが維持管理者の権限（維持管理者フラグがオン）である場合、逐次イベントの発行を拒絶し、CfwApplicationExceptionを戻します<br>
	   * このメソッドは、WebSphere MQ APIを使用し、逐次イベントをパブリッシュ（発行）します. パブリッシュにあたっては、アプリケーションサーバーにMQ(トピック) 設定が必要です<br>
	   * また、MQ設定と一致させるため、逐次イベント発行メソッドにevent.propertiesを設定しておく必要があります.
	   * @param eventId	逐次イベントＩＤ(アプリケーションサーバーに設定した、MQトピックと一致)
	   * @param eventDto 逐次イベントメッセージを格納したDTOオブジェクト
	   * @throws CfwApplicationException 発行拒否例外. 維持管理者権限では、逐次イベント発行は許可されません.
	   * @see "CIRIUS-FW利用ガイド.逐次処理.逐次イベント送信"
	   */
	public void publish(String eventId,Object eventDto) throws CfwApplicationException {	
		try {
			
			//ユーザーＩＤ、維持管理者フラグを確認し、維持管理ユーザーの場合、CfwApplicationExceptionを発行する.
			getUserInfo();
			if (this.adminUserFlg.equals("1"))  {
				logger.debug("Event Message : process end(Administrator-throw-CfwApplicationException).");
				throw new CfwApplicationException(adminErrMsgId,"");
			}
			//処理ＩＤを取得する.
			getStartFunctionId();
			  	 
	
			setUserID(eventDto);		//ユーザーIDを設定する.
			setFuncID(eventDto);		//起点IDを設定する.
			logger.debug("Event Message : 逐次イベントID=" + eventId);	//デバックログ出力
			logger.debug("Event Message : 逐次イベントDTOクラス=" + eventDto.getClass());				//デバックログ出力
			
			//プロパティファイル初回読込が必要な場合、プロパティ読込を実施
			if (!propetyLoaded) {
				// イベント発行部品の環境設定プロパティ情報を設定する.
				Properties properties = propertyMmanager.getProperties(EVENT_GENERATOR_PROPERTY);
				//プロパティファイルが存在する場合、設定情報の読込を行います（但し、キーが設定されていない場合、null値になる場合があります）.
				if (properties != null) {
				    		//トピック接続JNDI
					topicConnectionFactoryName = 
					properties.getProperty("asynchronous.process.queue.jmsTopicConnectionFactory",
					null); 
					//JNDIプレフィクス
					jndiPrefix = 
					properties.getProperty("asynchronous.process.queue.JNDIPrefix",null);
					//スタブモードのオン・オフ
					stub = 
					properties.getProperty("asynchronous.process.queue.stub","false");
					// 新規追跡IDを発行したいイベントメッセージリスト
					createTrackingIDEventID = properties.getProperty("asynchronous.process.queue.createTrackingIDEventID");
				}							
				//topicConnectionFactoryNameがプロパティ設定で上書きされている場合、EventPublisherの設定値を更新します.
				if (topicConnectionFactoryName != null) {
				//TopicConnectionFactoryNameが環境設定済みであれば、CAFEデフォルト値を上書きします.　
					//なお、setTopicConnectionFactoryNameメソッドは、引数がnullの場合、デフォルト値がそのまま引き継がれます.
					eventGenerator.setTopicConnectionFactoryName(topicConnectionFactoryName);
				}
				//jndiPrefixがプロパティ設定で上書きされている場合、EventPublisherの設定値を更新します.
				if (jndiPrefix != null) {	  
					//JNDIPrefixが環境設定済みであれば、CAFEデフォルト値を上書きする.　なお、setJNDIPrefixメソッドは、引数がnullの場合、デフォルト値がそのまま引き継がれます.
					eventGenerator.setJNDIPrefix(jndiPrefix); 
				}
				//逐次イベントを発行します. スタブモードがオンの場合、逐次イベントの発行要求は行われず、このメソッドはスタブとして動作します.
				//stubキーがnullの場合はfalseを、それ以外はstub文字列をBooleanに変更します.
				stubmode = stub == null ? false : new Boolean(stub); 
				//プロパティファイル初回読込済（再読込み不要）にセット
				propetyLoaded = true;
			}
			
			// 追跡ID設定
			if (createTrackingIDEventID != null) {
				// イベント発行先追跡ID新規設定
				createTrackingIDEventID = CfwStringConverter.trimSpaces(createTrackingIDEventID);
				String[] createTrackingIDEventIDArray = createTrackingIDEventID.split(",");
				List<String> createTrackingIDEventIDList = Arrays
					.asList(createTrackingIDEventIDArray);
				if (createTrackingIDEventIDList.contains(eventId)) {
					String trackingID = config.get("tgfw.tracking.pre", "") + new VMID(); // 追跡IDを設定する.
					eventDto.getClass().getMethod(TRACKINGID, String.class).invoke(eventDto,
						trackingID);
					logger.info("イベント発行先追跡ID={}", trackingID);
				} else {
					setTrackingID(eventDto); // 追跡IDを設定する.
				}
			} else {
				setTrackingID(eventDto); // 追跡IDを設定する.
			}
			
			if (stubmode)  {
				//通信ログ出力（スタブモード）
				logger.debug("Event Message :  stub mode on.");
				if (eventDto != null) {
					logger.debug("Event Message :  " + helper.toJSON(eventDto));
				}
			} else {
				//イベントの発行
				eventGenerator.publish(eventId, eventDto, this.userId);						
			}
			logger.debug("Event Message : event published. EventId=" + eventId); 	//デバック出力
		} catch (CfwApplicationException cfwae) {
			logger.error("Event Message : " + cfwae.getMessage(),cfwae);
			throw cfwae;
		} catch (CfwMassEventMessageException cfwmeme) {
			logger.error("Event Message : " + cfwmeme.getMessage(), cfwmeme);
			throw cfwmeme;
		} catch (Throwable e) {
			CfwRuntimeException cfwre = new CfwRuntimeException("逐次イベントの発行処理で、予期しない障害が発生しました。 " , e);
			logger.error("Event Message : " + cfwre.getMessage());
			throw cfwre;
		} 
	}
	  
	  /**
	   * 逐次イベント発行用メッセージDTOに、フレームワークが内部記憶している追跡ID値をセットします.<br>
	   * 追跡ID値は、業務基幹コンポーネント内からのイベント発行の場合、業務基幹コンポーネントティアが管理している追跡IDをセットします.<br>
	   * 非同期(MQ)受信連携処理から呼出された逐次イベント発行の場合は、IRIS非同期(MQ)受信・業務アプリケーション・ハンドラーの処理時に設定された追跡IDがセットされます.<br>
	   * @param dtoobj	逐次イベントメッセージDTOオブジェクト.メッセージDTOには、trackingIDがプロパティとして定義されている必要がああります.
	   * @throws NoSuchMethodException 前提となるプロパティがDTOにない時、内部処理例外として発生します.
	   * @throws IllegalAccessException 前提となるプロパティ値がない等の条件により、内部処理例外として発生します.
	   * @throws InvocationTargetException 前提となるプロパティ値がない等の条件により、内部処理例外として発生します.
	   * @throws SecurityException サーバーセキュリティ設定の不備により、内部処理例外として発生する場合があります（通常は発生しません）.
	   */
	private void setTrackingID(Object dtoobj) 
			throws NoSuchMethodException,SecurityException,IllegalAccessException,InvocationTargetException {
		String trackingID = manager.get(); 
		//イベント発行DTOに対し、追跡IDをセットする.
		dtoobj.getClass().getMethod(TRACKINGID,String.class).invoke(dtoobj,trackingID);
	}
	  
	 /**
	   * 逐次イベント発行用メッセージDTOに、フレームワークが記憶しているユーザーIDをセットします.<br>
	   * ユーザーID値は、業務基幹コンポーネント内からの逐次イベント発行の場合、業務基幹コンポーネントティアが記憶管理しているユーザーIDがセットされます. <br>
	   * フロントエンドティアからの逐次イベント発行の場合、フロントエンドティアはセッション管理しているユーザーIDがセットされます.<br>
	   * 非同期(MQ)受信連携処理から呼出された逐次イベント発行の場合は、IRIS非同期(MQ)受信・業務アプリケーション・ハンドラーの処理時に設定された、
	   * IRIS非同期(MQ)受信設定プロパティ(iris_transaction.properties)に定義されたユーザーIDがセットされます.<br>
	   * @param dtoobj	逐次イベントメッセージDTOオブジェクト.メッセージDTOには、userIdがプロパティとして事前に設定されている必要があります.
	   * @throws NoSuchMethodException 前提となるプロパティがDTOにない時、内部処理例外として発生します.
	   * @throws IllegalAccessException 前提となるプロパティ値がない等の条件により、内部処理例外として発生します.
	   * @throws InvocationTargetException 前提となるプロパティ値がない等の条件により、内部処理例外として発生します.
	   * @throws SecurityException サーバーセキュリティ設定の不備により、内部処理例外として発生する場合があります（通常は発生しません）.
	   */
	private void setUserID(Object dtoobj) 
			throws NoSuchMethodException,SecurityException, IllegalAccessException, InvocationTargetException {
		
		//セッション情報をJSON形式の文字列に変換し、イベント発行DTOのユーザーIDにセットする。
		Map<String, Object> sessionMap = new HashMap<>();
		sessionMap.put("dukeId", this.userId);
		sessionMap.put("compKbn", this.compKbn);
		sessionMap.put("compCd", this.compCd);
		sessionMap.put("officeCd", this.officeCd);
		sessionMap.put("departmentCd", this.departmentCd);
		sessionMap.put("workplaceCd", this.workplaceCd);
		sessionMap.put("nwPlaceCd", this.nwPlaceCd);
		sessionMap.put("allowedAuthorityCd", this.allowedAuthorityCd);
		CfwJSONConverter<Map> converter = CfwJSONConverter.getConverter(Map.class);
		
		dtoobj.getClass().getMethod(USERID,String.class).invoke(dtoobj,converter.encode(sessionMap));
	}	  
	  
	 /**
	   * 逐次イベント発行用メッセージDTOに、フレームワークが記憶している起点処理IDをセットします.<br>
	   * @param dtoobj	逐次イベントメッセージDTOオブジェクト.メッセージDTOには、startFuncIdがプロパティとして事前に設定されている必要があります.
	   * @throws NoSuchMethodException 前提となるプロパティがDTOにない時、内部処理例外として発生します.
	   * @throws IllegalAccessException 前提となるプロパティ値がない等の条件により、内部処理例外として発生します.
	   * @throws InvocationTargetException 前提となるプロパティ値がない等の条件により、内部処理例外として発生します.
	   * @throws SecurityException サーバーセキュリティ設定の不備により、内部処理例外として発生する場合があります（通常は発生しません）.
	   */
	private void setFuncID(Object dtoobj) 
			throws NoSuchMethodException,SecurityException, IllegalAccessException, InvocationTargetException {
		//イベント発行DTOに対し、起点処理ＩＤをセットする.
		dtoobj.getClass().getMethod(BASEFUNCID,String.class).invoke(dtoobj,this.startFunctionId);
	}	  
	  
	 /**
	　    * 引き継ぎ情報を参照し、ユーザーＩＤ等のセッション情報及び、維持管理者かどうかのフラグを取得します。
	   */
	private void getUserInfo() {

		// CfwSession(画面起点・Frot発行/JP1起点)/CfwSOAPMessageHolder(画面起点・業務基幹発行）のそれぞれを参照し、ユーザーＩＤ等のセッション情報、維持管理者フラグの取得を行う。
		// 上記いずれにも維持管理者の値が存在しない場合、他システム起点のため、維持管理者のチェックは行わない。
		
		// CfwSessionからの取得（画面・Front/JP1用）
		try {
			Object session = CDI.current().select(Class.forName(CLASS_CFWSESSION)).get();
			PropertyDescriptor prop = new PropertyDescriptor("userInfo", session.getClass());
			Method getter	= prop.getReadMethod();
			Object userInfo;
			userInfo = getter.invoke(session);
			prop   = new PropertyDescriptor("dukeId", userInfo.getClass());
			getter = prop.getReadMethod();
			this.userId = (String)getter.invoke(userInfo);
			prop   = new PropertyDescriptor("defaultNonUpdatableFlg", userInfo.getClass());
			getter = prop.getReadMethod();
			this.adminUserFlg = (String)getter.invoke(userInfo);
			prop   = new PropertyDescriptor("compKbn", userInfo.getClass());
			getter = prop.getReadMethod();
			this.compKbn = (String)getter.invoke(userInfo);
			prop   = new PropertyDescriptor("compCd", userInfo.getClass());
			getter = prop.getReadMethod();
			this.compCd = (String)getter.invoke(userInfo);
			prop   = new PropertyDescriptor("officeCd", userInfo.getClass());
			getter = prop.getReadMethod();
			this.officeCd = (String)getter.invoke(userInfo);
			prop   = new PropertyDescriptor("departmentCd", userInfo.getClass());
			getter = prop.getReadMethod();
			this.departmentCd = (String)getter.invoke(userInfo);
			prop   = new PropertyDescriptor("workplaceCd", userInfo.getClass());
			getter = prop.getReadMethod();
			this.workplaceCd = (String)getter.invoke(userInfo);
			prop   = new PropertyDescriptor("nwPlaceCd", userInfo.getClass());
			getter = prop.getReadMethod();
			this.nwPlaceCd = (String)getter.invoke(userInfo);
			prop   = new PropertyDescriptor("allowedAuthorityCd", userInfo.getClass());
			getter = prop.getReadMethod();
			this.allowedAuthorityCd = (List<String>)getter.invoke(userInfo);
			
			return;
		} catch (Exception e) {
			// ClassNotFoundの場合は、稼働ティアにCfwSessionがない場合と考えられるのでCfwSOAPMessageHolderでのチェックに移行する。
			// IRIS非同期受信の際にInject可能だが、userInfoがnullのためNullPointerExceptionが発生する。その場合はCfwSOAPMessageHolderでのチェックに移行する。
		}
		
		// CfwSOAPMessageHolderからの取得（画面・業務基幹用）
		try {
			Object contextHolder = CDI.current().select(Class.forName(CLASS_CFWBIZ)).get();
			PropertyDescriptor prop = new PropertyDescriptor("dukeId", contextHolder.getClass());
			Method getter = prop.getReadMethod();
			this.userId = (String)getter.invoke(contextHolder);
			prop = new PropertyDescriptor("compKbn", contextHolder.getClass());
			getter = prop.getReadMethod();
			this.compKbn = (String)getter.invoke(contextHolder);
			prop = new PropertyDescriptor("compCd", contextHolder.getClass());
			getter = prop.getReadMethod();
			this.compCd = (String)getter.invoke(contextHolder);
			prop = new PropertyDescriptor("officeCd", contextHolder.getClass());
			getter = prop.getReadMethod();
			this.officeCd = (String)getter.invoke(contextHolder);
			prop = new PropertyDescriptor("departmentCd", contextHolder.getClass());
			getter = prop.getReadMethod();
			this.departmentCd = (String)getter.invoke(contextHolder);
			prop = new PropertyDescriptor("workplaceCd", contextHolder.getClass());
			getter = prop.getReadMethod();
			this.workplaceCd = (String)getter.invoke(contextHolder);
			prop = new PropertyDescriptor("nwPlaceCd", contextHolder.getClass());
			getter = prop.getReadMethod();
			this.nwPlaceCd = (String)getter.invoke(contextHolder);
			prop = new PropertyDescriptor("allowedAuthorityCd", contextHolder.getClass());
			getter = prop.getReadMethod();
			this.allowedAuthorityCd = (List<String>)getter.invoke(contextHolder);
			// CfwSOAPMessageHolder自体をInjectできたが、値が入っていない場合は別ティア稼働と判断し、次処理に移行する。
			prop = new PropertyDescriptor("defaultNonUpdatableFlg", contextHolder.getClass());
			getter = prop.getReadMethod();
			String wkAdminFlgBz = (String)getter.invoke(contextHolder);
			if (wkAdminFlgBz != null) {
				this.adminUserFlg = wkAdminFlgBz;
				return;
			}
		
		} catch (Exception e) {
			// ClassNotFoundの場合は、稼働ティアにCfwSOAPMessageHolderがない場合と考えられるので他システム起点と判断し、次処理に移行する。
		}
		
		// 他システム起点(IRIS非同期受信)の場合
		this.userId = mqstate.getDukeId();	
		this.adminUserFlg = mqstate.getAdmiUserFlag();
	}
	
	/**
	 * 引き継ぎ情報を参照し、処理ＩＤを取得します。
	 */
	private void getStartFunctionId() {
		
		// CfwCommunicationLogInfoHolder(画面起点・Frot発行/JP1起点)/CfwSOAPMessageHolder(画面起点・業務基幹発行）のそれぞれを参照し、処理ＩＤの取得を行う。
		
		// CfwCommunicationLogInfoHolderでの処理ＩＤ取得（フロント・シス統用）
		try {
			Object comlogtHolder = CDI.current().select(Class.forName(CLASS_CFWCOMHOLDER)).get();
			PropertyDescriptor prop = new PropertyDescriptor("startFunctionId", comlogtHolder.getClass());
			Method getter = prop.getReadMethod();
			String wkStartFunctionIdFr = (String)getter.invoke(comlogtHolder);

			// CfwCommunicationLogInfoHolder自体をInjectできたが、値が入っていない場合は別ティア稼働と判断し、次処理に移行する。
			if (wkStartFunctionIdFr != null) {
				this.startFunctionId = wkStartFunctionIdFr;
				return;
			}
		} catch (Exception e) {
			// ClassNotFoundの場合は、稼働ティアにCfwCommunicationLogInfoHolderがない場合と考えられるので他システム起点と判断し、次処理に移行する。
		}
		
		// CfwSOAPMessageHolderでの処理ＩＤ取得（業務基幹用）
		try {
			Object contextHolder = CDI.current().select(Class.forName(CLASS_CFWBIZ)).get();
			PropertyDescriptor prop = new PropertyDescriptor("startFunctionId", contextHolder.getClass());
			Method getter = prop.getReadMethod();
			String wkStartFunctionIdBz = (String)getter.invoke(contextHolder);

			// CfwSOAPMessageHolder自体をInjectできたが、値が入っていない場合は初期値とする。
			if (wkStartFunctionIdBz != null) {
				this.startFunctionId = wkStartFunctionIdBz;
				return;
			}
		} catch (Exception e) {
			this.startFunctionId = " ";
		}
	}

}