package weld.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.OngoingStubbing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import weld.util.sample.CXXDA01644InDaoDTO;
import weld.util.sample.CXXDA01644Mapper;

public class WeldMockStaticUtil {
	// 最初に初期化
	final static Logger logger = LoggerFactory.getLogger(WeldMockStaticUtil.class);
	
	static WeldMockStaticUtil my;

	protected HashMap<Object, Object> injects = new HashMap<Object, Object>();
	protected HashMap<Object, String> names = new HashMap<Object, String>();

	protected Object realInstance;
	protected String realName;
	protected Class<?> realKlass;

	public static void start(Object real) {
		if(my == null){
			logger.info("start() ::  starting WeldMockUtil...");
			my = new WeldMockStaticUtil(real);
			return;
		}	
		logger.warn("start() ::  already started WeldMockUtil...(now not start)");
	}

	public static void stop() {
		if(my == null){
			logger.warn("stop() ::  already stoped WeldMockUtil...(now not top)");
			return;
		}	
		logger.info("stop() ::  stopping WeldMockUtil...");
		my.restoreInjects();
		my = null;
	}
	
	public WeldMockStaticUtil(Object real) {
		this.realInstance = real;
		this.realName = getClassName(this.realInstance.getClass().getName());
		try {
			this.realKlass = Class.forName(realName);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		logger.info("target = " + this.realInstance);
		logger.info("targetType = " + this.realName + ", target = " + this.realInstance.getClass().getSimpleName()
				+ ", " + this.realKlass);

	}

	static protected String getClassName(String in) {
		int start = 0;
		for (int i = 0; i < in.length(); i++) {
			char c = (char) in.charAt(i);
			/*
			 * if (c == '.') { start = i+1; } else
			 */
			if (c == '$') {
				return in.substring(start, i);
			}
		}
		return in;
	}

	static public void chgInject(Object o) {
		if(my != null){
			logger.debug("chgInject() :: my.myChgInject() calling...");
			my.myChgInject(o);
		}
		logger.debug("chgInject() :: my.myChgInject() not call. my may be null.");
	}
	
	public void myChgInject(Object o) {
		if (o == null) {
			logger.error("chgInject() :: 入力が不正です");
			return;
		}
		String oname = getClassName(o.getClass().getSimpleName());
		if (oname == null) {
			logger.error("chgInject() :: 入力クラス名が不正です");
			return;
		}
		
		if(injects.get(o) != null){
			logger.info("[" + oname + "]は既に交換されています。");
			return;
		}

		for (Field f : realKlass.getDeclaredFields()) {
			String ftype = f.getType().getSimpleName();
			String fname = f.getName();
			try {
				if (oname.equals(ftype)) {
					logger.debug("[" + fname + "::" + ftype + "]が[" + realKlass.getSimpleName() + "]に見つかりました。");
					Object inject = Whitebox.getInternalState(realInstance, fname);
					logger.debug("inject = " + inject);
					// 見つけたら即リターン
					injects.put(o, inject);
					names.put(o, fname);
					Whitebox.setInternalState(realInstance, fname, o);
					return;
				}
			} catch (IllegalArgumentException e1) {
				e1.printStackTrace();
			}
		}
		// logger.error("[" + klass + "]のメンバーが[" + rTarget + "]に見つかりません。");
	}

	static public <T> Object useInject(Object o) {
		if(my != null){
			logger.debug("useInject() :: my.myUseInject() calling...");
			return my.myUseInject(o);
		}
		logger.debug("useInject() :: my.myUseInject() not call. my may be null.");
		return null;
	}
	
	public <T> Object myUseInject(Object o) {
		Object ret = injects.get(o);
		if (ret == null) {
			logger.error("[" + o + "]が見つかりません。");
		} else {
			logger.debug("[" + o + "]が見つかりました。");
		}
		return ret;
	}

	public void restoreInjects() {
		Set<Object> s = injects.keySet();
		for (Object o : s.toArray()) {
			logger.debug("[" + realKlass.getSimpleName() + "]の[" + names.get(o) + "]を元に戻します。");
//			logger.debug("inject = ");
			Object inj = injects.get(o);
			logger.debug("inject = " + inj);
//			System.err.println("inject = " + inj.getClass().getSimpleName());
			Whitebox.setInternalState(realInstance, names.get(o), injects.get(o));
		}
		
		// 2回
//		this.injects = new HashMap<Object, Object>();
//		this.names = new HashMap<Object, String>();
	}
	
	static public <T> OngoingStubbing<Object> mockThenReal(Object targetInject, String methodName,
			ArgumentCaptor<?>... params) {
		if(my != null){
			logger.debug("mockThenReal() :: my.myMockThenReal() calling...");
			return my.myMockThenReal(targetInject,methodName,params);
		}
		logger.debug("mockThenReal() :: my.myMockThenReal() not call. my may be null.");
		return null;
	}

	public <T> OngoingStubbing<Object> myMockThenReal(Object targetInject, String methodName,
			ArgumentCaptor<?>... params) {
		myChgInject(targetInject);
		Method mm = getMethod(targetInject.getClass(), methodName, param2type(params));
		Method rm = getMethod(myUseInject(targetInject).getClass(), methodName, param2type(params));
		logger.debug("mm = " + mm + ", rm = " + rm);
		if (mm != null && rm != null) {
			try {
				return Mockito.when(mm.invoke(targetInject, param2cap(params))).then(new Answer<Object>() {
					public Object answer(InvocationOnMock invocation) throws Throwable {
						return rm.invoke(myUseInject(targetInject), invocation.getArguments());
					}
				});
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}
		} else {
			logger.error("not found [" + methodName + "] in [" + targetInject.getClass().getSimpleName() + "]");
			logger.error("not found [" + methodName + "] in [" + targetInject.getClass().getTypeName() + "]");
		}
		return null;
	}

	// public <T> OngoingStubbing<Object> when(Object target, String methodName,
	// Object... params) {
	// Method m = getMethod(target.getClass(), methodName, param2type(params));
	// if (m != null) {
	// try {
	// // m.invoke(target, params);
	// return Mockito.when(m.invoke(target, params));
	// } catch (IllegalAccessException | IllegalArgumentException |
	// InvocationTargetException e) {
	// e.printStackTrace();
	// }
	// } else {
	// System.out.println("not found [t1]");
	// logger.error("not found [" + methodName + "] in [" +
	// target.getClass().getSimpleName() + "]");
	// logger.error("not found [" + methodName + "] in [" +
	// target.getClass().getTypeName() + "]");
	// }
	// return null;
	// }

	public static Class<?>[] param2type(ArgumentCaptor<?>... params) {
		ArgumentCaptor<?>[] pars = (ArgumentCaptor[]) params;
		ArrayList<Class<?>> typs = new ArrayList<Class<?>>();
		for (ArgumentCaptor<?> p : pars) {
			Class<?> clazz = (Class<?>) Whitebox.getInternalState(p, "clazz");
			logger.debug("p is ArgumentCaptor. :: clazz:" + clazz);
			typs.add(clazz);
		}
		return typs.toArray(new Class<?>[] {});
	}

	public static Object[] param2cap(ArgumentCaptor<?>... params) {
		ArgumentCaptor<?>[] pars = (ArgumentCaptor[]) params;
		ArrayList<Object> caps = new ArrayList<Object>();
		for (ArgumentCaptor<?> p : pars) {
			caps.add(p.capture());
		}
		return caps.toArray(new Object[] {});
	}

	public static Class<?>[] param2type(Object... params) {
		Object[] pars = (Object[]) params;
		ArrayList<Class<?>> typs = new ArrayList<Class<?>>();
		for (Object p : pars) {
			logger.debug("p = " + p + "/ p.getClass = " + p.getClass());
			typs.add(p.getClass());
		}
		return typs.toArray(new Class<?>[] {});
	}

	public static Method getMethod(Class<?> klass, String name, Class<?>[] typs) {
		if (klass == null || name == null || typs == null) {
			logger.error("some parameters are null. will return null");
			return null;
		}
		Method[] mths = klass.getDeclaredMethods();
		for (Method m : mths) {
			// logger.debug("m.getName():" + m.getName());
			if (name.equals(m.getName())) {
				if (isSameParam(m.getParameters(), typs)) {
					logger.debug("same param[" + m.getName() + "]");
					return m;
				} else {
					logger.debug("not same param[" + m.getName() + "]");
				}
			} else {
				logger.debug("name unmatch[" + m.getName() + "]");
			}
		}
		return null;

	}

	public static boolean isSameParam(Parameter[] mps, Class<?>[] c) {
		if (mps.length == c.length) {
			for (int i = 0; i < mps.length; i++) {
				logger.debug(" p.getType():" + mps[i].getType());
				logger.debug(" typs.get():" + c[i]);
				logger.debug(" isImplemented():" + isImplemented(mps[i].getType(), c[i]));
				if (!isImplemented(mps[i].getType(), c[i])) {
					return false;
				}
			}
		} else {
			// logger.debug("size unmatch");
			return false;
		}
		return true;
	}

	public static boolean isImplemented(Class<?> interfaceClass, Class<?> targetClass) {
		if (interfaceClass.equals(targetClass)
				|| interfaceClass.isInterface() && interfaceClass.isAssignableFrom(targetClass)) {
			return true;
		}
		return false;
	}
}
