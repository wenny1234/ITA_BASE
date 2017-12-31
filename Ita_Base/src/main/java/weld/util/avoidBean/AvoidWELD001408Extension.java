package weld.util.avoidBean;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import javax.inject.Qualifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.co.tokyo_gas.aion.tgfw.parts.db.sql.annotation.FwSQLMapper;

/**
 *  CDI を利用した開発時、実装が揃わない状態で起動を試みると WELD-001408 エラーで起動できません。
 *  これを回避するために、起動時に必要な Bean の登録を自動で行う Extension です。
 *
 */
public abstract class AvoidWELD001408Extension implements Extension {
	// 最初に初期化
	static final Logger logger = LoggerFactory.getLogger(AvoidWELD001408Extension.class);

	private Set<InjectionPoint> cands = new HashSet<>();
	
	// 回避するクラスのパッケージを複数指定できます
	protected String[] avoidTypes = new String[0];
	
	// 回避に含めないアノテーションを複数指定できます
	@SuppressWarnings("unchecked")
	protected Class<?>[] excludeClazz = (Class<? extends Annotation>[]) new Class<?>[0];
	
	@SuppressWarnings("unchecked")
	public <T, X> void processInjectionPoint(@Observes ProcessInjectionPoint<T, X> ev) {
		InjectionPoint ip = ev.getInjectionPoint();
		Annotated atd = ip.getAnnotated();
		for(Class<?> c : excludeClazz){
			if (atd.isAnnotationPresent((Class<? extends Annotation>) c)) {
				return;
			}
		}
//		cands.add(ev.getInjectionPoint());
		cands.add(ip);
	}

	public void afterBeanDiscovery(@Observes AfterBeanDiscovery ev, BeanManager bm) {
		
		Set<AviodWELD001408Bean<?>> check = new HashSet<>();
		
		for (InjectionPoint ip : cands) {
			if (isAvoidTypes(ip.getType().getTypeName())) {				
				Set<Annotation> as = new HashSet<>();
				for (Annotation a : ip.getAnnotated().getAnnotations()) {
					Annotation [] aa = a.annotationType().getAnnotationsByType(Qualifier.class);
					if (aa != null && aa.length > 0) {
						as.add(a);
					}
				}
				
				Annotation [] aar = as.toArray(new Annotation[0]);
				Set<?> beans = bm.getBeans(ip.getType(), aar);

				if (beans.size() == 0) {
					AviodWELD001408Bean<?> bean = new AviodWELD001408Bean<>(ip.getType(), aar);
					if (!check.stream().anyMatch(bean::isSame)) {
						check.add(bean);
						ev.addBean(bean);
						logger.debug("Dummy Bean for CDI: " + ip.getType().getTypeName());
						System.out.println("Dummy Bean for CDI: " + ip.getType().getTypeName());
					}
				}
			}
		}
	}
	
	protected boolean isAvoidTypes(String in){
		for(String avoidType : avoidTypes){
			logger.debug("avoidType=["+avoidType+"]");
			if(in.startsWith(avoidType)){
				return true;
			}
		}
		return false;
	}
	
}
