package com.scwang.smartrefreshlayout;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.ScrollingView;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.ScrollView;

import com.scwang.smartrefreshlayout.api.DefaultRefreshFooterCreater;
import com.scwang.smartrefreshlayout.api.DefaultRefreshHeaderCreater;
import com.scwang.smartrefreshlayout.api.RefreshContent;
import com.scwang.smartrefreshlayout.api.RefreshFooter;
import com.scwang.smartrefreshlayout.api.RefreshHeader;
import com.scwang.smartrefreshlayout.api.RefreshLayout;
import com.scwang.smartrefreshlayout.api.SizeObserver;
import com.scwang.smartrefreshlayout.constant.RefreshState;
import com.scwang.smartrefreshlayout.constant.SpinnerStyle;
import com.scwang.smartrefreshlayout.footer.ballpulse.BallPulseFooter;
import com.scwang.smartrefreshlayout.header.bezier.BezierHeader;
import com.scwang.smartrefreshlayout.impl.RefreshBottomWrapper;
import com.scwang.smartrefreshlayout.impl.RefreshContentWrapper;
import com.scwang.smartrefreshlayout.impl.RefreshHeaderWrapper;
import com.scwang.smartrefreshlayout.listener.OnLoadmoreListener;
import com.scwang.smartrefreshlayout.listener.OnMultiPurposeListener;
import com.scwang.smartrefreshlayout.listener.OnRefreshListener;
import com.scwang.smartrefreshlayout.listener.OnRefreshLoadmoreListener;
import com.scwang.smartrefreshlayout.util.DensityUtil;

import static android.view.View.MeasureSpec.AT_MOST;
import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.getSize;
import static android.view.View.MeasureSpec.makeMeasureSpec;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static com.scwang.smartrefreshlayout.util.DensityUtil.dp2px;

/**
 * 智能刷新布局
 * Intelligent Refreshlayout
 * Created by SCWANG on 2017/5/26.
 */
@SuppressWarnings({"unused","WeakerAccess"})
public class SmartRefreshLayout extends ViewGroup implements NestedScrollingParent, NestedScrollingChild, RefreshLayout {

    //<editor-fold desc="属性变量 property and variable">

    protected RefreshState state = RefreshState.None;

    //<editor-fold desc="滑动属性">
    protected int mTouchSlop;
    protected int mSpinner;
    protected int mReboundDuration = 300;
    protected int mScreenHeightPixels;
    protected float mTouchX;
    protected float mTouchY;
    protected float mInitialMotionY;
    protected Interpolator mReboundInterpolator;
    //</editor-fold>

    //<editor-fold desc="功能属性">
    protected int[] mPrimaryColors;
    protected boolean mEnableRefresh = true;
    protected boolean mEnableLoadmore = true;
    protected boolean mDisableContentWhenRefresh = false;//是否开启在刷新时候禁止操作内容视图
    protected boolean mDisableContentWhenLoading = false;//是否开启在刷新时候禁止操作内容视图
    protected boolean mEnableHeaderTranslationContent = true;//是否启用内容视图拖动效果
    protected boolean mEnableFooterTranslationContent = true;//是否启用内容视图拖动效果
    //</editor-fold>

    //<editor-fold desc="监听属性">
    protected OnRefreshListener mRefreshListener;
    protected OnLoadmoreListener mLoadmoreListener;
    protected OnMultiPurposeListener mOnMultiPurposeListener;
    //</editor-fold>

    //<editor-fold desc="嵌套滚动">
    protected int[] mParentScrollConsumed = new int[2];
    protected int[] mParentOffsetInWindow = new int[2];
    protected float mTotalUnconsumed;
    protected boolean mNestedScrollInProgress;
    protected NestedScrollingChildHelper mNestedScrollingChildHelper;
    protected NestedScrollingParentHelper mNestedScrollingParentHelper;
    //</editor-fold>

    //<editor-fold desc="内部视图">
    /**
     * 下拉头部视图
     */
    protected RefreshHeader mRefreshHeader;
    /**
     * 显示内容视图
     */
    protected RefreshContent mRefreshContent;
    /**
     * 上拉底部视图
     */
    protected RefreshFooter mRefreshFooter;
    /**
     * 头部高度
     */
    protected int mHeaderHeight;
    /**
     * 底部高度
     */
    protected int mFooterHeight;
    /**
     * 扩展高度
     */
    protected int mExtendHeaderHeight;
    /**
     * 扩展高度
     */
    protected int mExtendFooterHeight;
    /**
     * 扩展比率
     */
    protected float mExtendRate = 2.0f;
    //</editor-fold>

    protected static DefaultRefreshFooterCreater mFooterCreater = (context, l) -> new BallPulseFooter(context);
    protected static DefaultRefreshHeaderCreater mHeaderCreater = (context, l) -> new BezierHeader(context);

    //</editor-fold>

    //<editor-fold desc="构造方法 construction methods">
    public SmartRefreshLayout(Context context) {
        super(context);
        this.init(context, null, 0);
    }

    public SmartRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.init(context, attrs, 0);
    }

    public SmartRefreshLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        setClipToPadding(false);

        mScreenHeightPixels = context.getResources().getDisplayMetrics().heightPixels;
        mReboundInterpolator = new DecelerateInterpolator();
        mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();

        mNestedScrollingParentHelper = new NestedScrollingParentHelper(this);
        mNestedScrollingChildHelper = new NestedScrollingChildHelper(this);
        ViewCompat.setNestedScrollingEnabled(this,true);

        DensityUtil density = new DensityUtil();
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.SmartRefreshLayout);

        mExtendRate = ta.getFloat(R.styleable.SmartRefreshLayout_srlExtendRate, mExtendRate);
        mEnableRefresh = ta.getBoolean(R.styleable.SmartRefreshLayout_srlEnableRefresh, mEnableRefresh);
        mReboundDuration = ta.getInt(R.styleable.SmartRefreshLayout_srlReboundDuration, mReboundDuration);
        mEnableLoadmore = ta.getBoolean(R.styleable.SmartRefreshLayout_srlEnableLoadmore, mEnableLoadmore);
        mHeaderHeight = ta.getDimensionPixelSize(R.styleable.SmartRefreshLayout_srlHeaderHeight, density.dip2px(100));
        mFooterHeight = ta.getDimensionPixelSize(R.styleable.SmartRefreshLayout_srlFooterHeight, density.dip2px(60));
        mExtendFooterHeight = ta.getDimensionPixelSize(R.styleable.SmartRefreshLayout_srlFooterExtendHeight, (int) (mFooterHeight * (mExtendRate - 1)));
        mExtendHeaderHeight = ta.getDimensionPixelSize(R.styleable.SmartRefreshLayout_srlHeaderExtendHeight, (int) (mHeaderHeight * (mExtendRate - 1)));
        mDisableContentWhenRefresh = ta.getBoolean(R.styleable.SmartRefreshLayout_srlDisableContentWhenRefresh, mDisableContentWhenRefresh);
        mDisableContentWhenLoading = ta.getBoolean(R.styleable.SmartRefreshLayout_srlDisableContentWhenLoading, mDisableContentWhenLoading);
        mEnableHeaderTranslationContent = ta.getBoolean(R.styleable.SmartRefreshLayout_srlEnableHeaderTranslationContent, mEnableHeaderTranslationContent);
        mEnableFooterTranslationContent = ta.getBoolean(R.styleable.SmartRefreshLayout_srlEnableFooterTranslationContent, mEnableFooterTranslationContent);

        int accentColor = ta.getColor(R.styleable.SmartRefreshLayout_srlAccentColor, 0);
        int primaryColor = ta.getColor(R.styleable.SmartRefreshLayout_srlPrimaryColor, 0);
        if (primaryColor != 0 ) {
            if (accentColor != 0) {
                mPrimaryColors = new int[]{primaryColor, accentColor};
            } else {
                mPrimaryColors = new int[]{primaryColor};
            }
        }

        ta.recycle();

    }
    //</editor-fold>

    //<editor-fold desc="生命周期 life cycle">

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        final int count = getChildCount();
        if (count > 3) {
            throw new RuntimeException("最多只支持3个子View，Most only support three sub view");
        }

        for (int i = 0; i < count; i++) {
            View view = getChildAt(i);
            if (mRefreshContent == null && ( view instanceof AbsListView
                    || view instanceof WebView
                    || view instanceof ScrollView
                    || view instanceof ScrollingView
                    || view instanceof NestedScrollingChild
                    || view instanceof NestedScrollingParent
                    || view instanceof ViewPager)) {
                mRefreshContent = new RefreshContentWrapper(view);
            } else if (view instanceof RefreshHeader && mRefreshHeader == null) {
                mRefreshHeader = ((RefreshHeader) view);
            } else if (view instanceof RefreshFooter && mRefreshFooter == null) {
                mRefreshFooter = ((RefreshFooter) view);
            } else if (count == 1 && mRefreshContent == null) {
                mRefreshContent = new RefreshContentWrapper(view);
            } else if (i == 0 && mRefreshHeader == null) {
                mRefreshHeader = new RefreshHeaderWrapper(view);
            } else if (count == 2 && mRefreshContent == null) {
                mRefreshContent = new RefreshContentWrapper(view);
            } else if (i == 2 && mRefreshFooter == null) {
                mRefreshFooter = new RefreshBottomWrapper(view);
            } else if (i == 1 && mRefreshContent == null) {
                mRefreshContent = new RefreshContentWrapper(view);
            }
        }

        if (isInEditMode()) {
            if (mPrimaryColors != null) {
                if (mRefreshHeader != null) {
                    mRefreshHeader.setPrimaryColors(mPrimaryColors);
                }
                if (mRefreshFooter != null) {
                    mRefreshFooter.setPrimaryColors(mPrimaryColors);
                }
            }

            //重新排序
            bringChildToFront(mRefreshContent.getView());
            if (mRefreshHeader.getSpinnerStyle() != SpinnerStyle.FixedBehind) {
                bringChildToFront(mRefreshHeader.getView());
            }
            if (mRefreshFooter.getSpinnerStyle() != SpinnerStyle.FixedBehind) {
                bringChildToFront(mRefreshFooter.getView());
            }

        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (isInEditMode()) return;
        if (mRefreshContent == null) {
            for (int i = 0, len = getChildCount(); i < len; i++) {
                View view = getChildAt(i);
                if ((mRefreshHeader == null || view != mRefreshHeader.getView())&&
                        (mRefreshFooter == null || view != mRefreshFooter.getView())) {
                    mRefreshContent = new RefreshContentWrapper(view);
                }
            }
            if (mRefreshContent == null) {
                mRefreshContent = new RefreshContentWrapper(getContext());
                mRefreshContent.getView().setLayoutParams(new LayoutParams(MATCH_PARENT, MATCH_PARENT));
            }
        }
        if (mRefreshHeader == null) {
            mRefreshHeader = mHeaderCreater.createRefreshHeader(getContext(), this);
            if (!(mRefreshHeader.getView().getLayoutParams() instanceof MarginLayoutParams)) {
                if (mRefreshHeader.getSpinnerStyle() == SpinnerStyle.Scale) {
                    addView(mRefreshHeader.getView(), MATCH_PARENT, MATCH_PARENT);
                } else {
                    addView(mRefreshHeader.getView(), MATCH_PARENT, WRAP_CONTENT);
                }
            }
        }
        if (mRefreshFooter == null) {
            mRefreshFooter = mFooterCreater.createRefreshFooter(getContext(), this);
            if (!(mRefreshFooter.getView().getLayoutParams() instanceof MarginLayoutParams)) {
                if (mRefreshFooter.getSpinnerStyle() == SpinnerStyle.Scale) {
                    addView(mRefreshFooter.getView(), MATCH_PARENT, MATCH_PARENT);
                } else {
                    addView(mRefreshFooter.getView(), MATCH_PARENT, WRAP_CONTENT);
                }
            }
        }

        //重新排序
        bringChildToFront(mRefreshContent.getView());
        if (mRefreshHeader.getSpinnerStyle() != SpinnerStyle.FixedBehind) {
            bringChildToFront(mRefreshHeader.getView());
        }
        if (mRefreshFooter.getSpinnerStyle() != SpinnerStyle.FixedBehind) {
            bringChildToFront(mRefreshFooter.getView());
        }


        if (mRefreshListener == null) {
            mRefreshListener = refresh -> refresh.finisRefresh(3000);
        }
        if (mLoadmoreListener == null) {
            mLoadmoreListener = refresh -> refresh.finisLoadmore(3000);
        }
        if (mPrimaryColors != null) {
            mRefreshHeader.setPrimaryColors(mPrimaryColors);
            mRefreshFooter.setPrimaryColors(mPrimaryColors);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int minimumHeight = 0;
        final boolean isInEditMode = isInEditMode();

        if (mRefreshHeader != null) {
            final View headerView = mRefreshHeader.getView();
            final LayoutParams lp = (LayoutParams) headerView.getLayoutParams();
            final int widthSpec = getChildMeasureSpec(widthMeasureSpec, lp.leftMargin + lp.rightMargin, lp.width);
            int heightSpec = heightMeasureSpec;
            if (lp.height > 0) {
                mHeaderHeight = lp.height + lp.topMargin + lp.bottomMargin;
                mExtendHeaderHeight = (int) (mHeaderHeight * (mExtendRate - 1));
                heightSpec = makeMeasureSpec(lp.height, EXACTLY);
                headerView.measure(widthSpec, heightSpec);
            } else /*if (mRefreshHeader instanceof SizeDefinition) {
                mHeaderHeight = ((SizeDefinition) mRefreshHeader).defineHeight();
                mExtendHeaderHeight = ((SizeDefinition) mRefreshHeader).defineExtendHeight();
                heightSpec = makeMeasureSpec(mHeaderHeight - lp.topMargin - lp.bottomMargin, EXACTLY);
                headerView.measure(widthSpec, heightSpec);
            } else */if (lp.height == WRAP_CONTENT) {
                heightSpec = makeMeasureSpec(getSize(heightMeasureSpec) - lp.topMargin - lp.bottomMargin, AT_MOST);
                headerView.measure(widthSpec, heightSpec);
                int measuredHeight = headerView.getMeasuredHeight();
                if (measuredHeight > 0) {
                    mHeaderHeight = headerView.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
                    mExtendHeaderHeight = (int) (mHeaderHeight * (mExtendRate - 1));
                } else {
                    heightSpec = makeMeasureSpec(mHeaderHeight - lp.topMargin - lp.bottomMargin, EXACTLY);
                    headerView.measure(widthSpec, heightSpec);
                }
            } else if (lp.height == MATCH_PARENT) {
                heightSpec = makeMeasureSpec(mHeaderHeight - lp.topMargin - lp.bottomMargin, EXACTLY);
                headerView.measure(widthSpec, heightSpec);
            } else {
                headerView.measure(widthSpec, heightSpec);
            }
            if (mRefreshHeader.getSpinnerStyle() == SpinnerStyle.Scale && !isInEditMode) {
                final int height = Math.max(0, mSpinner);
                heightSpec = makeMeasureSpec(height - lp.topMargin - lp.bottomMargin, EXACTLY);
                headerView.measure(widthSpec, heightSpec);
            }
            if (mRefreshHeader instanceof SizeObserver) {
                ((SizeObserver) mRefreshHeader).onSizeDefined(this, mHeaderHeight, mExtendHeaderHeight);
            }
            if (isInEditMode) {
                minimumHeight += headerView.getMeasuredHeight();
            }
        }

        if (mRefreshFooter != null) {
            final View footerView = mRefreshFooter.getView();
            final LayoutParams lp = (LayoutParams) footerView.getLayoutParams();
            final int widthSpec = getChildMeasureSpec(widthMeasureSpec, lp.leftMargin + lp.rightMargin, lp.width);
            int heightSpec = heightMeasureSpec;
            if (lp.height > 0) {
                mFooterHeight = lp.height + lp.topMargin + lp.bottomMargin;
                mExtendFooterHeight = (int) (mFooterHeight * (mExtendRate - 1));
                heightSpec = makeMeasureSpec(lp.height, EXACTLY);
                footerView.measure(widthSpec, heightSpec);
            } else /*if (mRefreshFooter instanceof SizeDefinition) {
                mFooterHeight = ((SizeDefinition) mRefreshFooter).defineHeight();
                mExtendFooterHeight = ((SizeDefinition) mRefreshFooter).defineExtendHeight();
                heightSpec = makeMeasureSpec(mFooterHeight - lp.topMargin - lp.bottomMargin, EXACTLY);
                footerView.measure(widthSpec, heightSpec);
            } else */if (lp.height == WRAP_CONTENT) {
                heightSpec = makeMeasureSpec(getSize(heightMeasureSpec) - lp.topMargin - lp.bottomMargin, AT_MOST);
                footerView.measure(widthSpec, heightSpec);
                int measuredHeight = footerView.getMeasuredHeight();
                if (measuredHeight > 0) {
                    mFooterHeight = footerView.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
                    mExtendFooterHeight = (int) (mFooterHeight * (mExtendRate - 1));
                } else {
                    heightSpec = makeMeasureSpec(mFooterHeight - lp.topMargin - lp.bottomMargin, EXACTLY);
                    footerView.measure(widthSpec, heightSpec);
                }
            } else if (lp.height == MATCH_PARENT) {
                heightSpec = makeMeasureSpec(mFooterHeight - lp.topMargin - lp.bottomMargin, EXACTLY);
                footerView.measure(widthSpec, heightSpec);
            } else {
                footerView.measure(widthSpec, heightSpec);
            }
            if (mRefreshFooter.getSpinnerStyle() == SpinnerStyle.Scale && !isInEditMode) {
                final int height = Math.max(0, mSpinner);
                heightSpec = makeMeasureSpec(height - lp.topMargin - lp.bottomMargin, EXACTLY);
                footerView.measure(widthSpec, heightSpec);
            }
            if (mRefreshFooter instanceof SizeObserver) {
                ((SizeObserver) mRefreshFooter).onSizeDefined(this, mFooterHeight, mExtendFooterHeight);
            }
            if (isInEditMode) {
                minimumHeight += footerView.getMeasuredHeight();
            }
        }

        if (mRefreshContent != null) {
            final LayoutParams lp = (LayoutParams) mRefreshContent.getLayoutParams();
            final int widthSpec = getChildMeasureSpec(widthMeasureSpec,
                    getPaddingLeft() + getPaddingRight() +
                            lp.leftMargin + lp.rightMargin, lp.width);
            final int heightSpec = getChildMeasureSpec(heightMeasureSpec,
                    getPaddingTop() + getPaddingBottom() +
                            lp.topMargin + lp.bottomMargin +
                            ((isInEditMode && mRefreshHeader != null && (mEnableHeaderTranslationContent||mRefreshHeader.getSpinnerStyle() == SpinnerStyle.FixedBehind)) ? mHeaderHeight : 0) +
                            ((isInEditMode && mRefreshFooter != null && (mEnableFooterTranslationContent||mRefreshFooter.getSpinnerStyle() == SpinnerStyle.FixedBehind)) ? mFooterHeight : 0), lp.height);
            mRefreshContent.measure(widthSpec, heightSpec);
            minimumHeight += mRefreshContent.getMeasuredHeight();
        }

        setMeasuredDimension(resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec), resolveSize(minimumHeight, heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int paddingLeft = getPaddingLeft();
        final int paddingTop = getPaddingTop();
        final int paddingBottom = getPaddingBottom();
        final boolean isInEditMode = isInEditMode();

        if (mRefreshContent != null) {
            final LayoutParams lp = (LayoutParams) mRefreshContent.getLayoutParams();
            int left = paddingLeft + lp.leftMargin;
            int top = paddingTop + lp.topMargin;
            int right = left + mRefreshContent.getMeasuredWidth();
            int bottom = top + mRefreshContent.getMeasuredHeight();
            if (isInEditMode && mRefreshHeader != null && (mEnableHeaderTranslationContent||mRefreshHeader.getSpinnerStyle() == SpinnerStyle.FixedBehind)) {
                top = top + mHeaderHeight;
                bottom = bottom + mHeaderHeight;
            }
            mRefreshContent.layout(left, top, right, bottom);
        }

        if (mRefreshHeader != null) {
            final View headerView = mRefreshHeader.getView();
            final LayoutParams lp = (LayoutParams) headerView.getLayoutParams();
            int left = lp.leftMargin;
            int top = lp.topMargin ;
            int right = left + headerView.getMeasuredWidth();
            int bottom = top + headerView.getMeasuredHeight();
            if (!isInEditMode) {
                if (mRefreshHeader.getSpinnerStyle() == SpinnerStyle.Translate) {
                    top = top - mHeaderHeight;// + Math.max(0, mSpinner);
                    bottom = top + headerView.getMeasuredHeight();
                } else if (mRefreshHeader.getSpinnerStyle() == SpinnerStyle.Scale) {
                    bottom = top + Math.max(0, mSpinner);
//                } else if (mRefreshHeader.getSpinnerStyle() == SpinnerStyle.FixedBehind
//                        || mRefreshHeader.getSpinnerStyle() == SpinnerStyle.FixedFront) {
//                    bottom = top + Math.max(headerView.getMeasuredHeight(), mSpinner);
                }
            }
            headerView.layout(left, top, right, bottom);
        }

        if (mRefreshFooter != null) {
            final View footerView = mRefreshFooter.getView();
            final LayoutParams lp = (LayoutParams) footerView.getLayoutParams();
            final SpinnerStyle style = mRefreshFooter.getSpinnerStyle();
            int left = lp.leftMargin;
            int top = lp.topMargin + getMeasuredHeight();

            if (isInEditMode
                    || style == SpinnerStyle.FixedFront
                    || style == SpinnerStyle.FixedBehind) {
                top = top - mFooterHeight;
            } else if (style == SpinnerStyle.Scale) {
                top = top + Math.min(mSpinner, 0);
            }

            int right = left + footerView.getMeasuredWidth();
            int bottom = top + footerView.getMeasuredHeight();
            footerView.layout(left, top, right, bottom);
        }
    }

    //</editor-fold>

    //<editor-fold desc="滑动判断 judgement of slide">
    MotionEvent mEventDown = null;

    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        if (!isEnabled() || mNestedScrollInProgress
                || (!mEnableRefresh && !mEnableLoadmore)
                || state == RefreshState.Loading
                || state == RefreshState.Refreshing ) {
            return (state == RefreshState.Loading && mDisableContentWhenLoading)
                    || (state == RefreshState.Refreshing && mDisableContentWhenRefresh)
                    || super.dispatchTouchEvent(e);
        }
        int action = e.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mTouchX = e.getX();
                mTouchY = e.getY();
                super.dispatchTouchEvent(e);
                return true;

            case MotionEvent.ACTION_MOVE:
                final float dx = e.getX() - mTouchX;
                final float dy = e.getY() - mTouchY;
                if(state == RefreshState.None){
                    if (Math.abs(dy) >= mTouchSlop && Math.abs(dx) < Math.abs(dy)) {//滑动允许最大角度为45度
                        if (dy > 0 && mEnableRefresh && !mRefreshContent.canScrollUp()) {
                            mInitialMotionY = dy + mTouchY - mTouchSlop;
                            setStatePullDownRefresh();
                            e.setAction(MotionEvent.ACTION_CANCEL);
                            super.dispatchTouchEvent(e);
                        } else if (dy < 0 && mEnableLoadmore && !mRefreshContent.canScrollDown()) {
                            mInitialMotionY = dy + mTouchY + mTouchSlop;
                            setStatePullUpLoad();
                            e.setAction(MotionEvent.ACTION_CANCEL);
                            super.dispatchTouchEvent(e);
                        } else {
                            return super.dispatchTouchEvent(e);
                        }
                    } else {
                        return super.dispatchTouchEvent(e);
                    }
                }
                final float spinner = dy + mTouchY - mInitialMotionY;
                if (((state == RefreshState.PullDownRefresh || state == RefreshState.ReleaseToRefresh) && spinner < 0)
                    ||((state == RefreshState.PullUpLoad || state == RefreshState.ReleaseToLoad) && spinner > 0)) {
                    long time = System.currentTimeMillis();
                    if (mEventDown == null) {
                        mEventDown = MotionEvent.obtain(time, time, MotionEvent.ACTION_DOWN, mTouchX + dx, mInitialMotionY, 0);
                        super.dispatchTouchEvent(mEventDown);
                    }
                    MotionEvent em = MotionEvent.obtain(time, time, MotionEvent.ACTION_MOVE, mTouchX + dx, mInitialMotionY + spinner, 0);
                    super.dispatchTouchEvent(em);
                    if (mSpinner != 0) {
                        moveSpinnerInfinitely(0);
                    }
                    return true;
                }
                if (moveSpinnerInfinitely(spinner)) {
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                final float y = e.getY();
                if (mEventDown != null) {
                    mEventDown = null;
                    long time = System.currentTimeMillis();
                    MotionEvent ec = MotionEvent.obtain(time, time, MotionEvent.ACTION_CANCEL, mTouchX, y, 0);
                    super.dispatchTouchEvent(ec);
                }
                if (overSpinner(y - mTouchY)) {
                    return true;
                }
                break;
        }
        return super.dispatchTouchEvent(e);
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean b) {
        // if this is a List < L or another view that doesn't support nested
        // scrolling, ignore this request so that the vertical scroll event
        // isn't stolen
        View target = mRefreshContent.getScrollableView();
        if ((android.os.Build.VERSION.SDK_INT < 21 && mRefreshContent instanceof AbsListView)
                || (target != null && !ViewCompat.isNestedScrollingEnabled(target))) {
            // Nope.
        } else {
            super.requestDisallowInterceptTouchEvent(b);
        }
    }

    //</editor-fold>

    //<editor-fold desc="状态更改 state changes">

    private void notifyStateChanged(RefreshState state) {
        if (mRefreshFooter != null) {
            mRefreshFooter.onStateChanged(state);
        }
        if (mRefreshHeader != null) {
            mRefreshHeader.onStateChanged(state);
        }
        if (mOnMultiPurposeListener != null) {
            mOnMultiPurposeListener.onStateChanged(state);
        }
    }

    protected void setStatePullUpLoad() {
        notifyStateChanged(state = RefreshState.PullUpLoad);
    }

    protected void setStateReleaseLoad() {
        notifyStateChanged(state = RefreshState.ReleaseToLoad);
    }

    protected void setStateReleaseRefresh() {
        notifyStateChanged(state = RefreshState.ReleaseToRefresh);
    }
    protected void setStatePullDownRefresh() {
        notifyStateChanged(state = RefreshState.PullDownRefresh);
    }

    private void setStatePullDownCanceled() {
        notifyStateChanged(state = RefreshState.PullDownCanceled);
        resetStatus();
    }

    private void setStatePullUpCanceled() {
        notifyStateChanged(state = RefreshState.PullUpCanceled);
        resetStatus();
    }

    protected void setStateLoding() {
        notifyStateChanged(state = RefreshState.Loading);
        animSpinner(-mFooterHeight);
        if (mLoadmoreListener != null) {
            mLoadmoreListener.onLoadmore(this);
        }
        if (mRefreshFooter != null) {
            mRefreshFooter.startAnimator(this, mFooterHeight, mExtendFooterHeight);
        }
        if (mOnMultiPurposeListener != null) {
            mOnMultiPurposeListener.onLoadmore(this);
            mOnMultiPurposeListener.onFooterStartAnimator(mRefreshFooter, mFooterHeight, mExtendFooterHeight);
        }
    }

    protected void setStateRefresing() {
        notifyStateChanged(state = RefreshState.Refreshing);
        animSpinner(mHeaderHeight);
        if (mRefreshListener != null) {
            mRefreshListener.onRefresh(this);
        }
        if (mRefreshHeader != null) {
            mRefreshHeader.startAnimator(this, mHeaderHeight, mExtendHeaderHeight);
        }
        if (mOnMultiPurposeListener != null) {
            mOnMultiPurposeListener.onRefresh(this);
            mOnMultiPurposeListener.onHeaderStartAnimator(mRefreshHeader, mHeaderHeight, mExtendHeaderHeight);
        }
    }

    /**
     * 重置状态
     */
    protected void resetStatus() {
        if (state != RefreshState.None) {
            if (state == RefreshState.Refreshing && mRefreshHeader != null) {
                notifyStateChanged(state = RefreshState.RefreshFinish);
                mRefreshHeader.onFinish(this);
                if (mOnMultiPurposeListener != null) {
                    mOnMultiPurposeListener.onHeaderFinish(mRefreshHeader);
                }
            } else if (state == RefreshState.Loading && mRefreshFooter != null) {
                notifyStateChanged(state = RefreshState.LoadingFinish);
                mRefreshFooter.onFinish(this);
                if (mOnMultiPurposeListener != null) {
                    mOnMultiPurposeListener.onFooterFinish(mRefreshFooter);
                }
            }
            if (mSpinner == 0) {
                notifyStateChanged(state = RefreshState.None);
            }
        }
        if (mSpinner != 0) {
            animSpinner(0);
        }
    }
    //</editor-fold>

    //<editor-fold desc="视图位移 displacement">

    //<editor-fold desc="动画监听 Animator Listener">
    protected AnimatorListener valueAnimatorEndListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            if ((int)((ValueAnimator)animation).getAnimatedValue() == 0) {
                if (state != RefreshState.None) {
                    notifyStateChanged(state = RefreshState.None);
                }
            }
        }
    };

    protected AnimatorUpdateListener driftAnimatorUpdateListener = animation -> moveSpinner((int) animation.getAnimatedValue(), true);
    //</editor-fold>

    protected boolean moveSpinnerInfinitely(float dy) {
        if (state == RefreshState.PullDownRefresh || state == RefreshState.ReleaseToRefresh) {
            final double M = mExtendHeaderHeight + mHeaderHeight;
            final double H = Math.max(mScreenHeightPixels/2, getHeight());
            final double x = Math.max(0, dy);
            final double y = Math.min(M*(1-Math.pow(100,-x/H)),x);// 公式 y = M(1-40^(-x/H))
            moveSpinner((int) y, false);
            return true;
        } else if (state == RefreshState.PullUpLoad || state == RefreshState.ReleaseToLoad) {
            final double M = mExtendFooterHeight + mFooterHeight;
            final double H = Math.max(mScreenHeightPixels/2, getHeight());
            final double x = -Math.min(0, dy);
            final double y = -Math.min(M*(1-Math.pow(100,-x/H)),x);// 公式 y = M(1-40^(-x/H))
            moveSpinner((int) y, false);
            return true;
        }
        return false;
    }

    protected void animSpinner(int endValue) {
        if (mSpinner != endValue) {
            ValueAnimator animator = ValueAnimator.ofInt(mSpinner, endValue);
            animator.setDuration(mReboundDuration);
            animator.setInterpolator(mReboundInterpolator);
            animator.addUpdateListener(driftAnimatorUpdateListener);
            animator.addListener(valueAnimatorEndListener);
            animator.start();
        }
    }

    private boolean overSpinner(float spinner) {
        if (state == RefreshState.PullDownRefresh) {
            setStatePullDownCanceled();
            return true;
        } else if (state == RefreshState.PullUpLoad) {
            setStatePullUpCanceled();
            return true;
        } else if (state == RefreshState.ReleaseToRefresh) {
            setStateRefresing();
            return true;
        } else if (state == RefreshState.ReleaseToLoad) {
            setStateLoding();
            return true;
        }
        return false;
    }

    protected void moveSpinner(int spinner, boolean isAnimator) {
        if (mSpinner == spinner) {
            return;
        }
        this.mSpinner = spinner;
        if (state == RefreshState.PullDownRefresh && Math.abs(mSpinner) > mHeaderHeight) {
            setStateReleaseRefresh();
        } else if (state == RefreshState.ReleaseToRefresh && Math.abs(mSpinner) < mHeaderHeight) {
            setStatePullDownRefresh();
        } else if (state == RefreshState.PullUpLoad && Math.abs(mSpinner) > mFooterHeight) {
            setStateReleaseLoad();
        } else if (state == RefreshState.ReleaseToLoad && Math.abs(mSpinner) < mFooterHeight) {
            setStatePullUpLoad();
        }
        if (mRefreshContent != null) {
            if (spinner >= 0) {
                if (mEnableHeaderTranslationContent || mRefreshHeader.getSpinnerStyle() == SpinnerStyle.FixedBehind) {
                    mRefreshContent.moveSpinner(spinner);
                }
            } else {
                if (mEnableFooterTranslationContent || mRefreshFooter.getSpinnerStyle() == SpinnerStyle.FixedBehind) {
                    mRefreshContent.moveSpinner(spinner);
                }
            }
        }
        if (spinner >= 0 && mRefreshHeader != null) {
            if (mRefreshHeader.getSpinnerStyle() == SpinnerStyle.Scale) {
                requestLayout();
            } else if (mRefreshHeader.getSpinnerStyle() == SpinnerStyle.Translate) {
                mRefreshHeader.getView().setTranslationY(spinner);
            }
            if (isAnimator) {
                mRefreshHeader.onReleasing(1f * spinner / mHeaderHeight, spinner, mHeaderHeight, mExtendHeaderHeight);
                if (mOnMultiPurposeListener != null) {
                    mOnMultiPurposeListener.onHeaderReleasing(mRefreshHeader, 1f * spinner / mHeaderHeight, spinner, mHeaderHeight, mExtendHeaderHeight);
                }
            } else {
                mRefreshHeader.onPullingDown(1f * spinner / mHeaderHeight, spinner, mHeaderHeight, mExtendHeaderHeight);
                if (mOnMultiPurposeListener != null) {
                    mOnMultiPurposeListener.onHeaderPulling(mRefreshHeader, 1f * spinner / mHeaderHeight, spinner, mHeaderHeight, mExtendHeaderHeight);
                }
            }
        }
        if (spinner <= 0 && mRefreshFooter != null) {
            if (mRefreshFooter.getSpinnerStyle() == SpinnerStyle.Scale) {
                requestLayout();
            } else if (mRefreshFooter.getSpinnerStyle() == SpinnerStyle.Translate) {
                mRefreshFooter.getView().setTranslationY(spinner);
            }
            if (isAnimator) {
                mRefreshFooter.onPullReleasing(1f * spinner / mFooterHeight, spinner, mFooterHeight, mExtendFooterHeight);
                if (mOnMultiPurposeListener != null) {
                    mOnMultiPurposeListener.onFooterReleasing(mRefreshFooter, 1f * spinner / mFooterHeight, spinner, mFooterHeight, mExtendFooterHeight);
                }
            } else {
                mRefreshFooter.onPullingUp(1f * spinner / mFooterHeight, spinner, mFooterHeight, mExtendFooterHeight);
                if (mOnMultiPurposeListener != null) {
                    mOnMultiPurposeListener.onFooterPulling(mRefreshFooter, 1f * spinner / mFooterHeight, spinner, mFooterHeight, mExtendFooterHeight);
                }
            }
        }
    }

    //</editor-fold>

    //<editor-fold desc="布局参数 LayoutParams">
    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(MATCH_PARENT, MATCH_PARENT);
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    public static class LayoutParams extends MarginLayoutParams {

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }
    //</editor-fold>

    //<editor-fold desc="嵌套滚动 NestedScrolling">

    //<editor-fold desc="NestedScrollingParent">
    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
//        || (!mEnableRefresh && !mEnableLoadmore)
//                || state == RefreshState.Loading
//                || state == RefreshState.Refreshing
        return isEnabled() && (mEnableRefresh||mEnableLoadmore) && !(state == RefreshState.Loading||state == RefreshState.Refreshing)
                && (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int axes) {
        // Reset the counter of how much leftover scroll needs to be consumed.
        mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes);
        // Dispatch up to the nested parent
        startNestedScroll(axes & ViewCompat.SCROLL_AXIS_VERTICAL);
        mTotalUnconsumed = 0;
        mNestedScrollInProgress = true;
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        // If we are in the middle of consuming, a scroll, then we want to move the spinner back up
        // before allowing the list to scroll
        if (mEnableRefresh && dy > 0 && mTotalUnconsumed > 0) {
            if (dy > mTotalUnconsumed) {
                consumed[1] = dy - (int) mTotalUnconsumed;
                mTotalUnconsumed = 0;
            } else {
                mTotalUnconsumed -= dy;
                consumed[1] = dy;
            }
            moveSpinnerInfinitely((int)mTotalUnconsumed);
        } else if (mEnableLoadmore && dy < 0 && mTotalUnconsumed < 0) {
            if (dy < mTotalUnconsumed) {
                consumed[1] = dy - (int) mTotalUnconsumed;
                mTotalUnconsumed = 0;
            } else {
                mTotalUnconsumed -= dy;
                consumed[1] = dy;
            }
            moveSpinnerInfinitely((int)mTotalUnconsumed);
        }

        // If a client layout is using a custom start position for the circle
        // view, they mean to hide it again before scrolling the child view
        // If we get back to mTotalUnconsumed == 0 and there is more to go, hide
        // the circle so it isn't exposed if its blocking content is moved
//        if (mUsingCustomStart && dy > 0 && mTotalUnconsumed == 0
//                && Math.abs(dy - consumed[1]) > 0) {
//            mCircleView.setVisibility(View.GONE);
//        }

        // Now let our nested parent consume the leftovers
        final int[] parentConsumed = mParentScrollConsumed;
        if (dispatchNestedPreScroll(dx - consumed[0], dy - consumed[1], parentConsumed, null)) {
            consumed[0] += parentConsumed[0];
            consumed[1] += parentConsumed[1];
        }
    }

    @Override
    public int getNestedScrollAxes() {
        return mNestedScrollingParentHelper.getNestedScrollAxes();
    }

    @Override
    public void onStopNestedScroll(View target) {
        mNestedScrollingParentHelper.onStopNestedScroll(target);
        mNestedScrollInProgress = false;
        // Finish the spinner for nested scrolling if we ever consumed any
        // unconsumed nested scroll
        if (mTotalUnconsumed != 0) {
            overSpinner(mTotalUnconsumed);
            mTotalUnconsumed = 0;
        }
        // Dispatch up our nested parent
        stopNestedScroll();
    }

    @Override
    public void onNestedScroll(final View target, final int dxConsumed, final int dyConsumed,
                               final int dxUnconsumed, final int dyUnconsumed) {
        // Dispatch up to the nested parent first
        dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                mParentOffsetInWindow);

        // This is a bit of a hack. Nested scrolling works from the bottom up, and as we are
        // sometimes between two nested scrolling views, we need a way to be able to know when any
        // nested scrolling parent has stopped handling events. We do that by using the
        // 'offset in window 'functionality to see if we have been moved from the event.
        // This is a decent indication of whether we should take over the event stream or not.
        final int dy = dyUnconsumed + mParentOffsetInWindow[1];
        if (mEnableRefresh && dy < 0 && (mRefreshContent == null || !mRefreshContent.canScrollUp())) {
            if (state == RefreshState.None) {
                setStatePullDownRefresh();
            }
            mTotalUnconsumed += Math.abs(dy);
            moveSpinnerInfinitely(mTotalUnconsumed);
        } else if (mEnableLoadmore && dy > 0 && (mRefreshContent == null || !mRefreshContent.canScrollDown())) {
            if (state == RefreshState.None) {
                setStatePullUpLoad();
            }
            mTotalUnconsumed -= Math.abs(dy);
            moveSpinnerInfinitely(mTotalUnconsumed);
        }
    }
    //</editor-fold>

    //<editor-fold desc="NestedScrollingChild">
    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mNestedScrollingChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mNestedScrollingChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return mNestedScrollingChildHelper.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        mNestedScrollingChildHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return mNestedScrollingChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                                        int dyUnconsumed, int[] offsetInWindow) {
        return mNestedScrollingChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed,
                dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return mNestedScrollingChildHelper.dispatchNestedPreScroll(
                dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX,
                                    float velocityY) {
        return dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY,
                                 boolean consumed) {
        return dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mNestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mNestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }
    //</editor-fold>

    //</editor-fold>

    //<editor-fold desc="开放接口 open interface">
    @Override
    public SmartRefreshLayout setFooterHeightDp(float height) {
        this.mFooterHeight = dp2px(height);
        return this;
    }
    @Override
    public SmartRefreshLayout setFooterHeightPx(int height) {
        this.mFooterHeight = height;
        return this;
    }
    @Override
    public SmartRefreshLayout setHeaderHeightDp(float height) {
        this.mHeaderHeight = dp2px(height);
        return this;
    }
    @Override
    public SmartRefreshLayout setHeaderHeightPx(int height) {
        this.mHeaderHeight = height;
        return this;
    }
    @Override
    public SmartRefreshLayout setExtendHeaderHeightDp(float height) {
        this.mExtendHeaderHeight = dp2px(height);
        return this;
    }
    @Override
    public SmartRefreshLayout setExtendHeaderHeightPx(int height) {
        this.mExtendHeaderHeight = height;
        return this;
    }
    @Override
    public SmartRefreshLayout setExtendFooterHeightDp(float height) {
        this.mExtendFooterHeight = dp2px(height);
        return this;
    }
    @Override
    public SmartRefreshLayout setExtendFooterHeightPx(int height) {
        this.mExtendFooterHeight = height;
        return this;
    }

    /**
     * 设置下拉最大高度和真实高度的比率（将会影响可以下拉的最大高度）
     */
    @Override
    public SmartRefreshLayout setExtendRate(float rate) {
        this.mExtendRate = rate;
        return this;
    }

    /**
     * 设置回弹显示插值器
     */
    @Override
    public SmartRefreshLayout setReboundInterpolator(Interpolator interpolator) {
        this.mReboundInterpolator = interpolator;
        return this;
    }

    /**
     * 设置是否启用上啦加载更多（默认启用）
     */
    @Override
    public SmartRefreshLayout setEnableLoadmore(boolean enable) {
        this.mEnableLoadmore = enable;
        return this;
    }

    /**
     * 是否启用下拉刷新（默认启用）
     */
    @Override
    public SmartRefreshLayout setEnableRefresh(boolean enable) {
        this.mEnableRefresh = enable;
        return this;
    }

    /**
     * 设置是否启用内容视图拖动效果
     */
    @Override
    public SmartRefreshLayout setEnableHeaderTranslationContent(boolean enable) {
        this.mEnableHeaderTranslationContent = enable;
        return this;
    }

    /**
     * 设置是否启用内容视图拖动效果
     */
    @Override
    public SmartRefreshLayout setEnableFooterTranslationContent(boolean enable) {
        this.mEnableFooterTranslationContent = enable;
        return this;
    }

    /**
     * 设置是否开启在刷新时候禁止操作内容视图
     */
    @Override
    public SmartRefreshLayout setDisableContentWhenRefresh(boolean disable) {
        this.mDisableContentWhenRefresh = disable;
        return this;
    }

    /**
     * 设置是否开启在刷新时候禁止操作内容视图
     */
    @Override
    public SmartRefreshLayout setDisableContentWhenLoading(boolean disable) {
        this.mDisableContentWhenLoading = disable;
        return this;
    }

    /**
     * 设置底部上啦组件的实现
     */
    @Override
    public SmartRefreshLayout setRefreshBottom(RefreshFooter bottom) {
        if (mRefreshFooter != null) {
            removeView(mRefreshFooter.getView());
        }
        this.mRefreshFooter = bottom;
        this.addView(mRefreshFooter.getView());
        return this;
    }

    /**
     * 设置顶部下拉组件的实现
     */
    @Override
    public SmartRefreshLayout setRefreshHeader(RefreshHeader header) {
        if (mRefreshHeader != null) {
            removeView(mRefreshHeader.getView());
        }
        this.mRefreshHeader = header;
        this.addView(mRefreshHeader.getView());
        return this;
    }

    /**
     * 单独设置刷新监听器
     */
    @Override
    public SmartRefreshLayout setOnRefreshListener(OnRefreshListener listener) {
        this.mRefreshListener = listener;
        return this;
    }

    /**
     * 单独设置加载监听器
     */
    @Override
    public SmartRefreshLayout setOnLoadmoreListener(OnLoadmoreListener listener) {
        this.mLoadmoreListener = listener;
        return this;
    }

    /**
     * 同时设置刷新和加载监听器
     */
    @Override
    public SmartRefreshLayout setOnRefreshLoadmoreListener(OnRefreshLoadmoreListener listener) {
        this.mRefreshListener = listener;
        this.mLoadmoreListener = listener;
        return this;
    }

    /**
     * 设置多功能监听器
     */
    @Override
    public SmartRefreshLayout setOnMultiPurposeListener(OnMultiPurposeListener listener) {
        this.mOnMultiPurposeListener = listener;
        return this;
    }

    /**
     * 设置主题颜色
     */
    @Override
    public SmartRefreshLayout setPrimaryColorsId(int... primaryColorId) {
        int[] colors = new int[primaryColorId.length];
        for (int i = 0; i < primaryColorId.length; i++) {
            colors[i] = ContextCompat.getColor(getContext(), primaryColorId[i]);
        }
        setPrimaryColors(colors);
        return this;
    }

    /**
     * 设置主题颜色
     */
    @Override
    public SmartRefreshLayout setPrimaryColors(int... colors) {
        if (mRefreshHeader != null) {
            mRefreshHeader.setPrimaryColors(colors);
        }
        if (mRefreshFooter != null) {
            mRefreshFooter.setPrimaryColors(colors);
        }
        mPrimaryColors = colors;
        return this;
    }
    /**
     * 完成刷新
     */
    @Override
    public SmartRefreshLayout finisRefresh(){
        return finisRefresh(1000);
    }

    /**
     * 完成加载
     */
    @Override
    public SmartRefreshLayout finisLoadmore(){
        return finisLoadmore(1000);
    }
    /**
     * 完成刷新
     */
    @Override
    public SmartRefreshLayout finisRefresh(int delayed){
        postDelayed(this::resetStatus, delayed);
        return this;
    }
    /**
     * 完成加载
     */
    @Override
    public SmartRefreshLayout finisLoadmore(int delayed){
        postDelayed(this::resetStatus, delayed);
        return this;
    }
    /**
     * 是否正在刷新
     */
    @Override
    public boolean isRefreshing() {
        return state == RefreshState.Refreshing;
    }
    /**
     * 是否正在加载
     */
    @Override
    public boolean isLoading() {
        return state == RefreshState.Loading;
    }
    /**
     * 自动刷新
     */
    @Override
    public boolean autoRefresh() {
        return autoRefresh(500);
    }
    /**
     * 自动刷新
     */
    @Override
    public boolean autoRefresh(int delayed) {
        if (state == RefreshState.None) {
            postDelayed(() -> {
                ValueAnimator animator = ValueAnimator.ofInt(mSpinner, (int) (mHeaderHeight * (mExtendRate - 1)));
                animator.setDuration(mReboundDuration);
                animator.setInterpolator(mReboundInterpolator);
                animator.addUpdateListener(animation -> moveSpinner((int) animation.getAnimatedValue(), false));
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        state = RefreshState.ReleaseToRefresh;
                        overSpinner(0);
                    }
                });
                animator.start();
            }, delayed);
            return true;
        } else {
            return false;
        }
    }

    /**
     * 设置默认Header构建器
     */
    public static void setDefaultRefreshHeaderCreater(@NonNull DefaultRefreshHeaderCreater creater) {
        mHeaderCreater = creater;
    }

    /**
     * 设置默认Footer构建器
     */
    public static void setDefaultRefreshFooterCreater(@NonNull DefaultRefreshFooterCreater creater) {
        mFooterCreater = creater;
    }

    //</editor-fold>
}