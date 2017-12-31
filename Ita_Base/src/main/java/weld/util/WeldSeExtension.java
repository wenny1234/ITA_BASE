package weld.util;

import java.lang.reflect.InvocationTargetException;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessInjectionPoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import weld.util.avoidBean.TgAvoidWELD001408Extension;

public class WeldSeExtension implements Extension {
	
	// 最初に初期化
	static final Logger logger = LoggerFactory.getLogger(WeldSeExtension.class);
	
	TgAvoidWELD001408Extension tgUtil = new TgAvoidWELD001408Extension();

	public <T, X> void processInjectionPoint(@Observes ProcessInjectionPoint<T, X> ev) {
		logger.info("Weld.se :: processInjectionPoint called.");		
		tgUtil.processInjectionPoint(ev);
	}
	
	public void afterDeployment(@Observes AfterDeploymentValidation adv, BeanManager bm) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException{
		logger.info("Weld.se :: afterDeployment called.");		
		WeldSeUtil.supportScopes(bm);
	}
	
	public void afterBeanDiscovery(@Observes AfterBeanDiscovery ev, BeanManager bm) {
		logger.info("Weld.se :: afterBeanDiscovery called.");		
		tgUtil.afterBeanDiscovery(ev,bm);
	}
}
