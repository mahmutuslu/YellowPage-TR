package dev.yellowpagetr.spam;
import java.lang.reflect.Method;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;
import java.util.concurrent.atomic.AtomicBoolean;
import android.net.Uri;
import android.os.Bundle;
import android.database.Cursor;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
/** Experimentally alter existing Xiaomi number-result objects only. */
public final class HookEntry extends XposedModule {
    private final AtomicBoolean initialized=new AtomicBoolean(false);
    private static final String LOG="YellowPageTR/2.0";
    private static final java.util.concurrent.atomic.AtomicInteger TRACE_COUNT = new java.util.concurrent.atomic.AtomicInteger();
    private static final java.util.concurrent.atomic.AtomicInteger PROVIDER_TRACE_COUNT = new java.util.concurrent.atomic.AtomicInteger();
    private static final java.util.concurrent.atomic.AtomicInteger RESOLVER_TRACE_COUNT = new java.util.concurrent.atomic.AtomicInteger();
    private static final java.util.concurrent.atomic.AtomicInteger ENTRY_TRACE_COUNT = new java.util.concurrent.atomic.AtomicInteger();
    private static final java.util.concurrent.atomic.AtomicLong LAST_NOTICE_AT = new java.util.concurrent.atomic.AtomicLong(0L);
    @Override public void onPackageReady(PackageReadyParam param) {
        String pkg=param.getPackageName();
        if (!"com.miui.yellowpage".equals(pkg) && !"com.android.contacts".equals(pkg)
            && !"com.android.incallui".equals(pkg)) return;
        if (!initialized.compareAndSet(false,true)) return;
        int total=0;
        total+=install(param.getClassLoader(),"miui.yellowpage.YellowPagePhone",false);
        total+=install(param.getClassLoader(),"com.miui.yellowpage.tool.QueryNumberResult",true);
        if ("com.miui.yellowpage".equals(pkg)) total+=installProvider(param.getClassLoader());
        total+=installResolver(param.getClassLoader());
        if ("com.android.incallui".equals(pkg)) {
            total+=installIncomingCallCard(param.getClassLoader());
            total+=installExtraInfoRenderProbe(param.getClassLoader());
        }
        // Offline daily feed is provided to Xiaomi's existing number matching hooks.
        // No network calls are initiated during incoming calls.
        // Entry points verified in the supplied Xiaomi Contacts APK.
        if ("com.android.contacts".equals(pkg)) {
            total+=observe(param.getClassLoader(),
                "com.android.contacts.list.TwelveKeyDialerFragment","E7","dialerFilter");
            total+=observe(param.getClassLoader(),
                "com.android.contacts.dialer.list.DialerListDataSource","Q0","t9Query");
            total+=observe(param.getClassLoader(),
                "com.android.contacts.detail.yellowpage.YellowPagePhoneLoader","J","detailLookup");
        }
        total+=observe(param.getClassLoader(),"miui.yellowpage.YellowPageUtils","getPhoneInfo","phoneInfo");
        total+=observe(param.getClassLoader(),"miui.yellowpage.YellowPageUtils","getAntispamNumberCategory","spamCategory");
        total+=observe(param.getClassLoader(),"miui.yellowpage.YellowPageUtils","queryPhoneInfo","queryPhoneInfo");
        log(LOG+": process="+pkg+" hooks="+total+" offlineEntries="+SpamDb.size());
    }

    /**
     * Use the refreshed on-device Turkish list, served locally from the module
     * app. No HTTPS, no OpenSpam and no telephone number appears in logs.
     * If the provider is unavailable, keep the bundled 201-number fallback.
     */
    private static boolean hasOfflineSpam(String raw) {
        String normalized=SpamDb.normalize(raw);
        if(normalized==null)return false;
        try {
            Class<?> thread=Class.forName("android.app.ActivityThread");
            Method current=thread.getDeclaredMethod("currentApplication");
            current.setAccessible(true);
            Object application=current.invoke(null);
            if(application instanceof Context) {
                Bundle extras=new Bundle();
                extras.putString("phone",normalized);
                Bundle response=((Context)application).getContentResolver().call(
                    Uri.parse("content://dev.yellowpagetr.spam.cache/risk"),
                    "hasLocalRisk",null,extras);
                if(response!=null && response.getBoolean("available",false))
                    return response.getBoolean("risk",false);
            }
        } catch(Throwable ignored) {}
        return SpamDb.contains(normalized);
    }

    /**
     * Diagnostic-only hooks to verified Xiaomi API entry points.
     * Does not alter any return values and never writes telephone numbers to logs.
     */
    private int observe(ClassLoader loader,String klass,String method,String label) {
        try {
            Class<?> target=findClassIfExists(klass,loader);
            if(target==null) {
                log(LOG+": missingEntry="+label);
                return 0;
            }
            int count=hookAllMethods(target,method,new HookCallback() {
                @Override protected void beforeHookedMethod(HookParam p) {
                    try {
                        boolean matched=inputMatches(p.args);
                        if(ENTRY_TRACE_COUNT.incrementAndGet()<=120)
                            log(LOG+": entry="+label+" inputMatch="+matched);
                        // Native call-card label only; no intrusive toast.
                    } catch(Throwable ignored) {}
                }
                @Override protected void afterHookedMethod(HookParam p) {
                    try {
                        if(ENTRY_TRACE_COUNT.get()<=120) {
                            Object result=p.getResult();
                            log(LOG+": exit="+label+" resultPresent="+
                                (result!=null)+" resultMatch="+matches(result));
                        }
                    } catch(Throwable ignored) {}
                }
            });
            log(LOG+": installedEntry="+label+" count="+count);
            return count;
        } catch(Throwable e) {
            log(LOG+": entryError="+label+" type="+e.getClass().getSimpleName());
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
                log(LOG+": dialerNotice noApplicationContext");
                return;
            }
            final Context context=(Context)value;
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override public void run() {
                    try {
                        Toast.makeText(context,"Olası spam (TR)",Toast.LENGTH_LONG).show();
                        log(LOG+": dialerNotice shown=true");
                    } catch(Throwable e) {
                        log(LOG+": dialerNotice error="+e.getClass().getSimpleName());
                    }
                }
            });
        } catch(Throwable e) {
            log(LOG+": dialerNotice error="+e.getClass().getSimpleName());
        }
    }

    /**
     * Native incoming-call label proof-of-concept for the inspected InCallUI APK.
     * No outgoing calls, call blocking, caller name, avatar, or call state changes.
     * Do not expose the phone number in LSPosed logs.
     */
    private int installIncomingCallCard(ClassLoader loader) {
        final String cls="com.android.incallui.model.CallCardInfo";
        try {
            Class<?> type=findClassIfExists(cls,loader);
            if(type==null) {
                log(LOG+": incomingCardClassMissing");
                return 0;
            }
            int count=hookAllMethods(type,"createCallCardInfo",new HookCallback() {
                private final java.util.concurrent.atomic.AtomicInteger traces=
                    new java.util.concurrent.atomic.AtomicInteger();
                @Override protected void afterHookedMethod(HookParam p) {
                    try {
                        Object card=p.getResult();
                        Object call=p.args!=null && p.args.length>0 ? p.args[0] : null;
                        Object cache=p.args!=null && p.args.length>1 ? p.args[1] : null;
                        if(card==null || call==null) return;
                        boolean incoming=readBool(card,"isIncoming");
                        boolean emergency=readBool(card,"isEmergencyNumber");
                        boolean matched=hasOfflineSpam(readString(card,"phoneNumber"));
                        if(!matched && cache!=null) matched=hasOfflineSpam(readString(cache,"number"));
                        if(!matched) {
                            try {
                                Object num=callMethod(call,"getNumber");
                                matched=num instanceof String && hasOfflineSpam((String)num);
                            } catch(Throwable ignored) {}
                        }
                        if(traces.incrementAndGet()<=80)
                            log(LOG+": incomingCard created=true incoming="+incoming+
                                " emergency="+emergency+" matched="+matched);
                        if(!incoming || emergency) return;
                        // Do not override an existing Xiaomi classification.
                        java.lang.reflect.Field title=card.getClass().getDeclaredField("markTitle");
                        title.setAccessible(true);
                        Object existing=title.get(card);
                        if(existing instanceof String && !((String)existing).trim().isEmpty()) {
                            log(LOG+": incomingCard existingLabelPreserved=true");
                            return;
                        }
                        String extra=readString(card,"extraInfo");
                        if(extra!=null && !extra.trim().isEmpty()) {
                            // Keep all Xiaomi-provided labels and other information.
                            log(LOG+": incomingCard existingExtraInfoPreserved=true");
                            return;
                        }
                        if(matched) {
                            title.set(card,"Olası spam (TR)");
                            writeStringField(card,"extraInfo","Olası spam (TR)");
                            log(LOG+": incomingCard extraInfoSpamSet=true source=dailyOffline");
                        } else if(isPreviewWindowOpen()) {
                            // Test only. Never label a trusted caller as spam.
                            title.set(card,"YellowPage TR (TEST)");
                            writeStringField(card,"extraInfo","YellowPage TR (TEST)");
                            log(LOG+": incomingCard extraInfoPreviewSet=true");
                        }
                    } catch(Throwable error) {
                        log(LOG+": incomingCard error="+error.getClass().getSimpleName());
                    }
                }
            });
            log(LOG+": incomingCardHooks="+count);
            return count;
        } catch(Throwable e) {
            log(LOG+": incomingCardHookError="+e.getClass().getSimpleName());
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
                log(LOG+": incomingCard previewContextMissing");
                return false;
            }
            long deadline=android.provider.Settings.Global.getLong(
                ((Context)application).getContentResolver(),
                "yellowpagetr_preview_until",0L);
            long remaining=deadline-System.currentTimeMillis();
            return remaining>0 && remaining<=180000L;
        } catch(Throwable e) {
            log(LOG+": incomingCard previewCheckError="+e.getClass().getSimpleName());
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
    private int installExtraInfoRenderProbe(ClassLoader loader) {
        try {
            Class<?> type=findClassIfExists(
                "com.android.incallui.view.CallCardOptionalInfoView",loader);
            if(type==null) {
                log(LOG+": renderViewClassMissing");
                return 0;
            }
            int count=hookAllMethods(type,"showExtraInfo",new HookCallback() {
                @Override protected void afterHookedMethod(HookParam p) {
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
                        log(LOG+": extraInfo renderedMethod=true textMatches="+textMatches);
                    } catch(Throwable e) {
                        log(LOG+": extraInfo renderProbeError="+e.getClass().getSimpleName());
                    }
                }
            });
            log(LOG+": extraInfoRenderHooks="+count);
            return count;
        } catch(Throwable e) {
            log(LOG+": extraInfoRenderHookError="+e.getClass().getSimpleName());
            return 0;
        }
    }

    /** Observe YellowPage's incoming ContentProvider requests; never print URIs or digits. */
    private int installProvider(ClassLoader loader) {
        try {
            Class<?> type = findClassIfExists(
                "com.miui.yellowpage.providers.yellowpage.YellowPageProvider", loader);
            if (type == null) {
                log(LOG+": providerClassMissing");
                return 0;
            }
            int count = hookAllMethods(type,"query",new HookCallback() {
                @Override protected void beforeHookedMethod(HookParam p) {
                    if (PROVIDER_TRACE_COUNT.incrementAndGet() <= 80)
                        log(LOG+": providerQuery invoked=true inputMatch="+inputMatches(p.args));
                }
                @Override protected void afterHookedMethod(HookParam p) {
                    if (PROVIDER_TRACE_COUNT.get() <= 80)
                        log(LOG+": providerResult "+cursorShape(p.getResult()));
                }
            });
            log(LOG+": providerHooks="+count);
            return count;
        } catch(Throwable e) {
            log(LOG+": providerHookError="+e.getClass().getSimpleName());
            return 0;
        }
    }
    /** Probe only yellowpage/antispam resolver authority; no URL, query or phone data logged. */
    private int installResolver(ClassLoader loader) {
        try {
            Class<?> type=findClassIfExists("android.content.ContentResolver",loader);
            if(type == null) return 0;
            int count=hookAllMethods(type,"query",new HookCallback() {
                @Override protected void beforeHookedMethod(HookParam p) {
                    try {
                        if (p.args == null || p.args.length == 0 || p.args[0] == null) return;
                        Object authority=callMethod(p.args[0],"getAuthority");
                        if (!(authority instanceof String)) return;
                        String name=((String)authority).toLowerCase(java.util.Locale.ROOT);
                        if (!name.contains("yellowpage") && !name.contains("antispam")) return;
                        if (RESOLVER_TRACE_COUNT.incrementAndGet() <= 80)
                            log(LOG+": resolverQuery relevantAuthority=true inputMatch="+inputMatches(p.args));
                    } catch(Throwable ignored) {}
                }
            });
            log(LOG+": resolverHooks="+count);
            return count;
        } catch(Throwable e) {
            log(LOG+": resolverHookError="+e.getClass().getSimpleName());
            return 0;
        }
    }

    /** Match in-memory only; never record input values, phone numbers, or complete URIs. */
    private static boolean inputMatches(Object[] args) {
        if (args == null) return false;
        try {
            for (Object arg : args) {
                if (arg instanceof String && hasOfflineSpam((String)arg)) return true;
                if (arg instanceof String[]) {
                    for (String value : (String[])arg) if (hasOfflineSpam(value)) return true;
                }
                if (arg instanceof Uri) {
                    Uri uri=(Uri)arg;
                    for (String value : uri.getPathSegments()) if (hasOfflineSpam(value)) return true;
                    if (uri.isHierarchical()) {
                        try {
                            for (String name : uri.getQueryParameterNames())
                                if (hasOfflineSpam(uri.getQueryParameter(name))) return true;
                        } catch (Throwable ignored) { }
                    }
                }
                if (arg instanceof Bundle) {
                    Bundle b=(Bundle)arg;
                    for (String key : b.keySet()) {
                        Object value=b.get(key);
                        if(value instanceof String && hasOfflineSpam((String)value)) return true;
                        if(value instanceof String[]) {
                            for(String v:(String[])value) if(hasOfflineSpam(v)) return true;
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
    private int install(ClassLoader loader,String name,boolean queryResult) {
        try {
            Class<?> type=findClassIfExists(name,loader);
            if(type==null) { log(LOG+": classMissing="+name); return 0; }
            int hooked=0;
            hooked+=hook(type,"isAntispam",true);
            hooked+=hook(type,"isMarkedSuspect",true);
            hooked+=hook(type,"getTag",false);
            if(queryResult) hooked+=hook(type,"getAntispamCategoryName",false);
            log(LOG+": class="+name+" hookedMethods="+hooked);
            return hooked;
        } catch(Throwable e) {
            log(LOG+": setup failed "+name+" "+e.getClass().getSimpleName());
            return 0;
        }
    }
    private int hook(Class<?> type,String name,boolean flag) {
        boolean found=false;
        for(Method m:type.getDeclaredMethods()) {
            if(m.getName().equals(name) && m.getParameterTypes().length==0
                && (flag ? (m.getReturnType()==boolean.class || m.getReturnType()==Boolean.class) : m.getReturnType()==String.class)) found=true;
        }
        if(!found) return 0;
        try {
            return hookAllMethods(type,name,new HookCallback() {
                @Override protected void afterHookedMethod(HookParam param) throws Throwable {
                    boolean matched = matches(param.thisObject);
                    // Diagnostic only: never log the telephone number or its digits.
                    int count = TRACE_COUNT.incrementAndGet();
                    if (count <= 100) log(LOG+": call="+type.getName()+"."+name+" matched="+matched);
                    if (!matched) return;
                    boolean changed = false;
                    if(flag && (param.getResult()==null || param.getResult() instanceof Boolean)) {
                        param.setResult(true);
                        changed = true;
                    } else if(!flag && (param.getResult()==null || param.getResult() instanceof String)) {
                        param.setResult("Olası spam (TR)");
                        changed = true;
                    }
                    if (changed) log(LOG+": override="+name+" matched=true");
                }
            });
        } catch(Throwable ignored) { return 0; }
    }
    private static boolean matches(Object phone) {
        if(phone==null) return false;
        try { Object n=callMethod(phone,"getNormalizedNumber");
            if(n instanceof String && hasOfflineSpam((String)n)) return true;
        } catch(Throwable ignored) {}
        try { Object n=callMethod(phone,"getNumber");
            return n instanceof String && hasOfflineSpam((String)n);
        } catch(Throwable ignored) {}
        return false;
    }

    /** API 102 hook adapter: performs legacy-style before/after callbacks through
     * modern libxposed interceptor chains. This is not an XposedBridge shim. */
    private static final class HookParam {
        final Object thisObject;
        final Object[] args;
        private Object result;
        HookParam(Object self,Object[] arguments) {
            thisObject=self;
            args=arguments;
        }
        Object getResult() { return result; }
        void setResult(Object value) { result=value; }
    }

    private static class HookCallback {
        protected void beforeHookedMethod(HookParam param) throws Throwable { }
        protected void afterHookedMethod(HookParam param) throws Throwable { }
    }

    /** Install every overload by its actual reflective Method, not by name only. */
    private int hookAllMethods(Class<?> type,String name,HookCallback callback) {
        int installed=0;
        try {
            for(Method method:type.getDeclaredMethods()) {
                if(!name.equals(method.getName())) continue;
                try {
                    hook(method).setId("yellowpagetr:"+method.toGenericString())
                        .intercept(chain -> {
                            HookParam param=new HookParam(chain.getThisObject(),
                                chain.getArgs().toArray());
                            try {
                                callback.beforeHookedMethod(param);
                            } catch(Throwable callbackError) {
                                log(LOG+": beforeHookError="+callbackError.getClass().getSimpleName());
                            }
                            Object result=chain.proceed();
                            param.setResult(result);
                            try {
                                callback.afterHookedMethod(param);
                            } catch(Throwable callbackError) {
                                log(LOG+": afterHookError="+callbackError.getClass().getSimpleName());
                            }
                            return param.getResult();
                        });
                    installed++;
                } catch(Throwable error) {
                    log(LOG+": installError="+name+" type="+error.getClass().getSimpleName());
                }
            }
        } catch(Throwable error) {
            log(LOG+": methodEnumerationError="+name+" type="+error.getClass().getSimpleName());
        }
        return installed;
    }

    private static Class<?> findClassIfExists(String name,ClassLoader loader) {
        try { return Class.forName(name,false,loader); }
        catch(Throwable ignored) { return null; }
    }

    /** Reflective zero-argument accessor; never logs the returned caller number. */
    private static Object callMethod(Object object,String methodName) throws Exception {
        if(object==null) throw new NullPointerException("receiver");
        Class<?> current=object.getClass();
        while(current!=null) {
            try {
                Method method=current.getDeclaredMethod(methodName);
                method.setAccessible(true);
                return method.invoke(object);
            } catch(NoSuchMethodException ignored) {
                current=current.getSuperclass();
            }
        }
        throw new NoSuchMethodException(methodName);
    }

    private static void log(String message) {
        android.util.Log.i("YellowPageTR",message);
    }
}
