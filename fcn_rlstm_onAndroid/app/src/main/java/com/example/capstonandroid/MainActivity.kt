package com.example.capstonandroid

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.capstonandroid.Data.Datas
import com.example.capstonandroid.settingViews.BindViews
import com.example.capstonandroid.runModel.ComputeData
import com.example.capstonandroid.runModel.Designs

/*
로딩 화면에서 권한 확인하도록 변경해야함
* */

class MainActivity : AppCompatActivity() {

    lateinit var computeData: ComputeData
    lateinit var bindViews: BindViews
    lateinit var designs: Designs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //datas instance생성
        Datas()
        Datas.instance.bindSubjects()

        computeData = ComputeData(this)
        computeData.bindRx()

        designs = Designs(this)
        designs.setDesigns()

        bindViews = BindViews(this)
        bindViews.bindViews()
    }

}