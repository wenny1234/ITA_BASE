package weld.util.sample;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.Test;

import jp.co.tokyo_gas.aion.tgfw.parts.db.sql.annotation.FwSQLMapper;
import jp.co.tokyo_gas.aion.tgfw.parts.db.sql.annotation.FwSQLTransactional;
//import jp.co.tokyo_gas.cirius.utils4ut.TestHelper4DAO;
import jp.co.tokyo_gas.cisfw.interceptor.CfwExceptionInterceptor4Other;
import jp.co.tokyo_gas.cisfw.utils.CfwDateUtils;
import jp.co.tokyo_gas.cisfw.ws.CfwCommunicationLogInfoHolder;

/**
 * @author Sogo
 * TGFWのDAOを含むアプリをJUnit上でテストするためのサンプルプログラム
 * ■2017/05/16現在分かったこと
 * (1)weld.seのバージョンは「CIRIUSUTHelper」をpomで組み込むと2.2.9.Finalが依存関係でロードされるが
 *    これだとweld.initialize()でnull pointer exceptionとなる。
 *		--> ローカルリポジトリ(LR)には2.2.13.Finalがあるため、pomでCIRIUSUTHelperより先に依存関係を指定する
 * (2)(1)に加えてpomの依存関係で「guava」が必要
 *		--> pomでLRにあった最新13.0を指定
 * (3)TGFWとして「CIRIUSUTHelper」の依存関係以外に最低「cisfw-afbiz」「cisfw-biz」が必要(詳細不明)
 *		--> pomで「cisfw-afbiz」バージョンなし、「cisfw-biz」1.5.8を指定
 * (4)アプリ共通としてCIRIUS_CXXが必要
 *		--> pomで「CIRIUS_CXX」${project.version}を指定
 * (5)設定ファイルとして最低「TGFW-RESOURCES/tgfw/sql-config/mybatis-config_DEFAULT.xml」「TGFW-RESOURCES/commonitem.properties」
 *		--> mybatis-config_DEFAULT.xmlは1期用のRTCを自分用にカスタマイズ、commonitem.propertiesは取り敢えず空で作成
 * (6)mybatis-config_DEFAULT.xmlで<environments default="xx">内のエレメントとして、
 *    導入ガイドで作成したDB2のインスタンスとスキーマ(ユーザーIDとパスワード)を指定。
 *		--> 導入ガイドで指定された「CIRI1062」「cirigt01」「cirigt01」を使用
 * (7)mybatis-config_DEFAULT.xmlで<mapper>と<package>が重複しているとエラーになる
 *		--> どちらでも動くようだが取り敢えず、<package>のみ使用
 * (8)WeldではRequestScopedが実装されていない
 *		--> CDIUNIT、DeltaSpike等利用する手もあるが、プリミティブに試したいため、
 *          ネットを参考に「WeldUtilities」「WeldTestExtension」を作成し、
 *          「META-INF/services/javax.enterprise.inject.spi.Extension」で「WeldTestExtension」を絶対パスで指定
 * (9)一番意味不明だが、アノテーションなしのpublicメソッドを経由すると「@FwSQLTransactionalが見つかりません」というエラーになる
 *		--> 経由したいメソッドをprivateにするか、アノテーション付きのpublicメソッドにする
 * (10)本利用までに解決が必要で一番難しい問題かもしれないが、テスト用プログラムを「src/test/java」に置くとWELD+TGFWの初期化が正常にできない
 *     (@FwSQLMapperがインジェクションされない)
 *		--> テスト用プログラムを「src/main/java」に置けば取り合えずWELD+TGFWの初期化
 * ()
 *		--> 
 */
@Dependent
public class DAOWeldJUnitSample {
	// RequestScoped実装の参考情報
	// http://memory.empressia.jp/article/107602460.html

	static Weld weld;
	static WeldContainer container;
	static Instance<Object> contInstance;
	static Instance<DAOWeldJUnitSample> myInstance;
	static {
		// Weldのコンテナを起動
		weld = new Weld();
		container = weld.initialize();
		
		BeanManager bm = container.getBeanManager();
//		bm.
		
		contInstance = container.instance();
		myInstance = contInstance.select(DAOWeldJUnitSample.class);
	}

	private static <U> boolean chkInstance(Instance<U> instance) {
		if (instance.isUnsatisfied()) {
			System.err.println("DAOWeldJUnitSample.chkInstance()::Controller is not implemented");
		} else if (instance.isAmbiguous()) {
			System.err.printf("DAOWeldJUnitSample.chkInstance()::Controller is implemented:%n");
			for (Object per : instance) {
				System.err.printf("DAOWeldJUnitSample.chkInstance():: - %s%n", per);
			}
		} else {
			System.err.printf("DAOWeldJUnitSample.chkInstance()::Controller is implemented: %s%n", instance.get());
			return true;
		}
		throw new RuntimeException("WELD+TGFWが正しく初期化されていないか、CDIの実装が間違っています");
	}

	public static void main(String[] args) {
		if (chkInstance(myInstance)) {
			DAOWeldJUnitSample DAOWeldJUnitSample = myInstance.get();
			DAOWeldJUnitSample.met();
		}

		weld.shutdown();
	}
	
//	@Test 
	public void normal1(){
		//　Weld+FWが正しく初期化されているかチェック
		if (chkInstance(myInstance)) {
		}
		myInstance.get().met();
	}

	/** データアクセス CXXDA01644 サービス契約番号取得用Mapper. */
	@Inject
	@FwSQLMapper
	private CXXDA01644Mapper cXXDA01644Mapper;

	// privateにしないで、publicだと@FwSQLTransactionalが見つからなく(動かなく)なる
	private void start() {
		if (cXXDA01644Mapper == null) {
			System.err.println("DAOWeldJUnitSample.start()::cXXDA01644Mapper is null.");
		}else{
			System.err.println("DAOWeldJUnitSample.start()::cXXDA01644Mapper is injected!!.");
		}
		met();
	}

	@FwSQLTransactional
	@Transactional
	public void met() {
		// ガス使用契約テーブルからサービス契約情報を取得する
		CXXDA01644InDaoDTO cXXDA01644InDaoDTO = new CXXDA01644InDaoDTO();
		cXXDA01644InDaoDTO.setSihKiyNo("111");

		// システム共通部品を呼び出し、ＯＰ日を取得する
		String operationDate = CfwDateUtils.getOperationDate();
		cXXDA01644InDaoDTO.setOpYmd(operationDate);

		CXXDA01644OutDaoDTO cXXDA01644OutDaoDTO = cXXDA01644Mapper.find(cXXDA01644InDaoDTO);
		System.err.println("result=["+cXXDA01644OutDaoDTO.getSyokyNo()+"]");
	}
	
	void dummy(){
		CfwExceptionInterceptor4Other aa;
		CfwCommunicationLogInfoHolder bb;
	}
}
