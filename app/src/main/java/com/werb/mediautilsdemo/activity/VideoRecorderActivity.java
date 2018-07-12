package com.werb.mediautilsdemo.activity;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.werb.mediautilsdemo.MediaUtils;
import com.werb.mediautilsdemo.R;
import com.werb.mediautilsdemo.widget.CustomVideoView;
import com.werb.mediautilsdemo.widget.SendView;
import com.werb.mediautilsdemo.widget.VideoProgressBar;
import java.util.UUID;

/**
 *
 * @author wanbo
 * @date 2017/1/18
 */

public class VideoRecorderActivity extends AppCompatActivity {

	private SurfaceView      surfaceView;
	private VideoProgressBar progressBar;
	private TextView         btnInfo, btn;
	private TextView       view;
	private SendView       send;
	private RelativeLayout recordLayout, switchLayout,rl_bottom_preview;
	private CustomVideoView vv_play;
	private ImageView       iv_photo;
	private ImageView       iv_preview_close;

	private MediaUtils mediaUtils;
	private boolean    isCancel;
	private int        mProgress;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_video);
		initViews();
		initData();
	}

	private void initViews() {
		surfaceView = (SurfaceView) findViewById(R.id.main_surface_view);
		vv_play = (CustomVideoView) findViewById(R.id.vv_play);
		iv_photo = (ImageView) findViewById(R.id.iv_photo);
		rl_bottom_preview = (RelativeLayout) findViewById(R.id.rl_bottom_preview);
		iv_preview_close = (ImageView) findViewById(R.id.iv_preview_close);
		send = (SendView) findViewById(R.id.view_send);
		btnInfo = (TextView) findViewById(R.id.tv_info);
		btn = (TextView) findViewById(R.id.main_press_control);
		vv_play = (CustomVideoView) findViewById(R.id.vv_play);
		recordLayout = (RelativeLayout) findViewById(R.id.record_layout);
		switchLayout = (RelativeLayout) findViewById(R.id.rl_switch);
		progressBar = (VideoProgressBar) findViewById(R.id.main_progress_bar);

		iv_preview_close.setOnClickListener(closePreviewClick);
		send.backLayout.setOnClickListener(backClick);
		send.selectLayout.setOnClickListener(selectClick);
		btn.setOnTouchListener(btnTouch);
		progressBar.setOnProgressEndListener(listener);
		progressBar.setCancel(true);
		findViewById(R.id.btn_close).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		switchLayout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mediaUtils.switchCamera();
			}
		});
	}

	private void initData(){
		vv_play.setVisibility(View.GONE);
		vv_play.pause();
		iv_photo.setVisibility(View.GONE);

		mediaUtils = new MediaUtils(this);
		mediaUtils.setRecorderType(MediaUtils.MEDIA_VIDEO);
		mediaUtils.setTargetDir(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES));
		mediaUtils.setTargetName(UUID.randomUUID() + ".mp4");
		mediaUtils.setSurfaceView(surfaceView);
	}

	@Override
	protected void onResume() {
		super.onResume();
		progressBar.setCancel(true);
	}

	View.OnTouchListener btnTouch = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			boolean ret = false;
			float downY = 0;
			int action = event.getAction();

			switch (v.getId()) {
				case R.id.main_press_control: {
					switch (action) {
						case MotionEvent.ACTION_DOWN:
							mediaUtils.record();
							startView();
							ret = true;
							break;
						case MotionEvent.ACTION_UP:
							if (!isCancel) {
								if (mProgress == 0) {
									stopView(false);
									break;
								}
								if (mProgress < 10) {
									//时间太短不保存
									mediaUtils.stopRecordUnSave();
									Toast.makeText(VideoRecorderActivity.this, "时间太短", Toast.LENGTH_SHORT).show();
									stopView(false);
									break;
								}
								//停止录制
								mediaUtils.stopRecordSave();
								stopView(true);
							} else {
								//现在是取消状态,不保存
								mediaUtils.stopRecordUnSave();
								Toast.makeText(VideoRecorderActivity.this, "取消保存", Toast.LENGTH_SHORT).show();
								stopView(false);
							}
							ret = false;
							break;
						case MotionEvent.ACTION_MOVE:
							float currentY = event.getY();
							isCancel = downY - currentY > 10;
							moveView();
							break;
							default:
					}
				}
				default:
			}
			return ret;
		}
	};

	VideoProgressBar.OnProgressEndListener listener = new VideoProgressBar.OnProgressEndListener() {
		@Override
		public void onProgressEndListener() {
			progressBar.setCancel(true);
			mediaUtils.stopRecordSave();
		}
	};

	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case 0:
					progressBar.setProgress(mProgress);
					if (mediaUtils.isRecording()) {
						mProgress = mProgress + 1;
						sendMessageDelayed(handler.obtainMessage(0), 100);
					}
					break;
				default:
					break;
			}
		}
	};

	private void startView() {
		startAnim();
		mProgress = 0;
		handler.removeMessages(0);
		handler.sendMessage(handler.obtainMessage(0));
	}

	private void moveView() {
		if (isCancel) {
			btnInfo.setText("松手取消");
		} else {
			btnInfo.setText("上滑取消");
		}
	}

	private void stopView(boolean isSave) {
		stopAnim();
		progressBar.setCancel(true);
		mProgress = 0;
		handler.removeMessages(0);
		btnInfo.setText("双击放大");
		if (isSave) {
			recordLayout.setVisibility(View.GONE);
			rl_bottom_preview.setVisibility(View.GONE);
			send.startAnim();
		}
	}

	private void startAnim() {
		AnimatorSet set = new AnimatorSet();
		set.playTogether(ObjectAnimator.ofFloat(btn, "scaleX", 1, 0.5f), ObjectAnimator.ofFloat(btn, "scaleY", 1, 0.5f),
				ObjectAnimator.ofFloat(progressBar, "scaleX", 1, 1.3f), ObjectAnimator.ofFloat(progressBar, "scaleY", 1, 1.3f));
		set.setDuration(250).start();
	}

	private void stopAnim() {
		AnimatorSet set = new AnimatorSet();
		set.playTogether(ObjectAnimator.ofFloat(btn, "scaleX", 0.5f, 1f), ObjectAnimator.ofFloat(btn, "scaleY", 0.5f, 1f),
				ObjectAnimator.ofFloat(progressBar, "scaleX", 1.3f, 1f),
				ObjectAnimator.ofFloat(progressBar, "scaleY", 1.3f, 1f));
		set.setDuration(250).start();
	}

	private View.OnClickListener backClick = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			send.stopAnim();
			btnInfo.setVisibility(View.VISIBLE);
			recordLayout.setVisibility(View.VISIBLE);
			mediaUtils.deleteTargetFile();
		}
	};

	private View.OnClickListener selectClick = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			String path = mediaUtils.getTargetFilePath();
			Toast.makeText(VideoRecorderActivity.this, "文件以保存至：" + path, Toast.LENGTH_SHORT).show();
			send.stopAnim();
			recordLayout.setVisibility(View.GONE);
			btnInfo.setVisibility(View.GONE);
			vv_play.setVisibility(View.VISIBLE);
			rl_bottom_preview.setVisibility(View.VISIBLE);
			vv_play.setVideoPath(mediaUtils.getTargetFilePath());
			vv_play.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
				@Override
				public void onPrepared(MediaPlayer mp) {
					mp.setLooping(true);
					vv_play.start();
				}
			});
			if(vv_play.isPrepared()){
				vv_play.setLooping(true);
				vv_play.start();
			}
		}
	};

	private View.OnClickListener closePreviewClick = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			btnInfo.setVisibility(View.VISIBLE);
			recordLayout.setVisibility(View.VISIBLE);
			vv_play.setVisibility(View.GONE);
			vv_play.pause();
		}
	};
}
