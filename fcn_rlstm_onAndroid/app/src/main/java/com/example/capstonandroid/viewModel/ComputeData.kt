package com.example.capstonandroid.viewModel

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.TextView
import com.example.capstonandroid.model.Datas.Companion.instance
import com.example.capstonandroid.model.DebugEvent
import com.example.capstonandroid.model.DesignEvent
import com.jakewharton.rxbinding4.widget.textChangeEvents
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_main.*
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.Module
import org.pytorch.Tensor
import org.pytorch.torchvision.TensorImageUtils
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.util.concurrent.TimeUnit

class ComputeData(context: Context) {

    var batch: Int = 5
    var activity_main: Activity = context as Activity
    var Utils: UtilsForPytorch = UtilsForPytorch()
    var module: Module =
        LiteModuleLoader.load(Utils.assetFilePath(context, "fcn_rlstm2_android.ptl"))
    var changeUI: PublishSubject<Boolean> = PublishSubject.create()


    fun bindRx() {
        instance.bitmapSubject
            .map { it!! }
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.computation())
            .filter { !instance.isComputing }
            .delay(1000L, TimeUnit.NANOSECONDS)
            .filter { !instance.isComputing }
            .subscribeBy(
                onNext = {
                    if (instance.bitmapArr.size < batch) {
                        var bitmap: Bitmap = Bitmap.createScaledBitmap(it, 160, 120, true)
//                        bitmap = Bitmap.createScaledBitmap(bitmap, 640, 480, true)
                        instance.bitmapArr.add(bitmap)
                        Log.e("subject", "비트맵 추가 ${instance.bitmapArr.size}개째")
                    }
                    if (instance.bitmapArr.size >= batch) {
                        instance.isComputing = true
                        runModel()
                        instance.designEventSubject.onNext(DesignEvent.DebugLoadEnd)
                        changeUI.onNext(true)
                        Log.e("subject", "계산 완료")
                    }
                },
                onComplete = {},
                onError = { Log.e("에러_subject", it.message.toString()) }
            )

        changeUI
            .subscribeOn(Schedulers.io()) // 데이터 흐름을 발생시키는데 (내려가며 읽는 동작) 영향
            .observeOn(AndroidSchedulers.mainThread()) // observer에게 데이터를 보낼 때 영향
            .subscribeBy(
                onNext = {
                    activity_main.resultTextView.text = instance.resultText
                    activity_main.imageDebug.setImageBitmap(instance.debugBitmap)
                    instance.isComputing = false
                    Log.e("UI", "UI 적용")
                },
                onComplete = {},
                onError = {
                    Log.e("changeText 에러", it.message.toString())
                }
            )
    }

    fun runModel() {

        var shape: LongArray = LongArray(5).also {
            it[0] = batch.toLong()
            it[1] = 1L
            it[2] = 3L
            it[3] = 120L
            it[4] = 160L
        }
        var floatArr = Utils.bitmapToRGBArr(instance.bitmapArr)
        var inputTensor: Tensor = Tensor.fromBlob(floatArr, shape)
//        Log.e("shape", inputTensor.shape().contentToString())
//        inputTensor = outputTest()

        // running the model
        var output = module.forward(IValue.from(inputTensor)).toTuple()
//         output[0].toTensor().dataAsFloatArray -> densitymap
//        output[1].toTensor().dataAsFloatArray -> result

        //결과 적용
        instance.resultText = output[1].toTensor().dataAsFloatArray[batch - 1].toString()
        instance.bitmapArr.clear()

        //디버그 적용
        var tmpsize = 120 * 160
        var indexStartLastbatch = (tmpsize * 3) * (batch - 1)
        var bitmap: Bitmap = instance.inputBitmap
        when (instance.debugEvent) {
            DebugEvent.InputR -> {
                bitmap = Utils.floatArrayToBitmap(
                    inputTensor.dataAsFloatArray.sliceArray(0 until tmpsize),
                    160,
                    120
                )
            }
            DebugEvent.InputG -> {
                bitmap = Utils.floatArrayToBitmap(
                    inputTensor.dataAsFloatArray.sliceArray(tmpsize until tmpsize * 2),
                    160,
                    120
                )
            }
            DebugEvent.InputB -> {
                bitmap = Utils.floatArrayToBitmap(
                    inputTensor.dataAsFloatArray.sliceArray(tmpsize * 2 until tmpsize * 3),
                    160,
                    120
                )
            }
            DebugEvent.outputDensitymap -> {
                var tmparr =
                    output[0].toTensor().dataAsFloatArray.sliceArray(tmpsize * (batch - 1) until tmpsize * (batch))
//                    for(i in 0 until tmparr.size){
//                        tmparr[i] = (tmparr[i].toBits() shl 8).toFloat()
//                    }
                bitmap = Utils.floatArrayToBitmap(
                    tmparr,
                    160,
                    120
                )
            }
            else -> {}
        }
        Log.e("debugSelect", instance.debugEvent.toString())
        instance.debugBitmap = Bitmap.createScaledBitmap(bitmap, 600, 450, true)
    }

//    fun inputTest() : Tensor{
//        var arr : FloatArray = FloatArray(120*160*3*batch)
//        var list = ArrayList<FloatArray>()
//        for( bitmap in instance.bitmapArr){
//            var bmp = Bitmap.createScaledBitmap(bitmap,160,120,true)
//            list.add(TensorImageUtils.bitmapToFloat32Tensor(bmp,0,0,160,120,
//                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB)
//                .dataAsFloatArray)
//        }
//        for(x in 0 until batch) {
//            var offset = 120 * 160 * 3 * x
//            for (i in 0 until 120 * 160 * 3) {
//                arr[offset+i] = list[x][i]
//            }
//        }
//
//        var shape : LongArray = LongArray(5).also {
//            it[0] = batch.toLong()
//            it[1] = 1L
//            it[2] = 3L
//            it[3] = 120L
//            it[4] = 160L
//        }
//        return Tensor.fromBlob(arr,shape)
//    }
}