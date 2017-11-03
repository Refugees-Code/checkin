package at.refugeescode.checkin.config;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.hibernate.LazyInitializationException;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Aspect
@Component
@Slf4j
@ToString
@Getter
@Setter
public class TracingAspect {

    /*-------------------------------------*\
     * Fields
    \*-------------------------------------*/

    /**
     * Whether to trace the execution of the advised methods.
     */
    @Value("${checkin.aop.enabled:false}")
    private boolean enabled;

    @Value("${checkin.aop.prettyPrint:false}")
    private boolean prettyPrint;

    @Value("${checkin.aop.maxStringLength:100}")
    private int maxStringLength;

    /*-------------------------------------*\
     * Boilerplate
    \*-------------------------------------*/

    @PostConstruct
    protected void initialize() {
        log.info("{}", this);
    }

    /*-------------------------------------*\
     * Pointcuts
    \*-------------------------------------*/

    @Pointcut("execution(public * *(..))")
    public void publicMethod() {}

    @Pointcut("execution(public * changePassword(..))")
    public void changePasswordMethod() {}

    @Pointcut("within(org.springframework.data.repository.Repository+)")
    public void withinRepository() {}

    @Pointcut("within(@org.springframework.stereotype.Controller *)")
    public void withinController() {}

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void withinRestController() {}

    @Pointcut("within(@org.springframework.data.rest.webmvc.RepositoryRestController *)")
    public void withinRepositoryRestController() {}

    @Pointcut("within(@org.springframework.data.rest.core.annotation.RepositoryEventHandler *)")
    public void withinRepositoryEventHandler() {}

    @Pointcut("within(@org.springframework.stereotype.Service *)")
    public void withinService() {}

    @Pointcut("within(org.springframework.security.core.userdetails.UserDetailsService+)")
    public void withinUserDetailsService() {}

    @Pointcut("@annotation(org.springframework.web.bind.annotation.RequestMapping)")
    public void requestMappingMethod() {}

    /*-------------------------------------*\
     * Advices
    \*-------------------------------------*/

    @Around("publicMethod() && withinRepository()")
    public Object profileRepositoryMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        return profileMethod(joinPoint);
    }

    @Around("publicMethod() && (withinController() || withinRestController() || withinRepositoryRestController()) && requestMappingMethod()")
    public Object profileRequestMappingMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        return profileMethod(joinPoint);
    }

    @Around("publicMethod() && withinService() && !changePasswordMethod()")
    public Object profileServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        return profileMethod(joinPoint);
    }

    @Around("publicMethod() && withinRepositoryEventHandler()")
    public Object profileRepositoryEventHandlerMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        return profileMethod(joinPoint);
    }

    @Around("publicMethod() && withinUserDetailsService()")
    public Object profileUserDetailsServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        return profileMethod(joinPoint);
    }

    /*-------------------------------------*\
     * Helper Methods
    \*-------------------------------------*/

    private String joinPointSignature(JoinPoint joinPoint, boolean prettyPrint) {
        Object[] joinPointArgs = joinPoint.getArgs();
        StringBuilder msg = new StringBuilder();
        boolean multipleArgs = joinPointArgs != null && joinPointArgs.length > 1;
        if (prettyPrint)
            msg.append("\n");
        msg.append(getTargetAndMethodName(joinPoint));
        if (prettyPrint && multipleArgs)
            msg.append("\n");
        msg.append("(");
        if (prettyPrint && multipleArgs)
            msg.append("\n\t");
        List<String> stringArgs = joinPointArgs == null ? ImmutableList.of() : Arrays.stream(joinPointArgs)
                .map(this::cleanToString)
                .collect(Collectors.toList());
        Joiner.on(prettyPrint && multipleArgs ? ",\n\t" : ",").useForNull("null").appendTo(msg, stringArgs);
        if (prettyPrint && multipleArgs)
            msg.append("\n");
        msg.append(")");
        if (prettyPrint)
            msg.append("\n");
        return msg.toString();
    }

    private static String getTargetAndMethodName(JoinPoint joinPoint) {
        return getTargetName(joinPoint) + "." + joinPoint.getSignature().getName();
    }

    private static String getTargetName(JoinPoint joinPoint) {
        Object target = joinPoint.getTarget();
        if (target == null)
            return "[no target]";

        if (target.getClass().getCanonicalName().contains("$Proxy")) {
            Advised advised = (Advised) target;

            try {
                String name = advised.getTargetSource().getTarget().getClass().getSimpleName();
                if (name.equals("SimpleJpaRepository") || name.endsWith("RepositoryImpl")) {
                    Class<?>[] interfaces = joinPoint.getTarget().getClass().getInterfaces();
                    if (interfaces.length > 0)
                        return interfaces[0].getSimpleName();
                }
                return name;
            } catch (Exception e) {
                return "[target can't be resolved]";
            }
        }

        return target.getClass().getSimpleName();
    }

    private Object profileMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        if (isEnabled() && log.isTraceEnabled()) {
            StopWatch stopWatch = new StopWatch();
            Object returnValue = "[no return value obtained]";
            try {
                stopWatch.start(getTargetAndMethodName(joinPoint));
                returnValue = joinPoint.proceed();
                return returnValue;
            }
            finally {
                stopWatch.stop();

                String returnString;
                try {
                    returnString = cleanToString(returnValue);
                }
                catch (LazyInitializationException e) {
                    returnString = "[" + e.getMessage() + "]";
                }

                log.trace("{} returned {}{} — execution time: {} ms",
                        joinPointSignature(joinPoint, prettyPrint),
                        returnString,
                        prettyPrint ? "\n" : "",
                        stopWatch.getLastTaskTimeMillis());
                }
        }
        else {
            return joinPoint.proceed();
        }
    }

    private String cleanToString(Object object) {
        // call String.valueOf twice to handle objects that return literal null in their toString() method
        String stringValue = String.valueOf(String.valueOf(object));
        if (prettyPrint)
            return stringValue;
        String singleLine = stringValue.replace("\n", "\\n");
        if (StringUtils.endsWithAny(singleLine, new String[]{")", "]", "}", ">", "\'", "\""}))
            return StringUtils.abbreviate(singleLine.substring(0, singleLine.length() - 1), maxStringLength) +
                    singleLine.substring(singleLine.length() - 1);
        else
            return StringUtils.abbreviate(singleLine, maxStringLength);
    }

}