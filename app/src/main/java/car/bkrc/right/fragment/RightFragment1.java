package car.bkrc.right.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import com.bkrc.camera.XcApplication;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import car.bkrc.com.car2018.FirstActivity;
import car.bkrc.com.car2018.LoginActivity;
import car.bkrc.com.car2018.R;
import car2017_demo.RGBLuminanceSource;

import static car.bkrc.right.fragment.LeftFragment.bitmap;


/**
 * Created by peter on 5/9/2019.
 */
public class RightFragment1 extends Fragment {

    // 接受传感器
    private long psStatus = 0;// 状态
    private long UltraSonic = 0;// 超声波
    private long Light = 0;// 光照
    private long CodedDisk = 0;// 码盘值
    private int angle_data =0;  //角度值
    String Camera_show_ip = null;

    private TextView Data_show =null;
    private EditText speededit = null;
    private EditText coded_discedit =null;
    private EditText angle_dataedit =null;
    private ImageButton up_bt,blew_bt,stop_bt,left_bt,right_bt;

    public static byte[] mByte = new byte[60];
    public static final String TAG = "RightFragment1";
    private View view =null;


    public static RightFragment1 getInstance(){
        return RightFragment1Holder.sInstance;
    }

    private static class RightFragment1Holder
    {
        private static final RightFragment1 sInstance = new RightFragment1();
    }

    // 接受显示小车发送的数据
    @SuppressLint("HandlerLeak")
    private Handler rehHandler = new Handler() {
        @SuppressLint("SetTextI18n")
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                mByte = (byte[]) msg.obj;
                if (mByte[0] == 0x55) {
                    // 任务B0
                    if (mByte[2] == (byte) 0xA2) {
                        //识别二维码且语音播报
                        try {
                            Xqrcode();
                            FirstActivity.Connect_Transport.TYPE = (short) 0xAA;
                            FirstActivity.Connect_Transport.MAJOR = (short) 0xA2;
                            FirstActivity.Connect_Transport.FIRST = 1;
                            FirstActivity.Connect_Transport.SECOND = 0;
                            FirstActivity.Connect_Transport.THRID = 0;
                            FirstActivity.Connect_Transport.send();
                            FirstActivity.Connect_Transport.yanchi(500);
                            FirstActivity.Connect_Transport.send();
                            FirstActivity.Connect_Transport.yanchi(500);
                            FirstActivity.Connect_Transport.send();
                        } catch (Exception arg) {
                            arg.printStackTrace();
                        }

                    } else if (mByte[2] == (byte) 0xA1)//预设位
                    {
                        Xcamera(mByte[3]);
                    } else if (mByte[2] == (byte) 0xA3)       //交通灯
                    {
                        try {
                            trafficShiBie();
                        } catch (Exception arg) {
                            arg.printStackTrace();
                        }
                    } else if (mByte[2] == (byte) 0xA4)      // 车牌识别
                    {

                    }
                    // 光敏状态
                    psStatus = mByte[3] & 0xff;
                    // 超声波数据
                    UltraSonic = mByte[5] & 0xff;
                    UltraSonic = UltraSonic << 8;
                    UltraSonic += mByte[4] & 0xff;
                    // 光照强度
                    Light = mByte[7] & 0xff;
                    Light = Light << 8;
                    Light += mByte[6] & 0xff;
                    // 码盘
                    CodedDisk = mByte[9] & 0xff;
                    CodedDisk = CodedDisk << 8;
                    CodedDisk += mByte[8] & 0xff;

                    Camera_show_ip = FirstActivity.IPCamera;
                    if (mByte[1] == (byte) 0xaa) {  //主车
                        if(FirstActivity.chief_status_flag == true)
                        {
                            // 显示数据
                            Data_show.setText("主车各状态信息: " + "超声波:" + UltraSonic
                                    + "mm 光照:" + Light + "lx" + "码盘:" + CodedDisk
                                    + " 光敏状态:" + psStatus + " 状态:" + (mByte[2]));
                        }
                    }
                    if(mByte[1] == (byte) 0x02) //从车
                    {
                        if(FirstActivity.chief_status_flag == false)
                        {


                            if(mByte[2] == -110)
                            {
                                byte [] newData = new byte[50];
                                Log.e("data",""+mByte[4]);
                                newData =  Arrays.copyOfRange(mByte, 5, mByte[4]+5);
                                Log.e("data",""+"长度"+newData.length);
                                try {
                                    String str= new String(newData,"ascii");//第二个参数指定编码方式
                                    Toast.makeText(getActivity(),""+str,Toast.LENGTH_LONG).show();
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }

                            } else {
                                // 显示数据
                                Data_show.setText("WIFI模块IP:" + FirstActivity.IPCar + "\n" + "从车各状态信息: " + "超声波:" + UltraSonic
                                        + "mm 光照:" + Light + "lx" + " 码盘:" + CodedDisk
                                        + " 光敏状态:" + psStatus + " 状态:" + (mByte[2]));
                            }
                        }
                    }
                }
            }
        }
    };

    private void trafficShiBie() {
        byte resultTrafficByte = 1;
        int a = 0, b = 0, c = 0;
        String check = "黄色";
        for (int i = 0; i < 1; i++) {   //识别次数，取相似值
            try {
                check = shibie();
            } catch (Exception e) {
                e.getMessage();
            }
            if (check.equals("红色"))
                a++;
            else if (check.equals("绿色"))
                b++;
            else if (check.equals("黄色"))
                c++;
        }
        if (a > b && a > c) {
            resultTrafficByte = 0x01;
        } else if (b > a && b > c) {
            resultTrafficByte = 0x02;
        } else if (c > a && c > b) {
            resultTrafficByte = 0x03;
        }
        FirstActivity.Connect_Transport.MAJOR = 0xA3;
        FirstActivity.Connect_Transport.FIRST = resultTrafficByte;
        FirstActivity.Connect_Transport.SECOND = 0;
        FirstActivity.Connect_Transport.THRID = 0;
        FirstActivity.Connect_Transport.send();
        FirstActivity.Connect_Transport.yanchi(500);
        FirstActivity.Connect_Transport.send();
        FirstActivity.Connect_Transport.send();
    }

    public String shibie() {
        int w_mode_num = 8;
        int h_mode_num = 8;
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        int pixel = 0;
        ArrayList<Integer> red_arry = new ArrayList<Integer>();
        ArrayList<Integer> red_w = new ArrayList<Integer>();
        ArrayList<Integer> green_arry = new ArrayList<Integer>();
        ArrayList<Integer> green_w = new ArrayList<Integer>();
        ArrayList<Integer> yellow_arry = new ArrayList<Integer>();
        ArrayList<Integer> yellow_w = new ArrayList<Integer>();
        int rNum = 0;
        int gNum = 0;
        int yNum = 0;
        int h_r = 0;
        int h_g = 0;
        int h_y = 0;
        int width_size = w / w_mode_num - 1;
        int height_size = h / h_mode_num - 1;
        int start_w = 0;
        int start_h = 0;
        for (int i = 0; i < width_size; i++) {
            start_w = i * w_mode_num;
            h_r = 0;
            h_g = 0;
            h_y = 0;
            for (int j = 0; j < height_size; j++) {
                start_h = j * h_mode_num;
                int sign = 0;
                for (int n = 0; n < w_mode_num; n++) {
                    for (int m = 0; m < h_mode_num; m++) {
                        pixel = bitmap.getPixel(start_w + n, start_h + m);
                        if (isred(pixel)) {
                            rNum++;
                            h_r++;
                            sign = 1;
                            break;
                        } else if (isgreen(pixel)) {
                            gNum++;
                            h_g++;
                            sign = 1;
                            break;
                        } else if (isyellow(pixel)) {
                            yNum++;
                            h_y++;
                            sign = 1;
                            break;
                        }
                    }
                    if (sign == 1) {
                        break;
                    }
                }
            }
            if (h_r > 10) {
                red_arry.add(h_r);
                red_w.add(i);
            } else if (h_g > 5) {
                green_arry.add(h_g);
                green_w.add(i);
            } else if (h_y > 5) {
                yellow_arry.add(h_g);
                yellow_w.add(i);
            }
        }
        String color = null;
        Log.e("shibie", gNum + "--" + rNum + "--" + yNum);
        rNum /= 3;
        if (rNum > (gNum) && rNum > yNum) {
            color = "红色";
        } else if (gNum > (rNum) && gNum > yNum) {
            color = "绿色";
        } else if ((yNum) >= ((rNum)) && yNum >= gNum) {
            color = "黄色";
        }
        Log.e("shibie", color);
        if (color.isEmpty())
            return "绿色";
        else
            return color;
    }

    private boolean isred(int pixel) {
        int t_r = 0;
        int t_g = 0;
        int t_b = 0;
        t_r = (pixel & 0xff0000) >> 16;
        t_g = (pixel & 0xff00) >> 8;
        t_b = (pixel & 0xff);
        if (t_r - t_g > 70 && t_r - t_b > 70) {
            return true;
        }
        return false;
    }

    private boolean isgreen(int pixel) {
        int t_r = 0;
        int t_g = 0;
        int t_b = 0;
        t_r = (pixel & 0xff0000) >> 16;
        t_g = (pixel & 0xff00) >> 8;
        t_b = (pixel & 0xff);
        if (t_g - t_r > 50 && t_g - t_b > 50) {
            return true;
        }
        return false;
    }


    /**
     *
     *
     * @param pixel
     * @return
     */
    private boolean isyellow(int pixel) {
        int t_r = 0;
        int t_g = 0;
        int t_b = 0;
        t_r = (pixel & 0xff0000) >> 16;
        t_g = (pixel & 0xff00) >> 8;
        t_b = (pixel & 0xff);
        if (Math.abs(t_r - t_g) < 50 && t_r - t_b > 70
                && t_g - t_b > 70) {
            return true;
        }
        return false;
    }

    private String Xqrcode() {
        String tempstr = null;
        // 解析10次
        for (int i = 0; i < 10; i++) {
            try {
                Map<DecodeHintType, String> hints = new HashMap<DecodeHintType, String>();
                hints.put(DecodeHintType.CHARACTER_SET, "utf-8");

                RGBLuminanceSource source = new RGBLuminanceSource(LeftFragment.bitmap);
                BinaryBitmap bitmap1 = new BinaryBitmap(new HybridBinarizer(
                        source));
                QRCodeReader reader = new QRCodeReader();
                Result result = reader.decode(bitmap1, hints);
                if (result.toString() != null) {
                    Log.e(TAG, "识别二维码结果为:" + result.toString());
                    // 二维码解析成功
                    tempstr = result.toString();
                    break;
                }
            } catch (Exception e) {
                Log.e(TAG, "识别二维码识别失败" + e.getMessage());
            }
        }
        return tempstr;
    }

    public void XVoiceControl(String src) {
        if (src.equals("")) {
            src = "二维码识别错误";
        }
        try {
            byte[] sbyte = bytesend(src.getBytes("GBK"));
            FirstActivity.Connect_Transport.send_voice(sbyte);
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private byte[] bytesend(byte[] sbyte) {
        byte[] textbyte = new byte[sbyte.length + 5];
        textbyte[0] = (byte) 0xFD;
        textbyte[1] = (byte) (((sbyte.length + 2) >> 8) & 0xff);
        textbyte[2] = (byte) ((sbyte.length + 2) & 0xff);
        textbyte[3] = 0x01;// 合成语音命令
        textbyte[4] = (byte) 0x01;// 编码格式
        for (int i = 0; i < sbyte.length; i++) {
            textbyte[i + 5] = sbyte[i];
        }
        return textbyte;
    }


    private void Xcamera(final byte b) {
        XcApplication.executorServicetor.execute(new Runnable() {
            @Override
            public void run() {
                switch (b) {
                    case 1:
                        LeftFragment.cameraCommandUtil.postHttp(FirstActivity.IPCamera, 31, 0);
                        break;
                    case 2:
                        LeftFragment.cameraCommandUtil.postHttp(FirstActivity.IPCamera, 33, 0);
                        break;
                    case 3:
                        LeftFragment.cameraCommandUtil.postHttp(FirstActivity.IPCamera, 35, 0);
                        break;
                    case 4:
                        LeftFragment.cameraCommandUtil.postHttp(FirstActivity.IPCamera, 37, 0);
                        break;
                    case 5:
                        LeftFragment.cameraCommandUtil.postHttp(FirstActivity.IPCamera, 39, 0);
                        break;
                    case 6:
                        LeftFragment.cameraCommandUtil.postHttp(FirstActivity.IPCamera, 41, 0);
                        break;
                }
            }
        });
    }

    private byte[] rbyte = new byte[40];
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        if (view != null) {
            ViewGroup parent = (ViewGroup) view.getParent();
            if (parent != null) {
                parent.removeView(view);
            }
        }else {
            if (LoginActivity.isPad(getActivity()))
                view = inflater.inflate(R.layout.right_fragment1, container, false);
            else
                view = inflater.inflate(R.layout.right_fragment1_mobilephone, container, false);
      }
        FirstActivity.recvhandler =rehHandler;
        control_init();
        if(XcApplication.isserial == XcApplication.Mode.SOCKET) {
            connect_thread();                            //开启网络连接线程
        }
        else if(XcApplication.isserial == XcApplication.Mode.SERIAL){
            serial_thread();   //使用纯串口uart4
        }

        return view;
    }

    private void control_init()
    {
        Data_show = view.findViewById(R.id.rvdata);
        speededit =view.findViewById(R.id.speed_data);
        coded_discedit =view.findViewById(R.id.coded_disc_data);
        angle_dataedit =view.findViewById(R.id.angle_data);

        up_bt = view.findViewById(R.id.up_button);
        blew_bt = view.findViewById(R.id.below_button);
        stop_bt = view.findViewById(R.id.stop_button);
        left_bt = view.findViewById(R.id.left_button);
        right_bt = view.findViewById(R.id.right_button);

        up_bt.setOnClickListener(new onClickListener2());
        blew_bt.setOnClickListener(new onClickListener2());
        stop_bt.setOnClickListener(new onClickListener2());
        left_bt.setOnClickListener(new onClickListener2());
        right_bt.setOnClickListener(new onClickListener2());
        up_bt.setOnLongClickListener(new onLongClickListener2());
    }

    private void connect_thread()
    {
        XcApplication.executorServicetor.execute(new Runnable() {
            @Override
            public void run() {
                FirstActivity.Connect_Transport.connect(rehHandler,FirstActivity.IPCar);
            }
        });
    }

    private  void serial_thread(){
        XcApplication.executorServicetor.execute(new Runnable() {
            @Override
            public void run() {
                FirstActivity.Connect_Transport.serial_connect(rehHandler);
            }
        });
    }

    // 速度和码盘方法
    private int getSpeed() {
        String src = speededit.getText().toString();
        int speed = 90;
        if (!src.equals("")) {
            speed = Integer.parseInt(src);
        } else {
            Toast.makeText(getActivity(), "请输入转弯速度值", Toast.LENGTH_SHORT).show();
        }
        return speed;
    }

    private int getEncoder() {
        String src = coded_discedit.getText().toString();
        int encoder =20;
        if (!src.equals("")) {
            encoder = Integer.parseInt(src);
        } else {
            Toast.makeText(getActivity(), "请输入码盘值", Toast.LENGTH_SHORT).show();
        }
        return encoder;
    }

    private int getAngle() {
        String src = angle_dataedit.getText().toString();
        int angle = 50;
        if (!src.equals("")) {
            angle = Integer.parseInt(src);
        } else {
            Toast.makeText(getActivity(), "请输入循迹速度值", Toast.LENGTH_SHORT).show();
        }
        return angle;
    }
    // 速度与码盘值
    private int sp_n, en_n;

    private class onClickListener2 implements View.OnClickListener
    {
        @Override
        public void onClick(View v) {
            sp_n = getSpeed();

            switch(v.getId())
            {
                case R.id.up_button:
                    en_n = getEncoder();
                    FirstActivity.Connect_Transport.go(sp_n, en_n);
                    break;
                case R.id.left_button:
                    FirstActivity.Connect_Transport.left(sp_n);
                    break;
                case R.id.right_button:
                    FirstActivity.Connect_Transport.right(sp_n);
                    break;
                case R.id.below_button:
                    en_n = getEncoder();
                    FirstActivity.Connect_Transport.back(sp_n, en_n);
                    break;
                case R.id.stop_button:
                    FirstActivity.Connect_Transport.stop();
                    break;
            }

        }
    }
    private class onLongClickListener2 implements View.OnLongClickListener
    {
        @Override
        public boolean onLongClick(View view) {
            if(view.getId() ==R.id.up_button)
            {
                sp_n = getAngle();
                FirstActivity.Connect_Transport.line(sp_n);
            }
    /*如果将onLongClick返回false，那么执行完长按事件后，还有执行单击事件。
    如果返回true，只执行长按事件*/
            return true;
        }
    }


}


