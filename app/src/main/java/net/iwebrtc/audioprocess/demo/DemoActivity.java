package net.iwebrtc.audioprocess.demo;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar;

import net.iwebrtc.audioprocess.sdk.AudioProcess;

public class DemoActivity extends Activity implements OnClickListener {
    private static final String TAG = "DemoActivity";
    SeekBar skbVolume;//调节音量
    boolean isProcessing = true;//是否处理
    boolean isRecording = false;//是否录放的标记

//    static final int frequency = 44100;
    static final int frequency = 16000;
    //	static final int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    static final int channelConfigurationIn = AudioFormat.CHANNEL_IN_MONO;//录制
    static final int channelConfigurationOut = AudioFormat.CHANNEL_OUT_MONO;//播放
    static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    int recBufSize, playBufSize;
    AudioRecord audioRecord;
    AudioTrack audioTrack;

    AudioProcess mAudioProcess;//处理声音


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);
        recBufSize = AudioRecord.getMinBufferSize(frequency, channelConfigurationIn, audioEncoding);


        Log.e("recBufSize录制", "recBufSize录制 最小大小 为:" + recBufSize);//TODO   1280     8k---640
        playBufSize = AudioTrack.getMinBufferSize(frequency, channelConfigurationOut, audioEncoding);

        Log.e("playBufSize录制", "playBufSize录制 最小大小 为:" + playBufSize);//TODO  3200     playBufSize录制 最小大小 为:1600
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, frequency, channelConfigurationIn, audioEncoding, recBufSize);

        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, frequency, channelConfigurationOut, audioEncoding, playBufSize, AudioTrack.MODE_STREAM);

        findViewById(R.id.btnRecord).setOnClickListener(this);
        findViewById(R.id.btnStop).setOnClickListener(this);

        skbVolume = (SeekBar) this.findViewById(R.id.skbVolume);
        skbVolume.setMax(100);//音量调节的极限
        skbVolume.setProgress(50);//设置seekbar的位置值
        audioTrack.setStereoVolume(1f, 1f);//设置当前音量大小
        skbVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                float vol = (float) (seekBar.getProgress()) / (float) (seekBar.getMax());
                Log.e(TAG, "当前音量大小vol" + vol);
                audioTrack.setStereoVolume(vol, vol);//设置音量
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }
        });
        ((CheckBox) findViewById(R.id.cb_ap)).setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton view, boolean checked) {
                isProcessing = checked;
                Log.e(TAG, "选中了吗？" + checked);
                Log.e(TAG, "是否处理？" + isProcessing);

            }
        });
        mAudioProcess = new AudioProcess();
        mAudioProcess.init(frequency, 2, 1);//初始化采样率、位数、通道数
    }

    @Override
    protected void onDestroy() {
        mAudioProcess.destroy();
        android.os.Process.killProcess(android.os.Process.myPid());
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnRecord) {//边录制边播放
            isRecording = true;  //录制=true
            new RecordPlayThread().start();
        } else if (v.getId() == R.id.btnStop) {
            isRecording = false;
        }
    }

    class RecordPlayThread extends Thread {
        public void run() {
            try {
                int bufferSize = mAudioProcess.calculateBufferSize(frequency, 2, 1);//TODO bufferSize大小 为:320
                // sample_rate * channels * number_bytes_per_sample / 100;  我也可以参照它来配置哦！！
                Log.e("hehe", "bufferSize大小 为:" + bufferSize);      // TODO if 8000 采样率  bufferSize大小 为:160
                byte[] buffer = new byte[bufferSize];  //用于存取录制好的数据

                audioRecord.startRecording();//开始录制
                audioTrack.play();//开始播放


                while (isRecording) {
                    //setp 1 从MIC保存数据到缓冲区
                    int bufferReadResult = audioRecord.read(buffer, 0, bufferSize);// TODO bufferReadResult大小 为:320
                    Log.e(TAG, "bufferReadResult大小 为:" + bufferReadResult);
                    byte[] tmpBuf_src = new byte[bufferReadResult];//2048  TODO 这里是数据哦！！
                    System.arraycopy(buffer, 0, tmpBuf_src, 0, bufferReadResult);//

                    //setp 2 进行处理
                    byte[] tmpBuf_processed = new byte[bufferReadResult];//TODO  bufferReadResult大小 为:320    8k-160
                    if (isProcessing) {//处理
                        Log.e(TAG, "开始处理啦！！！！！！！！！！！！");
                        mAudioProcess.processStream10msData(tmpBuf_src, tmpBuf_src.length, tmpBuf_processed);//TODO
                    } else {
                        tmpBuf_processed = tmpBuf_src;
                        Log.e(TAG, "没有处理哦！！！！！！！！！！！！");
                    }
                    //写入数据即播放
                    audioTrack.write(tmpBuf_processed, 0, tmpBuf_processed.length);
                    mAudioProcess.AnalyzeReverseStream10msData(tmpBuf_processed, tmpBuf_processed.length);
                }
                audioTrack.stop();
                audioRecord.stop();
            } catch (Throwable t) {
                Log.e(TAG, "发生了异常----------", t);
            }
        }
    }

    private class TaG {
    }

    ;
}