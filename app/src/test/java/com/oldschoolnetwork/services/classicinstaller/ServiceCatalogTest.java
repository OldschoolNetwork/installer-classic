package com.oldschoolnetwork.services.classicinstaller;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public class ServiceCatalogTest {
 private ServiceCatalog.Release release(String id,int version,String abi,int min,String... dependencies) {
  ServiceCatalog.Release row=new ServiceCatalog.Release();row.id=id;row.versionCode=version;row.abi=abi;row.minSdk=min;row.maxSdk=100;row.dependencies=Arrays.asList(dependencies);return row;
 }
 @Test public void resolvesCoreBeforeServiceAndUsesCompatibleAbi() throws Exception {
  ServiceCatalog.Release core=release("core",1,"universal",14),arm=release("store",2,"arm64-v8a",21,"core"),old=release("store",1,"armeabi-v7a",14,"core");
  List<ServiceCatalog.Release> chosen=ServiceCatalog.resolve(Arrays.asList(arm,old,core),Arrays.asList("store"),19,new String[]{"armeabi-v7a"});
  assertEquals(Arrays.asList(core,old),chosen);
 }
 @Test public void prefersNativeForSameVersionAndAvoidsDuplicateDependencies() throws Exception {
  ServiceCatalog.Release core=release("core",1,"universal",14),universal=release("store",2,"universal",14,"core"),nativeRow=release("store",2,"x86",14,"core");
  assertEquals(Arrays.asList(core,nativeRow),ServiceCatalog.resolve(Arrays.asList(universal,nativeRow,core),Arrays.asList("core","store"),22,new String[]{"x86"}));
 }
 @Test(expected=java.io.IOException.class) public void rejectsMissingCompatibleRelease() throws Exception {
  ServiceCatalog.resolve(Arrays.asList(release("core",1,"arm64-v8a",21)),Collections.<String>emptyList(),14,new String[]{"armeabi"});
 }
 @Test(expected=java.io.IOException.class) public void rejectsDependencyCycle() throws Exception {
  ServiceCatalog.resolve(Arrays.asList(release("core",1,"universal",14,"store"),release("store",1,"universal",14,"core")),Arrays.asList("store"),22,new String[]{"x86"});
 }
 @Test public void quotesShellArgumentsWithoutAllowingCommandSubstitution(){assertEquals("'a'\\''$(id)'",InstallEngine.quote("a'$(id)"));}
}
