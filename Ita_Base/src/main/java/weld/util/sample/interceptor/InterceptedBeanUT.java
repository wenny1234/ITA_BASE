package weld.util.sample.interceptor;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;

import weld.util.WeldRunner;

@Dependent
@RunWith(WeldRunner.class)
public class InterceptedBeanUT {

	@Inject
	InterceptedBean target;

	@Test
	public void execute_normal() {
		if (target != null) {
			System.out.println("time = " + target.execute());
		} else {
			System.err.println("new!!");
			new InterceptedBean().execute();
		}
	}

}