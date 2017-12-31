package weld.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.context.spi.Context;
import javax.enterprise.inject.spi.BeanManager;

import org.jboss.weld.bean.builtin.BeanManagerProxy;
import org.jboss.weld.context.AbstractBoundContext;
import org.jboss.weld.context.PassivatingContextWrapper;
import org.jboss.weld.context.bound.MutableBoundRequest;
import org.jboss.weld.manager.BeanManagerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WeldSeUtil {
	// 最初に初期化
	static final Logger logger = LoggerFactory.getLogger(WeldSeUtil.class);

	static final String RequestScoped = "javax.enterprise.context.RequestScoped";
	static final String DeclaredMethod = "getContexts";

	public static void supportScopes(BeanManager bm)
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

		Map<String, Object> reqMap = new HashMap<>();
		activate(bm, RequestScoped.class, reqMap);

		Map<String, Object> sessionMap = new HashMap<>();
		activate(bm, SessionScoped.class, sessionMap);

		activate(bm, ConversationScoped.class, new MutableBoundRequest(reqMap, sessionMap));
	}

	private static <S> void activate(BeanManager beanManager, Class<? extends Annotation> scopeType, S s)
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

		AbstractBoundContext<S> boundContext = findFirstContext(beanManager, scopeType);

		boundContext.associate(s);
		boundContext.activate();
	}

	public static <S> void activateContextWithStorage(BeanManager bm, Class<? extends Annotation> scopeType, S storage)
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		AbstractBoundContext<S> boundContext = WeldSeUtil.findFirstContext(bm, scopeType);
		boundContext.associate(storage);
		boundContext.activate();
	}

	public static <S> AbstractBoundContext<S> findFirstContext(BeanManager beanManager,
			Class<? extends Annotation> scopeType)
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		// method.invoke
		BeanManagerImpl beanManagerImpl;
		if (beanManager instanceof BeanManagerProxy) {
			BeanManagerProxy beanManagerProxy = (BeanManagerProxy) beanManager;
			beanManagerImpl = beanManagerProxy.delegate();
		} else if (beanManager instanceof BeanManagerImpl) {
			beanManagerImpl = (BeanManagerImpl) beanManager;
		} else {
			throw new IllegalStateException("BeanManagerが未知の型です");
		}

		Method method = BeanManagerImpl.class.getDeclaredMethod(DeclaredMethod);
		method.setAccessible(true);

		@SuppressWarnings("unchecked")
		Map<Class<? extends Annotation>, List<Context>> contextsMap = (Map<Class<? extends Annotation>, List<Context>>) method
				.invoke(beanManagerImpl);

		// context取得
		logger.debug("scopeType.getName():[" + scopeType.getName() + "]");
		List<Context> contexts = contextsMap.get(scopeType);
		if (contexts.isEmpty()) {
			throw new IllegalStateException("指定されたタイプ[" + scopeType.getName() + "]のコンテキストが見つかりません");
		}
		Context context = contexts.get(0);
		if (RequestScoped.equals(scopeType.getName())) {
			logger.info("Detecting..[javax.enterprise.context.RequestScoped]");
			logger.debug("contexts.size()=" + contexts.size());
			for (int i = 0; i < contexts.size(); i++) {
				context = contexts.get(i);
				logger.debug("contexts.get(" + i + ")=" + context);
				// System.err.println("context.toString()="+context.toString());
				// System.err.println("context.getScope()="+context.getScope());
			}
			context = contexts.get(0);
		}
		if (!(context instanceof AbstractBoundContext)) {
			context = PassivatingContextWrapper.unwrap(context);
		}

		// 活性化
		@SuppressWarnings("unchecked")
		AbstractBoundContext<S> boundContext = (AbstractBoundContext<S>) context;

		return boundContext;
	}

	public static void resetScope(BeanManager bm)
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		AbstractBoundContext<?>[] contexts = { findFirstContext(bm, RequestScoped.class),
				findFirstContext(bm, SessionScoped.class), findFirstContext(bm, ConversationScoped.class) };
		for (AbstractBoundContext<?> context : contexts) {
			if (context.isActive()) {
				context.deactivate();
			}
			context.cleanup();
		}
		supportScopes(bm);
	}
}
