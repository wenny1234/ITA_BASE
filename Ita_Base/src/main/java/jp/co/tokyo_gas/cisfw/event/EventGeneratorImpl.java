package jp.co.tokyo_gas.cisfw.event;

import static jp.co.tokyo_gas.cisfw.utils.CfwLogHelper.END_LOG_IDENTIFIER;
import static jp.co.tokyo_gas.cisfw.utils.CfwLogHelper.PARAM_LOG_IDENTIFIER;
import static jp.co.tokyo_gas.cisfw.utils.CfwLogHelper.START_LOG_IDENTIFIER;

import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.bind.DataBindingException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.co.tokyo_gas.aion.tgfw.parts.config.FwPropertyManager;
import jp.co.tokyo_gas.cisfw.event.annotation.ProductionMode;
import jp.co.tokyo_gas.cisfw.exception.CfwMassEventMessageException;
import jp.co.tokyo_gas.cisfw.utils.CfwLogHelper;

/**
 * イベントを生成します。
 * 
 * オンラインで利用し、共通ヘッダ項目の自動set機能を使う場合には、EventGeneratorをInjectしてください。
 * <pre>
 *   {@code @Inject @ProductionMode}
 *	 private EventGenerator publisher;
 * </pre>
 * 
 * Javaアプリケーション等でバッチ的に利用し、共通ヘッダ項目も各アプリでsetする場合には、getInstance()してください。
 * <pre>
 * 	 EventGenerator publisher = EventGeneratorImpl.getInstance();
 *   publisher.publish(イベント名, 引数);
 * </pre>
 * 
 * イベント名は、jndiPrefix （既定値 = jms/）を追加し、JNDI から Topic を取得するのに使用します。
 * 引数は、JAXB のオブジェクトを設定可能で、JAXB.marshal で生成された文字列を TextMessage として設定します。
 * 
 * このライブラリを利用する為には、以下設定が JNDI 経由で取得可能な環境で実行されていることが必要です。
 * 
 *  jms/cafe_topic_connection_factory : TopicConnectionFactory の取得
 *  jms/イベント名 : publish で設定される、各 Topic
 * 
 * @version $Id$
 */
@ApplicationScoped
@ProductionMode
public class EventGeneratorImpl implements EventGenerator {

	private static final String DEFAULT_JNDI_NAME = "jms/cafe_topic_connection_factory";
	private static final String DEFAULT_JNDI_PREFIX = "jms/";

	private static EventGeneratorImpl instance = null;

	private final Logger logger = LoggerFactory.getLogger(getClass());
	//通信ログ用ロガー
	private final Logger comLogger = LoggerFactory.getLogger(
			getClass().getPackage().getName() + ".EventGenerator");

	private String jndiname = DEFAULT_JNDI_NAME;
	private String jndiPrefix = DEFAULT_JNDI_PREFIX;
	private InitialContext ic = null;
	private TopicConnectionFactory cf = null;
//	private TopicConnection cn = null;
	private boolean ready = true;
	
	/** ログ出力内容の識別子です。 **/
	public static final String IDENTIFIER = "EM";
	/** 開始ログのフォーマットです。 **/
	public static final String START_LOG_FORMAT = "[{}] [{}] [{}] [{}] [{}] [{}] [{}] [{}] [{}] [{}]";
	/** パラメーターログのフォーマットです。 **/
	public static final String PARAM_LOG_FORMAT = "[{}] [{}] [{}] [{}] [{}]";
	/** 終了ログのフォーマットです。 **/
	public static final String END_LOG_FORMAT = "[{}] [{}] [{}] [{}] [{}] [{}] [{}ms] [{}] [{}] [{}]";

	/** インターセプター用共通部品です。 **/
	@Inject
	private CfwLogHelper cfwLogHelper;

	private Map<Class<?>, JAXBContext> jaxbctx = new ConcurrentHashMap<>();

	/** プロパティファイル名です。 */
	private static final String PROP_NAME = "tgfw/massEventMessage-file";
	/** プロパティ管理クラスです。 */
	@Inject
	private FwPropertyManager manager;
	
	public static EventGeneratorImpl getInstance() {
		if (instance == null) {
			synchronized (EventGeneratorImpl.class) {
				if (instance == null) {
					instance = new EventGeneratorImpl();
				}
			}
		}
		return instance;
	}
	
	public EventGeneratorImpl() {
	}
	
	private synchronized void init() {
		if (cf == null) {  // 念の為、Synchronized 後、もう一度
			logger.trace("init start");
			try {
				ic = new InitialContext();
				cf = (TopicConnectionFactory) ic.lookup(jndiname);
//				cn = cf.createTopicConnection();
				// publish だけで受信しないので start 不要
				//cn.start();
			} catch(NamingException e) {
				ready = false;
				throw new RuntimeException(e);
//			} catch (JMSException e) {
//				ready = false;
//				throw new RuntimeException(e);
			}
		}
	}


	/**
	 *  逐次処理を起動するイベントを発生させる
	 * 
	 * @param eventName   発行するイベント名。具体的には、Topic を指す JNDI 名を指定
	 * @param param イベントに対する引数。 JAXB により、XMLに marshal 出来るオブジェクトであること。
	 * @param userId ユーザーID。
	 */
//	@Interceptors({EventFilter.class, EventHeaderSetter.class})
	public void publish(String eventName, Object param, String userId) {
		if (!isReady()) {
			throw new RuntimeException("Not Ready!");
		}

		long startTime = System.currentTimeMillis();
		String customerNo = cfwLogHelper.find(param, cfwLogHelper.getKeys());
		// 開始ログ
		comLogger.info(START_LOG_FORMAT, START_LOG_IDENTIFIER, IDENTIFIER, eventName, "-", userId, "", "", "", "", customerNo);

		String result = "SUCCESS";
		String msg = null;
		TopicConnection cn = null;
		try {
			if (param != null) {
				msg = marshal(param);
			}
			comLogger.debug(PARAM_LOG_FORMAT, PARAM_LOG_IDENTIFIER, IDENTIFIER, eventName, "-", msg);

			if (cf == null) {
				// Connection は HeavyWeight 、Concurrent での利用が可能なので共有する 
				init();
			}
			
			// 逐次イベントメッセージを確認する。
			/** 層間ファイル管理用のプロパティです。 */
			Properties props = manager.getProperties(PROP_NAME);
			int threshold = Integer
				.parseInt((String) props.get("massEventMessage.file.threshold.size"));
			
			// XMLの長さが(閾値/3)を超えないかチェック
			if (msg.length() > (threshold / 3)) {
				int size = msg.getBytes("UTF-8").length;
				if (size > threshold) {
					throw new CfwMassEventMessageException(
						"逐次イベントメッセージが閾値を超えています。　[閾値]：%s　[メッセージサイズ]：%s", threshold, size);
				}
			}

			// 要求都度、Connection生成しているように見えるが、実際には同じオブジェクトIDが使いまわされているのでOKとする。
			cn = cf.createTopicConnection();

			// JMS トランザクションではなく、JTA に参加させる
			TopicSession s = cn.createTopicSession(false, TopicSession.AUTO_ACKNOWLEDGE);

			Topic t = (Topic) ic.lookup(jndiPrefix + eventName);
			TopicPublisher p = s.createPublisher(t);

			TextMessage tm = s.createTextMessage(msg);
			p.publish(tm);
			
			p.close();
			s.close();
		} catch(JMSException | NamingException | UnsupportedEncodingException e) {
			result = "ERROR";
			throw new RuntimeException("errored message:" + msg, e);
		} finally {
			long endTime = System.currentTimeMillis();
			long time = (endTime - startTime);
			// 終了ログ
			comLogger.info(END_LOG_FORMAT, END_LOG_IDENTIFIER, IDENTIFIER, eventName, "-", userId, result, time, "", "", "");
			if (null != cn) {
				try {
//					cn.stop();
					cn.close();
				} catch (JMSException e) {
					logger.warn(e.getMessage(), e);
				}
			}
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private String marshal(Object jaxbObject) {
/*
 *    以下のコードで済ませたかったが、名前空間を処理してくれない。
		StringWriter sw = new StringWriter();
		JAXB.marshal(param, sw);
		return sw.toString();
*/
		StringWriter sw = new StringWriter();
		try {
			JAXBContext context;

			if (jaxbObject instanceof JAXBElement) {
				context = jaxbCtxFactory(((JAXBElement<?>) jaxbObject).getDeclaredType());
			} else {
				Class<?> clazz = jaxbObject.getClass();
				XmlRootElement r = clazz.getAnnotation(XmlRootElement.class);
				context = jaxbCtxFactory(clazz);
				if (r == null) {
					XmlType xmltype = clazz.getAnnotation(XmlType.class);
					String nn;
					if (xmltype != null) {
						nn = xmltype.name();
					} else {
						// JAXB 生成オブジェクトでない可能性が高い。クラス名で代用。
						nn = clazz.getSimpleName();
					}
					XmlSchema ns = clazz.getPackage().getAnnotation(XmlSchema.class);
					QName qn = null;
					if (ns != null && ns.namespace() != null) {
						qn = new QName(ns.namespace(), nn);
					} else {
						qn = new QName(nn);
					}
					jaxbObject = new JAXBElement(qn, clazz, jaxbObject);
				}
			}

			Marshaller m = context.createMarshaller();
			// m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,true);
			m.marshal(jaxbObject, new StreamResult(sw));
			return sw.toString();
		} catch (JAXBException e) {
			throw new DataBindingException(e);
		}
	}
	
	/**
	 * JAXBContextのインスタンスを取得します。
	 * @param cls JAXBオブジェクト
	 * @return JAXBContext
	 * @throws JAXBException JAXB例外
	 */
	private JAXBContext jaxbCtxFactory(Class<?> cls) throws JAXBException {
		JAXBContext ret = jaxbctx.get(cls);
		if (ret == null) {
			synchronized (jaxbctx) {
				ret = jaxbctx.get(cls);
				if (ret == null) {
					ret = JAXBContext.newInstance(cls);
					jaxbctx.put(cls, ret);
				}
			}
		}
		return ret;
	}
	
	public boolean isReady() {
		return ready;
	}

	/**
	 * 既定値と異なるTopicConnectionFactory の JNDI 名を設定する。同時に、エラー状態を解除する
	 * @param name  TopicConnectionFactory の JNDI名
	 */
	public void setTopicConnectionFactoryName(String name) {
		jndiname = (name == null ? DEFAULT_JNDI_NAME : name);
		cf = null;
		ic = null;
		ready = true;
		logger.trace("TopicConnectionFactoryName is set to " + jndiname);
	}

	/**
	 * 既定値と異なるTopic名の Prefix を付けたい場合に設定する
	 * @param name  publish の Event 名を lookup する際に付加される文字列
	 */
	public void setJNDIPrefix(String name) {
		jndiPrefix = (name == null ? DEFAULT_JNDI_PREFIX : name);
		logger.trace("jndiPrefix is set to " + jndiPrefix);
	}

}
