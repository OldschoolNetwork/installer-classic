package com.oldschoolnetwork.services.classicinstaller;

import android.content.Context;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.util.*;

/** Bundled, verified release manifest. No APK name or ABI is guessed at runtime. */
public final class ServiceCatalog {
    public static final class Release {
        public String id, name, packageName, asset, sha256, abi, version;
        public int minSdk, maxSdk, versionCode;
        public List<String> dependencies = new ArrayList<>();
    }
    public static List<Release> load(Context context) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (InputStream input = context.getAssets().open("services.json")) {
            byte[] buffer = new byte[8192]; int read; while ((read=input.read(buffer))!=-1) bytes.write(buffer,0,read);
        }
        JSONArray rows = new JSONObject(bytes.toString("UTF-8")).getJSONArray("releases");
        List<Release> result = new ArrayList<>();
        for (int i=0;i<rows.length();i++) {
            JSONObject row=rows.getJSONObject(i); Release release=new Release();
            release.id=row.getString("id");release.name=row.getString("name");release.packageName=row.getString("package_name");
            release.asset=row.getString("asset");release.sha256=row.getString("sha256");release.abi=row.getString("abi");release.version=row.getString("version");
            release.minSdk=row.getInt("min_sdk");release.maxSdk=row.optInt("max_sdk",Integer.MAX_VALUE);release.versionCode=row.getInt("version_code");
            if (!release.id.matches("[a-z0-9_-]+") || !release.asset.matches("[a-zA-Z0-9_.-]+\\.apk") || !release.packageName.matches("[a-zA-Z0-9_.]+") || !release.sha256.matches("[a-fA-F0-9]{64}")) throw new IOException("Invalid bundled release metadata.");
            JSONArray dependencies=row.optJSONArray("dependencies"); if(dependencies!=null) for(int n=0;n<dependencies.length();n++)release.dependencies.add(dependencies.getString(n));
            result.add(release);
        }
        return result;
    }
    public static List<Release> select(List<Release> catalog, Collection<String> chosen) throws Exception {
        String[] abis=Build.VERSION.SDK_INT>=21?Build.SUPPORTED_ABIS:new String[]{Build.CPU_ABI,Build.CPU_ABI2};
        return resolve(catalog,chosen,Build.VERSION.SDK_INT,abis);
    }
    static List<Release> resolve(List<Release> catalog, Collection<String> chosen, int sdk, String[] abis) throws Exception {
        LinkedHashMap<String,Release> selected=new LinkedHashMap<>();Set<String> visiting=new HashSet<>();
        include("core",catalog,selected,visiting,sdk,abis);
        for(String id:chosen)include(id,catalog,selected,visiting,sdk,abis);
        return new ArrayList<>(selected.values());
    }
    private static void include(String id,List<Release> catalog,LinkedHashMap<String,Release> selected,Set<String> visiting,int sdk,String[] abis) throws Exception {
        if(selected.containsKey(id))return;
        if(!visiting.add(id))throw new IOException("Circular service dependency: "+id);
        Release best=null;int bestRank=Integer.MAX_VALUE;
        for(Release release:catalog) {
            if(!release.id.equals(id)||sdk<release.minSdk||sdk>release.maxSdk)continue;
            int rank="universal".equals(release.abi)?1000:Arrays.asList(abis).indexOf(release.abi);if(rank<0)continue;
            if(best==null||release.versionCode>best.versionCode||(release.versionCode==best.versionCode&&rank<bestRank)){best=release;bestRank=rank;}
        }
        if(best==null)throw new IOException("No compatible version of "+id+" for Android API "+sdk+" / "+Arrays.toString(abis));
        for(String dependency:best.dependencies)include(dependency,catalog,selected,visiting,sdk,abis);
        visiting.remove(id);selected.put(id,best);
    }
}
