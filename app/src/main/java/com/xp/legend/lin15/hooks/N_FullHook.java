package com.xp.legend.lin15.hooks;

import android.app.AndroidAppHelper;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;


import com.xp.legend.lin15.bean.Full;
import com.xp.legend.lin15.bean.Result;
import com.xp.legend.lin15.utils.BaseHook;
import com.xp.legend.lin15.utils.Conf;
import com.xp.legend.lin15.utils.ReceiverAction;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class N_FullHook extends BaseHook implements IXposedHookLoadPackage {

    private static final String CLASS = "com.android.systemui.qs.QSContainer";

    private static final String METHOD = "onFinishInflate";

    private static final String METHOD2="setQsExpansion";

    private static final String METHOD3="onConfigurationChanged";


    private View full;

    private int alphaValue = 255;

    private boolean isGao = false;

    private int gaoValue = 25;

    private int drawable = 0;

    private SharedPreferences sharedPreferences;

    private int rotation = -101;

    private FullReceiver receiver;

    private int quality = Conf.LOW_QUALITY;

//    private MyOrientationEventChangeListener myOrientationEventChangeListener;

    private View header;

    private ImageView bgView, hengBgView;
    private SlitImageView shuSlit, hengSlit;

    private boolean isScroll = true;//是否滚动背景
    private boolean isSlit=true;//是否卷轴背景


    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        if (!isN()){
            return;
        }

        if (!lpparam.packageName.equals("com.android.systemui")) {
            return;
        }

        XposedHelpers.findAndHookMethod(CLASS, lpparam.classLoader, METHOD, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);

                register();//注册广播

                sharedPreferences = AndroidAppHelper.currentApplication().getSharedPreferences(Conf.SHARE, Context.MODE_PRIVATE);

                quality = sharedPreferences.getInt(Conf.FULL_QUALITY, Conf.LOW_QUALITY);

                alphaValue = sharedPreferences.getInt(Conf.FULL_ALPHA_VALUE, 255);

                isGao = sharedPreferences.getBoolean(Conf.FULL_GAO, false);

                gaoValue = sharedPreferences.getInt(Conf.FULL_GAO_VALUE, 25);

                isScroll = sharedPreferences.getBoolean(Conf.FULL_SCROLL, true);
                isSlit=sharedPreferences.getBoolean(Conf.SLIT,true);

                drawable = AndroidAppHelper
                        .currentApplication()
                        .getResources()
                        .getIdentifier("qs_background_primary", "drawable", lpparam.packageName);


                full = (View) param.thisObject;

                header = (View) XposedHelpers.getObjectField(param.thisObject, "mHeader");

                saveFullWidthInfo();

                autoSetBg();

//                myOrientationEventChangeListener = new MyOrientationEventChangeListener(AndroidAppHelper.currentApplication(), SensorManager.SENSOR_DELAY_NORMAL);
//
//                myOrientationEventChangeListener.enable();
            }
        });


        XposedHelpers.findAndHookMethod(CLASS, lpparam.classLoader, METHOD2, float.class, float.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);

                float f= (float) param.args[0];

                autoChangeAlpha(f);

            }
        });



    }


    private void autoSetBg() {

        if (full==null){
            return;
        }


        if (isSlit){

            setSlitImage();

        }else if (isScroll){

            setScrollBg();

        }else {

            setBg();
        }



    }

    /**
     * 判断是否是N系列
     *
     * @return
     */
    private boolean isN() {

        return Build.VERSION.SDK_INT == Build.VERSION_CODES.N || Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1;
    }


    private void register() {

        if (this.receiver == null) {
            this.receiver = new FullReceiver();

            IntentFilter intentFilter = new IntentFilter();

            intentFilter.addAction(ReceiverAction.SET_N_FULL_VERTICAL_IMAGE);

            intentFilter.addAction(ReceiverAction.SET_N_FULL_HORIZONTAL_IMAGE);

            intentFilter.addAction(ReceiverAction.N_GET_EXPANSION_FLOAT);

            intentFilter.addAction(ReceiverAction.SET_N_FULL_GAO_SI);

            intentFilter.addAction(ReceiverAction.SET_N_FULL_GAO_VALUE);

            intentFilter.addAction(ReceiverAction.N_FULL_ALPHA_VALUE);

            intentFilter.addAction(ReceiverAction.DELETE_FULL_BG);

            intentFilter.addAction(ReceiverAction.GET_FULL_INFO);

            intentFilter.addAction(ReceiverAction.SET_FULL_QUALITY);

            intentFilter.addAction(ReceiverAction.UI_GET_FULL_INFO);

            intentFilter.addAction(ReceiverAction.SEND_FULL_SCROLL);
            intentFilter.addAction(ReceiverAction.SEND_CLEAN_ACTION);

            intentFilter.addAction(ReceiverAction.SEND_CUSTOM_HEIGHT);

            intentFilter.addAction(ReceiverAction.SEND_ORI);

            intentFilter.addAction(ReceiverAction.SEND_SLIT_INFO);

            AndroidAppHelper.currentApplication().registerReceiver(receiver, intentFilter);


        }

    }


    class FullReceiver extends BroadcastReceiver {


        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent == null) {
                return;
            }

            String action = intent.getAction();

            if (action == null) {
                return;
            }

            switch (action) {

                case ReceiverAction.SET_N_FULL_VERTICAL_IMAGE:

                    setFullVerticalImage(intent, context);

                    break;

                case ReceiverAction.SET_N_FULL_HORIZONTAL_IMAGE:

                    setFullHorizontalImage(context, intent);


                    break;
                case ReceiverAction.N_FULL_ALPHA_VALUE:
                    getAlpha(intent);

                    break;
                case ReceiverAction.SET_N_FULL_GAO_SI:

                    getGaoSi(intent);

                    break;
                case ReceiverAction.SET_N_FULL_GAO_VALUE:

                    getGaoValue(intent);

                    break;
                case ReceiverAction.DELETE_FULL_BG:

                    deleteBg(intent, context);

                    break;
                case ReceiverAction.GET_FULL_INFO:

                    sendInfo(intent,context);


                    break;
                case ReceiverAction.SET_FULL_QUALITY:

                    getQuality(intent);

                    break;

                case ReceiverAction.N_GET_EXPANSION_FLOAT:

//                    autoChangeAlpha(intent);

                    break;


                case ReceiverAction.UI_GET_FULL_INFO:

                    sendAllInfo(intent,context);

                    break;

                case ReceiverAction.SEND_FULL_SCROLL:

                    setScroll(intent);

                    break;

                case ReceiverAction.SEND_CLEAN_ACTION:

                    resetAll();

                    break;

                case ReceiverAction.SEND_CUSTOM_HEIGHT://自定义高度

                    setCustomHeight(intent);

                    break;

                case ReceiverAction.SEND_ORI://接收屏幕旋转信息

                    autoSetPosition(intent);

                    break;

                case ReceiverAction.SEND_SLIT_INFO://接收是否使用卷轴模式

                    getSlitInfo(intent);

                    break;

            }
        }
    }

    private void resetAll() {

        String path = "/data/user_de/0/" + AndroidAppHelper.currentApplication().getPackageName() + "/shared_prefs/" + Conf.SHARE+".xml";

        File file = new File(path);

        if (file.exists()) {
            file.delete();

            Toast.makeText(AndroidAppHelper.currentApplication(), "重置设置(reset success)", Toast.LENGTH_SHORT).show();
        }


        if (getHeaderFile(Conf.HORIZONTAL).exists()){

            getHeaderFile(Conf.HORIZONTAL).delete();

        }

        if (getHeaderFile(Conf.VERTICAL).exists()){
            getHeaderFile(Conf.VERTICAL).delete();
        }

        if (getNFullFile(Conf.HORIZONTAL).exists()){
            getNFullFile(Conf.HORIZONTAL).delete();
        }

        if (getNFullFile(Conf.VERTICAL).exists()){
            getNFullFile(Conf.VERTICAL).delete();
        }





//        Toast.makeText(AndroidAppHelper.currentApplication(), "重置成功(reset success)", Toast.LENGTH_SHORT).show();

    }



    /**
     * 设置竖屏图片
     *
     * @param intent
     */
    private void setFullVerticalImage(Intent intent, Context context) {

        String s = intent.getStringExtra(Conf.N_FULL_VERTICAL_FILE);

        if (s == null || s.isEmpty()) {
            return;
        }

        Uri uri = Uri.parse(s);

        if (uri == null) {
            return;
        }

        try {
            Bitmap bitmap = BitmapFactory.decodeStream(context.getContentResolver().openInputStream(uri));

            if (bitmap != null) {

                int result = saveBitmap(bitmap, Conf.VERTICAL);

                if (result > 0) {

                    if (isVertical) {

                        autoSetBg();

                        Toast.makeText(context, "设置成功", Toast.LENGTH_SHORT).show();

                    }
                } else {

                    Toast.makeText(context, "设置失败，保存失败", Toast.LENGTH_SHORT).show();
                }

            }


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }


    private void setFullHorizontalImage(Context context, Intent intent) {

        String s = intent.getStringExtra(Conf.N_FULL_HORIZONTAL_FILE);

        if (s == null || s.isEmpty()) {
            return;
        }

        Uri uri = Uri.parse(s);

        if (uri == null) {
            return;
        }

        try {
            Bitmap bitmap = BitmapFactory.decodeStream(context.getContentResolver().openInputStream(uri));

            if (bitmap != null) {

                int result = saveBitmap(bitmap, Conf.HORIZONTAL);

                if (result > 0) {

                    if (!isVertical) {

                        autoSetBg();

                    }

                    Toast.makeText(context, "设置成功", Toast.LENGTH_SHORT).show();
                } else {

                    Toast.makeText(context, "设置失败，保存失败", Toast.LENGTH_SHORT).show();
                }

            }


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


    }


    /**
     * 设置滚动背景
     */
    private void setScrollBg() {

        cleanSlitImage();

        File file = null;

        BitmapFactory.Options options = new BitmapFactory.Options();

        switch (this.quality) {

            case Conf.HEIGHT_QUALITY:

                options.inPreferredConfig = Bitmap.Config.ARGB_8888;

                break;


            case Conf.LOW_QUALITY:

                options.inPreferredConfig = Bitmap.Config.RGB_565;

                break;


        }


        if (isVertical) {//竖屏

            if (bgView == null) {//如果为null，则重新实例化
                bgView = new ImageView(AndroidAppHelper.currentApplication());
            }

            file = getNFullFile(Conf.VERTICAL);

            if (!file.exists()) {//文件不存在

                bgView.setVisibility(View.GONE);//实例化后的惨案
                full.setBackground(getDefaultDrawable());

                return;

            }


            if (bgView.getVisibility() == View.GONE) {//如果不可见，则设置为可见
                bgView.setVisibility(View.VISIBLE);
            }

            if (hengBgView != null) {//隐藏横屏背景

                if (hengBgView.getVisibility() == View.VISIBLE) {

                    hengBgView.setVisibility(View.GONE);
                }

            }


            ((ViewGroup) full).removeView(bgView);

            ((ViewGroup) full).addView(bgView, 0);//放在第一位


            if (isGao) {
                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                bitmap = getBitmap(AndroidAppHelper.currentApplication(), bitmap, gaoValue);
                bgView.setImageBitmap(bitmap);
                return;
            }else {
                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);

                bgView.setImageBitmap(bitmap);

                bgView.setImageAlpha(alphaValue);

            }


        } else {//横屏

            if (hengBgView == null) {

                hengBgView = new ImageView(AndroidAppHelper.currentApplication());
            }

            file = getNFullFile(Conf.HORIZONTAL);

            if (!file.exists()) {//文件不存在

                hengBgView.setVisibility(View.GONE);
                full.setBackground(getDefaultDrawable());

                return;

            }

            if (hengBgView.getVisibility() == View.GONE) {

                hengBgView.setVisibility(View.VISIBLE);
            }


            if (bgView != null) {

                if (bgView.getVisibility() == View.VISIBLE) {
                    bgView.setVisibility(View.GONE);
                }

            }


            ((ViewGroup) full).removeView(hengBgView);
            ((ViewGroup) full).addView(hengBgView, 0);//添加到第一位



            if (isGao) {
                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                bitmap = getBitmap(AndroidAppHelper.currentApplication(), bitmap, gaoValue);

                hengBgView.setImageBitmap(bitmap);
                hengBgView.setImageAlpha(alphaValue);

            }else {

                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);

                hengBgView.setImageBitmap(bitmap);

                hengBgView.setImageAlpha(alphaValue);
            }

        }

        autoSetHeaderBg();

    }



    /**
     * 自动判断并设置背景
     */
    private void setBg() {

//        if (bgView!=null&&bgView.getVisibility()==View.VISIBLE){
//            bgView.setVisibility(View.GONE);
//        }
//
//        if (hengBgView!=null&&hengBgView.getVisibility()==View.VISIBLE){
//            hengBgView.setVisibility(View.GONE);
//        }



        if (full == null) {
            return;
        }

        File file = null;

        if (isVertical) {

            file = getNFullFile(Conf.VERTICAL);

        } else {

            file = getNFullFile(Conf.HORIZONTAL);

        }

        if (!file.exists()) {

            full.setBackground(getDefaultDrawable());

            return;
        }

        cleanSlitImage();

        cleanScrollImage();


        BitmapFactory.Options options = new BitmapFactory.Options();

        switch (this.quality) {

            case Conf.HEIGHT_QUALITY:

                options.inPreferredConfig = Bitmap.Config.ARGB_8888;

                break;


            case Conf.LOW_QUALITY:

                options.inPreferredConfig = Bitmap.Config.RGB_565;

                break;
        }


        if (isGao){
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());

            full.setBackground(bitmap2Drawable(bitmap));

            full.getBackground().setAlpha(alphaValue);

        }else {

            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            full.setBackground(bitmap2Drawable(bitmap));

            full.getBackground().setAlpha(alphaValue);
        }



        //判断头部是否存在，如果不存在，则给头部设置上背景

        autoSetHeaderBg();

    }

    /**
     * 判断头部是否存在，如果不存在，则给头部设置上背景
     */
    private void autoSetHeaderBg(){

        if (header==null){
            return;
        }

//        if (header.getBackground()!=null){
//
//            return;
//        }

        if (isVertical){

            if (!getHeaderFile(Conf.VERTICAL).exists()){



                header.setBackground(getDefaultDrawable());

            }

        }else {

            if (!getHeaderFile(Conf.VERTICAL).exists()){



                header.setBackground(getDefaultDrawable());
            }

        }


    }



    /**
     * 获取默认背景
     *
     * @return
     */
    private Drawable getDefaultDrawable() {

        if (drawable == 0) {
            return null;
        }

        return AndroidAppHelper.currentApplication().getDrawable(drawable);

    }


    /**
     * 保存背景图
     *
     * @param bitmap
     * @param type
     * @return
     */
    private int saveBitmap(Bitmap bitmap, int type) {

        String path = AndroidAppHelper.currentApplication().getFilesDir().getAbsolutePath();

        int result = -1;


        File file = null;


        switch (type) {
            case Conf.VERTICAL:

                file = new File(path + "/" + Conf.N_FULL_VERTICAL_FILE);

                break;

            case Conf.HORIZONTAL:

                file = new File(path + "/" + Conf.N_FULL_HORIZONTAL_FILE);

                break;
        }

        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.WEBP, 100, outputStream);

            result = 1;
        } catch (FileNotFoundException e) {

            result = -1;
            e.printStackTrace();
        }

        return result;
    }


    /**
     * 获取图片文件
     *
     * @param type 类型
     * @return 返回文件
     */
    private File getNFullFile(int type) {

        String path = AndroidAppHelper.currentApplication().getFilesDir().getAbsolutePath();

        File file = null;

        switch (type) {
            case Conf.VERTICAL://竖屏图

                file = new File(path + "/" + Conf.N_FULL_VERTICAL_FILE);

                break;

            case Conf.HORIZONTAL://横屏图

                file = new File(path + "/" + Conf.N_FULL_HORIZONTAL_FILE);

                break;
        }

        return file;
    }


    private boolean deleteFile(int type) {

        File file = getNFullFile(type);

        return file.delete();

    }



    /**
     * 保存宽度信息
     */
    private void saveFullWidthInfo() {

        int horizontal_width = sharedPreferences.getInt(Conf.FULL_HENG_WIDTH, -1);

        int vertical_width = sharedPreferences.getInt(Conf.FULL_SHU_WIDTH, -1);

        if ((horizontal_width <= 0 && !isVertical)) {


            horizontal_width = full.getWidth();

            sharedPreferences.edit().putInt(Conf.FULL_HENG_WIDTH, horizontal_width).apply();

        }

        if ((vertical_width <= 0 && isVertical)) {

            vertical_width = full.getWidth();

            sharedPreferences.edit().putInt(Conf.FULL_SHU_WIDTH, vertical_width).apply();

        }


    }

    /**
     * 设置透明度
     *
     * @param intent
     */
    private void getAlpha(Intent intent) {


        int value = intent.getIntExtra(Conf.FULL_ALPHA_VALUE, -1);

        if (value==-1){
            return;
        }

        value = (int) (2.55 * value);

        if (value < 0) {
            value = 0;
        }

        if (value > 255) {
            value = 255;
        }

        alphaValue = value;

        if (isScroll) {

            if (bgView != null && bgView.getVisibility() == View.VISIBLE) {
                bgView.setImageAlpha(alphaValue);
            }

            if (hengBgView != null && hengBgView.getVisibility() == View.VISIBLE) {
                hengBgView.setImageAlpha(alphaValue);
            }

            return;
        }


        if (isSlit){

            if (shuSlit!=null){

                shuSlit.setAlpha(alphaValue);

            }

            if (hengSlit!=null){

                hengSlit.setAlpha(alphaValue);

            }


        }


        if (full == null || full.getBackground() == null) {
            return;
        }

        full.getBackground().setAlpha(alphaValue);


        sharedPreferences.edit().putInt(Conf.FULL_ALPHA_VALUE, alphaValue).apply();


    }

    /**
     * 设置是否高斯模糊
     *
     * @param intent
     */
    private void getGaoSi(Intent intent) {

        int b = intent.getIntExtra(Conf.FULL_GAO, -100);

        if (b == -100) {
            return;
        } else if (b > 0) {

            this.isGao = true;
        } else if (b < 0) {

            this.isGao = false;
        }

        sharedPreferences.edit().putBoolean(Conf.FULL_GAO, isGao).apply();

        autoSetBg();


    }

    /**
     * 接收高斯模糊半径
     *
     * @param intent
     */
    private void getGaoValue(Intent intent) {

        this.gaoValue = intent.getIntExtra(Conf.FULL_GAO_VALUE, 25);

        sharedPreferences.edit().putInt(Conf.FULL_GAO_VALUE, gaoValue).apply();

        if (isGao) {
            autoSetBg();
        }

//        if (isGao) {
//            setGaoImage();
//        }


    }

    private void getQuality(Intent intent) {

        int type = intent.getIntExtra(Conf.IMAGE_QUALITY, Conf.LOW_QUALITY);

        switch (type) {

            case Conf.HEIGHT_QUALITY:

                this.quality = Conf.HEIGHT_QUALITY;

                break;


            case Conf.LOW_QUALITY:

                this.quality = Conf.LOW_QUALITY;

                break;


        }

//        if (!isGao) {
//            setBg();//重新设置画质
//        }

        autoSetBg();


        sharedPreferences.edit().putInt(Conf.FULL_QUALITY, quality).apply();

    }

    private void deleteBg(Intent intent, Context context) {


        int type = intent.getIntExtra(Conf.FULL_DELETE_TYPE, -1);

        if (type == -1) {
            return;
        }

        if (deleteFile(type)) {

            Toast.makeText(context, "清除成功", Toast.LENGTH_SHORT).show();

            autoSetBg();

        }

    }


    /**
     * 发送信息到UI
     *
     * @param intent
     * @param context
     */
    private void sendInfo(Intent intent, Context context) {


        int type = intent.getIntExtra(Conf.FULL_INFO, -1);

        if (type == -1) {
            return;
        }

        Full full=new Full();
        Intent intent1=new Intent(ReceiverAction.SEND_FULL_INFO);

        switch (type) {

            case Conf.VERTICAL:

                int w=this.full.getWidth();

                int h=sharedPreferences.getInt(Conf.FULL_SHU_HEIGHT,-1);

                full.setWidth(w);
                full.setHeight(h);

                break;


            case Conf.HORIZONTAL:

                int w1=sharedPreferences.getInt(Conf.FULL_HENG_WIDTH,-1);

                int h1=sharedPreferences.getInt(Conf.FULL_HENG_HEIGHT,-1);

                full.setWidth(w1);
                full.setHeight(h1);

                break;

        }

        intent1.putExtra(Conf.FULL_RESULT,full);

        intent1.putExtra(Conf.FULL_INFO,type);

        context.sendBroadcast(intent1);



    }

    private void autoChangeAlpha(float f){

//        float f = intent.getFloatExtra(Conf.N_EXPAND_VALUE, -0.1f);

        scrollBgView(f,full.getHeight());

        if (shuSlit != null) {

            shuSlit.change(full.getHeight());
        }

        if (hengSlit!=null){
            hengSlit.change(full.getHeight());
        }

        if (f == 1) {//完全下拉状态，保存高度

            if (isVertical){

                int height=full.getHeight();

                sharedPreferences.edit().putInt(Conf.FULL_SHU_HEIGHT,height).apply();

            }else {

                int height=full.getHeight();

                sharedPreferences.edit().putInt(Conf.FULL_HENG_HEIGHT,height).apply();
            }

        }




        if (f < 0 || f > 1) {
            return;
        }





        if (full==null||full.getBackground()==null){
            return;
        }

        float alpha = f * alphaValue;

        if (alpha > alphaValue) {
            alpha = alphaValue;
        }

        if (alpha < 0) {
            alpha = 0;
        }

        if (alpha>255){
            alpha=255;
        }

        if (isVertical&&getNFullFile(Conf.VERTICAL).exists()){

            full.getBackground().setAlpha((int) alpha);

            if (f == 1) {//完全下拉
                full.getBackground().setAlpha(alphaValue);
            } else if (f == 0) {//完全收缩

                full.getBackground().setAlpha(0);
            }

        }

        if (!isVertical&&getNFullFile(Conf.HORIZONTAL).exists()){

            full.getBackground().setAlpha((int) alpha);

            if (f == 1) {//完全下拉
                full.getBackground().setAlpha(alphaValue);
            } else if (f == 0) {//完全收缩

                full.getBackground().setAlpha(0);
            }

        }


    }


    private File getHeaderFile(int type){

        String path = AndroidAppHelper.currentApplication().getFilesDir().getAbsolutePath();

        File file = null;

        switch (type) {
            case Conf.VERTICAL://竖屏图

                file = new File(path + "/" + Conf.N_HEADER_VERTICAL_FILE);

                break;

            case Conf.HORIZONTAL://横屏图

                file = new File(path + "/" + Conf.N_HEADER_HORIZONTAL_FILE);

                break;
        }

        return file;

    }


    private void sendAllInfo(Intent intent,Context context){

        int sdk=intent.getIntExtra(Conf.SDK,-1);

        if (sdk<=0){
            return;
        }

        //取7.0或7.1
        if (sdk==Build.VERSION_CODES.N||sdk==Build.VERSION_CODES.N_MR1){

            int alpha=sharedPreferences.getInt(Conf.FULL_ALPHA_VALUE,255);

            int quality=sharedPreferences.getInt(Conf.FULL_QUALITY,Conf.LOW_QUALITY);

            boolean gao=sharedPreferences.getBoolean(Conf.FULL_GAO,false);

            int gaoValue=sharedPreferences.getInt(Conf.FULL_GAO_VALUE,25);

            boolean slit=sharedPreferences.getBoolean(Conf.SLIT,true);

            boolean scroll = sharedPreferences.getBoolean(Conf.FULL_SCROLL, true);

            Result result=new Result();

            result.setAlpha(alpha);
            result.setGao(gao);
            result.setQuality(quality);
            result.setGaoValue(gaoValue);
            result.setScroll(scroll);
            result.setSlit(slit);

            Intent intent1=new Intent(ReceiverAction.FULL_TO_UI_INFO);

            intent1.putExtra(Conf.FULL_TO_UI_RESULT,result);

            context.sendBroadcast(intent1);

        }


    }

    private int record = -1;//记录者，避免重复设置浪费资源

    /**
     * 滚动背景
     *
     * @param height 传递此时设置面板的高度，根据高度设置滑动位置
     */
    private void scrollBgView(float f, int height) {

        int imageHeight = -1;

        if (isVertical) {

            imageHeight = sharedPreferences.getInt(Conf.FULL_SHU_HEIGHT, -1);

        } else {

            imageHeight = sharedPreferences.getInt(Conf.FULL_HENG_HEIGHT, -1);


        }


        if (record == height) {

            if (f == 0) {//未展开,不显示
//                bgView.scrollTo(0, imageHeight);

                if (isVertical && bgView != null&&bgView.getVisibility()==View.VISIBLE) {
                    bgView.scrollTo(0, imageHeight);
                } else if (!isVertical && hengBgView != null&&hengBgView.getVisibility()==View.VISIBLE) {
                    hengBgView.scrollTo(0,  imageHeight);
                }


            }

            return;
        }

        record = height;//记录

        if (record < 0) {
            return;
        }

        if (imageHeight < 0) {
            return;
        }

        if (isVertical && bgView != null&&bgView.getVisibility()==View.VISIBLE) {
            bgView.scrollTo(0, -(height - imageHeight));
        } else if (!isVertical && hengBgView != null&&hengBgView.getVisibility()==View.VISIBLE) {
            hengBgView.scrollTo(0, -(height - imageHeight));
        }

    }


    private void setScroll(Intent intent) {

        this.isScroll=intent.getBooleanExtra(Conf.FULL_SCROLL, true);
        //保存变量
        sharedPreferences.edit().putBoolean(Conf.FULL_SCROLL, isScroll).apply();

        autoSetBg();//重新设置背景
    }


    private void setCustomHeight(Intent intent){


        int type=intent.getIntExtra("type",-1);

        if (type==-1){
            return;
        }

        String h=intent.getStringExtra("height");

        int height=Integer.parseInt(h);

        switch (type){
            case 10://竖屏

                sharedPreferences.edit().putInt(Conf.FULL_SHU_HEIGHT, height).apply();

                break;

            case 20://横屏

                sharedPreferences.edit().putInt(Conf.FULL_HENG_HEIGHT, height).apply();

                break;
        }

        Toast.makeText(AndroidAppHelper.currentApplication(), "自定义高度成功", Toast.LENGTH_SHORT).show();

    }


    /**
     * 自动根据当前屏幕设置背景，代替之前的监听
     *
     * @param intent 信息
     */
    private void autoSetPosition(Intent intent) {

        int p = intent.getIntExtra("ori", -1);

        if (p == -1) {
            return;
        }

        switch (p) {

            case Configuration.ORIENTATION_LANDSCAPE://横屏

                if (!isVertical){//避免重复
                    return;
                }

                isVertical=false;

                autoSetBg();

                break;

            case Configuration.ORIENTATION_PORTRAIT://竖屏
            default:

                if (isVertical){//避免重复
                    return;
                }

                isVertical=true;

                autoSetBg();

                break;

        }

        saveFullWidthInfo();


    }


    /**
     * 接收卷轴信息
     * @param intent 信息
     */
    private void getSlitInfo(Intent intent){

        this.isSlit=intent.getBooleanExtra(Conf.SLIT,true);

        sharedPreferences.edit().putBoolean(Conf.SLIT,isSlit).apply();//保存

        autoSetBg();//重新设置背景

    }


    private void setSlitImage() {



        File file = null;

        BitmapFactory.Options options = new BitmapFactory.Options();

        if (!isGao) {//如果非高斯模糊，则读取配置

            switch (this.quality) {

                case Conf.HEIGHT_QUALITY:
                    options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                    break;


                case Conf.LOW_QUALITY:

                    options.inPreferredConfig = Bitmap.Config.RGB_565;

                    break;


            }
        }


        if (isVertical) {//竖屏

            if (hengSlit!=null&&hengSlit.getVisibility()==View.VISIBLE){//隐藏横向图
                hengSlit.setVisibility(View.GONE);
            }

            file = getNFullFile(Conf.VERTICAL);

            if (!file.exists()){//文件不存在

                cleanScrollImage();
                cleanSlitImage();
                cleanBg();

                return;
            }



            if (shuSlit == null) {
//                shuSlit = new SlitImageView(AndroidAppHelper.currentApplication());

                initShuSlitView();

                if (shuSlit==null){
                    return;
                }

            }else if (shuSlit.getVisibility()==View.GONE){//存在但是被隐藏
                shuSlit.setVisibility(View.VISIBLE);
            }

            cleanScrollImage();
            cleanBg();

            ((ViewGroup) full).removeView(shuSlit);
            ((ViewGroup) full).addView(shuSlit, 0);


            if (isGao) {//是否高斯模糊

                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                bitmap = getBitmap(AndroidAppHelper.currentApplication(), bitmap, gaoValue);

                shuSlit.setBitmap(bitmap);
                shuSlit.setAlpha(alphaValue);

            } else {

                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);

                shuSlit.setBitmap(bitmap);
                shuSlit.setAlpha(alphaValue);
            }

            shuSlit.setBackgroundColor(Color.TRANSPARENT);//设置透明背景

        } else {//横屏

            if (shuSlit!=null&&shuSlit.getVisibility()==View.VISIBLE){
                shuSlit.setVisibility(View.GONE);
            }

            file=getNFullFile(Conf.HORIZONTAL);

            if (!file.exists()){//文件不存在

                cleanSlitImage();
                cleanScrollImage();
                cleanBg();

                return;
            }



            if (hengSlit==null){

                initHengSlitView();

                if (hengSlit==null){
                    return;
                }

            }else if (hengSlit.getVisibility()==View.GONE){
                hengSlit.setVisibility(View.VISIBLE);
            }

            cleanScrollImage();
            cleanBg();

            ((ViewGroup) full).removeView(hengSlit);
            ((ViewGroup) full).addView(hengSlit, 0);


            if (isGao) {

                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());

                bitmap = getBitmap(AndroidAppHelper.currentApplication(), bitmap, gaoValue);
                hengSlit.setBitmap(bitmap);
                hengSlit.setAlpha(alphaValue);

            } else {

                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
                hengSlit.setBitmap(bitmap);
                hengSlit.setAlpha(alphaValue);

            }

            hengSlit.setBackgroundColor(Color.TRANSPARENT);

        }

        setSlitHeader();//设置卷轴头部
    }



    //清除滚动背景
    private void cleanScrollImage(){

        if (bgView!=null){
            bgView.setVisibility(View.GONE);
            ((ViewGroup) full).removeView(bgView);
            bgView=null;
        }

        if (hengBgView!=null){
            hengBgView.setVisibility(View.GONE);
            ((ViewGroup) full).removeView(hengBgView);
            hengBgView=null;

        }

    }

    //清除卷轴背景
    private void cleanSlitImage(){

        if (shuSlit!=null){

            shuSlit.setVisibility(View.GONE);
            ((ViewGroup) full).removeView(shuSlit);
            shuSlit=null;
        }

        if (hengSlit!=null){

            hengSlit.setVisibility(View.GONE);
            ((ViewGroup) full).removeView(hengSlit);
            hengSlit=null;

        }

    }


    /**
     * 在卷轴模式下，可选对头部进行显示
     */
    private void setSlitHeader(){

        if (header==null){
            return;
        }

        if (isVertical){//

            if (!getHeaderFile(Conf.VERTICAL).exists()){

                header.setBackground(null);
                header.setBackgroundColor(Color.TRANSPARENT);

            }

        }else {

            if (!getHeaderFile(Conf.HORIZONTAL).exists()){
                header.setBackground(null);
                header.setBackgroundColor(Color.TRANSPARENT);
            }
        }

    }


    private void cleanBg(){

        if (full==null){
            return;
        }

        full.setBackground(null);
        full.setBackground(getDefaultDrawable());

    }


    /**
     * 初始化
     */
    private void initShuSlitView(){

        if (shuSlit==null){

            int height=sharedPreferences.getInt(Conf.FULL_SHU_HEIGHT,-1);

            int width=sharedPreferences.getInt(Conf.FULL_SHU_WIDTH,-1);

            if (height<=0||width<=0){

                return;
            }


            shuSlit=new SlitImageView(AndroidAppHelper.currentApplication());

            ViewGroup.LayoutParams layoutParams=new FrameLayout.LayoutParams(width,height);

//            layoutParams.height=height;
//            layoutParams.width=width;

            shuSlit.setLayoutParams(layoutParams);

        }

    }


    private void initHengSlitView(){

        if (hengSlit==null){

            int height=sharedPreferences.getInt(Conf.FULL_HENG_HEIGHT,-1);

            int width=sharedPreferences.getInt(Conf.FULL_HENG_WIDTH,-1);

            if (height<=0||width<=0){
                return;
            }



            hengSlit=new SlitImageView(AndroidAppHelper.currentApplication());

            ViewGroup.LayoutParams layoutParams=new FrameLayout.LayoutParams(width,height);

//            layoutParams.height=height;
//            layoutParams.width=width;

            hengSlit.setLayoutParams(layoutParams);

        }

    }

}
