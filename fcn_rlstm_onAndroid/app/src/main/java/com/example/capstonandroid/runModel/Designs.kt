package com.example.capstonandroid.runModel

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.capstonandroid.Data.Datas
import com.example.capstonandroid.Data.DesignEvent
import com.github.florent37.fiftyshadesof.FiftyShadesOf
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_main.*

class Designs(mcontext: Context) {
    var activity_main: Activity = mcontext as Activity
    var context = mcontext
    lateinit var debugImageLoading: FiftyShadesOf

    fun setDesigns() {
        bindDebugLoading()
    }

    private fun bindDebugLoading() {
        debugImageLoading = FiftyShadesOf.with(context).on(activity_main.imageDebug).fadein(false)

        Datas.instance.designEventSubject
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = {
                    when (it) {
                        DesignEvent.DebugLoadStart -> {
                            debugImageLoading.start()
                        }
                        DesignEvent.DebugLoadEnd -> {
                            debugImageLoading.stop()
                        }
                    }
                },
                onError = { Log.e("DesignsErr", "Desigins의 bindDebugLoading에서 에러") }
            )
    }
}