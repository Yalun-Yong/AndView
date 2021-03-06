package org.andcreator.andview.activity;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v4.view.ViewPager;
import android.support.v4.view.animation.FastOutLinearInInterpolator;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.flask.floatingactionmenu.FadingBackgroundView;
import com.flask.floatingactionmenu.FloatingActionMenu;
import com.flask.floatingactionmenu.FloatingActionToggleButton;
import com.flask.floatingactionmenu.OnFloatingActionMenuSelectedListener;

import org.andcreator.andview.R;
import org.andcreator.andview.fragment.MainContributorFragment;
import org.andcreator.andview.fragment.MainEffectFragment;
import org.andcreator.andview.fragment.MainLayoutFragment;
import org.andcreator.andview.fragment.MainViewFragment;
import org.andcreator.andview.uilt.BottomNavigationViewHelper;
import org.andcreator.andview.uilt.ImageUtil;
import org.andcreator.andview.uilt.SetTheme;

import java.util.ArrayList;
import java.util.List;

import jp.wasabeef.glide.transformations.BlurTransformation;
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt;

import static com.bumptech.glide.request.RequestOptions.bitmapTransform;
import static org.andcreator.andview.uilt.GetThemeColor.getDarkColorPrimary;
import static org.andcreator.andview.uilt.ImageUtil.CHOOSE_PHOTO;
import static org.andcreator.andview.uilt.ImageUtil.drawableToBitmap;
import static org.andcreator.andview.uilt.ImageUtil.getRealFilePath;
import static org.andcreator.andview.uilt.ImageUtil.handleImageOnKitKat;

/**
 * @author hawvu
 */
public class MainActivity extends AppCompatActivity implements Application.OnProvideAssistDataListener {

    private long mPressedTime = 0;

    private ImageView background;
    private ImageView logo;
    private ViewPager mViewPager;
    private BottomNavigationView mNavigationView;
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private BottomSheetBehavior bottomSheetBehavior;

    private FloatingActionToggleButton fabToggle;

    private View appsBar;
    private View panelView;
    private boolean isOpen = false;


    // 声明一个集合，在后面的代码中用来存储用户拒绝授权的权
    private List<String> mPermissionList = new ArrayList<>();

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_layout:
                    mViewPager.setCurrentItem(0);
                    return true;
                case R.id.navigation_view:
                    mViewPager.setCurrentItem(1);
                    return true;
                case R.id.navigation_effect:
                    mViewPager.setCurrentItem(2);
                    return true;
                case R.id.navigation_contributor:
                    mViewPager.setCurrentItem(3);
                    return true;
                    default:
                        break;
            }
            return false;
        }
    };

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SetTheme.setStartTheme(this);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
        setContentView(R.layout.activity_main);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        editor = PreferenceManager.getDefaultSharedPreferences(this).edit();

        background = findViewById(R.id.background);

        //是否加载背景图片
        loadBackground();

        initView();

        //请求权限
        String[] permission;
        permission = new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.CAMERA
        };

        mPermissionList.clear();
        for (String permiss : permission) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, permiss) != PackageManager.PERMISSION_GRANTED) {
                mPermissionList.add(permiss);
            }
        }
        //未授予的权限为空，表示都授予了
        if (!mPermissionList.isEmpty()) {
            //请求权限方法
            //将List转为数组
            String[] permissions = mPermissionList.toArray(new String[0]);
            ActivityCompat.requestPermissions(MainActivity.this, permissions, 1);
        }
    }

    private void initView() {
        FadingBackgroundView fading = findViewById(R.id.fading);
        fabToggle = findViewById(R.id.fab_toggle);
        FloatingActionMenu fam = findViewById(R.id.fam);
        fam.setFadingBackgroundView(fading);
        fam.setOnFloatingActionMenuSelectedListener(new OnFloatingActionMenuSelectedListener() {
            @Override
            public void onFloatingActionMenuSelected(com.flask.floatingactionmenu.FloatingActionButton floatingActionButton) {
                if (!(floatingActionButton instanceof FloatingActionToggleButton)) {
                    if ("加入贡献者".equals(floatingActionButton.getLabelText())){
                        fabToggle.toggleOff();
                        joinQQGroup("a-pWwOHzOhvaQQeYtr9oPbYxuIF7VTT9");
                    }else if ("访问开源地址".equals(floatingActionButton.getLabelText())){
                        fabToggle.toggleOff();
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse("https://github.com/hujincan/AndView"));
                        startActivity(intent);
                    }
                }
            }
        });

        //是否加载过
        if (!sharedPreferences.getBoolean("first",false)){

            new MaterialTapTargetPrompt.Builder(MainActivity.this)
                    .setTarget(R.id.fab_toggle)
                    .setPrimaryText("这里了解开源地址")
                    .setSecondaryText("如果你也想加入贡献者，请联系我")
                    .setBackgroundColour(getDarkColorPrimary(MainActivity.this))
                    .setPromptStateChangeListener(new MaterialTapTargetPrompt.PromptStateChangeListener()
                    {
                        @Override
                        public void onPromptStateChanged(@NonNull MaterialTapTargetPrompt prompt, int state)
                        { }
                    })
                    .show();
            editor.putBoolean("first",true);
            editor.apply();
        }

        mNavigationView = findViewById(R.id.navigation);
        mNavigationView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        mViewPager = findViewById(R.id.view_pager);

        mViewPager.setOffscreenPageLimit(4);

        SectionsPagerAdapter adapter = new SectionsPagerAdapter(getSupportFragmentManager());

        adapter.addFragment(new MainLayoutFragment(),"");
        adapter.addFragment(new MainViewFragment(),"");
        adapter.addFragment(new MainEffectFragment(),"");
        adapter.addFragment(new MainContributorFragment(),"");

        mViewPager.setAdapter(adapter);
        mViewPager.addOnPageChangeListener(pageChangeListener);
        mViewPager.setPageTransformer(false, new DepthPageTransformer());

        //默认 >3 的选中效果会影响ViewPager的滑动切换时的效果，故利用反射去掉
        BottomNavigationViewHelper.disableShiftMode(mNavigationView);

        mSectionsPagerAdapter = (SectionsPagerAdapter) mViewPager.getAdapter();


        appsBar = findViewById(R.id.app_bar);
        panelView = findViewById(R.id.panel);
        View view = findViewById(R.id.sheet_view);
        appsBar.setAlpha(1);
        panelView.setAlpha(0);
        bottomSheetBehavior = BottomSheetBehavior.from(view);
        bottomSheetBehavior.setHideable(false);
        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback(){

            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                /*
                 STATE_COLLAPSED： 默认的折叠状态， bottom sheets只在底部显示一部分布局。显示高度可以通过 app:behavior_peekHeight 设置（默认是0）
                 STATE_DRAGGING ： 过渡状态，此时用户正在向上或者向下拖动bottom sheet
                 STATE_SETTLING: 视图从脱离手指自由滑动到最终停下的这一小段时间
                 STATE_EXPANDED： bottom sheet 处于完全展开的状态：当bottom sheet的高度低于CoordinatorLayout容器时，整个bottom sheet都可见；或者CoordinatorLayout容器已经被bottom sheet填满。
                 STATE_HIDDEN ： 默认无此状态（可通过app:behavior_hideable 启用此状态），启用后用户将能通过向下滑动完全隐藏 bottom sheet
                 */
                switch (newState){
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        isOpen = false;
                        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                        if (sharedPreferences.getBoolean("example_switch",true)){
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
                            }else {
                                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
                            }
                        }
                        break;
                    case BottomSheetBehavior.STATE_DRAGGING:
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED:
                        isOpen = true;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
                        }else {
                            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                        }
                        break;
                    case BottomSheetBehavior.STATE_SETTLING:
                        break;
                    case BottomSheetBehavior.STATE_HIDDEN:
                        break;
                        default:
                            break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                panelView.setAlpha(slideOffset);
                appsBar.setAlpha(1-slideOffset*2);
                fabToggle.setTranslationY(-slideOffset*1000);
            }
        });

        logo = findViewById(R.id.logo);
        Glide.with(this).load(R.drawable.logo).into(logo);

        PrefsFragment prefsFragment = new PrefsFragment();
        getFragmentManager().beginTransaction().replace(R.id.content,prefsFragment).commit();

        prefsFragment.setClickListener(new PrefsFragment.OnItemClickListener() {
            @Override
            public void onClick(int msg,Bitmap bitmap) {
                if (msg == 1){
                    if (background.getVisibility() == View.GONE){
                        background.setVisibility(View.VISIBLE);
                    }

                    Glide.with(MainActivity.this)
                            .load(bitmap)
                            .apply(bitmapTransform(new BlurTransformation(23)))
                            .into(background);
                }else if (msg == 2){
                    background.setVisibility(View.GONE);
                }else if (msg == 3){

                    Intent intent = new Intent(MainActivity.this, AboutActivity.class);
                    String transitionIcon = MainActivity.this.getString(R.string.transition_logo);
                    Pair<View, String> p1 = Pair.create((View) logo,transitionIcon);
                    ActivityOptionsCompat transitionActivityOptions = ActivityOptionsCompat.makeSceneTransitionAnimation(MainActivity.this, p1);
                    startActivity(intent,transitionActivityOptions.toBundle());
                }
            }
        });
    }

    //第一个Fragment必须在整个界面加载完成时在获取Fragment对象
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (!sharedPreferences.getBoolean("welcome",false)){
            startActivity(new Intent(MainActivity.this,WelcomeActivity.class));
            editor.putBoolean("welcome",true);
            editor.apply();
        }

        if (mSectionsPagerAdapter != null){
            //ViewPager加载完成，得到同一对象的Fragment
            MainLayoutFragment mainLayoutFragment = (MainLayoutFragment) mSectionsPagerAdapter.getFragment(0);
            mainLayoutFragment.setOnFragmentInteractionListener(new MainLayoutFragment.OnFragmentInteractionListener() {
                @Override
                public void onFragmentInteraction(int scroll) {
                    if (scroll == 0){
                        hide(fabToggle);
                        ObjectAnimator.ofFloat(mNavigationView,"translationY",0,mNavigationView.getHeight()+mNavigationView.getHeight()+mNavigationView.getPaddingBottom()).setDuration(200).start();
                    }else if (scroll == 1){
                        show(fabToggle);
                        ObjectAnimator.ofFloat(mNavigationView,"translationY",mNavigationView.getHeight()+mNavigationView.getHeight()+mNavigationView.getPaddingBottom(),0).setDuration(200).start();
                    }
                }
            });
        }else {
            Toast.makeText(this, "怎么可能", Toast.LENGTH_SHORT).show();
        }

    }

    //ViewPager监听
    private ViewPager.OnPageChangeListener pageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            switch (position) {
                case 0:
                    mNavigationView.setSelectedItemId(R.id.navigation_layout);
                    break;
                case 1:
                    mNavigationView.setSelectedItemId(R.id.navigation_view);

                    if (mSectionsPagerAdapter != null){
                        //ViewPager加载完成，得到同一对象的Fragment
                        MainViewFragment mainViewFragment = (MainViewFragment) mSectionsPagerAdapter.getFragment(1);
                        mainViewFragment.setOnFragmentInteractionListener(new MainViewFragment.OnFragmentInteractionListener() {
                            @Override
                            public void onFragmentInteraction(int scroll) {
                                if (scroll == 0){
                                    hide(fabToggle);
                                    ObjectAnimator.ofFloat(mNavigationView,"translationY",0,mNavigationView.getHeight()+mNavigationView.getHeight()+mNavigationView.getPaddingBottom()).setDuration(200).start();
                                }else if (scroll == 1){
                                    show(fabToggle);
                                    ObjectAnimator.ofFloat(mNavigationView,"translationY",mNavigationView.getHeight()+mNavigationView.getHeight()+mNavigationView.getPaddingBottom(),0).setDuration(200).start();
                                }
                            }
                        });
                    }else {
                        Toast.makeText(MainActivity.this, "怎么可能", Toast.LENGTH_SHORT).show();
                    }

                    break;
                case 2:
                    mNavigationView.setSelectedItemId(R.id.navigation_effect);

                    if (mSectionsPagerAdapter != null){
                        //ViewPager加载完成，得到同一对象的Fragment
                        MainEffectFragment mainEffectFragment = (MainEffectFragment) mSectionsPagerAdapter.getFragment(2);
                        mainEffectFragment.setOnFragmentInteractionListener(new MainEffectFragment.OnFragmentInteractionListener() {
                            @Override
                            public void onFragmentInteraction(int scroll) {
                                if (scroll == 0){
                                    hide(fabToggle);
                                    ObjectAnimator.ofFloat(mNavigationView,"translationY",0,mNavigationView.getHeight()+mNavigationView.getHeight()+mNavigationView.getPaddingBottom()).setDuration(200).start();
                                }else if (scroll == 1){
                                    show(fabToggle);
                                    ObjectAnimator.ofFloat(mNavigationView,"translationY",mNavigationView.getHeight()+mNavigationView.getHeight()+mNavigationView.getPaddingBottom(),0).setDuration(200).start();
                                }
                            }
                        });
                    }else {
                        Toast.makeText(MainActivity.this, "怎么可能", Toast.LENGTH_SHORT).show();
                    }

                    break;
                case 3:
                    mNavigationView.setSelectedItemId(R.id.navigation_contributor);

                    if (mSectionsPagerAdapter != null){
                        //ViewPager加载完成，得到同一对象的Fragment
                        MainContributorFragment mainContributorFragment = (MainContributorFragment) mSectionsPagerAdapter.getFragment(3);
                        mainContributorFragment.setOnFragmentInteractionListener(new MainContributorFragment.OnFragmentInteractionListener() {
                            @Override
                            public void onFragmentInteraction(int scroll) {
                                if (scroll == 0){
                                    hide(fabToggle);
                                    ObjectAnimator.ofFloat(mNavigationView,"translationY",0,mNavigationView.getHeight()+mNavigationView.getHeight()+mNavigationView.getPaddingBottom()).setDuration(200).start();
                                }else if (scroll == 1){
                                    show(fabToggle);
                                    ObjectAnimator.ofFloat(mNavigationView,"translationY",mNavigationView.getHeight()+mNavigationView.getHeight()+mNavigationView.getPaddingBottom(),0).setDuration(200).start();
                                }
                            }
                        });
                    }else {
                        Toast.makeText(MainActivity.this, "怎么可能", Toast.LENGTH_SHORT).show();
                    }

                    break;
                    default:
                        break;
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }
    };

    //返回键监听
    @Override
    public void onBackPressed() {
        if (isOpen){
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }else {
            int backStackEntryCount = getSupportFragmentManager().getBackStackEntryCount();
            if (backStackEntryCount == 0){
                //获取第一次按键时间
                long mNowTime = System.currentTimeMillis();

                //比较两次按键时间差
                int time = 1450;
                if((mNowTime - mPressedTime) > time){

                    Snackbar snackbar = Snackbar.make(mNavigationView, R.string.app_exit, Snackbar.LENGTH_SHORT);
                    View view = snackbar.getView();
                    view.setBackgroundColor(getResources().getColor(R.color.white));
                    snackbar.show();

                    mPressedTime = mNowTime;
                } else{
                    //退出程序
                    finish();
                }
            }else {
                super.onBackPressed();
            }
        }
    }

    @Override
    public void onProvideAssistData(Activity activity, Bundle data) {

    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public class DepthPageTransformer implements ViewPager.PageTransformer {

        @Override
        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        @SuppressLint("NewApi")
        public void transformPage(@NonNull View view, float position) {
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode){
            case CHOOSE_PHOTO :
                if (resultCode == RESULT_OK){
                    assert data != null;
                    Uri uri = handleImageOnKitKat(data);
                    if ("download".equals(getRealFilePath(this, uri))){
                        editor.putString("img",handleImageOnKitKat(data).toString());
                        editor.apply();
                    }else {
                        editor.putString("img",getRealFilePath(this,uri));
                        editor.apply();
                    }
                    loadBackground();
                }
                break;
                default:
                    break;
        }
    }

    //权限授权情况
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        final int writeExternalStorage = 1;
        final int readExternalStorage = 2;
        final int readContacts = 3;
        final int camera = 4;
        switch (requestCode){
            case writeExternalStorage:
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(MainActivity.this,"未得到权限",Toast.LENGTH_SHORT).show();
                }
                break;
            case readExternalStorage:
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(MainActivity.this,"未得到权限",Toast.LENGTH_SHORT).show();
                }
                break;
            case readContacts:
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(MainActivity.this,"未得到权限",Toast.LENGTH_SHORT).show();
                }
                break;
            case camera:
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(MainActivity.this,"未得到权限",Toast.LENGTH_SHORT).show();
                }
                break;
            default:break;
        }
    }


    /**
     * 显示的动画
     */
    private void show(final View view) {
        view.animate().cancel();

        // If the view isn't visible currently, we'll animate it from a single pixel
        view.setAlpha(0f);
        view.setScaleY(0f);
        view.setScaleX(0f);

        view.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(200)
                .setInterpolator(new LinearOutSlowInInterpolator())
                .setListener(new AnimatorListenerAdapter() {

                    @Override
                    public void onAnimationStart(Animator animation) {
                        view.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {

                    }
                });
    }

    /**
     * 隐藏的动画
     */
    private void hide(final View view) {
        view.animate().cancel();
        view.animate()
                .scaleX(0f)
                .scaleY(0f)
                .alpha(0f)
                .setDuration(200)
                .setInterpolator(new FastOutLinearInInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    private boolean mCancelled;

                    @Override
                    public void onAnimationStart(Animator animation) {
                        view.setVisibility(View.VISIBLE);
                        mCancelled = false;
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        mCancelled = true;
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (!mCancelled) {
                            view.setVisibility(View.INVISIBLE);
                        }
                    }
                });
    }


    /****************
     *
     * 发起添加群流程。群号：创艺者开发设计(677026563) 的 key 为： a-pWwOHzOhvaQQeYtr9oPbYxuIF7VTT9
     * 调用 joinQQGroup(a-pWwOHzOhvaQQeYtr9oPbYxuIF7VTT9) 即可发起手Q客户端申请加群 创艺者开发设计(677026563)
     *
     * @param key 由官网生成的key
     ******************/
    public void joinQQGroup(String key) {
        Intent intent = new Intent();
        intent.setData(Uri.parse("mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26k%3D" + key));
        // 此Flag可根据具体产品需要自定义，如设置，则在加群界面按返回，返回手Q主界面，不设置，按返回会返回到呼起产品界面    //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent);
        } catch (Exception e) {


            ClipboardManager cmb = (ClipboardManager)MainActivity.this.getSystemService(Context.CLIPBOARD_SERVICE);
            assert cmb != null;
            cmb.setPrimaryClip(ClipData.newPlainText(null,"677026563"));

            // 未安装手Q或安装的版本不支持
            Toast.makeText(this, "未安装QQ或安装的版本不支持,群号已复制到剪贴板", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * ViewPager适配器*/
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();
        Fragment currentFragment;

        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            this.currentFragment= (Fragment) object;
            super.setPrimaryItem(container, position, object);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }
        void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }
        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }

        Fragment getFragment(int position) {
            return mFragmentList.get(position);
        }
    }

    private void loadBackground(){
        if (sharedPreferences.getBoolean("example_switch",true)){
            if ("".equals(sharedPreferences.getString("img", ""))){
                Glide.with(MainActivity.this)
                        .load(R.drawable.background_a)
                        .apply(bitmapTransform(new BlurTransformation(23)))
                        .into(background);
            }else {

                if (sharedPreferences.getString("img","").contains("content://")){

                    Glide.with(MainActivity.this)
                            .load(Uri.parse(sharedPreferences.getString("img", "")))
                            .apply(bitmapTransform(new BlurTransformation(23)))
                            .into(background);
                }else {

                    Glide.with(MainActivity.this)
                            .load(sharedPreferences.getString("img", ""))
                            .apply(bitmapTransform(new BlurTransformation(23)))
                            .into(background);
                }
            }
        }else {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
            }else {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
    }

    /**
     * 设置页面
     */
    private static SwitchPreference mMistakeTouchPreference;
    private static PreferenceScreen mImage;
    private static final String MISTAKE_TOUCH_MODE_KEY = "example_switch";
    private static final String CHANGE_THEME_KEY = "theme_type_number";
    private static final String IMG = "img";
    private static final String JOIN = "join";
    private static final String ABOUT = "about";
    private static Bitmap bitmaps;

    public static class PrefsFragment extends PreferenceFragment {

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            mMistakeTouchPreference = (SwitchPreference) findPreference(MISTAKE_TOUCH_MODE_KEY);
            Preference mChangeTheme = findPreference(CHANGE_THEME_KEY);
            mImage = (PreferenceScreen) findPreference(IMG);
            PreferenceScreen mJoin = (PreferenceScreen) findPreference(JOIN);
            PreferenceScreen mAbout = (PreferenceScreen) findPreference(ABOUT);

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            if (!sharedPreferences.getBoolean("example_switch",true)){
                mImage.setShouldDisableView(true);
                mImage.setEnabled(false);
            }

            mChangeTheme.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(getActivity(),ThemeActivity.class);
                    startActivityForResult(intent,1);
                    return true;
                }
            });

            mMistakeTouchPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @SuppressLint("CheckResult")
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (mMistakeTouchPreference.isChecked() != (Boolean)newValue) {
                        boolean value = (Boolean)(newValue);
                        mMistakeTouchPreference.setChecked(value);

                        mImage.setShouldDisableView(!value);
                        mImage.setEnabled(value);

                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

                        if (sharedPreferences.getBoolean("example_switch",true)){
                            if (sharedPreferences.getString("img","").equals("")){

                                Glide.with(getActivity()).load(R.drawable.background_a).into(new SimpleTarget<Drawable>() {
                                    @Override
                                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                                        bitmaps = drawableToBitmap(resource);
                                        Message msg = new Message();
                                        msg.what = 1;
                                        handlers.sendMessage(msg);
                                    }
                                });

                            }else if (sharedPreferences.getString("img","").contains("content://")){
                                Glide.with(getActivity()).load(Uri.parse(sharedPreferences.getString("img",""))).into(new SimpleTarget<Drawable>() {
                                    @Override
                                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                                        bitmaps = drawableToBitmap(resource);
                                        Message msg = new Message();
                                        msg.what = 1;
                                        handlers.sendMessage(msg);
                                    }
                                });
                            }else {
                                Glide.with(getActivity()).load(sharedPreferences.getString("img","")).into(new SimpleTarget<Drawable>() {
                                    @Override
                                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                                        bitmaps = drawableToBitmap(resource);
                                        Message msg = new Message();
                                        msg.what = 1;
                                        handlers.sendMessage(msg);
                                    }
                                });
                            }
                        }else {
                            Message msg = new Message();
                            msg.what = 2;
                            handlers.sendMessage(msg);
                            getActivity().getWindow().setStatusBarColor(Color.TRANSPARENT);
                            getActivity().getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
                        }
                    }

                    return true;
                }
            });

            mImage.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new ImageUtil(getActivity(),getActivity()).openAlbum();
                    return true;
                }
            });

            mAbout.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (clickListener != null){
                        //传参
                        clickListener.onClick(3,null);
                    }
                    return true;
                }
            });

            mJoin.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {

                    Intent intent = new Intent();
                    intent.setData(Uri.parse("mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26k%3D" + "a-pWwOHzOhvaQQeYtr9oPbYxuIF7VTT9"));
                    // 此Flag可根据具体产品需要自定义，如设置，则在加群界面按返回，返回手Q主界面，不设置，按返回会返回到呼起产品界面    //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try {
                        startActivity(intent);
                    } catch (Exception e) {

                        ClipboardManager cmb = (ClipboardManager)getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                        assert cmb != null;
                        cmb.setPrimaryClip(ClipData.newPlainText(null,"677026563"));

                        // 未安装手Q或安装的版本不支持
                        Toast.makeText(getActivity(), "未安装QQ或安装的版本不支持,群号已复制到剪贴板", Toast.LENGTH_SHORT).show();
                    }

                    return true;
                }
            });

        }

        @SuppressLint("HandlerLeak")
        private Handler handlers = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what){
                    case 1:
                        if (clickListener != null){
                            //传参
                            clickListener.onClick(1,bitmaps);
                        }
                        break;
                    case 2:
                        if (clickListener != null){
                            //传参
                            clickListener.onClick(2,null);
                        }
                        break;
                    default:
                        break;
                }
            }
        };

        public interface OnItemClickListener{
            void onClick(int msg,Bitmap bitmap);
        }

        private OnItemClickListener clickListener;

        public void setClickListener(OnItemClickListener clickListener){
            this.clickListener = clickListener;
        }

    }
}
