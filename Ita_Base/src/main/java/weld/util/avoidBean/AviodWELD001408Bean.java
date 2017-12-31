package weld.util.avoidBean;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.PassivationCapable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  不足の Bean を提供するためのクラス。実際に呼び出されてしまうと、Runtime 例外を発生させます。
 *
 */

@Default
public class AviodWELD001408Bean<T> implements Bean<T>, PassivationCapable {
	// 最初に初期化
	static final Logger logger = LoggerFactory.getLogger(AvoidWELD001408Extension.class);

	private Type  cls;
	private Set<Annotation>   annotations;
	
	public AviodWELD001408Bean(Type c, Annotation [] an) {
		this.cls = c;
		this.annotations = new HashSet<>(Arrays.asList(an));
		if (annotations.size() == 0) {
			annotations.add(getClass().getAnnotation(Default.class));
		}
	}
	
	@Override
	public T create(CreationalContext<T> creationalContext) {

		InvocationHandler ih = new InvocationHandler() {

			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
//				throw new RuntimeException("CDIStartupHelper により挿入された Bean が呼び出されています。");
				System.out.println(cls.toString() + "が呼び出されましたが、Inject されていません。");
				throw new RuntimeException("「WELD-001408」エラー回避用のBeanを実行しようとしました。テスト対象がimportされていないか、pom.xmlの依存関係が不足しています。" + cls);
			}

		};
		
		return (T)Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] {(Class)cls}, ih);
	}

	@Override
	public void destroy(T instance, CreationalContext<T> creationalContext) {
	}

	@Override
	public Set<Type> getTypes() {
		return new HashSet<>(Arrays.asList(cls));
	}

	@Override
	public Set<Annotation> getQualifiers() {
		return annotations;
	}

	@Override
	public Class<? extends Annotation> getScope() {
		return Dependent.class;
	}

	@Override
	public String getName() {
		return ((Class)cls).getName();
	}

	@Override
	public Set<Class<? extends Annotation>> getStereotypes() {
		return Collections.emptySet();
	}

	@Override
	public boolean isAlternative() {
		return false;
	}

	@Override
	public Class<?> getBeanClass() {
		return (Class<?>)cls;
	}

	@Override
	public Set<InjectionPoint> getInjectionPoints() {
		return Collections.emptySet();
	}

	@Override
	public boolean isNullable() {
		return false;
	}

	public boolean isSame(AviodWELD001408Bean<?> t) {
		return (cls.equals(t.cls)
				&& annotations.stream().allMatch(t.annotations::contains));
	}

	@Override
	public String getId() {
		return "mock";
	}
}
