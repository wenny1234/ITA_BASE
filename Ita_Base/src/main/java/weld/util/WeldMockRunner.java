package weld.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import org.apache.poi.ss.formula.functions.T;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.reflection.Whitebox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.mockito.InjectMocks;

public class WeldMockRunner extends WeldRunner {

	// 最初に初期化
	static final Logger logger = LoggerFactory.getLogger(WeldMockRunner.class);

	// Junit対象クラス
	protected Class<?> klass;

	class WeldMockRunUtil {
		// Junit対象クラス
		protected Class<?> klass;

		// Junit対象クラスをMockitoで初期化
		protected Object mock;

		// weldで取得したオリジナルインスタンス
		protected Object runInstance;

		// weldで取得したrunInstanceのリアルターゲット
		protected Object weldMock;
		protected String weldMockName;

		// モック用に生成したインスタンスから
		protected Object injectMock;
		protected String injectMockName;

		protected String getTargetName(Class<? extends Annotation> annotationClass) {
			for (Field f : this.klass.getDeclaredFields()) {
				Annotation[] a = f.getAnnotationsByType(annotationClass);
				if (a != null && a.length == 1) {
					return f.getName();
				}
			}
			return null;
		}

		protected WeldMockRunUtil(Class<?> klass, Object runInstance) {
			try {
				this.klass = klass;
				this.runInstance = runInstance;

				// Mockito初期化
				this.mock = this.klass.newInstance();
				MockitoAnnotations.initMocks(this.mock);
				logger.debug("createMockInstance()::Mock initialized.");
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | SecurityException e) {
				e.printStackTrace();
			}
		}

		protected String getClassName(String in) {
			int start = 0;
			for (int i = 0; i < in.length(); i++) {
				char c = (char) in.charAt(i);
				if (c == '$') {
					return in.substring(start, i);
				}
			}
			return in;
		}

		protected void copyPrivateField(Object src, Object dst, Class<?> interfaceClass) {
			if (src != null && dst != null) {
				String srcName = getClassName(src.getClass().getTypeName());
				String dstName = getClassName(dst.getClass().getTypeName());
				logger.debug("copyPrivateField()::copy [" + srcName + "] to [" + dstName + "]");
				if (srcName != null && srcName.equals(dstName)) {
					for (Field f : dst.getClass().getDeclaredFields()) {
						logger.debug("copyPrivateField()::f = " + f);
						logger.debug("copyPrivateField()::f.getName = " + f.getName());
						logger.debug("copyPrivateField()::f.getType = " + f.getType());
						Object sObj = Whitebox.getInternalState(src, f.getName());
						logger.debug("copyPrivateField()::copy [" + srcName + "." + sObj + "] to [" + dstName + "]");
						Whitebox.setInternalState(dst, f.getName(), sObj);
					}
				}
			} else {
				// src.getClass().
				logger.warn("copyPrivateField()::invalid parameters. may be null or unmatched.["
						+ src.getClass().getTypeName() + "]to[" + dst.getClass().getTypeName() + "]");
			}
		}

		protected void setInjectMock() {
			this.weldMockName = getTargetName(WeldMocks.class);
			if (this.weldMockName != null) {
				this.weldMock = Whitebox.getInternalState(this.runInstance, this.weldMockName);
				logger.warn("setInjectMock()::WeldMock target found in [" + this.klass + "].");
			}else{
				logger.warn("setInjectMock()::WeldMock target not found in [" + this.klass + "].");
			}

			this.injectMockName = getTargetName(InjectMocks.class);
			if (this.injectMockName != null) {
				this.injectMock = Whitebox.getInternalState(this.mock, this.injectMockName);
				if (this.injectMock != null) {
					Whitebox.setInternalState(this.runInstance, this.injectMockName, this.injectMock);
					// Whitebox.setInternalState(this.weldMock,
					// this.injectMockName, this.injectMock);
					logger.debug("setInjectMock()::Mock target found in mock.");
				} else {
					logger.debug("setInjectMock()::Mock target not found in mock.");
				}
			} else {
				logger.warn("setInjectMock()::Mock target not found in [" + this.klass + "].");
			}
		}

		protected void setMocks() {
			if (this.klass == null || this.runInstance == null || this.mock == null) {
				// if (this.klass == null || this.weldMock == null || this.mock
				// == null) {
				logger.error("setMocks()::初期化が失敗しています(klass,weldmock,mock)=[" + this.klass + "][" + this.runInstance
						+ "][" + this.mock + "]");
			}
			for (Field f : this.klass.getDeclaredFields()) {
				Annotation[] a = f.getAnnotationsByType(Mock.class);
				if (a != null && a.length == 1) {
					// Whitebox.setInternalState(this.weldMock,f.getName(),
					Whitebox.setInternalState(this.runInstance, f.getName(),
							Whitebox.getInternalState(this.mock, f.getName()));
					logger.debug("setMocks()::setting Mocked instance[" + f.getName() + "]");
				}
			}
		}

		public Object getWeldMock() {
			return this.weldMock;
		}
	}

	// weld化されたインスタンスはsupper.runInstance

	public WeldMockRunner(Class<?> klass) throws InitializationError {
		super(klass);
		this.klass = klass;
	}

	@Override
	protected void runChild(FrameworkMethod method, RunNotifier notifier) {
		logger.info("runChild()::called::" + method.getMethod());
		WeldMockRunUtil wmru = new WeldMockRunUtil(this.klass, this.runInstance);
		wmru.setInjectMock();
		wmru.setMocks();
		super.runChild(method, notifier);
	}

}
