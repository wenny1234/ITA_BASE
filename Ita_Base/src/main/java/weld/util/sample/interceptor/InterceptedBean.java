package weld.util.sample.interceptor;

import javax.enterprise.context.Dependent;

@ElapsedTimeBinding
@Dependent
public class InterceptedBean {
    public String execute() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return getClass().getName();
    }
}
