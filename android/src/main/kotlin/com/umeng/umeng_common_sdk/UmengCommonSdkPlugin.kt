package com.umeng.umeng_common_sdk

import android.content.Context
import android.util.Log
import androidx.annotation.NonNull
import com.umeng.analytics.MobclickAgent
import com.umeng.commonsdk.UMConfigure
import com.umeng.commonsdk.debug.UMLog
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import kotlin.text.get

/** UmengCommonSdkPlugin */
class UmengCommonSdkPlugin : FlutterPlugin, MethodCallHandler {

    private lateinit var channel: MethodChannel
    private var context: Context? = null

    companion object {
        private const val TAG = "UMLog"
        private const val CHANNEL_NAME = "umeng_common_sdk"
        private var versionMatch = false

        private fun checkSDKVersion() {
            try {
                val agent = Class.forName("com.umeng.analytics.MobclickAgent")
                val methods = agent.declaredMethods

                versionMatch = methods.any { it.name == "onEventObject" }

                if (!versionMatch) {
                    //Log.e(TAG, "安卓SDK版本过低，建议升级至8以上")
                    UMLog.getLogger(0).log(TAG, "安卓SDK版本过低，建议升级至8以上")
                } else {
                    Log.e(TAG, "安卓依赖版本检查成功")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(TAG, "SDK版本过低，请升级至8以上: ${e.message}")
            }
        }

        private fun setWrapperType() {
            try {
                val config = Class.forName("com.umeng.commonsdk.UMConfigure")
                val method = config.getDeclaredMethod("setWraperType", String::class.java, String::class.java)
                method.isAccessible = true
                method.invoke(null, "flutter", "1.0")
                Log.i(TAG, "setWraperType:flutter1.0 success")
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(TAG, "setWraperType:flutter1.0 ${e.message}")
            }
        }

        private fun onAttachedEngineAdd() {
            checkSDKVersion()
            setWrapperType()
        }
    }

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        context = flutterPluginBinding.applicationContext
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, CHANNEL_NAME)
        channel.setMethodCallHandler(this)
        UMConfigure.setLogEnabled(true)
        onAttachedEngineAdd()
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        if (!versionMatch) {
            Log.e(TAG, "onMethodCall:${call.method}:安卓SDK版本过低，请升级至8以上")
            // 根据需求决定是否继续执行
        }

        try {
            when (call.method) {
                "getPlatformVersion" -> {
                    result.success("Android ${android.os.Build.VERSION.RELEASE}")
                }
                "preInit" -> {
                    val args = call.arguments as? Map<*, *>
                    args?.let { preInit(it) }
                    result.success(null)
                }
                "initCommon" -> {
                    val args = call.arguments as? List<*>
                    args?.let { initCommon(it) }
                    result.success(null)
                }
                "onEvent" -> {
                    val args = call.arguments as? List<*>
                    args?.let { onEvent(it) }
                    result.success(null)
                }
                "onProfileSignIn" -> {
                    val args = call.arguments as? List<*>
                    args?.let { onProfileSignIn(it) }
                    result.success(null)
                }
                "onProfileSignOff" -> {
                    onProfileSignOff()
                    result.success(null)
                }
                "setPageCollectionModeAuto" -> {
                    setPageCollectionModeAuto()
                    result.success(null)
                }
                "setPageCollectionModeManual" -> {
                    setPageCollectionModeManual()
                    result.success(null)
                }
                "onPageStart" -> {
                    val args = call.arguments as? List<*>
                    args?.let { onPageStart(it) }
                    result.success(null)
                }
                "onPageEnd" -> {
                    val args = call.arguments as? List<*>
                    args?.let { onPageEnd(it) }
                    result.success(null)
                }
                "reportError" -> {
                    val args = call.arguments as? List<*>
                    args?.let { reportError(it) }
                    result.success(null)
                }
                else -> {
                    result.notImplemented()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "Exception: ${e.message}")
            result.error("UMENG_ERROR", e.message, null)
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        context = null
    }

    private fun preInit(args: Map<*, *>) {
        context?.let {
            val appKey = args["appKey"] as String
            val channel = args["channel"] as String
            UMConfigure.preInit(it, appKey, channel)
            UMConfigure.submitPolicyGrantResult(context, true)
            Log.i(TAG, "preInit: appKey:$appKey; channel:${channel}")
        }
    }

    private fun initCommon(args: List<*>) {
        val appkey = args[0] as? String ?: return
        val channel = args.getOrNull(2) as? String ?: ""
        val pushSecret = args.getOrNull(3) as? String

        context?.let {
            UMConfigure.init(it, appkey, channel, UMConfigure.DEVICE_TYPE_PHONE, pushSecret)
        }
    }

    private fun onEvent(args: List<*>) {
        val event = args[0] as? String ?: return
        val map = args.getOrNull(1) as? Map<String, Any>

        context?.let {
            MobclickAgent.onEventObject(it, event, map)
        }
    }

    private fun onProfileSignIn(args: List<*>) {
        val userID = args[0] as? String ?: return
        MobclickAgent.onProfileSignIn(userID)
        Log.i(TAG, "onProfileSignIn:$userID")
    }

    private fun onProfileSignOff() {
        MobclickAgent.onProfileSignOff()
        Log.i(TAG, "onProfileSignOff")
    }

    private fun setPageCollectionModeAuto() {
        MobclickAgent.setPageCollectionMode(MobclickAgent.PageMode.AUTO)
        Log.i(TAG, "setPageCollectionModeAuto")
    }

    private fun setPageCollectionModeManual() {
        MobclickAgent.setPageCollectionMode(MobclickAgent.PageMode.MANUAL)
        Log.i(TAG, "setPageCollectionModeManual")
    }

    private fun onPageStart(args: List<*>) {
        val event = args[0] as? String ?: return
        MobclickAgent.onPageStart(event)
        Log.i(TAG, "onPageStart:$event")
    }

    private fun onPageEnd(args: List<*>) {
        val event = args[0] as? String ?: return
        MobclickAgent.onPageEnd(event)
        Log.i(TAG, "onPageEnd:$event")
    }

    private fun reportError(args: List<*>) {
        val error = args[0] as? String ?: return
        context?.let {
            MobclickAgent.reportError(it, error)
            Log.i(TAG, "reportError:$error")
        }
    }
}