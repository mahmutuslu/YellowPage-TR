package dev.yellowpagetr.spam;
import java.lang.reflect.Method;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
/** Experimentally alter existing Xiaomi number-result objects only. */
public final class HookEntry implements IXposedHookLoadPackage {
    private static final String LOG="YellowPageTR/0.1";
    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam param) {
        String pkg=param.packageName;
        if (!"com.miui.yellowpage".equals(pkg) && !"com.android.contacts".equals(pkg)
            && !"com.android.incallui".equals(pkg) && !"com.miui.antispam".equals(pkg)) return;
        int total=0;
        total+=install(param.classLoader,"miui.yellowpage.YellowPagePhone",false);
        total+=install(param.classLoader,"com.miui.yellowpage.tool.QueryNumberResult",true);
        XposedBridge.log(LOG+": process="+pkg+" hooks="+total+" offlineEntries="+SpamDb.size());
    }
    private static int install(ClassLoader loader,String name,boolean queryResult) {
        try {
            Class<?> type=XposedHelpers.findClassIfExists(name,loader);
            if(type==null) return 0;
            int hooked=0;
            hooked+=hook(type,"isAntispam",true);
            hooked+=hook(type,"isMarkedSuspect",true);
            hooked+=hook(type,"getTag",false);
            if(queryResult) hooked+=hook(type,"getAntispamCategoryName",false);
            XposedBridge.log(LOG+": class="+name+" hookedMethods="+hooked);
            return hooked;
        } catch(Throwable e) {
            XposedBridge.log(LOG+": setup failed "+name+" "+e.getClass().getSimpleName());
            return 0;
        }
    }
    private static int hook(Class<?> type,String name,boolean flag) {
        boolean found=false;
        for(Method m:type.getDeclaredMethods()) {
            if(m.getName().equals(name) && m.getParameterTypes().length==0
                && (flag ? (m.getReturnType()==boolean.class || m.getReturnType()==Boolean.class) : m.getReturnType()==String.class)) found=true;
        }
        if(!found) return 0;
        try {
            return XposedBridge.hookAllMethods(type,name,new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!matches(param.thisObject)) return;
                    if(flag && (param.getResult()==null || param.getResult() instanceof Boolean)) param.setResult(true);
                    else if(!flag && (param.getResult()==null || param.getResult() instanceof String)) param.setResult("Olası spam (TR)");
                }
            }).size();
        } catch(Throwable ignored) { return 0; }
    }
    private static boolean matches(Object phone) {
        if(phone==null) return false;
        try { Object n=XposedHelpers.callMethod(phone,"getNormalizedNumber");
            if(n instanceof String && SpamDb.contains((String)n)) return true;
        } catch(Throwable ignored) {}
        try { Object n=XposedHelpers.callMethod(phone,"getNumber");
            return n instanceof String && SpamDb.contains((String)n);
        } catch(Throwable ignored) {}
        return false;
    }
}
