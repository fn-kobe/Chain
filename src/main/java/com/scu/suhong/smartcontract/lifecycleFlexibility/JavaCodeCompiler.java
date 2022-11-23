 package com.scu.suhong.smartcontract.lifecycleFlexibility;
 
 import com.scu.suhong.block.BlockChain;
 import java.io.ByteArrayOutputStream;
 import java.io.File;
 import java.lang.reflect.InvocationTargetException;
 import java.lang.reflect.Method;
 import java.net.MalformedURLException;
 import java.net.URL;
 import java.net.URLClassLoader;
 import javax.tools.JavaCompiler;
 import javax.tools.ToolProvider;

 import com.scu.suhong.dynamic_definition.DynamicalAsset;
 import org.jetbrains.annotations.NotNull;
 import org.jetbrains.annotations.Nullable;
 import util.FileHelper;
 import util.GasHandler;

 public class JavaCodeCompiler
 {
   private static URLClassLoader urlcl;
   private GasHandler gasHandler = new GasHandler();
   private boolean checkGasOrNot;

   public JavaCodeCompiler() {
     if (null == urlcl) {
       File f = new File(System.getProperty("user.dir"));
       URL[] cp = new URL[0];
       try {
         cp = new URL[] { f.toURI().toURL() };
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
     if (-1 != pos) return className;
     
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
     
     for (int i = 0; i < lines.length; i++) {
       String[] words = lines[i].split("\\s");
       
       for (int j = 0; j < words.length - 1; j++) {
         if (words[j].equals("class") && (
           words[j + 1].equals(className) || words[j + 1].matches(className + "\\{.*"))) return true;
       
       } 
     } 
     return false;
   }

   public boolean compile(String fileName) {
     if (checkGasOrNot && !gasHandler.hasGas()) {
       System.out.println("[AssetCompiler][ERROR] Gas is not set to compile!");
       return false;
     } 
     
     JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
     
     ByteArrayOutputStream compileStdOut = new ByteArrayOutputStream();
     ByteArrayOutputStream compileErrorOutput = new ByteArrayOutputStream();
     long startTime = System.currentTimeMillis();
     int status = -1;
     try {
       status = javac.run(null, compileStdOut, compileErrorOutput, new String[] { "-d", ".", "-cp", "json.jar", "-cp", "annotation.jar", "-cp", "blockchain.jar", "-cp", ".", fileName });
     }
     catch (NullPointerException e) {
       e.printStackTrace();
     } 
     
     long endTime = System.currentTimeMillis();
     if (checkGasOrNot && !processGas(startTime, endTime)) return false;
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
     return gasHandler.processGas(startTime, endTime);
   }

   public boolean compile(String className, String packageName, String code) {
     if (!code.isEmpty() && !checkClassName(className, code)) {
       System.out.println("[AssetCompiler][ERROR] Class name: " + className + " is not defined in code \n" + code);
       return false;
     } 

     String fileName = FileHelper.getFileNameByClassOrFileName(className);
     if (FileHelper.doesFileOrFolderExist(fileName)) {
       System.out.println("[AssetCompiler][Info] " + fileName + " already exist. It means its class has been defined before and skip to recompile it.");
       return true;
     } 
     
     if (code.contains("package")) {
       System.out.printf("[AssetCompiler][ERROR] code should not contains package, as we use the default package.\n %s", new Object[] { code });
       return false;
     } 
     
     code = preProcess(code, className);
     
     if (!FileHelper.createFile(fileName, code)) {
       System.out.println("[AssetCompiler][ERROR] Cannot create file to comile for " + className);
       return false;
     } 
     
     System.out.println("[AssetCompiler][INFO] try to backup source file");
     FileHelper.createFile(fileName + ".backup", code);
     
     return compile(fileName);
   }

   public boolean compileAndStartGlobalInstance(String className, String packageName, String code) {
     if (!compile(className, packageName, code)) return false; 
     System.out.println("[AssetCompiler][INFO] Succeed to compile code");
     
     Object globalInstance = getInstance(className, packageName);
     if (null != globalInstance) {
       BlockChain.getInstance().setGlobalAssetInstance(className, globalInstance);
       System.out.println("[AssetCompiler][INFO] Succeed to start global instance");
       return true;
     } 
     System.out.println("[AssetCompiler][ERROR] Fail to create or get global instance");
     
     return false;
   }
   
   @NotNull
   private String preProcess(String code, String className) {
     String newCode = "package com.scu.suhong.dynamic_definition;\n";
 
     
     if (code.contains(getAssetInstanceSymbol())) {
       newCode = newCode + "import com.scu.suhong.block.BlockChain;\n";
       code = code.replace(getAssetInstanceSymbol(), "BlockChain.getInstance().getGlobalAssetInstance(\"" + className + "\")");
     } 
     
     newCode = newCode + code;
     if (newCode == null) return null;  return newCode;
   }
   
   public Object compileAndGetInstance(String className, String code) {
     if (className.contains(".")) {
       System.out.printf("[AssetCompiler][ERROR] %s contains package name. Retry to remove the package name \n", new Object[] { className });
       return null;
     } 
     return compileAndGetInstance(className, getCompilerPackageName(), code);
   }
   
   Object compileAndGetInstance(String className, String packageName, String code) {
     if (null != code && !code.isEmpty()) {
       if (!compileAndStartGlobalInstance(className, packageName, code)) {
         return null;
       }
       System.out.println("[AssetCompiler][INFO] Succeed to compile new asset " + className);
     } else {
       
       System.out.printf("[AssetCompiler][INFO] Asset already %s define. Just use legacy code\n", new Object[] { className });
     } 
     
     return getInstance(className, packageName);
   }

   private static String removePackageNamePrefix(String packageName) {
     String packagePrefix = "package";
     if (!packageName.contains(packageName)) return packageName;
     
     String[] spiltName = packageName.split("\\s");
     if (2 == spiltName.length && spiltName[0].equals(packagePrefix)) return spiltName[1];
     
     return packageName;
   }

   @NotNull
   public Object getInstance(String className, String packageName) {
     System.out.printf("[AssetCompiler][INFO] Try to instantiate the class: %s in package: %s\n", new Object[] { className, packageName });

     try {
       Class classDefine = getClassByName(className, packageName);
       if (null != classDefine) return classDefine.newInstance();
     } catch (IllegalAccessException e) {
       e.printStackTrace();
     } catch (InstantiationException e) {
       e.printStackTrace();
     }
     return null;
   }

   @Nullable
   public Object getInstance(String className) {
     System.out.printf("[AssetCompiler][INFO] Try to instantiate the class: %s\n", new Object[] { className });

     try {
       Class classDefine = getClassByName(className);
       if (null != classDefine) return classDefine.newInstance();
     } catch (IllegalAccessException e) {
       e.printStackTrace();
     } catch (InstantiationException e) {
       e.printStackTrace();
     }
     System.out.printf("[AssetCompiler][ERROR] Fail to instantiate the class: %s\n", new Object[] { className });
     return null;
   }

   public boolean runBooleanMethod(String className, String methodName)
           throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
     return runBooleanMethod(getClassName(className), 
         getPackageName(className, DynamicalAsset.class.getPackage().toString()), methodName);
   }

   public boolean runBooleanMethod(String className, String packageName, String methodName)
           throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
     if (checkGasOrNot && !gasHandler.hasGas()) {
       System.out.println("[AssetCompiler][ERROR] Gas is not set to run class " + className);
       return false;
     } 
     
     Class classDefine = getClassByName(className, packageName);
     Object object = classDefine.newInstance();
     long startTime = System.currentTimeMillis();
     boolean r = runBooleanMethod(object, className, methodName);
     long endTime = System.currentTimeMillis();
     if (!processGas(startTime, endTime)) return false; 
     return r;
   }

   public void runMethod(Object o, String className, String methodName)
           throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
     runMethod(o, className, getCompilerPackageName(), methodName);
   }

   public void runMethod(Object o, String className, String packageName, String methodName)
           throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
     Method method = getMethod(className, methodName, packageName);
     method.invoke(o);
   }

   public boolean runBooleanMethod(Object o, String className, String methodName)
           throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
     return runBooleanMethod(o, className, methodName, getCompilerPackageName());
   }

   public boolean runBooleanMethod(Object o, String className, String methodName, String packageName)
           throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
     Method method = getMethod(className, methodName, packageName);
     return (boolean)method.invoke(o);
   }

   public Method getMethod(String className, String methodName) throws NoSuchMethodException {
     return getMethod(className, methodName, getCompilerPackageName());
   }

   public Method getMethod(String className, String methodName, String packageName) throws NoSuchMethodException {
     Class classDefine = getClassByName(className, packageName);
     return classDefine.getDeclaredMethod(methodName);
   }

   @NotNull
   private String getCompilerPackageName() {
     return DynamicalAsset.class.getPackage().getName();
   }

   public Class getClassByName(String className) {
     return getClassByName(className, DynamicalAsset.class.getPackage().toString());
   }

   public Class getClassByName(String className, String packageName) {
     System.out.printf("[AssetCompiler][INFO] Try to lookup the class: %s in package: %s\n", className, packageName);
     packageName = removePackageNamePrefix(packageName);
     return getClassByFullName(packageName + "." + className);
   }
 
   
   public Class getClassByFullName(String fullClassName) {
     System.out.printf("[AssetCompiler][INFO] Try to lookup the class with full class name %s \n", fullClassName);
     if (!fullClassName.contains(".")) {
       fullClassName = getFullClassName(fullClassName);
       System.out.printf("[AssetCompiler][INFO] As no package name found in class name. Try to lookup the class: %s \n", fullClassName);
     } 
     
     try {
       return urlcl.loadClass(fullClassName);
     }
     catch (ClassNotFoundException e) {
       e.printStackTrace();
       
       return null;
     } 
   }

   public void setGivenGas(long givenGas) {
     gasHandler.setGivenGas(givenGas);
   }

   public void cleanCompiledFile(String fileName) {
     FileHelper.deleteFile(FileHelper.getFileNameByClassOrFileName(fileName));
   }

   public void shouldCheckGas(boolean checkGasOrNot) {
     this.checkGasOrNot = checkGasOrNot;
   }
 }


