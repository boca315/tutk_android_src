package com.camera.camerawithtutk;

import android.annotation.SuppressLint;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.camera.api.AVAPIsClient;
import com.camera.model.SaveFrames;
import com.decode.MediaCodecDecoder;
import com.decode.tools.BufferInfo;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.TimeUnit;

public class CameraActivity extends AppCompatActivity {
    private int audioFlag;
    public BlockingDeque<BufferInfo> bq;
    private TextView definition;
    SurfaceView surfaceViewDecode; // 视频播放绑定的surface
    MediaCodecDecoder mediaCodecDecoder; //解码器

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        ImageView back = findViewById(R.id.camera_back);
        back.setOnClickListener(view -> {
            onBackPressed();

        });
        ImageView mic = findViewById(R.id.camera_mic);
        ImageView saveVideo = findViewById(R.id.camera_setvideo);
        definition = findViewById(R.id.camera_definition);
        audioFlag =0;
        definition.setOnClickListener(this::initPopWindow);
        mic.setOnClickListener(view -> {
            if (audioFlag==0){
                AVAPIsClient.startAudioThread();
                AVAPIsClient.controlAudioThread(audioFlag);
                audioFlag = 1;
            } else {
                AVAPIsClient.controlAudioThread(audioFlag);
                audioFlag = 0;
            }
        });
        saveVideo.setOnClickListener(view -> {
            if (VideoThread.startReceive) {
                VideoThread.startReceive = false;
                System.out.println("Stop Receive");
            } else {
                VideoThread.startReceive = true;
                System.out.println("Start Receive");
            }
        });
        //--------------------------------------- 解码 ------------------------------------

        bq = MainActivity.bq;
        bq.clear();


        // 绑定surfaceview
        surfaceViewDecode=(SurfaceView) findViewById(R.id.camera_video);
        //实例化解码器
        mediaCodecDecoder=new MediaCodecDecoder();
        // 初始化
        try{
            mediaCodecDecoder.init();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // surfaceView绘制完成后 配置解码器并启动
        surfaceViewDecode.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                // 配置解码器
                mediaCodecDecoder.configure(surfaceViewDecode.getHolder().getSurface());
                // 启动解码器
                mediaCodecDecoder.start();
            }
            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
            }
            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                mediaCodecDecoder.release();
            }
        });

        // 此线程从阻塞队列poll buffer信息并送入解码器
        (new Thread() {
            public void run() {
                BufferInfo temp;
                while (true) {
                    try {
                        temp = bq.poll(3000, TimeUnit.MILLISECONDS);
//                        for(int i=0;i<1024;i++){
//                            System.out.print(temp.buffer[i]+",");
//                        }
//                        System.out.println();
                        if (temp == null) {
                            continue;
                        }
                        // 向解码器输入buffer
                        mediaCodecDecoder.input(temp.buffer, temp.len, System.nanoTime() / 1000);
                        mediaCodecDecoder.output();
                    } catch (Exception e) {
//                        e.printStackTrace();
                    }
                }

            }
        }).start();


        //---------------------------------------------------------------------------------


    }

    @SuppressLint("ClickableViewAccessibility")
    public void initPopWindow(View view) {
        View view1 = LayoutInflater.from(this).inflate(R.layout.definition_window, null);
        TextView superclear = view1.findViewById(R.id.superclear);
        TextView highclear = view1.findViewById(R.id.highclear);
        TextView standard = view1.findViewById(R.id.standardclear);
        TextView fluency = view1.findViewById(R.id.fluency);
        //1.构造一个PopupWindow，参数依次是加载的View，宽高
        final PopupWindow popWindow = new PopupWindow(view1,
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popWindow.setAnimationStyle(R.anim.window_pop);  //设置加载动画
        //这些为了点击非PopupWindow区域，PopupWindow会消失的，如果没有下面的
        //代码的话，你会发现，当你把PopupWindow显示出来了，无论你按多少次后退键
        //PopupWindow并不会关闭，而且退不出程序，加上下述代码可以解决这个问题
        popWindow.setTouchable(true);
        popWindow.setTouchInterceptor((v, event) -> {
            return false;
            // 这里如果返回true的话，touch事件将被拦截
            // 拦截后 PopupWindow的onTouchEvent不被调用，这样点击外部区域无法dismiss
        });
        popWindow.setBackgroundDrawable(new ColorDrawable(0x00000000));    //要为popWindow设置一个背景才有效
        //设置popupWindow显示的位置，参数依次是参照View，x轴的偏移量，y轴的偏移量
        popWindow.showAsDropDown(view, -10, -420);
        //设置点击事件
        superclear.setOnClickListener(view2 -> {
            AVAPIsClient.setQuality(3);
            popWindow.dismiss();
            definition.setText("超清");
//            bq.clear();
            mediaCodecDecoder.setQuanlity(3);
        });
        highclear.setOnClickListener(view2 -> {
            AVAPIsClient.setQuality(2);
            popWindow.dismiss();
            definition.setText("高清");
//            bq.clear();
            mediaCodecDecoder.setQuanlity(2);

        });
        standard.setOnClickListener(view2 ->{
            AVAPIsClient.setQuality(1);
            popWindow.dismiss();
            definition.setText("标清");
//            bq.clear();
            mediaCodecDecoder.setQuanlity(1);

        });
        fluency.setOnClickListener(view2 ->{
            AVAPIsClient.setQuality(0);
            popWindow.dismiss();
            definition.setText("流畅");
//            bq.clear();
            mediaCodecDecoder.setQuanlity(0);
        });
    }
}
