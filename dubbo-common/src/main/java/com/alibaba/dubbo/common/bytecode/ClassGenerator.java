package com.alibaba.dubbo.common.bytecode;

import com.alibaba.dubbo.common.utils.ReflectUtils;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import javassist.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class ClassGenerator {

    interface DC {
    } // dynamic class tag interface.

    private static final AtomicLong CLASS_NAME_COUNTER = new AtomicLong(0);

    private static final String SIMPLE_NAME_TAG = "<init>";

    private static final Map<ClassLoader, ClassPool> POOL_MAP = Maps.newConcurrentMap(); //ClassLoader - ClassPool

    public static ClassGenerator newInstance() {
        return new ClassGenerator(getClassPool(Thread.currentThread().getContextClassLoader()));
    }

    public static ClassGenerator newInstance(ClassLoader loader) {
        return new ClassGenerator(getClassPool(loader));
    }

    public static boolean isDynamicClass(Class<?> cl) {
        return ClassGenerator.DC.class.isAssignableFrom(cl);
    }

    public static ClassPool getClassPool(ClassLoader loader) {
        if (loader == null)
            return ClassPool.getDefault();

        ClassPool pool = POOL_MAP.get(loader);
        if (pool == null) {
            pool = new ClassPool(true);
            pool.appendClassPath(new LoaderClassPath(loader));
            POOL_MAP.put(loader, pool);
        }
        return pool;
    }

    private ClassPool mPool;

    private CtClass mCtc;

    private String mClassName, mSuperClass;

    private Set<String> mInterfaces;

    private List<String> mFields, mConstructors, mMethods;

    private Map<String, Method> mCopyMethods; // <method desc,method instance>

    private Map<String, Constructor<?>> mCopyConstructors; // <constructor desc,constructor instance>

    private boolean mDefaultConstructor = false;

    private ClassGenerator() {
    }

    private ClassGenerator(ClassPool pool) {
        mPool = pool;
    }

    public String getClassName() {
        return mClassName;
    }

    public ClassGenerator setClassName(String name) {
        mClassName = name;
        return this;
    }

    public ClassGenerator addInterface(String cn) {
        if (mInterfaces == null)
            mInterfaces = Sets.newHashSet();
        mInterfaces.add(cn);
        return this;
    }

    public ClassGenerator addInterface(Class<?> cl) {
        return addInterface(cl.getName());
    }

    public ClassGenerator setSuperClass(String cn) {
        mSuperClass = cn;
        return this;
    }

    public ClassGenerator setSuperClass(Class<?> cl) {
        mSuperClass = cl.getName();
        return this;
    }

    public ClassGenerator addField(String code) {
        if (mFields == null)
            mFields = Lists.newArrayList();
        mFields.add(code);
        return this;
    }

    public ClassGenerator addField(String name, int mod, Class<?> type) {
        return addField(name, mod, type, null);
    }

    public ClassGenerator addField(String name, int mod, Class<?> type, String def) {
        StringBuilder sb = new StringBuilder();
        sb.append(modifier(mod)).append(' ').append(ReflectUtils.getName(type)).append(' ');
        sb.append(name);
        if (!Strings.isNullOrEmpty(def)) {
            sb.append('=');
            sb.append(def);
        }
        sb.append(';');
        return addField(sb.toString());
    }

    public ClassGenerator addMethod(String code) {
        if (mMethods == null)
            mMethods = Lists.newArrayList();
        mMethods.add(code);
        return this;
    }

    public ClassGenerator addMethod(String name, int mod, Class<?> rt, Class<?>[] pts, String body) {
        return addMethod(name, mod, rt, pts, null, body);
    }

    public ClassGenerator addMethod(String name, int mod, Class<?> rt, Class<?>[] pts, Class<?>[] ets, String body) {
        StringBuilder sb = new StringBuilder();
        sb.append(modifier(mod)).append(' ').append(ReflectUtils.getName(rt)).append(' ').append(name);
        doBuild(sb, pts, ets, body);
        return addMethod(sb.toString());
    }

    public ClassGenerator addMethod(Method m) {
        addMethod(m.getName(), m);
        return this;
    }

    public ClassGenerator addMethod(String name, Method m) {
        String desc = name + ReflectUtils.getDescWithoutMethodName(m);
        addMethod(':' + desc);
        if (mCopyMethods == null)
            mCopyMethods = new ConcurrentHashMap<>(8);
        mCopyMethods.put(desc, m);
        return this;
    }

    public ClassGenerator addConstructor(String code) {
        if (mConstructors == null)
            mConstructors = Lists.newLinkedList();
        mConstructors.add(code);
        return this;
    }

    public ClassGenerator addConstructor(int mod, Class<?>[] pts, String body) {
        return addConstructor(mod, pts, null, body);
    }

    public ClassGenerator addConstructor(int mod, Class<?>[] pts, Class<?>[] ets, String body) {
        StringBuilder sb = new StringBuilder();
        sb.append(modifier(mod)).append(' ').append(SIMPLE_NAME_TAG);
        doBuild(sb, pts, ets, body);
        return addConstructor(sb.toString());
    }

    private void doBuild(StringBuilder sb, Class<?>[] pts, Class<?>[] ets, String body) {
        sb.append('(');
        for (int i = 0; i < pts.length; i++) {
            if (i > 0)
                sb.append(',');
            sb.append(ReflectUtils.getName(pts[i]));
            sb.append(" arg").append(i);
        }
        sb.append(')');
        if (ets != null && ets.length > 0) {
            sb.append(" throws ");
            for (int i = 0; i < ets.length; i++) {
                if (i > 0)
                    sb.append(',');
                sb.append(ReflectUtils.getName(ets[i]));
            }
        }
        sb.append('{').append(body).append('}');
    }

    public ClassGenerator addConstructor(Constructor<?> c) {
        String desc = ReflectUtils.getDesc(c);
        addConstructor(":" + desc);
        if (mCopyConstructors == null)
            mCopyConstructors = new ConcurrentHashMap<>(4);
        mCopyConstructors.put(desc, c);
        return this;
    }

    public ClassGenerator addDefaultConstructor() {
        mDefaultConstructor = true;
        return this;
    }

    public ClassPool getClassPool() {
        return mPool;
    }

    public Class<?> toClass() {
        return toClass(getClass().getClassLoader(), getClass().getProtectionDomain());
    }

    public Class<?> toClass(ClassLoader loader, ProtectionDomain pd) {
        if (mCtc != null)
            mCtc.detach();
        long id = CLASS_NAME_COUNTER.getAndIncrement();
        try {
            CtClass ctcs = mSuperClass == null ? null : mPool.get(mSuperClass);
            if (mClassName == null)
                mClassName = (mSuperClass == null || javassist.Modifier.isPublic(ctcs.getModifiers())
                        ? ClassGenerator.class.getName() : mSuperClass + "$sc") + id;
            mCtc = mPool.makeClass(mClassName);
            if (mSuperClass != null)
                mCtc.setSuperclass(ctcs);
            mCtc.addInterface(mPool.get(DC.class.getName())); // add dynamic class tag.
            if (mInterfaces != null)
                for (String cl : mInterfaces) mCtc.addInterface(mPool.get(cl));
            if (mFields != null)
                for (String code : mFields) mCtc.addField(CtField.make(code, mCtc));
            if (mMethods != null) {
                for (String code : mMethods) {
                    if (code.charAt(0) == ':')
                        mCtc.addMethod(CtNewMethod.copy(getCtMethod(mCopyMethods.get(code.substring(1))), code.substring(1, code.indexOf('(')), mCtc, null));
                    else
                        mCtc.addMethod(CtNewMethod.make(code, mCtc));
                }
            }
            if (mDefaultConstructor)
                mCtc.addConstructor(CtNewConstructor.defaultConstructor(mCtc));
            if (mConstructors != null) {
                for (String code : mConstructors) {
                    if (code.charAt(0) == ':') {
                        mCtc.addConstructor(CtNewConstructor.copy(getCtConstructor(mCopyConstructors.get(code.substring(1))), mCtc, null));
                    } else {
                        String[] sn = mCtc.getSimpleName().split("\\$+"); // inner class name include $.
                        mCtc.addConstructor(CtNewConstructor.make(code.replaceFirst(SIMPLE_NAME_TAG, sn[sn.length - 1]), mCtc));
                    }
                }
            }
            return mCtc.toClass(loader, pd);
        } catch (NotFoundException | CannotCompileException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public void release() {
        if (mCtc != null) mCtc.detach();
        if (mInterfaces != null) mInterfaces.clear();
        if (mFields != null) mFields.clear();
        if (mMethods != null) mMethods.clear();
        if (mConstructors != null) mConstructors.clear();
        if (mCopyMethods != null) mCopyMethods.clear();
        if (mCopyConstructors != null) mCopyConstructors.clear();
    }

    private CtClass getCtClass(Class<?> c) throws NotFoundException {
        return mPool.get(c.getName());
    }

    private CtMethod getCtMethod(Method m) throws NotFoundException {
        return getCtClass(m.getDeclaringClass()).getMethod(m.getName(), ReflectUtils.getDescWithoutMethodName(m));
    }

    private CtConstructor getCtConstructor(Constructor<?> c) throws NotFoundException {
        return getCtClass(c.getDeclaringClass()).getConstructor(ReflectUtils.getDesc(c));
    }

    private static String modifier(int mod) {
        if (Modifier.isPublic(mod)) return "public";
        if (Modifier.isProtected(mod)) return "protected";
        if (Modifier.isPrivate(mod)) return "private";
        return "";
    }
}