package dev.yellowpagetr.spam;
import java.lang.reflect.Method;
import android.net.Uri;
import android.os.Bundle;
import android.database.Cursor;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
/** Experimentally alter existing Xiaomi number-result objects only. */
public final class HookEntry implements IXposedHookLoadPackage {
    private static final String LOG="YellowPageTR/0.9";
    private static final java.util.concurrent.atomic.AtomicInteger TRACE_COUNT = new java.util.concurrent.atomic.AtomicInteger();
    private static final java.util.concurrent.atomic.AtomicInteger PROVIDER_TRACE_COUNT = new java.util.concurrent.atomic.AtomicInteger();
    private static final java.util.concurrent.atomic.AtomicInteger RESOLVER_TRACE_COUNT = new java.util.concurrent.atomic.AtomicInteger();
    private static final java.util.concurrent.atomic.AtomicInteger ENTRY_TRACE_COUNT = new java.util.concurrent.atomic.AtomicInteger();
    private static final java.util.concurrent.atomic.AtomicLong LAST_NOTICE_AT = new java.util.concurrent.atomic.AtomicLong(0L);
    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam param) {
        String pkg=param.packageName;
        if (!"com.miui.yellowpage".equals(pkg) && !"com.android.contacts".equals(pkg)
            && !"com.android.incallui".equals(pkg) && !"com.miui.antispam".equals(pkg)) return;
        int total=0;
        total+=install(param.classLoader,"miui.yellowpage.YellowPagePhone",false);
        total+=install(param.classLoader,"com.miui.yellowpage.tool.QueryNumberResult",true);
        if ("com.miui.yellowpage".equals(pkg)) total+=installProvider(param.classLoader);
        total+=installResolver(param.classLoader);
        if ("com.android.incallui".equals(pkg)) {
            total+=installIncomingCallCard(param.classLoader);
            total+=installExtraInfoRenderProbe(param.classLoader);
        }
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
                        boolean matched=inputMatches(p.args);
                        if(ENTRY_TRACE_COUNT.incrementAndGet()<=120)
                            XposedBridge.log(LOG+": entry="+label+" inputMatch="+matched);
                        if("t9Query".equals(label) && matched) showDialerNotice();
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

    /**
     * v0.6 dialer proof-of-concept. A short on-screen toast, NOT a system
     * caller-ID label, and no call blocking. Shows at most once every 10s.
     * No number is sent or written to log.
     */
    private static void showDialerNotice() {
        try {
            final long now=android.os.SystemClock.elapsedRealtime();
            long previous=LAST_NOTICE_AT.get();
            if(now-previous<10000L || !LAST_NOTICE_AT.compareAndSet(previous,now)) return;
            Class<?> thread=Class.forName("android.app.ActivityThread");
            java.lang.reflect.Method current=thread.getDeclaredMethod("currentApplication");
            current.setAccessible(true);
            Object value=current.invoke(null);
            if(!(value instanceof Context)) {
                XposedBridge.log(LOG+": dialerNotice noApplicationContext");
                return;
            }
            final Context context=(Context)value;
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override public void run() {
                    try {
                        Toast.makeText(context,"Olası spam (TR)",Toast.LENGTH_LONG).show();
                        XposedBridge.log(LOG+": dialerNotice shown=true");
                    } catch(Throwable e) {
                        XposedBridge.log(LOG+": dialerNotice error="+e.getClass().getSimpleName());
                    }
                }
            });
        } catch(Throwable e) {
            XposedBridge.log(LOG+": dialerNotice error="+e.getClass().getSimpleName());
        }
    }

    /**
     * Native incoming-call label proof-of-concept for the inspected InCallUI APK.
     * No outgoing calls, call blocking, caller name, avatar, or call state changes.
     * Do not expose the phone number in LSPosed logs.
     */
    private static int installIncomingCallCard(ClassLoader loader) {
        final String cls="com.android.incallui.model.CallCardInfo";
        try {
            Class<?> type=XposedHelpers.findClassIfExists(cls,loader);
            if(type==null) {
                XposedBridge.log(LOG+": incomingCardClassMissing");
                return 0;
            }
            int count=XposedBridge.hookAllMethods(type,"createCallCardInfo",new XC_MethodHook() {
                private final java.util.concurrent.atomic.AtomicInteger traces=
                    new java.util.concurrent.atomic.AtomicInteger();
                @Override protected void afterHookedMethod(MethodHookParam p) {
                    try {
                        Object card=p.getResult();
                        Object call=p.args!=null && p.args.length>0 ? p.args[0] : null;
                        Object cache=p.args!=null && p.args.length>1 ? p.args[1] : null;
                        if(card==null || call==null) return;
                        boolean incoming=readBool(card,"isIncoming");
                        boolean emergency=readBool(card,"isEmergencyNumber");
                        boolean matched=SpamDb.contains(readString(card,"phoneNumber"));
                        if(!matched && cache!=null) matched=SpamDb.contains(readString(cache,"number"));
                        if(!matched) {
                            try {
                                Object num=XposedHelpers.callMethod(call,"getNumber");
                                matched=num instanceof String && SpamDb.contains((String)num);
                            } catch(Throwable ignored) {}
                        }
                        if(traces.incrementAndGet()<=80)
                            XposedBridge.log(LOG+": incomingCard created=true incoming="+incoming+
                                " emergency="+emergency+" matched="+matched);
                        if(!incoming || emergency) return;
                        // Do not override an existing Xiaomi classification.
                        java.lang.reflect.Field title=card.getClass().getDeclaredField("markTitle");
                        title.setAccessible(true);
                        Object existing=title.get(card);
                        if(existing instanceof String && !((String)existing).trim().isEmpty()) {
                            XposedBridge.log(LOG+": incomingCard existingLabelPreserved=true");
                            return;
                        }
                        String extra=readString(card,"extraInfo");
                        if(extra!=null && !extra.trim().isEmpty()) {
                            // Keep all Xiaomi-provided labels and other information.
                            XposedBridge.log(LOG+": incomingCard existingExtraInfoPreserved=true");
                            return;
                        }
                        if(matched) {
                            title.set(card,"Olası spam (TR)");
                            writeStringField(card,"extraInfo","Olası spam (TR)");
                            XposedBridge.log(LOG+": incomingCard extraInfoSpamSet=true");
                        } else if(isPreviewWindowOpen()) {
                            // Test only. Never label a trusted caller as spam.
                            title.set(card,"YellowPage TR (TEST)");
                            writeStringField(card,"extraInfo","YellowPage TR (TEST)");
                            XposedBridge.log(LOG+": incomingCard extraInfoPreviewSet=true");
                        }
                    } catch(Throwable error) {
                        XposedBridge.log(LOG+": incomingCard error="+error.getClass().getSimpleName());
                    }
                }
            }).size();
            XposedBridge.log(LOG+": incomingCardHooks="+count);
            return count;
        } catch(Throwable e) {
            XposedBridge.log(LOG+": incomingCardHookError="+e.getClass().getSimpleName());
            return 0;
        }
    }
    /**
     * Opt-in, automatically expiring native-label preview.
     * Only adb shell can write Settings.Global on normal devices.
     * Reject windows longer than 3 minutes to guard stale debug settings.
     */
    private static boolean isPreviewWindowOpen() {
        try {
            Class<?> thread=Class.forName("android.app.ActivityThread");
            Method current=thread.getDeclaredMethod("currentApplication");
            current.setAccessible(true);
            Object application=current.invoke(null);
            if(!(application instanceof Context)) {
                XposedBridge.log(LOG+": incomingCard previewContextMissing");
                return false;
            }
            long deadline=android.provider.Settings.Global.getLong(
                ((Context)application).getContentResolver(),
                "yellowpagetr_preview_until",0L);
            long remaining=deadline-System.currentTimeMillis();
            return remaining>0 && remaining<=180000L;
        } catch(Throwable e) {
            XposedBridge.log(LOG+": incomingCard previewCheckError="+e.getClass().getSimpleName());
            return false;
        }
    }

    private static boolean readBool(Object target,String field) throws Exception {
        java.lang.reflect.Field f=target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        return f.getBoolean(target);
    }
    private static void writeStringField(Object target,String field,String value) throws Exception {
        java.lang.reflect.Field f=target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target,value);
    }
    private static String readString(Object target,String field) throws Exception {
        java.lang.reflect.Field f=target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        Object value=f.get(target);
        return value instanceof String?(String)value:null;
    }

    /** Confirm the native TextView rendering path without logging private data. */
    private static int installExtraInfoRenderProbe(ClassLoader loader) {
        try {
            Class<?> type=XposedHelpers.findClassIfExists(
                "com.android.incallui.view.CallCardOptionalInfoView",loader);
            if(type==null) {
                XposedBridge.log(LOG+": renderViewClassMissing");
                return 0;
            }
            int count=XposedBridge.hookAllMethods(type,"showExtraInfo",new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam p) {
                    try {
                        if(p.args==null || p.args.length==0 || !(p.args[0] instanceof String)) return;
                        String value=(String)p.args[0];
                        if(!"YellowPage TR (TEST)".equals(value) &&
                            !"Olası spam (TR)".equals(value)) return;
                        java.lang.reflect.Field f=p.thisObject.getClass().getDeclaredField("mCallCardExtraTitle");
                        f.setAccessible(true);
                        Object title=f.get(p.thisObject);
                        boolean textMatches=false;
                        if(title instanceof android.widget.TextView)
                            textMatches=value.contentEquals(
                                ((android.widget.TextView)title).getText());
                        XposedBridge.log(LOG+": extraInfo renderedMethod=true textMatches="+textMatches);
                    } catch(Throwable e) {
                        XposedBridge.log(LOG+": extraInfo renderProbeError="+e.getClass().getSimpleName());
                    }
                }
            }).size();
            XposedBridge.log(LOG+": extraInfoRenderHooks="+count);
            return count;
        } catch(Throwable e) {
            XposedBridge.log(LOG+": extraInfoRenderHookError="+e.getClass().getSimpleName());
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
