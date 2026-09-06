package com.openlauncher.app.debug.embedding

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.openlauncher.app.feature.navigation.external.EmbeddedNavigationView
import com.openlauncher.app.feature.navigation.external.EmbeddingHostState
import com.openlauncher.app.launcher.LauncherApp

/** Activity-scoped ActivityView host. Provider/task lifecycle lives in NavigationEmbeddingEngine. */
internal class ActivityViewNavigationWidget(context: Context) :
    FrameLayout(context),
    EmbeddedNavigationView,
    NavigationEmbeddingHostBridge {
    private val capabilities = EmbeddingCapabilities.detect(context)
    private val activityView = ActivityViewCompat(context, capabilities)
    private val engine = NavigationEmbeddingEngine.get(context.applicationContext)
    private val debugInput = NavigationDebugInput(this, activityView)

    private var stateListener: (EmbeddingHostState) -> Unit = {}
    private var readyPollCount = 0
    private var permanentlyReleased = false
    private var localHostFailure = false

    override val supportsInteractiveEmbedding: Boolean
        get() = capabilities.canHostExternalActivity

    override val supportsEmbeddedBack: Boolean
        get() = capabilities.canHostExternalActivity

    override val navigationDisplayId: Int?
        get() = activityView.virtualDisplayId

    init {
        addView(
            activityView,
            LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        engine.attachHost(this)
        attachActivityView()
    }

    override fun bind(
        app: LauncherApp?,
        onStateChanged: (EmbeddingHostState) -> Unit,
    ) {
        // Provider selection is observed by the process engine from SettingsRepository. Compose
        // only supplies the latest UI listener; it no longer drives task lifecycle from update().
        stateListener = onStateChanged
        stateListener(engine.currentState)
    }

    override fun ensureRunning() = Unit

    override fun performBackPress(): Boolean = engine.performBack()

    override fun stop(): Boolean = engine.stopSelected()

    override fun restart(): Boolean {
        if (engine.restartSelected()) return true
        if (!localHostFailure || permanentlyReleased) return false
        localHostFailure = false
        activityView.release()
        attachActivityView()
        return true
    }

    override fun release() {
        // AndroidView.onRelease only means the Compose holder was disposed. The Activity-scoped
        // ActivityView stays alive until the Activity itself is destroyed.
        stateListener = {}
    }

    override fun startExternalActivity(intent: Intent): Result<Unit> =
        activityView.startActivity(intent, external = true)

    override fun performEmbeddedBack(): Result<Unit> = activityView.performBackPress()

    override fun onEngineStateChanged(state: EmbeddingHostState) {
        stateListener(state)
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) engine.resumeStopped()
        return false
    }

    private fun attachActivityView() {
        activityView.attach().fold(
            onSuccess = {
                localHostFailure = false
                readyPollCount = 0
                waitForReady()
            },
            onFailure = { error ->
                localHostFailure = true
                onEngineStateChanged(
                    EmbeddingHostState.Failed(
                        "ActivityView unavailable: ${error.message ?: error.javaClass.simpleName}",
                    ),
                )
            },
        )
    }

    private fun waitForReady() {
        if (permanentlyReleased) return
        if (activityView.virtualDisplayId != null) {
            localHostFailure = false
            engine.onHostReady(this)
            return
        }
        if (readyPollCount++ >= MaxReadyPolls) {
            localHostFailure = true
            onEngineStateChanged(EmbeddingHostState.Failed("ActivityView did not become ready"))
            return
        }
        postDelayed(::waitForReady, ReadyPollMillis)
    }

    private fun releasePermanently() {
        if (permanentlyReleased) return
        permanentlyReleased = true
        engine.destroyHost(this) {
            activityView.release()
            stateListener(EmbeddingHostState.Idle)
            stateListener = {}
            engine.onHostReleased()
        }
    }

    private fun sendDebugGesture(kind: String, xFraction: Float, yFraction: Float): Boolean =
        debugInput.gesture(kind, xFraction, yFraction)

    private fun sendDebugType(text: String): Boolean = debugInput.type(text)

    companion object {
        private const val Tag = "OpenLauncherMapWidget"
        private const val ReadyPollMillis = 100L
        private const val MaxReadyPolls = 50

        private var retainedActivity: Activity? = null
        private var retainedHost: ActivityViewNavigationWidget? = null
        private var lifecycleCallbacksRegistered = false

        @JvmStatic
        fun isSupported(context: Context): Boolean =
            EmbeddingCapabilities.detect(context).canHostExternalActivity

        @JvmStatic
        fun acquire(context: Context): View {
            val activity = context.findActivity()
            val current = retainedHost
            val host = if (
                current != null &&
                !current.permanentlyReleased &&
                (activity == null || retainedActivity == null || retainedActivity === activity)
            ) {
                current
            } else {
                current?.releasePermanently()
                ActivityViewNavigationWidget(activity ?: context).also {
                    retainedActivity = activity
                    retainedHost = it
                }
            }
            if (activity != null) {
                retainedActivity = activity
                registerLifecycleCallbacks(activity.application)
            }
            (host.parent as? ViewGroup)?.removeView(host)
            return host
        }

        @JvmStatic
        fun debugGesture(kind: String, xFraction: Float = 0.5f, yFraction: Float = 0.5f): Boolean =
            retainedHost?.sendDebugGesture(kind, xFraction, yFraction) == true

        @JvmStatic
        fun debugType(text: String): Boolean = retainedHost?.sendDebugType(text) == true

        @JvmStatic
        fun debugBack(): Boolean = retainedHost?.performBackPress() == true

        private fun registerLifecycleCallbacks(application: Application) {
            if (lifecycleCallbacksRegistered) return
            lifecycleCallbacksRegistered = true
            application.registerActivityLifecycleCallbacks(
                object : Application.ActivityLifecycleCallbacks {
                    override fun onActivityDestroyed(activity: Activity) {
                        if (activity !== retainedActivity) return
                        val host = retainedHost
                        retainedHost = null
                        retainedActivity = null
                        host?.releasePermanently()
                    }

                    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
                    override fun onActivityStarted(activity: Activity) = Unit
                    override fun onActivityResumed(activity: Activity) = Unit
                    override fun onActivityPaused(activity: Activity) = Unit
                    override fun onActivityStopped(activity: Activity) = Unit
                    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
                },
            )
            Log.i(Tag, "registered Activity-scoped ActivityView lifecycle")
        }

        private tailrec fun Context.findActivity(): Activity? = when (this) {
            is Activity -> this
            is ContextWrapper -> baseContext.findActivity()
            else -> null
        }
    }
}
