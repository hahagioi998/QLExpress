package com.ql.util.express.config;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ql.util.express.config.whitelist.WhiteChecker;
import com.ql.util.express.exception.QLSecurityRiskException;

/**
 * ExpressRunner设置全局生效的配置，直接使用静态方法控制
 */
public class QLExpressRunStrategy {
    /**
     * 沙箱模式开关
     */
    private static boolean sandboxMode = false;
    /**
     * 编译期类型白名单
     * null 表示不进行校验
     * 如果编译时发现引用了白名单之外的类, 就会抛出异常
     */
    private static List<WhiteChecker> compileWhiteClassList = null;

    /**
     * 预防空指针
     */
    private static boolean avoidNullPointer = false;

    /**
     * 当空对象进行大小比较时，返回false, 例如 1 > null 和 null > 1都返回false
     */
    private static boolean compareNullLessMoreAsFalse = false;

    /**
     * 禁止调用不安全的方法
     */
    private static boolean forbidInvokeSecurityRiskMethods = false;

    /**
     * 黑名单控制
     */
    private static final Set<String> SECURITY_RISK_METHOD_LIST = new HashSet<>();

    /**
     * 白名单控制
     */
    private static Set<String> SECURE_METHOD_LIST = new HashSet<>();

    static {
        // 系统退出
        SECURITY_RISK_METHOD_LIST.add(System.class.getName() + "." + "exit");

        // 运行脚本命令
        SECURITY_RISK_METHOD_LIST.add(Runtime.getRuntime().getClass().getName() + ".exec");
        SECURITY_RISK_METHOD_LIST.add(ProcessBuilder.class.getName() + ".start");

        // 反射相关
        SECURITY_RISK_METHOD_LIST.add(Method.class.getName() + ".invoke");
        SECURITY_RISK_METHOD_LIST.add(Class.class.getName() + ".forName");
        SECURITY_RISK_METHOD_LIST.add(ClassLoader.class.getName() + ".loadClass");
        SECURITY_RISK_METHOD_LIST.add(ClassLoader.class.getName() + ".findClass");
        SECURITY_RISK_METHOD_LIST.add(ClassLoader.class.getName() + ".defineClass");
        SECURITY_RISK_METHOD_LIST.add(ClassLoader.class.getName() + ".getSystemClassLoader");
    }

    private QLExpressRunStrategy() {
        throw new IllegalStateException("Utility class");
    }

    public static void setSandBoxMode(boolean sandboxMode) {
        QLExpressRunStrategy.sandboxMode = sandboxMode;
    }

    public static boolean isSandboxMode() {
        return sandboxMode;
    }

    public static boolean isCompareNullLessMoreAsFalse() {
        return compareNullLessMoreAsFalse;
    }

    public static void setCompareNullLessMoreAsFalse(boolean compareNullLessMoreAsFalse) {
        QLExpressRunStrategy.compareNullLessMoreAsFalse = compareNullLessMoreAsFalse;
    }

    public static boolean isAvoidNullPointer() {
        return avoidNullPointer;
    }

    public static void setAvoidNullPointer(boolean avoidNullPointer) {
        QLExpressRunStrategy.avoidNullPointer = avoidNullPointer;
    }

    public static boolean isForbidInvokeSecurityRiskMethods() {
        return forbidInvokeSecurityRiskMethods;
    }

    public static void setForbidInvokeSecurityRiskMethods(boolean forbidInvokeSecurityRiskMethods) {
        QLExpressRunStrategy.forbidInvokeSecurityRiskMethods = forbidInvokeSecurityRiskMethods;
    }

    /**
     * TODO 未考虑方法重载的场景
     *
     * @param clazz
     * @param methodName
     */
    public static void addSecurityRiskMethod(Class<?> clazz, String methodName) {
        QLExpressRunStrategy.SECURITY_RISK_METHOD_LIST.add(clazz.getName() + "." + methodName);
    }

    public static void setSecurityRiskMethod(Set<String> securityRiskMethod) {
        SECURE_METHOD_LIST = securityRiskMethod;
    }

    public static void addSecureMethod(Class<?> clazz, String methodName) {
        SECURE_METHOD_LIST.add(clazz.getName() + "." + methodName);
    }

    public static void assertSecurityRiskMethod(Method method) throws QLSecurityRiskException {
        if (!forbidInvokeSecurityRiskMethods || method == null) {
            return;
        }

        String fullMethodName = method.getDeclaringClass().getName() + "." + method.getName();
        if (!SECURE_METHOD_LIST.isEmpty()) {
            // 有白名单配置时则黑名单失效
            if (!SECURE_METHOD_LIST.contains(fullMethodName)) {
                throw new QLSecurityRiskException("使用QLExpress调用了不安全的系统方法:" + method);
            }
            return;
        }

        if (SECURITY_RISK_METHOD_LIST.contains(fullMethodName)) {
            throw new QLSecurityRiskException("使用QLExpress调用了不安全的系统方法:" + method);
        }
    }

    /**
     * @param clazz
     * @return true 表示位于白名单中, false 表示不在白名单中
     */
    public static boolean checkWhiteClassList(Class<?> clazz) {
        if (compileWhiteClassList == null) {
            return true;
        }
        for (WhiteChecker whiteChecker : compileWhiteClassList) {
            if (whiteChecker.check(clazz)) {
                return true;
            }
        }
        return false;
    }

    public static void setCompileWhiteClassList(List<WhiteChecker> compileWhiteClassList) {
        QLExpressRunStrategy.compileWhiteClassList = compileWhiteClassList;
    }
}
