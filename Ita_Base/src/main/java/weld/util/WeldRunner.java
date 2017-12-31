package weld.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

import javax.enterprise.inject.Instance;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.dbunit.Assertion;
import org.dbunit.DatabaseUnitException;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.database.QueryDataSet;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.SortedTable;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.ext.db2.Db2MetadataHandler;
import org.dbunit.operation.DatabaseOperation;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.internal.AssumptionViolatedException;
import org.junit.internal.runners.model.EachTestNotifier;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import jp.co.tokyo_gas.aion.tgfw.parts.db.sql.annotation.FwSQLTransactional;

public class WeldRunner extends BlockJUnit4ClassRunner {

	// 最初に初期化
	static final Logger logger = LoggerFactory.getLogger(WeldRunner.class);

	protected static Weld weld;
	protected static WeldContainer container;
	protected static Instance<Object> contInstance;

	static {
		// Weldのコンテナを起動
		weld = new Weld();
		container = weld.initialize();
		contInstance = container.instance();
		createSession();
	}

	protected static SqlSessionFactory sqlSessionFactory;
	protected static SqlSession sqlSession;
	protected static IDatabaseConnection connection;

	/**
	 * <ul>
	 * <li>DBセッションを作成。</li>
	 * </ul>
	 * 
	 * @throws Exception
	 *             例外発生
	 */
	public static void createSession() {
		logger.debug("createSession()::called");
		// create an SqlSessionFactory
		Reader reader;
		try {
			reader = Resources.getResourceAsReader("TGFW-RESOURCES/tgfw/sql-config/mybatis-config_DEFAULT.xml");
			sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
			sqlSession = sqlSessionFactory.openSession();
			reader.close();
			connection = new DatabaseConnection(sqlSession.getConnection());

			// dbunit jdbcバグ対応
			DatabaseConfig config = connection.getConfig();
			config.setProperty(DatabaseConfig.PROPERTY_METADATA_HANDLER, new Db2MetadataHandler());

			// dbunit ブランクフィールド対応(2017/11/13)
			config.setProperty(DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS, Boolean.TRUE);
			
		} catch (IOException | DatabaseUnitException e) {
			e.printStackTrace();
		}
	}

	protected static <U> boolean chkInstance(Instance<U> instance) {
		if (instance.isUnsatisfied()) {
			logger.warn("chkInstance()::インスタンスが見つからないため、Injecされません。");
		} else if (instance.isAmbiguous()) {
			logger.warn("chkInstance()::複数のインスタンスが見つかったため、正しくInjectされない可能性があります。");
			for (Object per : instance) {
				logger.warn("chkInstance():: - %s%n", per);
			}
		} else {
			logger.info("chkInstance()::インスタンスが見つかりました。%s%n", instance.get());
			return true;
		}
		throw new RuntimeException("WELD+TGFWが正しく初期化されていないか、CDIの実装が間違っています");
	}

	protected Class<?> myClass;
	protected Instance<?> myInstance;
	protected Object runInstance;

	public WeldRunner(Class<?> klass) throws InitializationError {
		super(klass);
		myClass = klass;
		logger.debug("WeldRunner()::myClass = " + myClass);
		myInstance = contInstance.select(myClass);
		if (chkInstance(myInstance)) {
			runInstance = myInstance.get();
		}
	}

	protected void restoreTables(String infile) {
		String dir = System.getProperty("user.dir");
		logger.info("tableRestore()::called::restore[" + dir + "/" + infile + "]");
		try {
			File file = new File(dir + "/" + infile);
			// テストデータを投入する
			@SuppressWarnings("deprecation")
			IDataSet dataSet = new FlatXmlDataSet(new FileInputStream(file));
//			FlatXmlDataSetBuilder builder = new FlatXmlDataSetBuilder();
//			IDataSet dataSet = builder.build(new InputSource(file.getAbsolutePath()));
			
			DatabaseOperation.CLEAN_INSERT.execute(connection, dataSet);
		} catch (DatabaseUnitException | SQLException | IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (connection != null)
					connection.getConnection().commit();
			} catch (SQLException e) {
			}
		}
	}

	protected void backupTables(String outfile, String[] tables) {
		String dir = System.getProperty("user.dir");
		logger.info("backupTables()::called::backup[" + dir + "/" + outfile + "]");
		try {
			// 現状のバックアップを取得
			QueryDataSet partialDataSet = new QueryDataSet(connection);
			for (int i = 0; i < tables.length; i++) {
				partialDataSet.addTable(tables[i]);
			}
			File file = new File(dir + "/" + outfile);
			FlatXmlDataSet.write(partialDataSet, new FileOutputStream(file));
		} catch (DatabaseUnitException | IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (connection != null)
					connection.getConnection().commit();
			} catch (SQLException e) {
			}
		}
	}

	protected void compareTables(String resultfile, DBunitTable[] tables) {
		String dir = System.getProperty("user.dir");
		logger.info("compareTables()::called::compare[" + dir + "/" + resultfile + "]");
		try {
			// 期待値を読み込む
			File file = new File(dir + "/" + resultfile);
			@SuppressWarnings("deprecation")
			IDataSet xmlDataSet = new FlatXmlDataSet(new FileInputStream(file));
//			FlatXmlDataSetBuilder builder = new FlatXmlDataSetBuilder();
//			IDataSet xmlDataSet = builder.build(new InputSource(file.getAbsolutePath()));

			// 現在値を読み込む
			IDataSet dbDataSet = connection.createDataSet();

			// テーブルごとに比較
			for (int i = 0; i < tables.length; i++) {
				ITable xmlTable = xmlDataSet.getTable(tables[i].name());
				xmlTable = new SortedTable(xmlTable, tables[i].sort());

				ITable dbTable = dbDataSet.getTable(tables[i].name());
				dbTable = new SortedTable(dbTable, tables[i].sort());

				// 比較実行
				Assertion.assertEqualsIgnoreCols(xmlTable, dbTable, tables[i].ignore());
			}

		} catch (DatabaseUnitException | SQLException | IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (connection != null)
					connection.getConnection().commit();
			} catch (SQLException e) {
			}
		}
	}

	protected final void myRunLeaf(Statement statement, Description description, FrameworkMethod method,
			RunNotifier notifier) {
		EachTestNotifier eachNotifier = new EachTestNotifier(notifier, description);
		eachNotifier.fireTestStarted();
		try {
			Annotation[] ann = method.getAnnotations();
			DBunitCompare compare = null;

			for (int i = 0; i < ann.length; i++) {
				logger.info("myRunLeaf()::" + ann[i].annotationType());
				if (ann[i].annotationType().equals(DBunitCompare.class)) {
					compare = (DBunitCompare) ann[i];
				}
			}

			statement.evaluate();

			// 実行直後に期待値と比較する
			if (compare != null) {
				compareTables(compare.file(), compare.tables());
			}

		} catch (AssumptionViolatedException e) {
			eachNotifier.addFailedAssumption(e);
		} catch (Throwable e) {
			eachNotifier.addFailure(e);
		} finally {
			eachNotifier.fireTestFinished();
		}
	}

	@Override
	protected void runChild(FrameworkMethod method, RunNotifier notifier) {
		logger.info("runChild()::called::" + method.getMethod());
		try {
			WeldSeUtil.resetScope(container.getBeanManager());
		} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		}

		Annotation[] ann = method.getAnnotations();
		boolean isFwSQLTransactional = false;
		// DBunitCompare compare = null;
		DBunitPostRestore restore = null;
		DBunitSaveResult save = null;

		for (int i = 0; i < ann.length; i++) {
			logger.info("runChild()::" + ann[i].annotationType());
			if (ann[i].annotationType().equals(FwSQLTransactional.class)) {
				// logger.debug("runChild():("+i+"):annoteated");
				isFwSQLTransactional = true;
			} else if (ann[i].annotationType().equals(DBunitBackup.class)) {
				DBunitBackup an = (DBunitBackup) ann[i];
				backupTables(an.file(), an.tables());
			} else if (ann[i].annotationType().equals(DBunitPreRestore.class)) {
				DBunitPreRestore an = (DBunitPreRestore) ann[i];
				restoreTables(an.file());
			} else if (ann[i].annotationType().equals(DBunitPostRestore.class)) {
				restore = (DBunitPostRestore) ann[i];
				// } else if
				// (ann[i].annotationType().equals(DBunitCompare.class)) {
				// compare = (DBunitCompare) ann[i];
			} else if (ann[i].annotationType().equals(DBunitSaveResult.class)) {
				save = (DBunitSaveResult) ann[i];
			} else {
				// logger.debug("runChild():("+i+"):"+ann[i]);
			}
		}
		if (!isFwSQLTransactional) {
			logger.warn("runChild():「@FwSQLTransactional」が指定されていないため、DAO実行がスキップされる可能性ががあります。(エラーも何も出力されません！！！)");
		}

		// super.runChild(method, notifier);
		Description description = describeChild(method);
		if (isIgnored(method)) {
			notifier.fireTestIgnored(description);
		} else {
			// runLeaf(methodBlock(method), description, notifier);
			// finalのため名前変更して修正
			myRunLeaf(methodBlock(method), description, method, notifier);
		}

		// ここで比較してもeachNotifierが終了していてどのメソッドの比較か分からなくなるため、myRunLeaf()の中で比較するように修正
		// if (compare != null) {
		// compareTables(compare.file(), compare.tables());
		// }

		// 戻す前にセーブする
		if (save != null) {
			logger.info("runChild()::save result::file=" + save.file() + "tables=" + save.tables());
			backupTables(save.file(), save.tables());
		}

		// 元、もしくは指定された内容に戻す
		if (restore != null) {
			logger.info("runChild()::restore tables::" + restore.file());
			restoreTables(restore.file());
		}

		logger.debug("runChild()::end");
	}

	@Override
	protected Statement methodInvoker(FrameworkMethod method, Object test) {
		logger.info("methodInvoker()::called::" + method);
		System.out.println("1.1:thread : " + Thread.currentThread());
		return super.methodInvoker(method, runInstance);
	}
}
