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

public class WeldMockExRunner extends WeldMockRunner {

	// 最初に初期化
	static final Logger logger = LoggerFactory.getLogger(WeldMockExRunner.class);

	public WeldMockExRunner(Class<?> klass) throws InitializationError {
		super(klass);
	}

	@Override
	protected void runChild(FrameworkMethod method, RunNotifier notifier) {
		logger.info("runChild()::called::" + method.getMethod());
		WeldMockRunUtil wmru = new WeldMockRunUtil(this.klass, this.runInstance);
		wmru.setInjectMock();
		wmru.setMocks();

		if(wmru.getWeldMock() != null){
			WeldMockStaticUtil.start(wmru.getWeldMock());
			logger.info("runChild()::WeldMockUtil.start() called.");
		}else{
			logger.warn("runChild()::WeldMockUtil.start() did not call. may be no @WeldMocks.");
		}
		
		super.runChild(method, notifier);

		WeldMockStaticUtil.stop();
	}

}
