package dev.yellowpagetr.spam;
import java.lang.reflect.Method;
import android.net.Uri;
import android.os.Bundle;
import android.database.Cursor;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
/** Experimentally alter existing Xiaomi number-result objects only. */
public final class HookEntry implements IXposedHookLoadPackage {
    private static final String LOG="YellowPageTR/0.5";
    private static final java.util.concurrent.atomic.AtomicInteger TRACE_COUNT = new java.util.concurrent.atomic.AtomicInteger();
    private static final java.util.concurrent.atomic.AtomicInteger PROVIDER_TRACE_COUNT = new java.util.concurrent.atomic.AtomicInteger();
    private static final java.util.concurrent.atomic.AtomicInteger RESOLVER_TRACE_COUNT = new java.util.concurrent.atomic.AtomicInteger();
    private static final java.util.concurrent.atomic.AtomicInteger ENTRY_TRACE_COUNT = new java.util.concurrent.atomic.AtomicInteger();
    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam param) {
        String pkg=param.packageName;
        if (!"com.miui.yellowpage".equals(pkg) && !"com.android.contacts".equals(pkg)
            && !"com.android.incallui".equals(pkg) && !"com.miui.antispam".equals(pkg)) return;
        int total=0;
        total+=install(param.classLoader,"miui.yellowpage.YellowPagePhone",false);
        total+=install(param.classLoader,"com.miui.yellowpage.tool.QueryNumberResult",true);
        if ("com.miui.yellowpage".equals(pkg)) total+=installProvider(param.classLoader);
        total+=installResolver(param.classLoader);
        // Entry points verified in the supplied Xiaomi Contacts APK.
        if ("com.android.contacts".equals(pkg)) {
            total+=observe(param.classLoader,
                "com.android.contacts.list.TwelveKeyDialerFragment","E7","dialerFilter");
            total+=observe(param.classLoader,
                "com.android.contacts.dialer.list.DialerListDataSource","Q0","t9Query");
            total+=observe(param.classLoader,
                "com.android.contacts.detail.yellowpage.YellowPagePhoneLoader","J","detailLookup");
        }
        total+=observe(param.classLoader,"miui.yellowpage.YellowPageUtils","getPhoneInfo","phoneInfo");
        total+=observe(param.classLoader,"miui.yellowpage.YellowPageUtils","getAntispamNumberCategory","spamCategory");
        total+=observe(param.classLoader,"miui.yellowpage.YellowPageUtils","queryPhoneInfo","queryPhoneInfo");
        XposedBridge.log(LOG+": process="+pkg+" hooks="+total+" offlineEntries="+SpamDb.size());
    }

    /**
     * Diagnostic-only hooks to verified Xiaomi API entry points.
     * Does not alter any return values and never writes telephone numbers to logs.
     */
    private static int observe(ClassLoader loader,String klass,String method,String label) {
        try {
            Class<?> target=XposedHelpers.findClassIfExists(klass,loader);
            if(target==null) {
                XposedBridge.log(LOG+": missingEntry="+label);
                return 0;
            }
            int count=XposedBridge.hookAllMethods(target,method,new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam p) {
                    try {
                        if(ENTRY_TRACE_COUNT.incrementAndGet()<=120)
                            XposedBridge.log(LOG+": entry="+label+" inputMatch="+inputMatches(p.args));
                    } catch(Throwable ignored) {}
                }
                @Override protected void afterHookedMethod(MethodHookParam p) {
                    try {
                        if(ENTRY_TRACE_COUNT.get()<=120) {
                            Object result=p.getResult();
                            XposedBridge.log(LOG+": exit="+label+" resultPresent="+
                                (result!=null)+" resultMatch="+matches(result));
                        }
                    } catch(Throwable ignored) {}
                }
            }).size();
            XposedBridge.log(LOG+": installedEntry="+label+" count="+count);
            return count;
        } catch(Throwable e) {
            XposedBridge.log(LOG+": entryError="+label+" type="+e.getClass().getSimpleName());
            return 0;
        }
    }

    /** Observe YellowPage's incoming ContentProvider requests; never print URIs or digits. */
    private static int installProvider(ClassLoader loader) {
        try {
            Class<?> type = XposedHelpers.findClassIfExists(
                "com.miui.yellowpage.providers.yellowpage.YellowPageProvider", loader);
            if (type == null) {
                XposedBridge.log(LOG+": providerClassMissing");
                return 0;
            }
            int count = XposedBridge.hookAllMethods(type,"query",new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam p) {
                    if (PROVIDER_TRACE_COUNT.incrementAndGet() <= 80)
                        XposedBridge.log(LOG+": providerQuery invoked=true inputMatch="+inputMatches(p.args));
                }
                @Override protected void afterHookedMethod(MethodHookParam p) {
                    if (PROVIDER_TRACE_COUNT.get() <= 80)
                        XposedBridge.log(LOG+": providerResult "+cursorShape(p.getResult()));
                }
            }).size();
            XposedBridge.log(LOG+": providerHooks="+count);
            return count;
        } catch(Throwable e) {
            XposedBridge.log(LOG+": providerHookError="+e.getClass().getSimpleName());
            return 0;
        }
    }
    /** Probe only yellowpage/antispam resolver authority; no URL, query or phone data logged. */
    private static int installResolver(ClassLoader loader) {
        try {
            Class<?> type=XposedHelpers.findClassIfExists("android.content.ContentResolver",loader);
            if(type == null) return 0;
            int count=XposedBridge.hookAllMethods(type,"query",new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam p) {
                    try {
                        if (p.args == null || p.args.length == 0 || p.args[0] == null) return;
                        Object authority=XposedHelpers.callMethod(p.args[0],"getAuthority");
                        if (!(authority instanceof String)) return;
                        String name=((String)authority).toLowerCase(java.util.Locale.ROOT);
                        if (!name.contains("yellowpage") && !name.contains("antispam")) return;
                        if (RESOLVER_TRACE_COUNT.incrementAndGet() <= 80)
                            XposedBridge.log(LOG+": resolverQuery relevantAuthority=true inputMatch="+inputMatches(p.args));
                    } catch(Throwable ignored) {}
                }
            }).size();
            XposedBridge.log(LOG+": resolverHooks="+count);
            return count;
        } catch(Throwable e) {
            XposedBridge.log(LOG+": resolverHookError="+e.getClass().getSimpleName());
            return 0;
        }
    }

    /** Match in-memory only; never record input values, phone numbers, or complete URIs. */
    private static boolean inputMatches(Object[] args) {
        if (args == null) return false;
        try {
            for (Object arg : args) {
                if (arg instanceof String && SpamDb.contains((String)arg)) return true;
                if (arg instanceof String[]) {
                    for (String value : (String[])arg) if (SpamDb.contains(value)) return true;
                }
                if (arg instanceof Uri) {
                    Uri uri=(Uri)arg;
                    for (String value : uri.getPathSegments()) if (SpamDb.contains(value)) return true;
                    if (uri.isHierarchical()) {
                        try {
                            for (String name : uri.getQueryParameterNames())
                                if (SpamDb.contains(uri.getQueryParameter(name))) return true;
                        } catch (Throwable ignored) { }
                    }
                }
                if (arg instanceof Bundle) {
                    Bundle b=(Bundle)arg;
                    for (String key : b.keySet()) {
                        Object value=b.get(key);
                        if(value instanceof String && SpamDb.contains((String)value)) return true;
                        if(value instanceof String[]) {
                            for(String v:(String[])value) if(SpamDb.contains(v)) return true;
                        }
                    }
                }
            }
        } catch(Throwable ignored) {}
        return false;
    }
    /** Only structural cursor metadata, not rows, URI, selection, or numeric values. */
    private static String cursorShape(Object result) {
        if (!(result instanceof Cursor)) return "cursor=false";
        try {
            String[] cols=((Cursor)result).getColumnNames();
            StringBuilder out=new StringBuilder("cursor=true fields=");
            if(cols==null) return out.append("none").toString();
            out.append("[");
            for(int i=0;i<cols.length && i<20;i++) {
                if(i>0) out.append(",");
                String name=cols[i];
                // A field name could be dynamically generated. Reject anything
                // that looks like user data, including long numeric fragments.
                if(name==null || !name.matches("[A-Za-z_][A-Za-z0-9_]{0,39}")
                    || name.matches(".*[0-9]{3,}.*")) name="?";
                out.append(name);
            }
            if(cols.length>20) out.append(",...");
            return out.append("]").toString();
        } catch(Throwable e) { return "cursor=true fields=unavailable"; }
    }
    private static int install(ClassLoader loader,String name,boolean queryResult) {
        try {
            Class<?> type=XposedHelpers.findClassIfExists(name,loader);
            if(type==null) { XposedBridge.log(LOG+": classMissing="+name); return 0; }
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
                    boolean matched = matches(param.thisObject);
                    // Diagnostic only: never log the telephone number or its digits.
                    int count = TRACE_COUNT.incrementAndGet();
                    if (count <= 100) XposedBridge.log(LOG+": call="+type.getName()+"."+name+" matched="+matched);
                    if (!matched) return;
                    boolean changed = false;
                    if(flag && (param.getResult()==null || param.getResult() instanceof Boolean)) {
                        param.setResult(true);
                        changed = true;
                    } else if(!flag && (param.getResult()==null || param.getResult() instanceof String)) {
                        param.setResult("Olası spam (TR)");
                        changed = true;
                    }
                    if (changed) XposedBridge.log(LOG+": override="+name+" matched=true");
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
