package com.example.capstonandroid.model

import android.graphics.Bitmap
import io.reactivex.rxjava3.subjects.PublishSubject

class Datas {
    companion object{
        var bitmapSubject : PublishSubject<Bitmap> = PublishSubject.create()
        var isComputing : Boolean = false
        // 글자 변환은 mainThread에서 처리하기때문에 따로 선언
        var textSubject : PublishSubject<String> = PublishSubject.create()
    }
}