package com.scu.suhong.dynamic_definition;

import com.scu.suhong.block.BlockChain;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import util.FileHelper;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

//
// https://blog.csdn.net/zhoufanyang_china/article/details/82767406
//
public class AssetCompiler {
    private long requiredGas = 0;
    private long givenGas = 0;
    private long leftGas = 0;
    private boolean isGasRefreshed = false;
    private static URLClassLoader urlcl;

    public AssetCompiler(){
        if (null == urlcl) {
            File f = new File(System.getProperty("user.dir"));
            URL[] cp = new URL[0];
            try {
                cp = new URL[]{f.toURI().toURL()};
                urlcl = new URLClassLoader(cp);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
    }

    public static String getAssetInstanceSymbol() {
        return "@GlobalInstance";
    }

    public static String getLinechangerReplacement() {
        return ":::";
    }

    public static String getFullClassName(String className) {
        int pos = className.lastIndexOf('.');
        if (-1 != pos) return className; // return when contains package

        return removePackageNamePrefix(DynamicalAsset.class.getPackage().toString()) + "." + className;
    }

    public static String getClassName(String className) {
        int pos = className.lastIndexOf('.');
        if (-1 == pos) return className;

        return className.substring(pos + 1);
    }

    public static String getPackageName(String className, String defaultPackage) {
        int pos = className.lastIndexOf('.');
        if (-1 == pos) return defaultPackage;

        return className.substring(0, pos);
    }

    public static boolean checkClassName(String className, String code) {
        String[] lines = code.split("\n");

        for (int i = 0; i < lines.length; ++i) {
            String[] words = lines[i].split("\\s");

            for (int j = 0; j < words.length - 1; ++j) {
                if (words[j].equals("class")) {
                    if (words[j + 1].equals(className) || words[j + 1].matches(className + "\\{.*")) return true;
                }
            }
        }
        return false;
    }

    public boolean compile(String fileName) {
        if (!isGasRefreshed) {
            System.out.println("[AssetCompiler][ERROR] Gas is not set to compile!");
            return false;
        }
        //dynamically compile
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();

        ByteArrayOutputStream compileStdOut = new ByteArrayOutputStream();
        ByteArrayOutputStream compileErrorOutput = new ByteArrayOutputStream();
        long startTime = System.currentTimeMillis();
        int status = -1;
        try {
            status = javac.run(null, compileStdOut, compileErrorOutput,  "-d", ".","-cp",
                    "json.jar", "-cp", "annotation.jar", "-cp", "blockchain.jar", "-cp", ".", fileName);
        } catch (NullPointerException e){
            e.printStackTrace();
        }

        long endTime = System.currentTimeMillis();
        if (!processGas(startTime, endTime)) return false;
        if (0 != status) {
            System.out.println("[AssetCompiler][ERROR] Failed to compile!" + compileStdOut.toString() + ":" + compileErrorOutput.toString());
            System.out.println("[AssetCompiler][ERROR] delete file " + fileName);
            FileHelper.deleteFile(fileName);
            return false;
        }

        System.out.println("[AssetCompiler][INFO] Succeed to compile code");
        return true;
    }

    private boolean processGas(long startTime, long endTime) {
        requiredGas = endTime - startTime;
        isGasRefreshed = false;
        System.out.println("[AssetCompiler][Info] Gas required is " + requiredGas);

        if (givenGas - requiredGas < 0) {
            System.out.println("[AssetCompiler][ERROR] Not enough gas.");
            return false;
        }
        return true;
    }

    public boolean compile(String className, String packageName, String code) {
        if (!code.isEmpty() && !checkClassName(className, code)) {
            System.out.println("[AssetCompiler][ERROR] Class name: " + className + " is not defined in code \n" + code);
            return false;
        }

        String fileName = className + ".java";
        if (FileHelper.doesFileOrFolderExist(fileName)) {
            System.out.println("[AssetCompiler][Info] " + fileName + " already exist. It means its class has been defined before");
            return true;
        }

        if (code.contains("package")) {
            System.out.printf("[AssetCompiler][ERROR] code should not contains package, as we use the default package.\n %s", code);
            return false;
        }

        code = preProcess(code, className);

        if (!FileHelper.createFile(fileName, code)) {
            System.out.println("[AssetCompiler][ERROR] Cannot create file to comile for " + className);
            return false;
        }

        System.out.println("[AssetCompiler][INFO] try to backup source file");
        FileHelper.createFile(fileName+".backup", code);

        return compile(fileName);
    }

    public boolean compileAndStartGlobalInstance(String className, String packageName, String code) {

        if (!compile(className, packageName, code)) return false;
        System.out.println("[AssetCompiler][INFO] Succeed to compile code");
        //start the global process, it is used to cache all transactions of this kind, just for the efficienecy
        Object globalInstance = getInstance(className, packageName);
        if (null != globalInstance) {
            BlockChain.getInstance().setGlobalAssetInstance(className, globalInstance);
            System.out.println("[AssetCompiler][INFO] Succeed to start global instance");
            return true;
        } else {
            System.out.println("[AssetCompiler][ERROR] Fail to create or get global instance");
        }
        return false;
    }

    @NotNull
    private String preProcess(String code, String className) {
        String newCode = "package com.scu.suhong.dynamic_definition;\n";
        //  add default package
        // check get global instance;
        if (code.contains(getAssetInstanceSymbol())) {
            newCode += "import com.scu.suhong.block.BlockChain;\n";
            code = code.replace(getAssetInstanceSymbol(), "BlockChain.getInstance().getGlobalAssetInstance(\"" + className + "\")");
        }

        newCode += code;
        return newCode;
    }

    public Object compileAndGetInstance(String className, String code) {
        if (className.contains(".")) {
            System.out.printf("[AssetCompiler][ERROR] %s contains package name. Retry to remove the package name \n", className);
            return null;
        }
        return compileAndGetInstance(className, DynamicalAsset.class.getPackage().getName(), code);
    }

    Object compileAndGetInstance(String className, String packageName, String code) {
        if (null != code && !code.isEmpty()) {
            if (!compileAndStartGlobalInstance(className, packageName, code)) {
                return null;
            } else {
                System.out.println("[AssetCompiler][INFO] Succeed to compile new asset " + className);
            }
        } else {
            System.out.printf("[AssetCompiler][INFO] Asset already %s define. Just use legacy code\n", className);
        }

        return getInstance(className, packageName);
    }

    /*
    * Class.getPackage() is like " package com.suhong.*"
    * In this function, we remove package prefix
    * */
    static private String removePackageNamePrefix(String packageName){
        String packagePrefix = "package";
        if (!packageName.contains(packageName)) return packageName;

        String[] spiltName = packageName.split("\\s");
        if (2 == spiltName.length && spiltName[0].equals(packagePrefix)) return spiltName[1];

        return packageName;
    }

    @Nullable
    public Object getInstance(String className, String packageName) {
        System.out.printf("[AssetCompiler][INFO] Try to instantiate the class: %s in package: %s\n", className, packageName);
        Class classDefine;
        try {
            classDefine = getClassByName(className, packageName);
            if (null != classDefine) return classDefine.newInstance();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Nullable
    public Object getInstance(String fullClassName) {
        System.out.printf("[AssetCompiler][INFO] Try to instantiate the class: %s\n", fullClassName);
        Class classDefine;
        try {
            classDefine = getClassByName(fullClassName);
            if (null != classDefine) return classDefine.newInstance();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
        System.out.printf("[AssetCompiler][ERROR] Fail to instantiate the class: %s\n", fullClassName);
        return null;
    }

    public boolean runBooleanMethod(String className, String methodName) throws ClassNotFoundException,
            IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        return runBooleanMethod(AssetCompiler.getClassName(className)
                , AssetCompiler.getPackageName(className, DynamicalAsset.class.getPackage().toString())
                , methodName);
    }

    public boolean runBooleanMethod(String className, String packageName, String methodName) throws
            IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        if (!isGasRefreshed) {
            System.out.println("[AssetCompiler][ERROR] Gas is not set to run class " + className);
            return false;
        }

        Class classDefine = getClassByName(className, packageName);
        Object object = classDefine.newInstance();
        Method method = classDefine.getDeclaredMethod(methodName);
        long startTime = System.currentTimeMillis();
        boolean r = (boolean) method.invoke(object);
        long endTime = System.currentTimeMillis();
        if (!processGas(startTime, endTime)) return false;
        return r;
    }

    private Class getClassByName(String className, String packageName){
        System.out.printf("[AssetCompiler][INFO] Try to lookup the class: %s in package: %s\n", className, packageName);
        packageName = removePackageNamePrefix(packageName);
        System.out.printf("[AssetCompiler][Debug] Real lookup the class: %s in package: %s\n", className, packageName);
        return getClassByName(packageName + "." + className);
    }


    private Class getClassByName(String fullClassName){
        System.out.printf("[AssetCompiler][INFO] Try to lookup the class: %s \n", fullClassName);
        if (!fullClassName.contains(".")){
            fullClassName = AssetCompiler.getFullClassName(fullClassName);
            System.out.printf("[AssetCompiler][INFO] As no package name found in class name. Try to lookup the class: %s \n", fullClassName);
        }
        try {

            return urlcl.loadClass(fullClassName);
            //return Class.forName(packageName + "." + className);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void cleanGasInformation() {
        setGivenGas(0);
        setLeftGas(0);
        setRequiredGas(0);
    }

    public long getRequiredGas() {
        return requiredGas;
    }

    public void setRequiredGas(long requiredGas) {
        this.requiredGas = requiredGas;
    }

    public long getGivenGas() {
        return givenGas;
    }

    public void setGivenGas(long givenGas) {
        isGasRefreshed = true;
        this.givenGas = givenGas;
    }

    public long getLeftGas() {
        return leftGas;
    }

    public void setLeftGas(long leftGas) {
        this.leftGas = leftGas;
    }
}
