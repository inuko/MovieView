package net.kokohadoko.instil.android.view;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Movie;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.RawRes;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;

import java.io.InputStream;

/**
 * {@link android.graphics.Movie} を再生可能な {@link android.view.View}
 *
 * @author inuko
 * @since 1.0.0
 */
public class MovieView extends View {

    /** ログ出力用文字列 */
    @SuppressWarnings("unused")
    private static final String LOG_TAG = MovieView.class.getSimpleName();

    /** フレーム間隔 初期値 */
    private static final int FRAME_DURATION_DEFAULT = 67;
    /** ループ再生 初期値 */
    private static final boolean LOOP_PLAY_DEFAULT = false;
    /** Scale 初期値 */
    private static final float SCALE_DEFAULT = -1;

    /** イベントリスナー */
    private OnMovieEventListener mOnMovieEventListener;
    /** 映像 */
    private Movie mMovie;
    /** 映像の開始秒数 */
    private long mMovieStart;
    /** 描画更新スレッド */
    private Thread mThread;
    /** DisplayMetrics */
    private DisplayMetrics mDisplayMetrics;
    /** Handler */
    private final Handler mHandler = new Handler();
    /** フレーム間隔 */
    private int mFrameDuration;
    /** ループ再生 */
    private boolean mLoopPlay;
    /** ScaleType */
    private ImageView.ScaleType mScaleType;
    /** Scale */
    private float mScale;

    /**
     * コンストラクタ
     *
     * @since 1.0.0
     * @param context Android Application Context.
     */
    public MovieView(final Context context) {
        super(context);

        initialize(context, null);
    }

    /**
     * コンストラクタ
     *
     * @since 1.0.0
     * @param context Android Application Context.
     * @param is
     */
    public MovieView(final Context context, final InputStream is) {
        super(context);

        initialize(context, Movie.decodeStream(is));
    }

    /**
     * コンストラクタ
     *
     * @since 1.0.0
     * @param context Android Application Context.
     * @param data
     * @param offset
     * @param length
     */
    public MovieView(final Context context, final byte[] data, final int offset, final int length) {
        super(context);

        initialize(context, Movie.decodeByteArray(data, offset, length));
    }

    /**
     * コンストラクタ
     *
     * @since 1.0.0
     * @param context Android Application Context.
     * @param attrs
     */
    public MovieView(final Context context, final AttributeSet attrs) {
        super(context, attrs);

        Movie movie = null;
        ImageView.ScaleType scaleType = ImageView.ScaleType.CENTER_CROP;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MovieView);

        if (a.hasValue(R.styleable.MovieView_scaleType)) {
            int scaleTypeValue = a.getInt(R.styleable.MovieView_scaleType, 0);
            scaleType = ImageView.ScaleType.valueOf(String.valueOf(scaleTypeValue));
        }

        if (a.hasValue(R.styleable.MovieView_rawResourceId)) {
            int resourceId = a.getResourceId(R.styleable.MovieView_rawResourceId, -1);
            InputStream is = context.getResources().openRawResource(resourceId);
            movie = Movie.decodeStream(is);
        }

        initialize(context, movie, scaleType);
    }

    /**
     * 初期化処理
     *
     * @since 1.0.0
     * @param context Android Application Context.
     * @param movie {@link android.graphics.Movie}
     */
    private void initialize(final Context context, final Movie movie) {
        initialize(context, movie, ImageView.ScaleType.CENTER_CROP);
    }

    /**
     * 初期化処理
     *
     * @since 1.0.0
     * @param context Android Application Context.
     * @param movie {@link android.graphics.Movie}
     * @param scaleType {@link android.widget.ImageView.ScaleType}
     */
    private void initialize(final Context context, final Movie movie, final ImageView.ScaleType scaleType) {
        mMovie = movie;
        mDisplayMetrics = context.getResources().getDisplayMetrics();
        mThread = null;
        mFrameDuration = FRAME_DURATION_DEFAULT;
        mLoopPlay = LOOP_PLAY_DEFAULT;
        mOnMovieEventListener = null;
        mScaleType = scaleType;
        mScale = SCALE_DEFAULT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.TRANSPARENT);

        if (mMovie != null) {
            if (isPlaying()) {
                canvas.drawColor(Color.TRANSPARENT);

                long now = SystemClock.uptimeMillis();

                if (mMovieStart == 0) {
                    mMovieStart = now;
                }

                final int relTime;
                if (mLoopPlay) {
                    relTime = (int) ((now - mMovieStart) % mMovie.duration());
                } else {
                    relTime = (int)(now - mMovieStart);

                    if (mMovie.duration() < relTime) {
                        stop();
                    }
                }

                mMovie.setTime(relTime);
            }

            int saveCount = canvas.save(Canvas.MATRIX_SAVE_FLAG);

            float[] movieParams = getScaleType(canvas);
            mMovie.draw(canvas, movieParams[0], movieParams[1]);

            canvas.restoreToCount(saveCount);
        } else {
            super.onDraw(canvas);
        }
    }

    /**
     *
     *
     * @since 1.0.0
     * @param widthMeasureSpec
     * @param heightMeasureSpec
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mScale = getScale();

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    /**
     * {@link android.graphics.Movie} を設定する
     *
     * @since 1.0.0
     * @param data
     * @param offset
     * @param length
     */
    public void setMovie(final byte[] data, final int offset, final int length) {
        setMovie(Movie.decodeByteArray(data, offset, length));
    }

    /**
     * {@link android.graphics.Movie} を設定する
     *
     * @since 1.0.0
     * @param pathName
     */
    public void setMovie(final String pathName) {
        setMovie(Movie.decodeFile(pathName));
    }

    /**
     * {@link android.graphics.Movie} を設定する
     *
     * @since 1.0.0
     * @param resId
     */
    public void setMovie(@RawRes final int resId) {
        InputStream is = getContext().getResources().openRawResource(resId);
        setMovie(Movie.decodeStream(is));
    }

    /**
     * {@link android.graphics.Movie} を設定する
     *
     * @since 1.0.0
     * @param resId
     * @param value
     */
    public void setMovie(@RawRes final int resId, final TypedValue value) {
        InputStream is = getContext().getResources().openRawResource(resId, value);
        setMovie(Movie.decodeStream(is));
    }

    /**
     * {@link android.graphics.Movie} を設定する
     *
     * @since 1.0.0
     * @param movie {@link android.graphics.Movie}
     */
    public void setMovie(final Movie movie) {
        if (isPlaying()) {
            throw new IllegalStateException("");
        }

        mMovie = movie;
    }

    /**
     * 再生可能かどうか
     *
     * @since 1.0.0
     * @return
     */
    public boolean canPlay() {
        return mMovie != null;
    }

    /**
     * 再生中かどうか
     *
     * @since 1.0.0
     * @return
     */
    public boolean isPlaying() {
        return mThread != null && mThread.isAlive();
    }

    /**
     * 再生する
     *
     * @since 1.0.0
     */
    public void play() {
        if (mThread == null || !mThread.isAlive()) {
            if (!canPlay()) {

            }

            mThread = new Thread() {
                @Override
                public void run() {
                    while (mThread != null && !mThread.isInterrupted()) {
                        mHandler.post(new Runnable() {
                            public void run() {
                                invalidate();
                            }
                        });

                        // The thread sleeps until the next frame
                        try {
                            Thread.sleep(mFrameDuration);
                        }
                        catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            };

            mThread.start();

            if (mOnMovieEventListener != null) {
                mOnMovieEventListener.onPlayed();
            }
        }
    }

    /**
     * 停止する
     *
     * @since 1.0.0
     */
    public void stop() {
        stop(0);
    }

    /**
     * 停止する
     *
     * @since 1.0.0
     * @param time
     */
    private void stop(final long time) {
        if (mThread != null && mThread.isAlive() && canPlay()) {
            mThread.interrupt();
            mMovieStart = time;

            if (mOnMovieEventListener != null) {
                mOnMovieEventListener.onStoped();
            }
        }
    }

    /**
     * 一時停止する
     *
     * @since 1.0.0
     */
    public void pause() {
        stop(mMovieStart);
    }

    /**
     * ScaleType を計算して取得する
     *
     * @since 1.0.0
     * @param canvas {@link android.graphics.Canvas}
     * @return
     */
    private float[] getScaleType(final Canvas canvas) {
        float viewWidth = getWidth();
        float viewHeight = getHeight();
        float movieWidth = mMovie.width() * mScale;
        float movieHeight = mMovie.height() * mScale;

        float x = 0;
        float y = 0;
        float s = 1;

        switch (mScaleType) {
            case CENTER:
                x = (viewWidth - movieWidth) / 2 / mScale;
                y = (viewHeight - movieHeight) / 2 / mScale;
                break;

            case CENTER_CROP:
                float min = Math.min(movieWidth, movieHeight);
                if (min == movieWidth) {
                    s = viewWidth / movieWidth;
                } else {
                    s = viewHeight / movieHeight;
                }
                x = (viewWidth - movieWidth * s) / 2 / (s * mScale);
                y = (viewHeight - movieHeight * s) / 2 / (s * mScale);
                canvas.scale(s, s);
                break;

            case CENTER_INSIDE:
                if (movieWidth > viewWidth || movieHeight > viewHeight) {
                    float max = Math.max(movieWidth, movieHeight);
                    if (max == movieWidth) {
                        s = viewWidth / movieWidth;
                    } else {
                        s = viewHeight / movieHeight;
                    }
                }
                x = (viewWidth - movieWidth * s) / 2 / (s * mScale);
                y = (viewHeight - movieHeight * s) / 2 / (s * mScale);
                canvas.scale(s, s);
                break;

            case FIT_CENTER:
                float max = Math.max(movieWidth, movieHeight);
                if (max == movieWidth) {
                    s = viewWidth / movieWidth;
                } else {
                    s = viewHeight / movieHeight;
                }
                x = (viewWidth - movieWidth * s) / 2 / (s * mScale);
                y = (viewHeight - movieHeight * s) / 2 / (s * mScale);
                canvas.scale(s, s);
                break;

            case FIT_START:
                float start = Math.max(movieWidth, movieHeight);
                if (start == movieWidth) {
                    s = viewWidth / movieWidth;
                } else {
                    s = viewHeight / movieHeight;
                }
                x = 0;
                y = 0;
                canvas.scale(s, s);
                break;

            case FIT_END:
                float end = Math.max(movieWidth, movieHeight);
                if (end == movieWidth) {
                    s = viewWidth / movieWidth;
                } else {
                    s = viewHeight / movieHeight;
                }
                x = (viewWidth - movieWidth * s) / mScale / s;
                y = (viewHeight - movieHeight * s) / mScale / s;
                canvas.scale(s, s);
                break;

            case FIT_XY:
                float fitX = viewWidth / movieWidth;
                s = viewHeight / movieHeight;
                x = 0;
                y = 0;
                canvas.scale(fitX, s);
                break;

            default:
                break;
        }

        return new float[] {x, y, s};
    }

    /**
     * Scale を取得する
     *
     * @since 1.0.0
     * @return
     */
    public float getScale() {
        mScale = mDisplayMetrics.densityDpi / getDensity();

        if (mScale < 0.1f) mScale = 0.1f;
        if (mScale > 5.0f) mScale = 5.0f;
        return mScale;
    }

    /**
     * Density を取得する
     *
     * @since 1.0.0
     * @return
     */
    public int getDensity() {
        if (getContext() instanceof Activity) {
            DisplayMetrics metrics = new DisplayMetrics();
            Activity activity = (Activity) getContext();
            activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
            return metrics.densityDpi;
        } else {
            return DisplayMetrics.DENSITY_HIGH;
        }
    }

    /**
     * ループ再生を取得する
     *
     * @since 1.0.0
     * @return
     */
    public boolean isLoopPlay() {
        return mLoopPlay;
    }

    /**
     * ループ再生を設定する
     *
     * @since 1.0.0
     */
    public void setLoopPlay(final boolean loopPlay) {
        mLoopPlay = loopPlay;
    }

    /**
     * フレーム間隔を取得する
     *
     * @since 1.0.0
     * @return
     */
    public int getFrameDuration() {
        return mFrameDuration;
    }

    /**
     *
     *
     * @since 1.0.0
     * @param frameDuration
     */
    public void setFrameDuration(final int frameDuration) {
        if (frameDuration < 1) {
            throw new IllegalArgumentException("");
        }

        mFrameDuration = frameDuration;
    }

    /**
     * {@link android.widget.ImageView.ScaleType} を取得する
     *
     * @since 1.0.0
     * @return
     */
    public ImageView.ScaleType getScaleType() {
        return mScaleType;
    }

    /**
     * {@link android.widget.ImageView.ScaleType} を設定する
     *
     * @since 1.0.0
     * @param scaleType {@link android.widget.ImageView.ScaleType}
     */
    public void setScaleType(final ImageView.ScaleType scaleType) {
        mScaleType = scaleType;
    }

    /**
     * {@link net.kokohadoco.instil.android.view.MovieView.OnMovieEventListener} を設定する
     *
     * @since 1.0.0
     * @param listener {@link net.kokohadoco.instil.android.view.MovieView.OnMovieEventListener}
     */
    public void setOnMoviewListener(final OnMovieEventListener listener) {
        mOnMovieEventListener = listener;
    }

    /**
     * {@link android.graphics.Movie} に関するイベントリスナー
     *
     * @author inuko
     * @since 1.0.0
     */
    public interface OnMovieEventListener {

        /**
         * 再生した場合に呼ばれる
         *
         * @since 1.0.0
         */
        void onPlayed();

        /**
         * 停止した場合に呼ばれる
         *
         * @since 1.0.0
         */
        void onStoped();
    }
}
