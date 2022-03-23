package com.example.capstonandroid.viewModel

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.capstonandroid.model.Datas.Companion.instance
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_main.*
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.Module
import org.pytorch.Tensor
import java.util.concurrent.TimeUnit

class ComputeData(context : Context) {

    var batch : Int = 5
    var activity_main : Activity = context as Activity
    var Utils : UtilsForPytorch = UtilsForPytorch()
    var module : Module = LiteModuleLoader.load( Utils.assetFilePath(context, "fcn_rlstm2_android.ptl"))
    var changeText : PublishSubject<Boolean> = PublishSubject.create()

    /** for inputTensor Test*/
    lateinit var bitmapR : Bitmap
    lateinit var bitmapG : Bitmap
    lateinit var bitmapB : Bitmap

    fun bindRx(){
        instance.bitmapSubject
            .map { it!! }
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.computation())
            .filter { !instance.isComputing }
            .subscribeBy(
                onNext = {
                    if( instance.bitmapArr.size < batch) {
                        var bitmap: Bitmap = it
                        bitmap = Bitmap.createScaledBitmap(bitmap, 120, 160, true)
//                        bitmap = Bitmap.createScaledBitmap(bitmap, 640, 480, true)
                        instance.bitmapArr.add(bitmap)
                        Log.e("subject","비트맵 추가 ${instance.bitmapArr.size}개째")
                    }
                    if(instance.bitmapArr.size >= batch) {
//                        var Farr =  Utils.bitmapToRGBArr(instance.bitmapArr)
                        instance.resultText = getModelResult()
                        changeText.onNext(true)
                        Log.e("subject","계산 완료")
                    }
                },
                onComplete = {},
                onError = { Log.e("에러_subject",it.message.toString())}
            )

        changeText
            .delay(5000L, TimeUnit.NANOSECONDS)
            .subscribeOn(Schedulers.io()) // 데이터 흐름을 발생시키는데 (내려가며 읽는 동작) 영향
            .observeOn(AndroidSchedulers.mainThread()) // observer에게 데이터를 보낼 때 영향
            .subscribeBy (
                onNext = {
                    instance.isComputing = false
//                    activity_main.testR.setImageBitmap(bitmapR)
//                    activity_main.testG.setImageBitmap(bitmapG)
//                    activity_main.testB.setImageBitmap(bitmapB)
                    activity_main.image_density1.setImageBitmap(instance.densityBitmaps[0])
                    activity_main.image_density2.setImageBitmap(instance.densityBitmaps[1])
                    activity_main.image_density3.setImageBitmap(instance.densityBitmaps[2])
                    activity_main.image_density4.setImageBitmap(instance.densityBitmaps[3])
                    activity_main.image_density5.setImageBitmap(instance.densityBitmaps[4])
                    activity_main.resultTextView.text = instance.resultText
                    Log.e("UI","UI 적용")
                },
                onComplete = {},
                onError = {
                    Log.e("changeText 에러", it.message.toString())
                }
            )
    }

    fun getModelResult() : String{

        var shape : LongArray = LongArray(5).also {
            it[0] = batch.toLong()
            it[1] = 1L
            it[2] = 3L
            it[3] = 120L
            it[4] = 160L
        }
        var floatArr = Utils.bitmapToRGBArr(instance.bitmapArr)
        var inputTensor : Tensor = Tensor.fromBlob(floatArr, shape)
//        Log.e("shape", inputTensor.shape().contentToString())

        // running the model
        var output = module.forward(IValue.from(inputTensor)).toTuple()
//         output[0].toTensor().dataAsFloatArray -> densitymap
//        output[1].toTensor().dataAsFloatArray -> result

        instance.bitmapArr.clear()

        /** For densitymap test*/
        var tmpsize = 120*160
        instance.densityBitmaps.clear()
        var densityArr = output[0].toTensor().dataAsFloatArray
        for(i in 0 until batch){
            var tmparr : FloatArray = FloatArray(tmpsize)
            for(x in 0 until tmpsize){
                tmparr[x] = densityArr[tmpsize*i + x]
            }
            var bmp = Utils.floatArrayToBitmap(tmparr,120,160)
            instance.densityBitmaps.add( Bitmap.createScaledBitmap(bmp,450,600,true) )
        }
        /** For inputTensor test*/
//        var inputarr = inputTensor.dataAsFloatArray
//        var tmpR : FloatArray = FloatArray(tmpsize)
//        var tmpG : FloatArray = FloatArray(tmpsize)
//        var tmpB : FloatArray = FloatArray(tmpsize)
//        for(x in 0 until tmpsize){
//            tmpR[x] = inputarr[x]
//        }
//        bitmapR = Utils.floatArrayToBitmap(tmpR,120,160)
//        bitmapR = Bitmap.createScaledBitmap(bitmapR,450,600,true)
//        for(x in 0 until tmpsize){
//            tmpG[x] = inputarr[tmpsize+x]
//        }
//        bitmapG = Utils.floatArrayToBitmap(tmpG,120,160)
//        bitmapG = Bitmap.createScaledBitmap(bitmapG,450,600,true)
//        for(x in 0 until tmpsize){
//            tmpB[x] = inputarr[tmpsize*2+x]
//        }
//        bitmapB = Utils.floatArrayToBitmap(tmpB,120,160)
//        bitmapB = Bitmap.createScaledBitmap(bitmapB,450,600,true)

        /** Test End*/

        return output[1].toTensor().dataAsFloatArray[batch-1].toString()
    }
}