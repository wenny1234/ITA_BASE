package weld.util.sample.interceptor;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@Interceptor
@ElapsedTimeBinding
public class ElapsedTimeInterceptor {
    @AroundInvoke
    public Object invoke(InvocationContext context) throws Exception {
        long start = System.nanoTime();
        Object result = context.proceed();
        long end = System.nanoTime();
        System.out.format("%,dns\r\n", end - start);
        return result;
    }
}
