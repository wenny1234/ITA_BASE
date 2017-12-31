package weld.util;

import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.apache.poi.ss.formula.functions.T;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.OngoingStubbing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import weld.util.sample.CXXDA01644InDaoDTO;
import weld.util.sample.CXXDA01644Mapper;
import weld.util.sample.CXXDA01644OutDaoDTO;

public class WeldMockUtil {
	// 最初に初期化
	static final Logger logger = LoggerFactory.getLogger(WeldMockUtil.class);

	protected HashMap<Object, Object> injects = new HashMap<Object, Object>();
	protected HashMap<Object, String> names = new HashMap<Object, String>();

	protected Object realInstance;
	protected String realName;
	protected Class<?> realKlass;

	public WeldMockUtil(Object real) {
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

	protected String getClassName(String in) {
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

	public void chgInject(Object o) {
		if (o == null) {
			logger.error("chgInject() :: 入力が不正です");
			return;
		}
		String oname = getClassName(o.getClass().getSimpleName());
		if (oname == null) {
			logger.error("chgInject() :: 入力クラス名が不正です");
			return;
		}

		if (injects.get(o) != null) {
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

	public <T> Object useInject(Object o) {
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
			// logger.debug("inject = ");
			Object inj = injects.get(o);
			logger.debug("inject = " + inj);
			// System.err.println("inject = " + inj.getClass().getSimpleName());
			Whitebox.setInternalState(realInstance, names.get(o), injects.get(o));
		}
	}

	public <T> OngoingStubbing<Object> mockAnyThenReal(Object targetInject, String methodName, Object[] params) {
		chgInject(targetInject);

		Method mm = getMethod(targetInject.getClass(), methodName, param2type(params));
		Method rm = getMethod(useInject(targetInject).getClass(), methodName, param2type(params));
		logger.debug("mm = " + mm + ", rm = " + rm);
		if (mm != null && rm != null) {
			try {
				return Mockito.when(mm.invoke(targetInject, param2any(params))).then(new Answer<Object>() {
					public Object answer(InvocationOnMock invocation) throws Throwable {
						return rm.invoke(useInject(targetInject), invocation.getArguments());
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

	public <T> OngoingStubbing<Object> mockThenReal(Object targetInject, String methodName,
			ArgumentCaptor<?>... params) {
		// 最後に戻すために格納
		chgInject(targetInject);

		// メソッド取得
		Class<?>[] paraTypes = param2type(params);
		Method mm = getMethod(targetInject.getClass(), methodName, paraTypes);
		Method rm = getMethod(useInject(targetInject).getClass(), methodName, paraTypes);
		logger.debug("mm = " + mm + ", rm = " + rm);
		if (mm != null && rm != null) {
			try {
				return Mockito.when(mm.invoke(targetInject, param2cap(params))).thenAnswer(new Answer<Object>() {
					public Object answer(InvocationOnMock invocation) throws Throwable {
						logger.debug("hello:rm=[" + rm + "]tg=[" + useInject(targetInject) + "]ag=["
								+ invocation.getArguments() + "]");
						Object ret = null;
						try {
							ret = rm.invoke(useInject(targetInject), invocation.getArguments());
						} catch (Exception e) {
							logger.error("exception occured ::: " + e.getCause());
							e.printStackTrace();
//							return null;
							throw e.getCause();
						}
						return ret;
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

	public Class<?>[] param2type(ArgumentCaptor<?>[] pars) {
		ArrayList<Class<?>> typs = new ArrayList<Class<?>>();
		for (ArgumentCaptor<?> p : pars) {
			Class<?> clazz = (Class<?>) Whitebox.getInternalState(p, "clazz");
			logger.debug("p is ArgumentCaptor. :: clazz:" + clazz);
			typs.add(clazz);
		}
		return typs.toArray(new Class<?>[] {});
	}

	public Object[] param2cap(ArgumentCaptor<?>[] pars) {
		ArrayList<Object> caps = new ArrayList<Object>();
		for (ArgumentCaptor<?> p : pars) {
			caps.add(p.capture());
		}
		return caps.toArray(new Object[] {});
	}

	private Object param2any(Object[] params) {
		Object[] pars = (Object[]) params;
		ArrayList<Object> typs = new ArrayList<Object>();
		for (Object p : pars) {
			logger.debug("p = " + p + "/ p.getClass = " + p.getClass());
			typs.add(Matchers.any(p.getClass()));
		}
		return typs.toArray(new Class<?>[] {});
	}

	public Class<?>[] param2type(Object[] pars) {
		ArrayList<Class<?>> typs = new ArrayList<Class<?>>();
		for (Object p : pars) {
			logger.debug("p = " + p + "/ p.getClass = " + p.getClass());
			typs.add(p.getClass());
		}
		return typs.toArray(new Class<?>[] {});
	}

	public Method getMethod(Class<?> klass, String name, Class<?>[] typs) {
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

	public boolean isSameParam(Parameter[] mps, Class<?>[] c) {
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

	public boolean isImplemented(Class<?> interfaceClass, Class<?> targetClass) {
		if (interfaceClass.equals(targetClass)
				|| interfaceClass.isInterface() && interfaceClass.isAssignableFrom(targetClass)) {
			return true;
		}
		return false;
	}
}
