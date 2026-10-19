package com.oldschoolnetwork.services.classicinstaller;
import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.View;
import android.widget.*;
import androidx.core.content.FileProvider;
import java.io.File;
import java.util.*;

public class MainActivity extends Activity {
    private TransferNotification transferNotification;
    private static final int INSTALL=1001,PERMISSION=1002;
    private final Handler main=new Handler(Looper.getMainLooper());
    private List<ServiceCatalog.Release> catalog,selected;
    private final List<File> prepared=new ArrayList<>();
    private TextView status,log;
    private int next;
    private boolean busy,awaitingPermission;
    private List<GoogleComponents.Entry> googleRemoval=Collections.emptyList();
    private CheckBox removeGoogle;
    private String rollback;
    protected void onCreate(Bundle state) {
        super.onCreate(state);setContentView(R.layout.activity_main);
        android.widget.Button permissions=new android.widget.Button(this);permissions.setText("Permissions and notifications");permissions.setOnClickListener(v->PermissionPrompt.show(this,true));((android.view.ViewGroup)((ScrollView)findViewById(R.id.installScroll)).getChildAt(0)).addView(permissions);
        PermissionPrompt.show(this,false);
        removeGoogle=new CheckBox(this);removeGoogle.setText("Remove legacy Google services during full installation");removeGoogle.setTextColor(0xFFFFFFFF);removeGoogle.setChecked(false);((android.view.ViewGroup)((ScrollView)findViewById(R.id.installScroll)).getChildAt(0)).addView(removeGoogle);
        ((ImageView)findViewById(R.id.servicesLogo)).setImageDrawable(androidx.vectordrawable.graphics.drawable.VectorDrawableCompat.create(getResources(),R.drawable.ic_services_logo,getTheme()));
        status=findViewById(R.id.installStatus);log=findViewById(R.id.installLog);
        findViewById(R.id.btnOrdinary).setOnClickListener(v->begin(false));
        findViewById(R.id.btnSystem).setOnClickListener(v->new AlertDialog.Builder(this,AlertDialog.THEME_HOLO_DARK).setTitle("Full system installation").setMessage("The selected services and their dependencies will be installed as system apps. Root permission and a restart are required. Back up your device and read the recovery guide first.").setNegativeButton("Cancel",null).setPositiveButton("Install with root",(d,w)->begin(true)).show());
        findViewById(R.id.btnGuide).setOnClickListener(v->open("https://oldschoolnetwork.online/downloads/docs/full-install/"));
        findViewById(R.id.btnAbout).setOnClickListener(v->startActivity(new Intent(this,AboutActivity.class)));
        findViewById(R.id.btnRestart).setOnClickListener(v->new AlertDialog.Builder(this,AlertDialog.THEME_HOLO_DARK).setTitle("Restart device?").setMessage("Save your work in other apps first.").setNegativeButton("Cancel",null).setPositiveButton("Restart",(d,w)->new Thread(()->{try{InstallEngine.root("reboot");}catch(Exception error){append(error.getMessage());}}).start()).show());
        try {
            catalog=ServiceCatalog.load(this);List<ServiceCatalog.Release> preview=ServiceCatalog.select(catalog,Arrays.asList("store"));
            StringBuilder text=new StringBuilder(Build.MANUFACTURER+" "+Build.MODEL+" · Android "+Build.VERSION.RELEASE+"\n");
            for(ServiceCatalog.Release release:preview)text.append(release.name).append(" ").append(release.version).append(" · ").append(release.abi).append('\n');
            ((TextView)findViewById(R.id.compatibilityInfo)).setText(text);
        } catch(Exception error){status.setText(error.getMessage());findViewById(R.id.btnOrdinary).setEnabled(false);findViewById(R.id.btnSystem).setEnabled(false);}
        if(state!=null&&state.getBoolean("busy"))status.setText("Installation was interrupted. Check installed services, then run the installer again. Do not assume a full installation succeeded.");
    }
    private void open(String url){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(ActivityNotFoundException error){status.setText(url);}}
    private void begin(final boolean system) {
        if(busy||catalog==null)return;
        if(system && removeGoogle.isChecked()){
            final List<GoogleComponents.Entry> targets=GoogleComponents.installed(this);
            if(!targets.isEmpty() && googleRemoval.isEmpty()){
                StringBuilder list=new StringBuilder("These Google services will be removed from the active system. Google sign-in, Play Store, push delivery and synchronisation may stop working. Other apps can depend on them. Existing app data is kept. A recovery script will be saved; keep a recovery method available before proceeding.\n\n");
                for(GoogleComponents.Entry entry:targets)list.append(entry.label).append(" (").append(entry.packageName).append(")\n");
                new AlertDialog.Builder(this,AlertDialog.THEME_HOLO_DARK).setTitle("Remove Google services?").setMessage(list).setNegativeButton("Keep Google services",(d,w)->{removeGoogle.setChecked(false);googleRemoval=Collections.emptyList();begin(true);}).setNeutralButton("Cancel",null).setPositiveButton("Remove listed services",(d,w)->{googleRemoval=targets;begin(true);}).show();return;
            }
        }else googleRemoval=Collections.emptyList();

        transferNotification=new TransferNotification(this,4202,"Oldschool Network Installer");
        busy=true;next=0;prepared.clear();rollback=null;setBusy(true);log.setText("");status.setText("Checking selected services…");
        final boolean includeStore=((CheckBox)findViewById(R.id.chooseStore)).isChecked();
        new Thread(()->{
            try {
                selected=ServiceCatalog.select(catalog,includeStore?Arrays.asList("store"):Collections.<String>emptyList());
                for(ServiceCatalog.Release release:selected){append("Verifying "+release.name+" "+release.version+"…");prepared.add(InstallEngine.prepare(this,release,(bytes,total)->transferNotification.preparing(bytes,total)));}
                if(system){transferNotification.installing();rollback=InstallEngine.installSystem(this,selected,prepared,googleRemoval,this::append);main.post(this::finishSystem);}
                else main.post(this::installNext);
            }catch(Exception error){main.post(()->fail(error.getMessage()));}
        },"on-service-install").start();
    }
    private void finishSystem(){if(transferNotification!=null)transferNotification.finish();busy=false;setBusy(false);status.setText("System files staged. Restart to activate, then verify Full installation in your Store profile.");append("Recovery command: su -c \"sh "+rollback+"\"");findViewById(R.id.btnRestart).setVisibility(View.VISIBLE);}
    private void installNext() {
        if(next>=selected.size()){if(transferNotification!=null)transferNotification.finish();busy=false;setBusy(false);status.setText("Ordinary installation complete. These services are installed as user apps.");scrollLog();return;}
        if(Build.VERSION.SDK_INT>=26&&!getPackageManager().canRequestPackageInstalls()){
            awaitingPermission=true;status.setText("Allow this installer to install APKs, then return here.");
            try{startActivityForResult(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+getPackageName())),PERMISSION);}catch(Exception error){fail("Open Android Settings and allow installation from this app.");}return;
        }
        if(transferNotification!=null)transferNotification.installing();
        ServiceCatalog.Release release=selected.get(next);status.setText("Install "+release.name+" ("+(next+1)+"/"+selected.size()+")");
        File apk=prepared.get(next);
        Uri uri;
        if(Build.VERSION.SDK_INT<24) {
            // Classic PackageInstaller accepts file: URIs. Only this public APK is readable.
            apk.getParentFile().setExecutable(true,false);getFilesDir().setExecutable(true,false);apk.setReadable(true,false);
            uri=Uri.fromFile(apk);
        } else uri=FileProvider.getUriForFile(this,getPackageName()+".provider",apk);
        Intent intent=new Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).putExtra(Intent.EXTRA_RETURN_RESULT,true);
        try{startActivityForResult(intent,INSTALL);}catch(Exception error){fail("Android's package installer could not open this APK: "+error.getMessage());}
    }
    protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==PERMISSION&&awaitingPermission){awaitingPermission=false;if(Build.VERSION.SDK_INT<26||getPackageManager().canRequestPackageInstalls())installNext();else fail("Installation permission was not granted.");}
        if(request==INSTALL&&busy){
            if(Build.VERSION.SDK_INT<24){prepared.get(next).setReadable(false,false);prepared.get(next).setReadable(true,true);}
            if(result!=RESULT_OK){fail("Installation cancelled or failed. Already installed services were kept.");return;}
            try{if(getPackageManager().getPackageInfo(selected.get(next).packageName,0).versionCode<selected.get(next).versionCode)throw new Exception();next++;installNext();}catch(Exception error){fail("Android did not confirm the selected package version.");}
        }
    }
    private void setBusy(boolean value){if(removeGoogle!=null)removeGoogle.setEnabled(!value);findViewById(R.id.btnOrdinary).setEnabled(!value);findViewById(R.id.btnSystem).setEnabled(!value);findViewById(R.id.chooseStore).setEnabled(!value);findViewById(R.id.installProgress).setVisibility(value?View.VISIBLE:View.GONE);findViewById(R.id.btnRestart).setVisibility(View.GONE);}
    private void fail(String message){if(transferNotification!=null)transferNotification.finish();busy=false;setBusy(false);status.setText("Installation did not finish");append(message==null?"Unknown installation error.":message);}
    private void scrollLog(){final ScrollView scroll=findViewById(R.id.installScroll);scroll.post(()->scroll.fullScroll(View.FOCUS_DOWN));}
    private void append(final String text){main.post(()->{if(!isFinishing()){log.append(text+"\n\n");scrollLog();}});}
    public void onBackPressed(){if(busy)new AlertDialog.Builder(this,AlertDialog.THEME_HOLO_DARK).setTitle("Installation in progress").setMessage("Wait for the current operation to finish. System writes cannot be interrupted safely.").setPositiveButton("OK",null).show();else super.onBackPressed();}
    protected void onSaveInstanceState(Bundle out){out.putBoolean("busy",busy);super.onSaveInstanceState(out);}
}
