package com.example.capstonandroid.model

import android.graphics.Bitmap
import android.util.Log
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlin.collections.ArrayList

class Datas {
    companion object{
        var instance : Datas = Datas()
    }

    var isComputing : Boolean = false

    // input관련
    lateinit var inputBitmap : Bitmap
    var bitmapArr : ArrayList<Bitmap> = ArrayList()

    // output 관련
    var resultText : String = ""
    var densityBitmaps : ArrayList<Bitmap> = ArrayList()

    //debug 관련
    var debugEvent : DebugEvent = DebugEvent.outputDensitymap
    lateinit var debugBitmap : Bitmap
    var listFordbug = listOf<DebugEvent>(DebugEvent.InputR,DebugEvent.InputG,DebugEvent.InputB,DebugEvent.outputDensitymap)

    // subjects
    var bitmapSubject : PublishSubject<Bitmap?> = PublishSubject.create()
    var debugEventSubject : PublishSubject<DebugEvent> = PublishSubject.create()
    var designEventSubject : PublishSubject<DesignEvent> = PublishSubject.create()

    fun bindSubjects(){
        bitmapSubject
            .map { it!! }
            .filter { !isComputing }
            .subscribe(
                {
                    // 마지막 input으로 업데이트 될 것임
                    inputBitmap = it!!
                },
                { Log.e("bitmapSubjectErr",it.message.toString()) }
            )

        debugEventSubject
            .subscribeBy(
                onNext = { debugEvent = it },
                onError = {Log.e("deBugEventSubjectErr",it.message.toString())}
            )
    }
}

enum class DebugEvent{
    InputR, InputG, InputB, outputDensitymap
}
enum class DesignEvent{
    DebugLoadStart, DebugLoadEnd
}