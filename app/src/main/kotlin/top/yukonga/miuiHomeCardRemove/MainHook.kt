package top.yukonga.miuiHomeCardRemove

import android.animation.ObjectAnimator
import android.animation.TimeInterpolator
import android.view.View
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createBeforeHook
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class MainHook : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        EzXHelper.initHandleLoadPackage(lpparam)
        EzXHelper.setLogTag("MiuiHomeCardRemove")
        when (lpparam.packageName) {
            "com.miui.home" -> {
                val swipeHelperForRecentsClass = loadClass("com.miui.home.recents.views.SwipeHelperForRecents")
                val taskStackViewLayoutStyleHorizontalClass = loadClass("com.miui.home.recents.TaskStackViewLayoutStyleHorizontal")
                val deviceConfigClass = loadClass("com.miui.home.launcher.DeviceConfig")
                val physicBasedInterpolatorClass = loadClass("com.miui.home.launcher.anim.PhysicBasedInterpolator")
                val verticalSwipe = loadClass("com.miui.home.recents.views.VerticalSwipe")
                swipeHelperForRecentsClass.methodFinder().filterByName("onTouchEvent").first().createAfterHook {
                    val mCurrView = it.thisObject.objectHelper().getObjectOrNullUntilSuperclassAs<View>("mCurrView")
                    if (mCurrView != null) {
                        mCurrView.alpha = 1f
                        mCurrView.scaleX = 1f
                        mCurrView.scaleY = 1f
                    }
                }
                taskStackViewLayoutStyleHorizontalClass.methodFinder().filterByName("createScaleDismissAnimation").first().createBeforeHook {
                    val view = it.args[0] as View
                    val getScreenHeight = XposedHelpers.callStaticMethod(deviceConfigClass, "getScreenHeight") as Int
                    val ofFloat = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, view.translationY, -getScreenHeight * 1.15f)
                    val physicBasedInterpolator = XposedHelpers.newInstance(physicBasedInterpolatorClass, 0.72f, 0.72f) as TimeInterpolator
                    ofFloat.interpolator = physicBasedInterpolator
                    ofFloat.duration = 450L
                    it.result = ofFloat
                }
                verticalSwipe.methodFinder().filterByName("calculate").first().createAfterHook {
                    val f = it.args[0] as Float
                    val asScreenHeightWhenDismiss = XposedHelpers.callStaticMethod(verticalSwipe, "getAsScreenHeightWhenDismiss") as Int
                    val f2 = f / asScreenHeightWhenDismiss
                    val mTaskViewHeight = it.thisObject.objectHelper().getObjectOrNullAs<Float>("mTaskViewHeight")
                    val mCurScale = it.thisObject.objectHelper().getObjectOrNullAs<Float>("mCurScale")
                    val f3: Float = mTaskViewHeight!! * mCurScale!!
                    val i = if (f2 > 0.0f) 1 else if (f2 == 0.0f) 0 else -1
                    val afterFrictionValue = XposedHelpers.callMethod(it.thisObject, "afterFrictionValue", f, asScreenHeightWhenDismiss) as Float
                    if (i < 0) it.thisObject.objectHelper().setObject("mCurTransY", (mTaskViewHeight / 2f + afterFrictionValue * 2f) - (f3 / 2f))
                }
            }

            else -> return
        }
    }
}