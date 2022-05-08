package com.example.capstonandroid.runModel

import android.content.Context
import android.graphics.Bitmap
import java.io.*
import java.nio.ByteBuffer

class UtilsForPytorch {

    fun floatArrayToBitmap(floatArray: FloatArray, width : Int, height : Int) : Bitmap{

        var alpha : Byte = 255.toByte()

        var bitmap : Bitmap = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888)

        var byteBuffer : ByteBuffer = ByteBuffer.allocate(width*height*4*3)

        var Maximum : Float = floatArray.maxOrNull()!!
        var minmum : Float = floatArray.minOrNull()!!
        var delta : Float = Maximum - minmum

        var i : Int = 0
        for( value in floatArray){
            var temValue : Byte = ((((value-minmum)/delta)*255)).toInt().toByte()
            byteBuffer.put(4*i, temValue)
            byteBuffer.put(4*i+1, temValue)
            byteBuffer.put(4*i+2, temValue)
            byteBuffer.put(4*i+3, alpha)
            i++
        }
        bitmap.copyPixelsFromBuffer(byteBuffer)
        return bitmap
    }

    fun bitmapToRGBArr(bitmapArray : ArrayList<Bitmap>) : FloatArray{
        var batchsize = bitmapArray.size
        var width = bitmapArray[0].width
        var height = bitmapArray[0].height
        var bitmapSize = height*width
        var arr : FloatArray = FloatArray(height * width * 3 * batchsize) {0f}

        // 한 행씩 읽는다.
        for( batch in 0 until batchsize) {
            for (j in 0 until height) {
                for (i in 0 until width) {
                    var p = bitmapArray[batch].getPixel(i, j)

                    var R = (p and 0xff0000) shr 16
                    var G = (p and 0x00ff00) shr 8
                    var B = (p and 0x0000ff) shr 0

                    var index = j * width + i
                    arr[index] = R.toFloat()
                    arr[bitmapSize+index] = G.toFloat()
                    arr[(bitmapSize*2)+index] = B.toFloat()
                }
            }
        }

        // 열을 기준으로 읽기
//        for( batch in 0 until batchsize) {
//            for (i in 0 until width) {
//                for (j in 0 until height) {
//                    var p = bitmapArray[batch].getPixel(i, j)
//
//                    var R = (p and 0xff0000) shr 16
//                    var G = (p and 0x00ff00) shr 8
//                    var B = (p and 0x0000ff) shr 0
//
//                    var index = i*height + j
//                    arr[index] = R.toFloat()
//                    arr[bitmapSize+index] = G.toFloat()
//                    arr[(bitmapSize*2)+index] = B.toFloat()
//                }
//            }
//        }

        return arr
    }

    fun assetFilePath(context: Context, assetName: String): String {
        var file: File = File(context.filesDir, assetName)
        if (file.exists() && file.length() > 0) {
            return file.absolutePath
        }
        var iStream: InputStream = context.assets.open(assetName)
        var oStream: OutputStream = FileOutputStream(file)

        var buffer: ByteArray = ByteArray(4 * 1024)
        var read: Int
        while (true) {
            read = iStream.read(buffer)
            if (read == -1) break
            oStream.write(buffer, 0, read)
        }
        oStream.flush()

        return file.absolutePath
    }
}